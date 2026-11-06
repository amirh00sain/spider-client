@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package dev.spider.spider.ui.spider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.spider_add_tile
import dev.spider.spider.shared.resources.spider_add_tile_empty
import dev.spider.spider.shared.resources.spider_all_settings
import dev.spider.spider.shared.resources.spider_done
import dev.spider.spider.shared.resources.spider_edit_hint
import dev.spider.spider.shared.resources.spider_edit_tiles
import dev.spider.spider.shared.resources.spider_hidden_title
import dev.spider.spider.shared.resources.spider_hide_tile
import dev.spider.spider.shared.resources.spider_move_earlier
import dev.spider.spider.shared.resources.spider_move_later
import dev.spider.spider.shared.resources.spider_no_results
import dev.spider.spider.shared.resources.spider_reset
import dev.spider.spider.shared.resources.spider_resize_tile
import dev.spider.spider.shared.resources.spider_search_settings
import dev.spider.spider.shared.resources.spider_size_small
import dev.spider.spider.shared.resources.spider_size_tall
import dev.spider.spider.shared.resources.spider_size_wide
import dev.spider.spider.shared.resources.ic_add
import dev.spider.spider.shared.resources.ic_check
import dev.spider.spider.shared.resources.ic_chevron_right
import dev.spider.spider.shared.resources.ic_edit
import dev.spider.spider.shared.resources.ic_open_in_full
import dev.spider.spider.shared.resources.ic_remove
import dev.spider.spider.shared.resources.ic_restart_alt
import dev.spider.spider.shared.resources.settings_title
import dev.spider.spider.shared.resources.settings_open_category
import dev.spider.spider.shared.resources.settings_quick_language_toggle
import dev.spider.spider.shared.resources.settings_quick_theme_toggle
import dev.spider.spider.shared.resources.settings_display_details
import dev.spider.spider.shared.resources.ic_spider_theme_sun
import dev.spider.spider.shared.resources.ic_spider_theme_moon
import dev.spider.spider.shared.resources.ic_spider_language
import dev.spider.spider.shared.resources.ic_spider_display
import dev.spider.spider.domain.model.AppLanguage
import dev.spider.spider.domain.model.ThemeMode
import dev.spider.spider.ui.onboarding.tourTarget
import dev.spider.spider.ui.platform.appVerticalScroll
import dev.spider.spider.ui.platform.rememberAppSheetState
import dev.spider.spider.ui.settings.SettingsArt
import dev.spider.spider.ui.settings.sharedArt
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Immutable
data class SpiderHubEntry(
    val key: String,
    val title: String,
    val summary: String,
    val art: SettingsArt,
    val keywords: List<String> = emptyList(),
    val tourKey: String? = null,
    val onOpen: () -> Unit,
)

