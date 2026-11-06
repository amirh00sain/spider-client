package dev.spider.spider.platform

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeFirefoxVpnCaptcha(dataUrl: String): ImageBitmap? = runCatching {
    val encoded = dataUrl.substringAfter(',', dataUrl)
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()
