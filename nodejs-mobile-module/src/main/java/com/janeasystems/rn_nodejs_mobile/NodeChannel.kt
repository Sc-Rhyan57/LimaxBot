package com.janeasystems.rn_nodejs_mobile

import android.util.Log

class NodeChannel {

    private var listener: ((String) -> Unit)? = null

    fun setMessageListener(l: (String) -> Unit) {
        listener = l
    }

    fun send(message: String) {
        try {
            nativeSendMessage(message)
        } catch (e: Exception) {
            Log.e("NodeChannel", "Erro ao enviar mensagem: ${e.message}")
        }
    }

    // Chamado pelo JNI quando Node.js envia mensagem para o Android
    @Suppress("unused")
    fun receiveFromNode(message: String) {
        listener?.invoke(message)
    }

    private external fun nativeSendMessage(message: String)
}
