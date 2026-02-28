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

// ─── Logger global acessível de qualquer lugar do app ────────────────────────
object AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 500, replay = 200)
    val logs: SharedFlow<String> = _logs

    fun log(tag: String, msg: String) {
        Log.d(tag, msg)
        scope.launch { _logs.emit("[${tag}] $msg") }
    }

    fun err(tag: String, msg: String) {
        Log.e(tag, msg)
        scope.launch { _logs.emit("[ERR/$tag] $msg") }
    }

    fun node(msg: String) {
        Log.d("Node.js", msg)
        scope.launch { _logs.emit("[Node] $msg") }
    }
}

// ─── NodeBridge ──────────────────────────────────────────────────────────────
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
        AppLogger.log("NodeBridge", "Inicializando Node.js...")
        scope.launch {
            try {
                val dataDir = context.filesDir
                AppLogger.log("NodeBridge", "Extraindo binário node...")
                val nodeBin = extractNodeBinary(context, dataDir)

                if (nodeBin == null) {
                    AppLogger.err("NodeBridge", "Binário node NÃO encontrado em assets/bin/<abi>/node")
                    AppLogger.err("NodeBridge", "ABIs suportadas: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
                    // Mesmo sem o binário, marcamos como pronto para não travar a UI
                    _nodeReady.emit(true)
                    return@launch
                }

                nodeBin.setExecutable(true)
                AppLogger.log("NodeBridge", "Binário encontrado: ${nodeBin.absolutePath} (${nodeBin.length()} bytes)")

                val projectDir = extractNodeProject(context, dataDir)
                AppLogger.log("NodeBridge", "Projeto em: ${projectDir.absolutePath}")

                val pb = ProcessBuilder(nodeBin.absolutePath, "${projectDir.absolutePath}/src/index.js")
                pb.directory(projectDir)
                pb.environment()["HOME"] = dataDir.absolutePath
                pb.environment()["NODE_PATH"] = "${projectDir.absolutePath}/node_modules"
                pb.redirectErrorStream(false)

                process = pb.start()
                writer = PrintWriter(process!!.outputStream, true)

                AppLogger.log("NodeBridge", "Processo Node.js iniciado com sucesso")
                _nodeReady.emit(true)

                // Ler stdout (mensagens JSON do bot)
                scope.launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        try {
                            val json = gson.fromJson(l, JsonObject::class.java)
                            _messages.emit(json)
                            AppLogger.node("→ ${json["type"]?.asString ?: l}")
                        } catch (_: Exception) {
                            AppLogger.node(l)
                        }
                    }
                    AppLogger.err("NodeBridge", "stdout do Node.js fechou — processo encerrou")
                }

                // Ler stderr do Node.js
                scope.launch(Dispatchers.IO) {
                    val errReader = BufferedReader(InputStreamReader(process!!.errorStream))
                    var line: String?
                    while (errReader.readLine().also { line = it } != null) {
                        AppLogger.err("Node.stderr", line ?: "")
                    }
                }

            } catch (e: Exception) {
                AppLogger.err("NodeBridge", "Falha ao iniciar: $e")
                _nodeReady.emit(true) // libera a UI mesmo com erro
            }
        }
    }

    fun send(action: String, payload: Any? = null) {
        scope.launch {
            try {
                val msg = if (payload != null)
                    gson.toJson(mapOf("action" to action, "payload" to payload))
                else
                    gson.toJson(mapOf("action" to action))
                writer?.println(msg)
                AppLogger.log("NodeBridge", "← $action")
            } catch (e: Exception) {
                AppLogger.err("NodeBridge", "Send error: $e")
            }
        }
    }

    fun isRunning() = process?.isAlive == true

    private fun extractNodeBinary(context: Context, dataDir: File): File? {
        val abis = android.os.Build.SUPPORTED_ABIS
        AppLogger.log("NodeBridge", "ABIs do dispositivo: ${abis.joinToString()}")
        for (abi in abis) {
            val assetPath = "bin/$abi/node"
            val outFile = File(dataDir, "node_bin_$abi")
            try {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
                AppLogger.log("NodeBridge", "Binário extraído para ABI=$abi")
                return outFile
            } catch (_: Exception) {
                AppLogger.log("NodeBridge", "ABI=$abi não encontrada em assets")
            }
        }
        return null
    }

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
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(destDir).use { output -> input.copyTo(output) }
                }
            } else {
                destDir.mkdirs()
                for (asset in assets) {
                    copyAssetFolder(context, "$assetPath/$asset", File(destDir, asset))
                }
            }
        } catch (e: Exception) {
            AppLogger.err("NodeBridge", "Erro ao copiar asset $assetPath: $e")
        }
    }
}
