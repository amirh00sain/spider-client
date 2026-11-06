package dev.spider.spider.ui.platform

import androidx.compose.ui.Modifier

expect fun Modifier.verticalResizeCursor(enabled: Boolean = true): Modifier

expect fun Modifier.hideNativePointer(enabled: Boolean = true): Modifier
