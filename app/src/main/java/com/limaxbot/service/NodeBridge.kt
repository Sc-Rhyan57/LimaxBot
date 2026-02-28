package com.limaxbot.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.janeasystems.rn_nodejs_mobile.RNNodeJsMobile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

object NodeBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private var nodejs: RNNodeJsMobile? = null

    private val _messages = MutableSharedFlow<JsonObject>(extraBufferCapacity = 100)
    val messages: SharedFlow<JsonObject> = _messages

    private val _nodeReady = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val nodeReady: SharedFlow<Boolean> = _nodeReady

    fun init(context: Context) {
        try {
            nodejs = RNNodeJsMobile(context)
            nodejs?.channel?.setMessageListener { message ->
                scope.launch {
                    try {
                        val json = gson.fromJson(message, JsonObject::class.java)
                        _messages.emit(json)
                    } catch (e: Exception) {
                        Log.e("NodeBridge", "Parse error: $e")
                    }
                }
            }
            nodejs?.startNodeWithArguments(arrayOf("node", "nodejs-project/src/index.js"))
            scope.launch { _nodeReady.emit(true) }
        } catch (e: Exception) {
            Log.e("NodeBridge", "Failed to start Node.js: $e")
            scope.launch { _nodeReady.emit(false) }
        }
    }

    fun send(action: String, payload: Any? = null) {
        try {
            val msg = if (payload != null) {
                gson.toJson(mapOf("action" to action, "payload" to payload))
            } else {
                gson.toJson(mapOf("action" to action))
            }
            nodejs?.channel?.send(msg)
        } catch (e: Exception) {
            Log.e("NodeBridge", "Send error: $e")
        }
    }

    fun isRunning() = nodejs != null
}
