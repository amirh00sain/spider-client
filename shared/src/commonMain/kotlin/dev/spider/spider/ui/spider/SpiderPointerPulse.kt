package dev.spider.spider.ui.spider

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp

fun Modifier.spiderPointerPulse(enabled: Boolean, color: Color): Modifier = composed {
    var pointer by remember { mutableStateOf(Offset(-100f, -100f)) }
    var visible by remember { mutableStateOf(false) }

    this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Exit) {
                        visible = false
                    } else if (event.type == PointerEventType.Move) {
                        event.changes.lastOrNull { it.type == PointerType.Mouse }?.let { change ->
                            pointer = change.position
                            visible = true
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (!enabled || !visible) return@drawWithContent

            drawCircle(Color.White, radius = 8.dp.toPx(), center = pointer)
            drawCircle(color, radius = 5.dp.toPx(), center = pointer)
        }
}
