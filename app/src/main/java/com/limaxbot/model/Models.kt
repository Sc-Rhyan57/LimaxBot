package com.limaxbot.model

data class BotSettings(
    val mediaPrefix: String = "!salvar",
    val notifyOnDownload: Boolean = true,
    val antiDelete: Boolean = true,
    val phoneNumber: String = ""
)

data class MediaEntry(
    val id: Long = 0L,
    val filename: String = "",
    val filepath: String = "",
    val type: String = "image",
    val from: String = "",
    val sender: String = "",
    val isGroup: Boolean = false,
    val size: Long = 0L,
    val downloadedAt: Long = 0L,
    val preview: String? = null
)

data class DeletedEntry(
    val id: String = "",
    val from: String = "",
    val participant: String? = null,
    val deletedAt: Long = 0L,
    val isGroup: Boolean = false,
    val content: String? = null
)

data class ContactInfo(
    val number: String = "",
    val jid: String = "",
    val status: String? = null,
    val profilePic: String? = null,
    val fetchedAt: Long = 0L
)

enum class BotStatus { IDLE, CONNECTING, CONNECTED, ERROR }

data class BotState(
    val status: BotStatus = BotStatus.IDLE,
    val settings: BotSettings = BotSettings(),
    val pairingCode: String? = null,
    val mediaList: List<MediaEntry> = emptyList(),
    val deletedList: List<DeletedEntry> = emptyList(),
    val contactResult: ContactInfo? = null,
    val error: String? = null,
    val connectedAt: Long? = null,
    val nodeReady: Boolean = false,
    val logLines: List<String> = emptyList()
)
