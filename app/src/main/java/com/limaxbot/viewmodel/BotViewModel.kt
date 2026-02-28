package com.limaxbot.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.limaxbot.model.*
import com.limaxbot.service.AppLogger
import com.limaxbot.service.BotNotificationService
import com.limaxbot.service.NodeBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("limax_prefs")

class BotViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val gson = Gson()

    private val _state = MutableStateFlow(BotState())
    val state: StateFlow<BotState> = _state.asStateFlow()

    private val KEY_PHONE = stringPreferencesKey("phone")
    private val KEY_PREFIX = stringPreferencesKey("prefix")
    private val KEY_NOTIFY = booleanPreferencesKey("notify")
    private val KEY_ANTI = booleanPreferencesKey("anti_delete")

    init {
        viewModelScope.launch { loadPrefs() }
        NodeBridge.init(ctx)
        observeNode()
        observeNodeReady()
        observeLogs()
    }

    private suspend fun loadPrefs() {
        val p = ctx.dataStore.data.first()
        _state.update {
            it.copy(settings = BotSettings(
                phoneNumber = p[KEY_PHONE] ?: "",
                mediaPrefix = p[KEY_PREFIX] ?: "!salvar",
                notifyOnDownload = p[KEY_NOTIFY] ?: true,
                antiDelete = p[KEY_ANTI] ?: true
            ))
        }
    }

    private fun observeNodeReady() {
        viewModelScope.launch {
            NodeBridge.nodeReady.collect { ready ->
                _state.update { it.copy(nodeReady = ready) }
                if (ready) NodeBridge.send("get_state")
            }
        }
    }

    private fun observeNode() {
        viewModelScope.launch {
            NodeBridge.messages.collect { json -> handleNodeMessage(json) }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            AppLogger.logs.collect { line ->
                _state.update { s ->
                    val updated = (s.logLines + line).takeLast(500)
                    s.copy(logLines = updated)
                }
            }
        }
    }

    private fun handleNodeMessage(json: JsonObject) {
        val type = json["type"]?.asString ?: return
        val data = json["data"]

        when (type) {
            "state" -> {
                val connected = data?.asJsonObject?.get("connected")?.asBoolean ?: false
                _state.update { it.copy(status = if (connected) BotStatus.CONNECTED else BotStatus.IDLE) }
                if (connected) {
                    val ml = gson.fromJson(data?.asJsonObject?.get("media"), Array<MediaEntry>::class.java)?.toList() ?: emptyList()
                    val dl = gson.fromJson(data?.asJsonObject?.get("deleted"), Array<DeletedEntry>::class.java)?.toList() ?: emptyList()
                    _state.update { it.copy(mediaList = ml, deletedList = dl) }
                }
            }
            "pairing_code" -> {
                _state.update { it.copy(pairingCode = data?.asJsonObject?.get("code")?.asString, status = BotStatus.CONNECTING) }
            }
            "connection" -> {
                val status = data?.asJsonObject?.get("status")?.asString
                when (status) {
                    "connected" -> {
                        val at = data?.asJsonObject?.get("connectedAt")?.asLong
                        _state.update { it.copy(status = BotStatus.CONNECTED, pairingCode = null, connectedAt = at) }
                        BotNotificationService.notifyConnected(ctx)
                        BotNotificationService.start(ctx)
                        NodeBridge.send("get_media")
                        NodeBridge.send("get_deleted")
                    }
                    "disconnected" -> {
                        val reason = data?.asJsonObject?.get("reason")?.asString ?: "Desconectado"
                        _state.update { it.copy(status = BotStatus.IDLE, pairingCode = null) }
                        BotNotificationService.notifyDisconnected(ctx, reason)
                        BotNotificationService.stop(ctx)
                    }
                }
            }
            "media_downloaded" -> {
                val entry = gson.fromJson(data, MediaEntry::class.java) ?: return
                _state.update { it.copy(mediaList = listOf(entry) + it.mediaList) }
                if (_state.value.settings.notifyOnDownload) {
                    BotNotificationService.notifyMediaDownloading(ctx, entry.type)
                }
            }
            "download_start" -> {
                val st = data?.asJsonObject?.get("streamType")?.asString ?: "mídia"
                if (_state.value.settings.notifyOnDownload) BotNotificationService.notifyMediaDownloading(ctx, st)
            }
            "message_deleted" -> {
                val entry = gson.fromJson(data, DeletedEntry::class.java) ?: return
                _state.update { it.copy(deletedList = listOf(entry) + it.deletedList) }
            }
            "media_list" -> {
                val list = gson.fromJson(data, Array<MediaEntry>::class.java)?.toList() ?: emptyList()
                _state.update { it.copy(mediaList = list) }
            }
            "deleted_list" -> {
                val list = gson.fromJson(data, Array<DeletedEntry>::class.java)?.toList() ?: emptyList()
                _state.update { it.copy(deletedList = list) }
            }
            "contact_info" -> {
                val info = if (data != null && !data.isJsonNull && data.isJsonObject) gson.fromJson(data, ContactInfo::class.java) else null
                _state.update { it.copy(contactResult = info) }
            }
            "settings_updated" -> {
                val s = gson.fromJson(data, BotSettings::class.java) ?: return
                _state.update { it.copy(settings = s) }
            }
            "error" -> {
                val msg = data?.asJsonObject?.get("message")?.asString ?: data?.asString ?: "Erro"
                _state.update { it.copy(error = msg) }
                viewModelScope.launch { delay(4000); _state.update { it.copy(error = null) } }
            }
        }
    }

    fun connectBot(phone: String) {
        viewModelScope.launch { ctx.dataStore.edit { it[KEY_PHONE] = phone } }
        _state.update { it.copy(status = BotStatus.CONNECTING, error = null) }
        NodeBridge.send("start_bot", mapOf("phoneNumber" to phone))
    }

    fun disconnectBot() {
        NodeBridge.send("disconnect")
        BotNotificationService.stop(ctx)
        _state.update { it.copy(status = BotStatus.IDLE, pairingCode = null) }
    }

    fun saveSettings(settings: BotSettings) {
        viewModelScope.launch {
            ctx.dataStore.edit {
                it[KEY_PREFIX] = settings.mediaPrefix
                it[KEY_NOTIFY] = settings.notifyOnDownload
                it[KEY_ANTI] = settings.antiDelete
            }
        }
        _state.update { it.copy(settings = settings) }
        NodeBridge.send("update_settings", mapOf(
            "mediaPrefix" to settings.mediaPrefix,
            "notifyOnDownload" to settings.notifyOnDownload,
            "antiDelete" to settings.antiDelete
        ))
    }

    fun fetchContact(number: String) {
        _state.update { it.copy(contactResult = null) }
        NodeBridge.send("get_contact_info", mapOf("number" to number))
    }

    fun clearLogs() = _state.update { it.copy(logLines = emptyList()) }
    fun refreshMedia() = NodeBridge.send("get_media")
    fun refreshDeleted() = NodeBridge.send("get_deleted")
    fun clearError() = _state.update { it.copy(error = null) }
}
