package dev.spider.spider.ui.platform

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.Point
import java.awt.image.BufferedImage

private val VerticalResizeIcon = PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR))

actual fun Modifier.verticalResizeCursor(enabled: Boolean): Modifier =
    if (enabled) pointerHoverIcon(VerticalResizeIcon) else this

private val TransparentCursor by lazy {
    val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    PointerIcon(Toolkit.getDefaultToolkit().createCustomCursor(image, Point(0, 0), "hidden"))
}

actual fun Modifier.hideNativePointer(enabled: Boolean): Modifier =
    if (enabled) pointerHoverIcon(TransparentCursor) else this
