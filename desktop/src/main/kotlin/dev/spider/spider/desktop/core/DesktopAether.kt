package dev.spider.spider.desktop.core

import dev.spider.spider.core.LogBus
import dev.spider.spider.domain.config.AetherProfile
import dev.spider.spider.domain.config.LocalPorts
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Starts Spider's bundled Aether process and keeps its WARP identity in the app work directory. */
class DesktopAether(private val profile: AetherProfile, private val work: File) {
    @Volatile private var process: Process? = null
    val isRunning: Boolean get() = process?.isAlive == true

    fun start(): Result<Int> = runCatching {
        profile.validationError()?.let { error(it) }
        val binary = BundledBinary.extract(work, "aether", "aether.exe")
            ?: error("Aether is not bundled for ${Os.current}")
        val identityDir = File(work, "aether").apply { mkdirs() }
        ensureKeys(binary, identityDir)
        val active = launch(binary, identityDir, profile.arguments(LocalPorts.AETHER_SOCKS))
        process = active
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (System.nanoTime() < deadline && active.isAlive) {
            if (socksReady(LocalPorts.AETHER_SOCKS)) return@runCatching LocalPorts.AETHER_SOCKS
            Thread.sleep(150)
        }
        error("Aether did not open its local SOCKS port")
    }.onFailure { stop() }

    private fun ensureKeys(binary: File, identityDir: File) {
        val protocol = profile.protocol
        val files = when (protocol) {
            "masque" -> listOf("aether-masque.toml")
            "gool" -> listOf("aether-wg.toml", "aether-wg-secondary.toml")
            "mim" -> listOf("aether-masque.toml", "aether-masque-secondary.toml")
            "wg-over-masque" -> listOf("aether-masque.toml", "aether-masque-gool.toml")
            else -> listOf("aether-wg.toml")
        }
        if (files.all { File(identityDir, it).isFile && File(identityDir, it).length() > 0 }) return
        val kind = when (protocol) {
            "masque" -> "masque"
            "gool" -> "gool-classic"
            "mim" -> "mim"
            "wg-over-masque" -> "gool"
            else -> "wg"
        }
        LogBus.append("I/Aether: registering the tunnel identity")
        val registration = launch(binary, identityDir, listOf("--register", kind, "--log-level", "info"))
        if (!registration.waitFor(4, TimeUnit.MINUTES)) {
            registration.destroyForcibly()
            error("Aether identity registration timed out")
        }
        if (registration.exitValue() != 0 || files.any { !File(identityDir, it).isFile || File(identityDir, it).length() == 0L }) {
            error("Aether could not register the tunnel identity")
        }
    }

    private fun launch(binary: File, identityDir: File, arguments: List<String>): Process =
        ProcessBuilder(listOf(binary.absolutePath) + arguments)
            .directory(identityDir)
            .redirectErrorStream(true)
            .apply {
                environment().apply {
                    put("HOME", identityDir.absolutePath)
                    put("AETHER_CONFIG", File(identityDir, "aether.toml").absolutePath)
                    put("AETHER_WG_CONFIG", File(identityDir, "aether-wg.toml").absolutePath)
                    put("AETHER_MASQUE_CONFIG", File(identityDir, "aether-masque.toml").absolutePath)
                }
            }
            .start()
            .also { child ->
                thread(name = "spider-aether-output", isDaemon = true) {
                    runCatching { child.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { if (it.isNotBlank()) LogBus.append("I/Aether: ${it.take(500)}") }
                    } }
                }
            }

    private fun socksReady(port: Int): Boolean = runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), 350)
            socket.soTimeout = 350
            socket.getOutputStream().write(byteArrayOf(5, 1, 0))
            val response = ByteArray(2)
            socket.getInputStream().read(response) == 2 && response[0] == 5.toByte()
        }
    }.getOrDefault(false)

    fun stop() {
        val old = process ?: return
        process = null
        XrayCore.stopProcess(old)
    }
}
