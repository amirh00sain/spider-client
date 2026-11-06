package dev.spider.spider.platform

import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private object FirefoxDesktopProcess {
    private val dataDir: File by lazy { firefoxDataDir().apply { mkdirs() } }
    @Volatile private var process: Process? = null

    @Synchronized
    fun start() {
        if (process?.isAlive == true) return
        val platform = when {
            System.getProperty("os.name").lowercase(Locale.ROOT).contains("win") -> "windows"
            System.getProperty("os.name").lowercase(Locale.ROOT).contains("mac") -> "macos"
            else -> "linux"
        }
        val suffix = if (platform == "windows") ".exe" else ""
        val resource = "/bin/$platform/arman-vpn-core$suffix"
        val input = FirefoxDesktopProcess::class.java.getResourceAsStream(resource)
            ?: error("Bundled Firefox VPN core is missing for $platform")
        val executable = File(dataDir, "arman-vpn-core$suffix")
        input.use { source -> executable.outputStream().use { source.copyTo(it) } }
        executable.setExecutable(true, true)
        process = ProcessBuilder(executable.absolutePath, "-app", "-data-dir", dataDir.absolutePath)
            .directory(dataDir)
            .redirectOutput(File(dataDir, "core-stdout.log"))
            .redirectError(File(dataDir, "core-stderr.log"))
            .start()
    }

    fun dataDir() = dataDir
}

actual suspend fun startFirefoxVpnCore() = withContext(Dispatchers.IO) {
    FirefoxDesktopProcess.start()
    waitForFirefoxApi(FirefoxDesktopProcess.dataDir())
}

actual suspend fun firefoxVpnRequest(method: String, path: String, body: String?): String = withContext(Dispatchers.IO) {
    startFirefoxVpnCore()
    FirefoxVpnHttp.request(FirefoxDesktopProcess.dataDir(), method, path, body)
}

private suspend fun waitForFirefoxApi(dataDir: File) {
    var last: Throwable? = null
    repeat(60) {
        if (File(dataDir, "local-api.token").isFile) {
            runCatching { FirefoxVpnHttp.request(dataDir, "GET", "status", null) }
                .onSuccess { return }
                .onFailure { last = it }
        }
        delay(250)
    }
    throw IllegalStateException("Firefox VPN core did not start${last?.message?.let { ": $it" }.orEmpty()}")
}

private fun firefoxDataDir(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase(Locale.ROOT)
    return when {
        os.contains("win") -> File(System.getenv("APPDATA") ?: home, "Spider/firefoxvpn")
        os.contains("mac") -> File(home, "Library/Application Support/Spider/firefoxvpn")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "spider/firefoxvpn")
    }
}
