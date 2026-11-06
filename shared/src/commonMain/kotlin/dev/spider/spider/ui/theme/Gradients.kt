package dev.spider.spider.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object SpiderGradients {
    val connected: Brush
        get() = Brush.linearGradient(
            colors = listOf(SpiderViolet, SpiderHotPink, SpiderLime),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    private val sessionPalettes: List<List<Color>> = listOf(
        listOf(SpiderViolet, SpiderHotPink, SpiderLime),
        listOf(SpiderDeepViolet, SpiderViolet, SpiderCyan),
        listOf(SpiderHotPink, SpiderViolet, SpiderCyan),
        listOf(SpiderViolet, SpiderCyan, SpiderLime),
        listOf(SpiderDeepViolet, SpiderHotPink, SpiderViolet),
    )

    fun forSession(sessionId: Int): Brush = Brush.linearGradient(
        colors = paletteFor(sessionId),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    private fun paletteFor(sessionId: Int): List<Color> =
        sessionPalettes[((sessionId % sessionPalettes.size) + sessionPalettes.size) % sessionPalettes.size]

    fun connectedStops(sessionId: Int): List<Color> =
        listOf(SpiderDeepViolet) + paletteFor(sessionId)

    val connectingStops: List<Color> = listOf(SpiderDeepViolet, SpiderViolet, SpiderHotPink)

    val idle: Brush
        get() = Brush.linearGradient(
            colors = listOf(SpiderDeepViolet, SpiderViolet, SpiderHotPink.copy(alpha = 0.7f)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    val connecting: Brush
        get() = Brush.linearGradient(
            colors = listOf(SpiderViolet, SpiderCyan, SpiderLime),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
}
