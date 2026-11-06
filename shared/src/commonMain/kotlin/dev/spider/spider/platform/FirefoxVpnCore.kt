package dev.spider.spider.platform

object FirefoxVpnCore {
    /** Optional app-private data directory, set by the host before first use. */
    var dataDirectory: String = ""
}

expect fun decodeFirefoxVpnCaptcha(dataUrl: String): androidx.compose.ui.graphics.ImageBitmap?

/** Starts the bundled Arman Firefox VPN core and waits until its API is ready. */
expect suspend fun startFirefoxVpnCore()

/** Sends one authenticated request to the core's loopback dashboard API. */
expect suspend fun firefoxVpnRequest(method: String, path: String, body: String? = null): String

