package com.janeasystems.rn_nodejs_mobile

import android.content.Context
import android.util.Log

class RNNodeJsMobile(private val context: Context) {

    val channel = NodeJsChannel()

    companion object {
        private const val TAG = "NodeJsMobile"

        init {
            try {
                System.loadLibrary("node")
                Log.d(TAG, "libnode.so carregada com sucesso")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Falha ao carregar libnode.so: ${e.message}")
            }
        }
    }

    fun startNodeWithArguments(arguments: Array<String>) {
        Thread {
            try {
                startNodeJsWithArguments(arguments, getNodePath())
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar Node.js: ${e.message}")
            }
        }.apply {
            name = "NodeJsThread"
            start()
        }
    }

    private fun getNodePath(): String {
        return context.filesDir.absolutePath
    }

    private external fun startNodeJsWithArguments(arguments: Array<String>, nodePath: String)
}

// Serviço de foreground exigido pelo Manifest
class RNNodeJsMobileService : android.app.Service() {
    override fun onBind(intent: android.content.Intent?) = null
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
