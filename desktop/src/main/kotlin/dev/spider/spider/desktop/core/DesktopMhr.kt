package dev.spider.spider.desktop.core

import dev.spider.spider.core.LogBus
import dev.spider.spider.domain.config.MhrProfile
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class DesktopMhr(private val profile: MhrProfile, private val workDir: File) {
    private var process: Process? = null
    private var socksPort = 0
    private var configFile: File? = null
    val isRunning: Boolean get() = process?.isAlive == true

    fun start(): Int {
        profile.validationError()?.let { LogBus.append("E/MHR $it"); return -1 }
        val root = extractSource() ?: run { LogBus.append("E/MHR bundled source is missing"); return -1 }
        val http = freePort(18080) ?: run { LogBus.append("E/MHR no local port available"); return -1 }
        val socks = freePort(http + 1) ?: run { LogBus.append("E/MHR no SOCKS port available"); return -1 }
        val config = File(root, "spider-config.json")
        configFile = config
        val configJson = buildJsonObject {
            put("google_ip", "216.239.38.120")
            put("front_domain", "www.google.com")
            putJsonArray("front_domains") { listOf("www.google.com", "mail.google.com", "accounts.google.com").forEach { add(it) } }
            putJsonArray("script_ids") { profile.deploymentIds.forEach { add(it) } }
            put("auth_key", profile.auth)
            put("listen_host", "127.0.0.1")
            put("http_port", http)
            put("socks5_port", socks)
            put("socks5_enabled", true)
            put("verify_ssl", true)
            put("lan_sharing", false)
            put("log_level", "INFO")
        }
        config.writeText(configJson.toString())
        runCatching { config.setReadable(false, false); config.setReadable(true, true); config.setWritable(false, false); config.setWritable(true, true) }
        val python = if (Os.current == Os.WINDOWS) "python" else "python3"
        return try {
            val builder = ProcessBuilder(python, File(root, "main.py").absolutePath, "--config", config.absolutePath, "--no-cert-check")
                .directory(root)
                .redirectErrorStream(true)
            val env = builder.environment()
            val vendor = File(root, "vendor").absolutePath
            env["PYTHONPATH"] = listOfNotNull(env["PYTHONPATH"], vendor).joinToString(File.pathSeparator)
            val child = builder.start()
            process = child
            Thread({
                runCatching { child.inputStream.bufferedReader().forEachLine { LogBus.append("I/MHR $it") } }
            }, "mhr-output").apply { isDaemon = true }.start()
            if (!waitReady(socks, child, 20_000)) {
                LogBus.append("E/MHR did not start a SOCKS listener; install Python 3.10 or newer and verify the MHR config")
                stop()
                -1
            } else {
                socksPort = socks
                LogBus.append("I/MHR connected with ${profile.deploymentIds.size} deployment(s) on 127.0.0.1:$socks")
                socks
            }
        } catch (e: Exception) {
            LogBus.append("E/MHR start failed: ${e.message ?: e.javaClass.simpleName}")
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
        socksPort = 0
        configFile?.delete()
        configFile = null
    }

    private fun extractSource(): File? {
        val root = File(workDir, "mhr-runtime")
        val marker = File(root, "main.py")
        if (marker.isFile) return root
        val input = javaClass.getResourceAsStream("/engines/mhr.zip") ?: return null
        root.deleteRecursively()
        root.mkdirs()
        return try {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val relative = entry.name.removePrefix("mhr/")
                    if (relative.isNotBlank() && !relative.startsWith("../")) {
                        val out = File(root, relative).canonicalFile
                        if (!out.toPath().startsWith(root.canonicalFile.toPath())) return null
                        if (entry.isDirectory) out.mkdirs() else {
                            out.parentFile.mkdirs()
                            out.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                }
            }
            root.takeIf { marker.isFile }
        } catch (e: Exception) {
            LogBus.append("E/MHR cannot extract runtime: ${e.message}")
            null
        }
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
