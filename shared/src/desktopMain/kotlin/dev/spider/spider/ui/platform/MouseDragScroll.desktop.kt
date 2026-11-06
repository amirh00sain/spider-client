package dev.spider.spider.ui.platform

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

actual fun Modifier.mouseDragScroll(state: ScrollableState): Modifier = composed {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    pointerInput(state, rtl) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type != PointerType.Mouse) return@awaitEachGesture
            var dragging = false
            var pending = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - change.previousPosition.x
                if (!dragging) {
                    pending += dx
                    if (abs(pending) < viewConfiguration.touchSlop) continue
                    dragging = true
                    state.dispatchRawDelta(if (rtl) pending else -pending)
                } else {
                    state.dispatchRawDelta(if (rtl) dx else -dx)
                }
                change.consume()
            }
        }
    }
}

actual fun Modifier.mouseWheelScroll(state: ScrollableState): Modifier =
    pointerInput(state) {
        val step = WHEEL_STEP.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Scroll) continue
                val change = event.changes.firstOrNull() ?: continue
                val delta = change.scrollDelta
                val notches = if (delta.x != 0f) delta.x else delta.y
                if (notches == 0f) continue
                if (state.dispatchRawDelta(notches * step) != 0f) change.consume()
            }
        }
    }

actual fun Modifier.mouseDragScrollVertical(state: ScrollableState, reverse: Boolean): Modifier =
    pointerInput(state, reverse) {
        val decay = exponentialDecay<Float>()
        coroutineScope {
            var fling: Job? = null
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (down.type != PointerType.Mouse || !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                fling?.cancel()
                val tracker = VelocityTracker()
                tracker.addPosition(down.uptimeMillis, down.position)
                var dragging = false
                var pendingX = 0f
                var pendingY = 0f
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (dragging) {
                            change.consume()
                            val velocity = tracker.calculateVelocity().y
                            val start = if (reverse) velocity else -velocity
                            if (abs(start) > MIN_FLING_VELOCITY) {
                                fling = launch {
                                    state.scroll {
                                        var last = 0f
                                        AnimationState(0f, start).animateDecay(decay) {
                                            val delta = value - last
                                            last = value
                                            if (abs(delta - scrollBy(delta)) > 0.5f) cancelAnimation()
                                        }
                                    }
                                }
                            }
                        }
                        break
                    }
                    if (!dragging && change.isConsumed) break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val dx = change.position.x - change.previousPosition.x
                    val dy = change.position.y - change.previousPosition.y
                    if (!dragging) {
                        pendingX += dx
                        pendingY += dy
                        if (abs(pendingY) < viewConfiguration.touchSlop) continue
                        if (abs(pendingY) < abs(pendingX)) break
                        dragging = true
                        state.dispatchRawDelta(if (reverse) pendingY else -pendingY)
                    } else {
                        state.dispatchRawDelta(if (reverse) dy else -dy)
                    }
                    change.consume()
                }
            }
        }
    }

actual fun Modifier.desktopScrollbar(state: ScrollState, padding: PaddingValues): Modifier = this

private val WHEEL_STEP = 64.dp
private const val MIN_FLING_VELOCITY = 400f
