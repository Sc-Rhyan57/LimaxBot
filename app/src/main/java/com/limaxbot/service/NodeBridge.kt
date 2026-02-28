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

// ─── Logger global ────────────────────────────────────────────────────────────
object AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 500, replay = 200)
    val logs: SharedFlow<String> = _logs

    fun log(tag: String, msg: String) { Log.d(tag, msg); scope.launch { _logs.emit("[$tag] $msg") } }
    fun err(tag: String, msg: String) { Log.e(tag, msg); scope.launch { _logs.emit("[ERR/$tag] $msg") } }
    fun node(msg: String) { scope.launch { _logs.emit("[Node] $msg") } }
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
        AppLogger.log("NodeBridge", "Inicializando...")
        scope.launch {
            try {
                val dataDir = context.filesDir

                // ── Binário: usar nativeLibraryDir ─────────────────────────────────────────
                // Android 10+ (W^X policy) bloqueia execução de qualquer binário escrito pelo
                // próprio app em filesDir/cacheDir. O único diretório executável sem root é
                // nativeLibraryDir, onde o PackageManager instala as .so do APK.
                // Solução: empacotar o binário node como libnode.so em jniLibs/<abi>/ no APK.
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                AppLogger.log("NodeBridge", "nativeLibraryDir: $nativeLibDir")
                AppLogger.log("NodeBridge", "Conteúdo: ${File(nativeLibDir).list()?.joinToString() ?: "vazio"}")

                val nodeBin = File(nativeLibDir, "libnode.so")
                if (!nodeBin.exists()) {
                    AppLogger.err("NodeBridge", "ERRO: libnode.so não encontrado em nativeLibraryDir!")
                    AppLogger.err("NodeBridge", "O workflow precisa copiar o binário node para app/src/main/jniLibs/<abi>/libnode.so")
                    _nodeReady.emit(true)
                    return@launch
                }
                AppLogger.log("NodeBridge", "libnode.so: ${nodeBin.length() / 1024 / 1024}MB | executável: ${nodeBin.canExecute()}")

                // ── Projeto Node.js: extrair dos assets ────────────────────────────────────
                val projectDir = extractNodeProject(context, dataDir)
                val indexJs = File(projectDir, "src/index.js")
                if (!indexJs.exists()) {
                    AppLogger.err("NodeBridge", "index.js não encontrado!")
                    AppLogger.err("NodeBridge", "Assets disponíveis: ${context.assets.list("")?.joinToString()}")
                    _nodeReady.emit(true)
                    return@launch
                }
                AppLogger.log("NodeBridge", "index.js: ${indexJs.absolutePath}")

                // ── Iniciar processo ───────────────────────────────────────────────────────
                val pb = ProcessBuilder(nodeBin.absolutePath, indexJs.absolutePath)
                pb.directory(projectDir)
                pb.environment()["HOME"] = dataDir.absolutePath
                pb.environment()["NODE_PATH"] = "${projectDir.absolutePath}/node_modules"
                pb.redirectErrorStream(false)

                process = pb.start()
                writer = PrintWriter(process!!.outputStream, true)

                AppLogger.log("NodeBridge", "Processo Node.js iniciado com sucesso!")
                _nodeReady.emit(true)

                // stdout
                scope.launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        try {
                            val json = gson.fromJson(l, JsonObject::class.java)
                            _messages.emit(json)
                            AppLogger.node("→ ${json["type"]?.asString ?: l}")
                        } catch (_: Exception) { AppLogger.node(l) }
                    }
                    AppLogger.err("NodeBridge", "stdout fechou — processo encerrou")
                }
                // stderr
                scope.launch(Dispatchers.IO) {
                    val er = BufferedReader(InputStreamReader(process!!.errorStream))
                    var line: String?
                    while (er.readLine().also { line = it } != null) AppLogger.err("Node.stderr", line ?: "")
                }

            } catch (e: Exception) {
                AppLogger.err("NodeBridge", "Falha crítica: $e")
                _nodeReady.emit(true)
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

    private fun extractNodeProject(context: Context, dataDir: File): File {
        val projectDir = File(dataDir, "nodejs-project")
        // Só extrai se index.js não existe (evita re-extrair toda vez)
        if (!File(projectDir, "src/index.js").exists()) {
            AppLogger.log("NodeBridge", "Extraindo nodejs-project dos assets...")
            projectDir.deleteRecursively()
            projectDir.mkdirs()
            copyAssetFolder(context, "nodejs-project", projectDir)
        } else {
            AppLogger.log("NodeBridge", "nodejs-project já extraído, reutilizando")
        }
        return projectDir
    }

    private fun copyAssetFolder(context: Context, assetPath: String, destDir: File) {
        try {
            val children = context.assets.list(assetPath)
            if (children == null || children.isEmpty()) {
                context.assets.open(assetPath).use { i ->
                    FileOutputStream(destDir).use { o -> i.copyTo(o) }
                }
            } else {
                destDir.mkdirs()
                for (child in children) {
                    copyAssetFolder(context, "$assetPath/$child", File(destDir, child))
                }
            }
        } catch (e: Exception) {
            AppLogger.err("NodeBridge", "Erro ao copiar '$assetPath': $e")
        }
    }
}
