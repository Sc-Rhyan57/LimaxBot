package com.janeasystems.rn_nodejs_mobile

import android.content.Context
import android.util.Log

class RNNodeJsMobile(private val context: Context) {

    val channel = NodeChannel()

    companion object {
        private const val TAG = "NodeJsMobile"

        init {
            try {
                System.loadLibrary("node")
                Log.d(TAG, "libnode.so carregada")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Falha ao carregar libnode.so: ${e.message}")
            }
        }
    }

    fun startNodeWithArguments(arguments: Array<String>) {
        val dataDir = context.filesDir.absolutePath
        Thread {
            try {
                nativeStartNode(arguments, dataDir)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar Node.js: ${e.message}")
            }
        }.apply {
            name = "NodeJsThread"
            isDaemon = true
            start()
        }
    }

    private external fun nativeStartNode(arguments: Array<String>, dataDir: String)
}
