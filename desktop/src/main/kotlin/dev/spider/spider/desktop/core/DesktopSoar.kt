package dev.spider.spider.desktop.core

import dev.spider.spider.core.LogBus
import dev.spider.spider.domain.config.SoarProfile
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class DesktopSoar(private val profile: SoarProfile, private val workDir: File) {
    private var process: Process? = null
    private var configFile: File? = null
    val isRunning: Boolean get() = process?.isAlive == true

    fun start(): Int {
        profile.validationError()?.let { LogBus.append("E/Soar $it"); return -1 }
        if (profile.resolvers.isEmpty()) { LogBus.append("E/Soar add one or more DNS resolvers to the pasted JSON"); return -1 }
        val binary = extractBinary() ?: run { LogBus.append("E/Soar runtime is missing for ${Os.current}"); return -1 }
        val port = freePort(18253) ?: run { LogBus.append("E/Soar no local SOCKS port available"); return -1 }
        return try {
            val config = File(workDir, "soar-${System.nanoTime()}.json")
            configFile = config
            val json = buildJsonObject {
                put("zone", profile.zone.trim())
                put("serverKey", profile.serverKey.trim())
                putJsonArray("resolvers") { profile.resolvers.forEach { add(it.trim()) } }
                put("listen", "127.0.0.1:$port")
            }
            config.writeText(json.toString())
            val child = ProcessBuilder(binary.absolutePath, "-config", config.absolutePath)
                .directory(workDir).redirectErrorStream(true).start()
            process = child
            Thread({
                runCatching { child.inputStream.bufferedReader().forEachLine { LogBus.append("I/Soar $it") } }
            }, "soar-output").apply { isDaemon = true }.start()
            if (!waitReady(port, child, 15_000)) {
                LogBus.append("E/Soar did not start a local SOCKS listener")
                stop()
                -1
            } else {
                LogBus.append("I/Soar connected to ${profile.zone} on 127.0.0.1:$port")
                port
            }
        } catch (e: Exception) {
            LogBus.append("E/Soar start failed: ${e.message ?: e.javaClass.simpleName}")
            stop()
            -1
        }
    }

    fun stop() {
        val child = process
        if (child != null) {
            runCatching { child.destroy() }
            runCatching { if (!child.waitFor(2, TimeUnit.SECONDS)) child.destroyForcibly() }
        }
        process = null
        configFile?.delete()
        configFile = null
    }

    private fun extractBinary(): File? {
        val (platform, name) = when (Os.current) {
            Os.LINUX -> "linux" to "soar-socks"
            Os.MACOS -> "macos" to "soar-socks"
            Os.WINDOWS -> "windows" to "soar-socks.exe"
            else -> return null
        }
        val target = File(workDir, name)
        if (target.isFile && target.canExecute()) return target
        val input = javaClass.getResourceAsStream("/bin/$platform/$name") ?: return null
        return runCatching {
            input.use { source -> target.outputStream().use { source.copyTo(it) } }
            target.setExecutable(true)
            target.takeIf { it.canExecute() }
        }.getOrNull()
    }

    private fun waitReady(port: Int, child: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (!child.isAlive) return false
            try { Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 200); return true } }
            catch (_: Exception) { Thread.sleep(100) }
        }
        return false
    }

    private fun freePort(preferred: Int): Int? = (preferred..preferred + 32).firstOrNull { port ->
        runCatching { ServerSocket().use { it.reuseAddress = true; it.bind(InetSocketAddress("127.0.0.1", port)) }; true }.getOrDefault(false)
    }
}