@Composable
fun SpiderHub(
    entries: List<SpiderHubEntry>,
    layoutCode: String,
    onLayoutChange: (String) -> Unit,
    themeMode: ThemeMode,
    language: AppLanguage,
    reduceMotion: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenDisplaySettings: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val haptics = LocalHapticFeedback.current
    val byKey = remember(entries) { entries.associateBy { it.key } }
    val available = remember(entries) { entries.map { it.key } }
    val saved = remember(layoutCode, available) { SpiderTiles.decode(layoutCode, available) }
    var working by remember(saved) { mutableStateOf(saved) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var rootTransitioning by remember { mutableStateOf(false) }
    var themeTurns by remember { mutableFloatStateOf(0f) }
    var languageTurns by remember { mutableFloatStateOf(0f) }
    val rootAlpha by animateFloatAsState(
        if (rootTransitioning) 0.08f else 1f,
        tween(if (rootTransitioning) 150 else 460),
        label = "settings-language-transition",
    )
    val themeRotation by animateFloatAsState(themeTurns, tween(720), label = "settings-theme-icon")
    val languageRotation by animateFloatAsState(languageTurns, tween(720), label = "settings-language-icon")
    val transitionScope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableFloatStateOf(0f) }

    fun commit(next: SpiderTileLayout) {
        working = next
        onLayoutChange(SpiderTiles.encode(next))
    }

    fun transitionContent(change: () -> Unit) {
        if (reduceMotion) {
            change()
        } else {
            transitionScope.launch {
                rootTransitioning = true
                delay(170)
                change()
                delay(50)
                rootTransitioning = false
            }
        }
    }

    dev.spider.spider.ui.platform.BackHandler(enabled = editing) { editing = false }

    Column(
        modifier
            .graphicsLayer { alpha = rootAlpha }
            .fillMaxSize()
            .onGloballyPositioned {
                viewportTop = it.positionInRoot().y
                viewportHeight = it.size.height.toFloat()
            }
            .appVerticalScroll(scroll, contentPadding)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SpiderTopBar(title = stringResource(Res.string.settings_title)) {
            val themeLabel = stringResource(Res.string.settings_quick_theme_toggle)
            val languageLabel = stringResource(Res.string.settings_quick_language_toggle)
            SettingsQuickButton(
                icon = if (themeMode == ThemeMode.Dark) Res.drawable.ic_spider_theme_sun else Res.drawable.ic_spider_theme_moon,
                label = themeLabel,
                rotation = themeRotation,
                onClick = {
                    if (!reduceMotion) themeTurns += 360f
                    transitionContent(onToggleTheme)
                },
            )
            SettingsQuickButton(
                icon = Res.drawable.ic_spider_language,
                label = languageLabel,
                rotation = languageRotation,
                badge = language.tag?.uppercase() ?: "OS",
                onClick = {
                    if (!reduceMotion) languageTurns += 360f
                    transitionContent(onToggleLanguage)
                },
            )
            if (editing) {
                DonePill { editing = false }
            } else {
                MotifButton(
                    shape = androidx.compose.material3.MaterialShapes.Circle,
                    container = c.stage,
                    content = c.onStage,
                    icon = Res.drawable.ic_edit,
                    label = stringResource(Res.string.spider_edit_tiles),
                    size = 52.dp,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        query = ""
                        editing = true
                    },
                )
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(!editing) {
                SpiderSearchField(query, { query = it }, stringResource(Res.string.spider_search_settings))
            }
            if (editing) {
                Text(
                    stringResource(Res.string.spider_edit_hint),
                    style = SpiderType.body(),
                    color = c.muted,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            } else if (query.isBlank()) {
                SpiderCaption(stringResource(Res.string.spider_all_settings))
            }
        }

        if (query.isBlank() && !editing) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpiderMetrics.gutter)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onOpenDisplaySettings)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_spider_display), contentDescription = null, tint = c.bad, modifier = Modifier.size(17.dp))
                Text(stringResource(Res.string.settings_display_details), style = SpiderType.body(), color = c.muted, modifier = Modifier.weight(1f))
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
            }
        }

        if (query.isNotBlank() && !editing) {
            val needle = query.trim().lowercase()
            val found = entries.filter { e ->
                e.title.lowercase().contains(needle) || e.summary.lowercase().contains(needle) ||
                    e.keywords.any { it.lowercase().contains(needle) }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (found.isEmpty()) {
                    Text(
                        stringResource(Res.string.spider_no_results),
                        style = SpiderType.body(),
                        color = c.muted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
                    )
                }
                found.forEachIndexed { i, entry ->
                    val hit = entry.keywords.firstOrNull { it.lowercase().contains(needle) }
                        ?.takeUnless { entry.title.lowercase().contains(needle) }
                    EntryRow(entry, i, found.size, subtitle = hit ?: entry.summary) { entry.onOpen() }
                }
            }
        } else {
            if (editing) {
                TileBoard(
                    layout = working,
                    entries = byKey,
                    editing = true,
                    scroll = scroll,
                    viewportTop = { viewportTop },
                    viewportHeight = { viewportHeight },
                    bottomInset = contentPadding.calculateBottomPadding(),
                    onReorder = { working = it },
                    onCommit = { commit(it) },
                    onEnterEdit = {},
                )
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlineAction(stringResource(Res.string.spider_add_tile), Res.drawable.ic_add, Modifier.weight(1f)) { adding = true }
                    OutlineAction(stringResource(Res.string.spider_reset), Res.drawable.ic_restart_alt, Modifier) {
                        commit(SpiderTiles.decode("", available))
                    }
                }
            } else {
                AccordionBoard(
                    entries = working.visible.mapNotNull { byKey[it.key] },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter),
                )
                val hiddenEntries = working.hidden.mapNotNull { byKey[it] }
                if (hiddenEntries.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpiderCaption(stringResource(Res.string.spider_hidden_title))
                        AccordionBoard(entries = hiddenEntries, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (adding) {
        ModalBottomSheet(onDismissRequest = { adding = false }, sheetState = rememberAppSheetState(), containerColor = c.ground) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = SpiderMetrics.gutter, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(Res.string.spider_add_tile),
                    style = SpiderType.title(24.sp),
                    color = c.ink,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                )
                val hiddenEntries = working.hidden.mapNotNull { byKey[it] }
                if (hiddenEntries.isEmpty()) {
                    Text(
                        stringResource(Res.string.spider_add_tile_empty),
                        style = SpiderType.body(),
                        color = c.muted,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                hiddenEntries.forEachIndexed { i, entry ->
                    EntryRow(entry, i, hiddenEntries.size, subtitle = entry.summary, trailingIcon = Res.drawable.ic_add) {
                        commit(working.with(entry.key))
                        if (working.hidden.isEmpty()) adding = false
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsQuickButton(
    icon: DrawableResource,
    label: String,
    rotation: Float,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val c = Spider.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(42.dp)
            .shadow(5.dp, shape, ambientColor = c.bad.copy(alpha = 0.08f), spotColor = c.bad.copy(alpha = 0.08f))
            .clip(shape)
            .background(c.coral.container)
            .border(1.dp, c.bad.copy(alpha = 0.18f), shape)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = c.bad,
            modifier = Modifier.size(19.dp).graphicsLayer { rotationZ = rotation },
        )
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(c.surface)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge.take(2), style = SpiderType.number(6.sp, FontWeight.Bold), color = c.bad)
            }
        }
    }
}

@Composable
private fun AccordionBoard(entries: List<SpiderHubEntry>, modifier: Modifier = Modifier) {
    val c = Spider.colors
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            val expanded = expandedKey == entry.key
            val tone = c.tone(SpiderTiles.toneOf(entry.key))
            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(c.surface)
                    .border(1.dp, c.line, shape)
                    .animateContentSize(tween(460)),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { expandedKey = if (expanded) null else entry.key }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpiderBadge(entry.art.shape, tone.container, tone.content, entry.art.icon, 42.dp, rotation = 0f)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(entry.title, style = SpiderType.rowTitle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.summary, style = SpiderType.body().copy(fontSize = 12.sp), color = c.muted, maxLines = if (expanded) 3 else 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = c.muted,
                        modifier = Modifier.size(19.dp).graphicsLayer { rotationZ = if (expanded) -90f else 0f },
                    )
                }
                AnimatedVisibility(visible = expanded, enter = fadeIn(tween(280)), exit = fadeOut(tween(180))) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = entry.onOpen)
                            .padding(start = 67.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(Res.string.settings_open_category), style = SpiderType.body().copy(fontWeight = FontWeight.SemiBold), color = c.bad)
                        Spacer(Modifier.width(6.dp))
                        Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = null, tint = c.bad, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TileBoard(
    layout: SpiderTileLayout,
    entries: Map<String, SpiderHubEntry>,
    editing: Boolean,
    scroll: androidx.compose.foundation.ScrollState,
    viewportTop: () -> Float,
    viewportHeight: () -> Float,
    bottomInset: Dp,
    onReorder: (SpiderTileLayout) -> Unit,
    onCommit: (SpiderTileLayout) -> Unit,
    onEnterEdit: () -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val haptics = LocalHapticFeedback.current
    val playful = Spider.prefs.playfulTouch
    var boardTop by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SpiderMetrics.gutter)
            .onGloballyPositioned { boardTop = it.positionInRoot().y },
    ) {
        val columns = if (maxWidth >= 600.dp) 4 else 2
        val gap = SpiderMetrics.gap()
        val rowHeight = SpiderMetrics.tileRow()
        val gapPx = with(density) { gap.toPx() }
        val rowPx = with(density) { rowHeight.toPx() }
        val colPx = (with(density) { maxWidth.toPx() } - gapPx * (columns - 1)) / columns
        val unitX = colPx + gapPx
        val unitY = rowPx + gapPx
        val placed = remember(layout, columns) { SpiderTiles.pack(layout.visible, columns) }
        val rows = SpiderTiles.rowsOf(placed)
        val height = with(density) { (rowPx * rows + gapPx * (rows - 1).coerceAtLeast(0)).toDp() }
        val placedNow by rememberUpdatedState(placed)
        val layoutNow by rememberUpdatedState(layout)

        var dragKey by remember { mutableStateOf<String?>(null) }
        var dragPos by remember { mutableStateOf(Offset.Zero) }

        fun originOf(p: SpiderPlaced) = Offset(unitX * p.col, unitY * p.row)
        fun sizeOf(p: SpiderPlaced) = Offset(colPx * p.cols + gapPx * (p.cols - 1), rowPx * p.rows + gapPx * (p.rows - 1))

        fun follow(key: String) {
            val me = placedNow.firstOrNull { it.slot.key == key } ?: return
            val size = sizeOf(me)
            val gridX = (dragPos.x + size.x / 2f + gapPx / 2f) / unitX
            val gridY = (dragPos.y + size.y / 2f + gapPx / 2f) / unitY
            val from = layoutNow.visible.indexOfFirst { it.key == key }
            val to = SpiderTiles.dropIndex(layoutNow, key, columns, gridX, gridY)
            if (to >= 0 && to != from) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onReorder(layoutNow.moved(key, to))
            }
        }

        LaunchedEffect(dragKey) {
            if (dragKey == null) return@LaunchedEffect
            val edge = with(density) { 96.dp.toPx() }
            val bottomPad = with(density) { bottomInset.toPx() }
            while (isActive && dragKey != null) {
                withFrameNanos { }
                val key = dragKey ?: break
                val p = placedNow.firstOrNull { it.slot.key == key } ?: continue
                val viewport = viewportHeight()
                val centerY = boardTop - viewportTop() + dragPos.y + sizeOf(p).y / 2f
                val speed = when {
                    centerY < edge -> -((edge - centerY) / edge).coerceIn(0f, 1f) * 28f
                    centerY > viewport - bottomPad - edge -> ((centerY - (viewport - bottomPad - edge)) / edge).coerceIn(0f, 1f) * 28f
                    else -> 0f
                }
                if (speed != 0f) {
                    val moved = scroll.scrollBy(speed)
                    if (moved != 0f) {
                        dragPos = dragPos.copy(y = dragPos.y + moved)
                        follow(key)
                    }
                }
            }
        }

        val wiggle = if (editing && playful) rememberInfiniteTransition(label = "tile-wiggle") else null

        Box(Modifier.fillMaxWidth().height(height)) {
            placed.forEachIndexed { index, p ->
                val entry = entries[p.slot.key] ?: return@forEachIndexed
                key(p.slot.key) {
                    val target = originOf(p)
                    val targetInt = IntOffset(target.x.roundToInt(), target.y.roundToInt())
                    val size = sizeOf(p)
                    val dragged = dragKey == p.slot.key
                    val position = remember { Animatable(targetInt, IntOffset.VectorConverter) }
                    LaunchedEffect(targetInt, dragged) {
                        if (dragged) {
                            snapshotFlow { dragPos }.collect { position.snapTo(IntOffset(it.x.roundToInt(), it.y.roundToInt())) }
                        } else {
                            position.animateTo(targetInt, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                    val lift by animateFloatAsState(if (dragged) 1.05f else 1f, spring(dampingRatio = 0.6f), label = "tile-lift")
                    val tilt = wiggle?.animateFloat(
                        initialValue = -0.6f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(tween(150 + (index % 5) * 18), RepeatMode.Reverse),
                        label = "tile-tilt",
                    )
                    val order = layout.visible.indexOfFirst { it.key == p.slot.key }
                    val earlier = stringResource(Res.string.spider_move_earlier)
                    val later = stringResource(Res.string.spider_move_later)
                    Box(
                        Modifier
                            .zIndex(if (dragged) 2f else 0f)
                            .offset { position.value }
                            .size(with(density) { size.x.toDp() }, with(density) { size.y.toDp() })
                            .graphicsLayer {
                                scaleX = lift
                                scaleY = lift
                                rotationZ = if (dragged) 0f else tilt?.value ?: 0f
                                shadowElevation = if (dragged) 18f else 0f
                            }
                            .then(
                                if (editing) {
                                    Modifier
                                        .semantics {
                                            customActions = listOf(
                                                CustomAccessibilityAction(earlier) {
                                                    if (order > 0) onCommit(layoutNow.moved(p.slot.key, order - 1))
                                                    true
                                                },
                                                CustomAccessibilityAction(later) {
                                                    if (order < layoutNow.visible.lastIndex) onCommit(layoutNow.moved(p.slot.key, order + 1))
                                                    true
                                                },
                                            )
                                        }
                                        .pointerInput(p.slot.key) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                var travelled = Offset.Zero
                                                val held = withTimeoutOrNull(PICK_UP_MS) {
                                                    while (true) {
                                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                                        if (!change.pressed) break
                                                        travelled += change.positionChange()
                                                        if (travelled.getDistance() > viewConfiguration.touchSlop) break
                                                    }
                                                    false
                                                }
                                                if (held != null) return@awaitEachGesture
                                                val me = placedNow.firstOrNull { it.slot.key == p.slot.key } ?: return@awaitEachGesture
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                dragPos = originOf(me)
                                                dragKey = p.slot.key
                                                try {
                                                    drag(down.id) { change ->
                                                        val amount = change.positionChange()
                                                        change.consume()
                                                        dragPos += Offset(if (rtl) -amount.x else amount.x, amount.y)
                                                        follow(p.slot.key)
                                                    }
                                                } finally {
                                                    dragKey = null
                                                    onCommit(layoutNow)
                                                }
                                            }
                                        }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        HubTile(
                            entry = entry,
                            slot = p.slot,
                            index = SpiderTiles.ordinalOf(entry.key),
                            editing = editing,
                            onOpen = entry.onOpen,
                            onLongPress = onEnterEdit,
                            onHide = { onCommit(layoutNow.without(entry.key)) },
                            onResize = { onCommit(layoutNow.resized(entry.key)) },
                            modifier = Modifier.fillMaxSize().then(entry.tourKey?.let { Modifier.tourTarget(it) } ?: Modifier),
                        )
                    }
                }
            }
        }
    }
}

private const val PICK_UP_MS = 160L

@Composable
private fun HubTile(
    entry: SpiderHubEntry,
    slot: SpiderTileSlot,
    index: Int,
    editing: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onHide: () -> Unit,
    onResize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val prefs = Spider.prefs
    val prominent = SpiderTiles.isProminent(entry.key)
    val tone = c.tone(SpiderTiles.toneOf(entry.key))
    val accent = if (prominent) c.lime else tone.content
    val accentInk = if (prominent) c.onLime else tone.container
    val big = prominent || slot.size != SpiderTileSize.Small
    val shape = if (editing) RoundedCornerShape(16.dp) else SpiderShapes.tile(prominent)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed && !editing) (if (prefs.playfulTouch) 0.955f else 0.98f) else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "tile-press",
    )
    val spin by animateFloatAsState(
        if (pressed && !editing && prefs.playfulTouch) 22f else 0f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "tile-badge-spin",
    )
    val sizeName = stringResource(
        when (slot.size) {
            SpiderTileSize.Small -> Res.string.spider_size_small
            SpiderTileSize.Wide -> Res.string.spider_size_wide
            SpiderTileSize.Tall -> Res.string.spider_size_tall
        },
    )
    val rotation = if (index % 2 == 0) 14f else -18f
    Box(
        modifier
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(shape)
            .background(tone.container)
            .then(if (editing) Modifier.border(2.dp, tone.content.copy(alpha = 0.2f), shape) else Modifier)
            .spiderMotif(entry.art.shape, tone.content, if (big) 168.dp else 128.dp, rotation, enabled = prefs.motifs, alpha = 0.1f)
            .then(
                if (editing) {
                    Modifier.semantics { contentDescription = "${entry.title}, $sizeName" }
                } else {
                    Modifier.combinedClickable(
                        interactionSource = source,
                        indication = ripple(color = tone.content),
                        role = Role.Button,
                        onClickLabel = entry.title,
                        onLongClick = onLongPress,
                        onClick = onOpen,
                    )
                },
            ),
    ) {
        val compact = prefs.density == dev.spider.spider.domain.model.SpiderDensity.Compact
        var titleHeight by remember { androidx.compose.runtime.mutableIntStateOf(0) }
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tone.content) {
            Column(
                Modifier.fillMaxSize().padding(
                    if (prominent) PaddingValues(horizontal = 18.dp, vertical = if (compact) 12.dp else 16.dp) else PaddingValues(SpiderMetrics.pad()),
                ),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    if (prefs.indexes && !editing) SpiderIndex(index, tone.content) else Spacer(Modifier.size(1.dp))
                    SpiderBadge(
                        shape = entry.art.shape,
                        container = accent,
                        content = accentInk,
                        icon = entry.art.icon,
                        size = when {
                            prominent -> if (compact) 38.dp else 44.dp
                            compact -> 32.dp
                            else -> 38.dp
                        },
                        rotation = (if (index % 2 == 0) 10f else -10f) + spin,
                        modifier = if (editing) Modifier else Modifier.sharedArt(entry.art),
                    )
                }
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp, end = if (editing) 34.dp else 0.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    val density = LocalDensity.current
                    val bodyStyle = SpiderType.body().copy(fontSize = 12.5.sp, lineHeight = 17.sp)
                    val lineHeight = with(density) { 17.sp.toPx() }
                    val gap = with(density) { 3.dp.toPx() }
                    val room = if (titleHeight <= 0) 0 else ((constraints.maxHeight - titleHeight - gap) / lineHeight).toInt()
                    val lines = room.coerceIn(0, if (slot.size == SpiderTileSize.Tall) 4 else 3)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            entry.title,
                            style = SpiderType.title(
                                when {
                                    prominent -> if (compact) 22.sp else 26.sp
                                    slot.size == SpiderTileSize.Small -> if (compact) 17.sp else 19.sp
                                    else -> if (compact) 19.sp else 21.sp
                                },
                            ),
                            color = tone.content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { titleHeight = it.size.height },
                        )
                        if (lines > 0) {
                            Text(
                                if (editing) sizeName else entry.summary,
                                style = bodyStyle,
                                color = tone.content.copy(alpha = 0.82f),
                                maxLines = lines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = editing,
            enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            SpiderIconButton(
                Res.drawable.ic_remove,
                stringResource(Res.string.spider_hide_tile, entry.title),
                onHide,
                container = c.danger,
                content = c.onDanger,
                size = 32.dp,
            )
        }
        AnimatedVisibility(
            visible = editing,
            enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(c.ground)
                    .clickable(role = Role.Button, onClick = onResize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Res.drawable.ic_open_in_full), contentDescription = stringResource(Res.string.spider_resize_tile, entry.title), tint = c.ink, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: SpiderHubEntry,
    index: Int,
    count: Int,
    subtitle: String,
    trailingIcon: org.jetbrains.compose.resources.DrawableResource = Res.drawable.ic_chevron_right,
    onClick: () -> Unit,
) {
    val c = Spider.colors
    val tone = c.tone(SpiderTiles.toneOf(entry.key))
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SpiderMetrics.rowMin())
            .clip(SpiderShapes.row(index, count))
            .background(c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpiderBadge(entry.art.shape, tone.container, tone.content, entry.art.icon, 40.dp, rotation = 0f)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(entry.title, style = SpiderType.rowTitle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = SpiderType.body().copy(fontSize = 12.5.sp), color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(trailingIcon), contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DonePill(onClick: () -> Unit) {
    val c = Spider.colors
    Row(
        Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(c.lime)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = c.onLime, modifier = Modifier.size(18.dp))
        Text(
            stringResource(Res.string.spider_done),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            color = c.onLime,
        )
    }
}

@Composable
private fun OutlineAction(
    label: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val c = Spider.colors
    val shape = RoundedCornerShape(25.dp)
    Row(
        modifier
            .height(50.dp)
            .clip(shape)
            .border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = c.ink, maxLines = 1)
    }
}
