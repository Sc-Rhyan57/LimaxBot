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
import java.util.zip.ZipInputStream

object AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logs = MutableSharedFlow<String>(extraBufferCapacity = 500, replay = 200)
    val logs: SharedFlow<String> = _logs

    fun log(tag: String, msg: String) { Log.d(tag, msg); scope.launch { _logs.emit("[$tag] $msg") } }
    fun err(tag: String, msg: String) { Log.e(tag, msg); scope.launch { _logs.emit("[ERR/$tag] $msg") } }
    fun node(msg: String) { scope.launch { _logs.emit("[Node] $msg") } }
}

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
                val nativeLibDir = context.applicationInfo.nativeLibraryDir
                AppLogger.log("NodeBridge", "nativeLibraryDir: $nativeLibDir")
                AppLogger.log("NodeBridge", "Conteudo: ${File(nativeLibDir).list()?.joinToString() ?: "vazio"}")

                val nodeBin = File(nativeLibDir, "libnode.so")
                if (!nodeBin.exists()) {
                    AppLogger.err("NodeBridge", "ERRO: libnode.so nao encontrado em nativeLibraryDir!")
                    _nodeReady.emit(true)
                    return@launch
                }
                AppLogger.log("NodeBridge", "libnode.so: ${nodeBin.length() / 1024 / 1024}MB | executavel: ${nodeBin.canExecute()}")

                val projectDir = prepareNodeProject(context, dataDir)
                val indexJs = File(projectDir, "src/index.js")
                if (!indexJs.exists()) {
                    AppLogger.err("NodeBridge", "index.js nao encontrado em ${projectDir.absolutePath}!")
                    AppLogger.err("NodeBridge", "Assets disponiveis: ${context.assets.list("")?.joinToString()}")
                    _nodeReady.emit(true)
                    return@launch
                }
                AppLogger.log("NodeBridge", "index.js: ${indexJs.absolutePath}")

                val nodeModules = File(projectDir, "node_modules")
                AppLogger.log("NodeBridge", "node_modules: ${nodeModules.list()?.size ?: 0} pacotes")

                val pb = ProcessBuilder(nodeBin.absolutePath, indexJs.absolutePath)
                pb.directory(projectDir)
                pb.environment()["HOME"] = dataDir.absolutePath
                pb.environment()["NODE_PATH"] = "${projectDir.absolutePath}/node_modules"
                pb.redirectErrorStream(false)

                process = pb.start()
                writer = PrintWriter(process!!.outputStream, true)

                AppLogger.log("NodeBridge", "Processo Node.js iniciado com sucesso!")
                _nodeReady.emit(true)

                scope.launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line ?: continue
                        try {
                            val json = gson.fromJson(l, JsonObject::class.java)
                            _messages.emit(json)
                            AppLogger.node("-> ${json["type"]?.asString ?: l}")
                        } catch (_: Exception) { AppLogger.node(l) }
                    }
                    AppLogger.err("NodeBridge", "stdout fechou — processo encerrou")
                }

                scope.launch(Dispatchers.IO) {
                    val er = BufferedReader(InputStreamReader(process!!.errorStream))
                    var line: String?
                    while (er.readLine().also { line = it } != null) {
                        AppLogger.err("Node.stderr", line ?: "")
                    }
                }

            } catch (e: Exception) {
                AppLogger.err("NodeBridge", "Falha critica: $e")
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
                AppLogger.log("NodeBridge", "<- $action")
            } catch (e: Exception) {
                AppLogger.err("NodeBridge", "Send error: $e")
            }
        }
    }

    fun isRunning() = process?.isAlive == true

    private fun prepareNodeProject(context: Context, dataDir: File): File {
        val projectDir = File(dataDir, "nodejs-project")
        val versionFile = File(dataDir, "nodejs-project.version")
        val currentVersion = try { context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString() } catch (_: Exception) { "0" }
        val installedVersion = if (versionFile.exists()) versionFile.readText().trim() else ""

        val needsUpdate = !File(projectDir, "src/index.js").exists() || installedVersion != currentVersion

        if (needsUpdate) {
            AppLogger.log("NodeBridge", "Extraindo nodejs-project dos assets...")
            projectDir.deleteRecursively()
            projectDir.mkdirs()

            copyAssetFolder(context, "nodejs-project", projectDir)

            AppLogger.log("NodeBridge", "Extraindo node_modules do ZIP...")
            extractNodeModulesZip(context, projectDir)

            versionFile.writeText(currentVersion)
            AppLogger.log("NodeBridge", "Projeto extraido com sucesso!")
        } else {
            AppLogger.log("NodeBridge", "nodejs-project ja esta atualizado, reutilizando")
        }

        return projectDir
    }

    private fun extractNodeModulesZip(context: Context, projectDir: File) {
        val nodeModulesDir = File(projectDir, "node_modules")
        nodeModulesDir.deleteRecursively()
        nodeModulesDir.mkdirs()

        try {
            context.assets.open("node_modules.zip").use { assetStream ->
                ZipInputStream(assetStream.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    var count = 0
                    while (entry != null) {
                        val entryName = entry.name
                        val strippedName = if (entryName.startsWith("node_modules/")) {
                            entryName.removePrefix("node_modules/")
                        } else {
                            entryName
                        }

                        val destFile = File(nodeModulesDir, strippedName)

                        if (entry.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { out ->
                                zip.copyTo(out)
                            }
                            count++
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    AppLogger.log("NodeBridge", "node_modules extraido: $count arquivos")
                }
            }
        } catch (e: Exception) {
            AppLogger.err("NodeBridge", "Erro ao extrair node_modules.zip: $e")
        }
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
                    if (child == "node_modules") continue
                    copyAssetFolder(context, "$assetPath/$child", File(destDir, child))
                }
            }
        } catch (e: Exception) {
            AppLogger.err("NodeBridge", "Erro ao copiar '$assetPath': $e")
        }
    }
}
