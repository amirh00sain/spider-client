package dev.spider.spider.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SpiderPalette { Brand, Accent, Pastel, Vivid, Mono, Citrus, Matcha, Neon, Pistachio, Ocean, Sunset, Berry }

@Serializable
enum class SpiderCorners { Leaf, Round, Soft }

@Serializable
enum class SpiderTitle { Huge, Large, Compact }

@Serializable
enum class SpiderDensity { Comfortable, Compact }

@Serializable
enum class SpiderDockLabels { Active, Always, Never }

@Serializable
enum class SpiderConnect { Slide, Button, Morph, Rail, Hold, Tile }

@Serializable
data class SpiderPrefs(
    val palette: SpiderPalette = SpiderPalette.Brand,
    val corners: SpiderCorners = SpiderCorners.Soft,
    val title: SpiderTitle = SpiderTitle.Large,
    val density: SpiderDensity = SpiderDensity.Comfortable,
    val motifs: Boolean = true,
    val playfulTouch: Boolean = true,
    val indexes: Boolean = true,
    val liveTiles: Boolean = true,
    val tiles: String = "",
    val dockLabels: SpiderDockLabels = SpiderDockLabels.Active,
    val dockAutoHide: Boolean = true,
    val navStyle: NavBarStyle = NavBarStyle.Dock,
    val connect: SpiderConnect = SpiderConnect.Slide,
    val wave: Boolean = true,
    val homeSpeed: Boolean = true,
    val homeSession: Boolean = true,
    val homeQuick: Boolean = true,
    val homeChips: Boolean = true,
)
