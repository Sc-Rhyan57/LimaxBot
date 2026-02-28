package com.limaxbot.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter

object NodeBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _messages = MutableSharedFlow<JsonObject>(extraBufferCapacity = 100)
    val messages: SharedFlow<JsonObject> = _messages

    private val _nodeReady = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val nodeReady: SharedFlow<Boolean> = _nodeReady

    private var process: Process? = null
    private var writer: PrintWriter? = null

    fun init(context: Context) {
        scope.launch {
            try {
                // Extrair o binário node e o projeto dos assets para o armazenamento interno
                val dataDir = context.filesDir
                val nodeBin = extractNodeBinary(context, dataDir)
                val projectDir = extractNodeProject(context, dataDir)

                if (nodeBin == null) {
                    Log.e("NodeBridge", "Binário node não encontrado nos assets")
                    _nodeReady.emit(false)
                    return@launch
                }

                nodeBin.setExecutable(true)

                val pb = ProcessBuilder(nodeBin.absolutePath, "$projectDir/src/index.js")
                pb.directory(projectDir)
                pb.environment()["HOME"] = dataDir.absolutePath
                pb.environment()["NODE_PATH"] = "$projectDir/node_modules"
                pb.redirectErrorStream(false)

                process = pb.start()
                writer = PrintWriter(process!!.outputStream, true)

                Log.d("NodeBridge", "Processo Node.js iniciado")
                _nodeReady.emit(true)

                // Ler stdout do Node.js (mensagens JSON)
                scope.launch {
                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        try {
                            val json = gson.fromJson(line, JsonObject::class.java)
                            _messages.emit(json)
                        } catch (e: Exception) {
                            Log.w("NodeBridge", "Linha não-JSON do Node: $line")
                        }
                    }
                }

                // Logar stderr (erros do Node.js)
                scope.launch {
                    val errReader = BufferedReader(InputStreamReader(process!!.errorStream))
                    var line: String?
                    while (errReader.readLine().also { line = it } != null) {
                        Log.d("NodeBridge/stderr", line ?: "")
                    }
                }

            } catch (e: Exception) {
                Log.e("NodeBridge", "Falha ao iniciar Node.js: $e")
                _nodeReady.emit(false)
            }
        }
    }

    fun send(action: String, payload: Any? = null) {
        scope.launch {
            try {
                val msg = if (payload != null) {
                    gson.toJson(mapOf("action" to action, "payload" to payload))
                } else {
                    gson.toJson(mapOf("action" to action))
                }
                writer?.println(msg)
            } catch (e: Exception) {
                Log.e("NodeBridge", "Send error: $e")
            }
        }
    }

    fun isRunning() = process?.isAlive == true

    /**
     * Extrai o binário 'node' dos assets para o armazenamento interno.
     * O binário fica em assets/bin/<abi>/node
     */
    private fun extractNodeBinary(context: Context, dataDir: File): File? {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        val assetPath = "bin/$abi/node"
        val outFile = File(dataDir, "node_bin")

        return try {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            // Tentar ABI de fallback
            val fallbackAbi = when {
                abi.contains("arm64") -> "arm64-v8a"
                abi.contains("arm") -> "armeabi-v7a"
                abi.contains("x86_64") -> "x86_64"
                else -> "x86"
            }
            try {
                context.assets.open("bin/$fallbackAbi/node").use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
                outFile
            } catch (e2: Exception) {
                Log.e("NodeBridge", "Binário node não encontrado: $e2")
                null
            }
        }
    }

    /**
     * Extrai o nodejs-project dos assets para o armazenamento interno.
     */
    private fun extractNodeProject(context: Context, dataDir: File): File {
        val projectDir = File(dataDir, "nodejs-project")
        projectDir.mkdirs()
        copyAssetFolder(context, "nodejs-project", projectDir)
        return projectDir
    }

    private fun copyAssetFolder(context: Context, assetPath: String, destDir: File) {
        try {
            val assets = context.assets.list(assetPath) ?: return
            if (assets.isEmpty()) {
                // É um arquivo
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destDir).use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                destDir.mkdirs()
                for (asset in assets) {
                    copyAssetFolder(context, "$assetPath/$asset", File(destDir, asset))
                }
            }
        } catch (e: Exception) {
            Log.w("NodeBridge", "Erro ao copiar asset $assetPath: $e")
        }
    }
}
