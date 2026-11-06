package dev.spider.spider.platform

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mobile.Mobile

actual suspend fun startFirefoxVpnCore() = withContext(Dispatchers.IO) {
    val dataDir = File(FirefoxVpnCore.dataDirectory)
    check(FirefoxVpnCore.dataDirectory.isNotBlank()) { "Firefox VPN storage is not ready" }
    dataDir.mkdirs()
    if (!Mobile.running()) Mobile.start(dataDir.absolutePath, false)
    waitForFirefoxApi(dataDir)
}

actual suspend fun firefoxVpnRequest(method: String, path: String, body: String?): String = withContext(Dispatchers.IO) {
    startFirefoxVpnCore()
    FirefoxVpnHttp.request(File(FirefoxVpnCore.dataDirectory), method, path, body)
}

private suspend fun waitForFirefoxApi(dataDir: File) {
    var last: Throwable? = null
    repeat(60) {
        if (!Mobile.running()) {
            val stopped = Mobile.lastError()
            if (stopped.isNotBlank()) last = IllegalStateException(stopped)
        } else if (File(dataDir, "local-api.token").isFile) {
            runCatching { FirefoxVpnHttp.request(dataDir, "GET", "status", null) }
                .onSuccess { return }
                .onFailure { last = it }
        }
        delay(250)
    }
    throw IllegalStateException("Firefox VPN core did not start${last?.message?.let { ": $it" }.orEmpty()}")
}
