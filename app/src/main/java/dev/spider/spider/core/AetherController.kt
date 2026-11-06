package dev.spider.spider.core

import android.content.Context
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Owns the Android Aether executable and its identity files for the lifetime of a VPN session. */
object AetherController {
    @Volatile private var process: Process? = null
    @Volatile var lastError: String? = null
        private set
    @Volatile var onExit: (() -> Unit)? = null

    val isRunning: Boolean get() = process?.isAlive == true

    @Synchronized
    fun start(context: Context, settings: dev.spider.spider.domain.config.AetherProfile): Boolean {
        stop()
        lastError = null
        val binary = File(context.applicationInfo.nativeLibraryDir, "libaether.so")
        if (!binary.isFile || !binary.canExecute()) {
            lastError = context.getString(dev.spider.spider.R.string.aether_core_missing)
            return false
        }
        val work = File(context.filesDir, "aether").apply { mkdirs() }
        if (!ensureKeys(context, binary, work, settings.protocol)) return false

        val port = dev.spider.spider.domain.config.LocalPorts.AETHER_SOCKS
        val active = try {
            launch(context, binary, work, settings.arguments(port))
        } catch (e: Exception) {
            lastError = e.message ?: "Aether could not start"
            return false
        }
        process = active
        if (!waitForSocks(active, port)) {
            lastError = lastError ?: "Aether did not open its local SOCKS port"
            stop()
            return false
        }
        thread(name = "spider-aether-watch", isDaemon = true) {
            runCatching { active.waitFor() }
            if (process === active) {
                process = null
                if (active.exitValue() != 0) lastError = lastError ?: "Aether stopped unexpectedly"
                onExit?.invoke()
            }
        }
        return true
    }

    private fun ensureKeys(
        context: Context,
        binary: File,
        work: File,
        protocol: String,
    ): Boolean {
        val needed = when (protocol) {
            "masque" -> listOf("aether-masque.toml")
            "gool" -> listOf("aether-wg.toml", "aether-wg-secondary.toml")
            "mim" -> listOf("aether-masque.toml", "aether-masque-secondary.toml")
            "wg-over-masque" -> listOf("aether-masque.toml", "aether-masque-gool.toml")
            else -> listOf("aether-wg.toml")
        }
        if (needed.all { File(work, it).isFile && File(work, it).length() > 0L }) return true
        val kind = when (protocol) {
            "masque" -> "masque"
            "gool" -> "gool-classic"
            "mim" -> "mim"
            "wg-over-masque" -> "gool"
            else -> "wg"
        }
        LogBus.append("I/Aether: registering the tunnel identity")
        val registration = try {
            launch(context, binary, work, listOf("--register", kind, "--log-level", "info"), retain = false)
        } catch (e: Exception) {
            lastError = e.message ?: "Aether identity registration failed"
            return false
        }
        val finished = runCatching { registration.waitFor(4, TimeUnit.MINUTES) }.getOrDefault(false)
        if (!finished) {
            registration.destroyForcibly()
            lastError = "Aether identity registration timed out"
            return false
        }
        if (registration.exitValue() != 0 || needed.any { !File(work, it).isFile || File(work, it).length() == 0L }) {
            lastError = "Aether could not register the tunnel identity"
            return false
        }
        return true
    }

    private fun launch(
        context: Context,
        binary: File,
        work: File,
        arguments: List<String>,
        retain: Boolean = true,
    ): Process {
        val builder = ProcessBuilder(listOf(binary.absolutePath) + arguments)
            .directory(work)
            .redirectErrorStream(true)
        builder.environment().apply {
            put("HOME", work.absolutePath)
            put("TMPDIR", context.cacheDir.absolutePath)
            put("AETHER_CONFIG", File(work, "aether.toml").absolutePath)
            put("AETHER_WG_CONFIG", File(work, "aether-wg.toml").absolutePath)
            put("AETHER_MASQUE_CONFIG", File(work, "aether-masque.toml").absolutePath)
            put("SSL_CERT_DIR", "/apex/com.android.conscrypt/cacerts:/system/etc/security/cacerts")
        }
        val child = builder.start()
        thread(name = "spider-aether-output", isDaemon = true) {
            runCatching {
                child.inputStream.bufferedReader().useLines { lines -> lines.forEach { line ->
                    if (line.isNotBlank()) LogBus.append("I/Aether: ${line.take(500)}")
                } }
            }
        }
        if (!retain) return child
        return child
    }

    private fun waitForSocks(child: Process, port: Int): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (System.nanoTime() < deadline && child.isAlive) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 350)
                    socket.soTimeout = 350
                    socket.getOutputStream().write(byteArrayOf(5, 1, 0))
                    val response = ByteArray(2)
                    if (socket.getInputStream().read(response) == 2 && response[0] == 5.toByte()) return true
                }
            }
            Thread.sleep(150)
        }
        return false
    }

    @Synchronized
    fun stop() {
        val old = process
        process = null
        if (old != null && old.isAlive) {
            runCatching { old.destroy() }
            runCatching { if (!old.waitFor(1200, TimeUnit.MILLISECONDS)) old.destroyForcibly() }
        }
    }
}
