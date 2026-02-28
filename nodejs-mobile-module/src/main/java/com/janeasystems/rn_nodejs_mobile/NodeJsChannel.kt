package com.janeasystems.rn_nodejs_mobile

interface MessageListener {
    fun onMessage(message: String)
}

class NodeJsChannel {
    private var listener: MessageListener? = null

    fun setMessageListener(l: MessageListener) {
        listener = l
    }

    fun send(message: String) {
        sendToNode(message)
    }

    // Chamado pelo código nativo (JNI) quando Node.js envia mensagem
    @Suppress("unused")
    fun receiveMessage(message: String) {
        listener?.onMessage(message)
    }

    private external fun sendToNode(message: String)
}
