package dev.spider.spider.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun decodeFirefoxVpnCaptcha(dataUrl: String): ImageBitmap? = runCatching {
    val encoded = dataUrl.substringAfter(',', dataUrl)
    Image.makeFromEncoded(java.util.Base64.getDecoder().decode(encoded)).toComposeImageBitmap()
}.getOrNull()
