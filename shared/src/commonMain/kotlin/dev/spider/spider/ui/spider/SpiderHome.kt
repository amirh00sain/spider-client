@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.spider.spider.ui.spider

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.spider.spider.core.ConnectionFault
import dev.spider.spider.data.net.NetworkInfo
import dev.spider.spider.domain.model.ConnectionState
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.action_retry
import dev.spider.spider.shared.resources.app_name
import dev.spider.spider.shared.resources.spider_connected_for
import dev.spider.spider.shared.resources.spider_current_server
import dev.spider.spider.shared.resources.spider_data_session
import dev.spider.spider.shared.resources.state_connecting
import dev.spider.spider.shared.resources.state_connected
import dev.spider.spider.shared.resources.state_error
import dev.spider.spider.shared.resources.state_idle
import dev.spider.spider.shared.resources.action_connect
import dev.spider.spider.shared.resources.spider_fastest
import dev.spider.spider.shared.resources.spider_no_server
import dev.spider.spider.shared.resources.spider_no_server_hint
import dev.spider.spider.shared.resources.copied
import dev.spider.spider.shared.resources.fault_open_logs
import dev.spider.spider.shared.resources.fault_unknown
import dev.spider.spider.shared.resources.home_download
import dev.spider.spider.shared.resources.home_local_proxy
import dev.spider.spider.shared.resources.home_promo_action
import dev.spider.spider.shared.resources.home_promo_body
import dev.spider.spider.shared.resources.home_promo_title
import dev.spider.spider.shared.resources.home_upload
import dev.spider.spider.shared.resources.home_rule
import dev.spider.spider.shared.resources.home_rule_global
import dev.spider.spider.shared.resources.home_rule_ir
import dev.spider.spider.shared.resources.map_share_lan
import dev.spider.spider.shared.resources.map_share_lan_on
import dev.spider.spider.shared.resources.map_share_lan_no_ipv4
import dev.spider.spider.shared.resources.action_ok
import dev.spider.spider.shared.resources.ic_chevron_right
import dev.spider.spider.shared.resources.ic_description
import dev.spider.spider.shared.resources.ic_download
import dev.spider.spider.shared.resources.ic_fill_map
import dev.spider.spider.shared.resources.ic_fill_speed
import dev.spider.spider.shared.resources.ic_lock
import dev.spider.spider.shared.resources.ic_spider_glyph
import dev.spider.spider.shared.resources.ic_upload
import dev.spider.spider.shared.resources.logs_title
import dev.spider.spider.shared.resources.map_title
import dev.spider.spider.shared.resources.pixel_other_server
import dev.spider.spider.shared.resources.title_speedtest
import dev.spider.spider.ui.components.NoteText
import dev.spider.spider.ui.connection.ConnectionUiState
import dev.spider.spider.ui.format.formatBytes
import dev.spider.spider.ui.format.formatElapsed
import dev.spider.spider.ui.format.ltrIsolate
import dev.spider.spider.ui.format.formatRate
import dev.spider.spider.ui.home.faultHint
import dev.spider.spider.ui.home.faultTitle
import dev.spider.spider.ui.onboarding.TourTargets
import dev.spider.spider.ui.onboarding.tourTarget
import dev.spider.spider.ui.platform.LocalPlatform
import dev.spider.spider.ui.platform.appVerticalScroll
import dev.spider.spider.ui.platform.verticalResizeCursor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Immutable
data class SpiderActiveServer(
    val name: String,
    val detail: String?,
    val country: String?,
    val engine: String,
    val locked: Boolean,
    val subscription: dev.spider.spider.domain.config.Subscription? = null,
)

@Immutable
data class SpiderServerPick(
    val id: String,
    val name: String,
    val detail: String,
    val country: String?,
    val pingMs: Int?,
)

private const val TITLE_TAPS = 5
private const val BURST_TAPS = 3
private const val TAP_WINDOW_MS = 1_200L

@Composable
internal fun SpiderHome(
    ui: ConnectionUiState,
    info: NetworkInfo,
    active: SpiderActiveServer?,
    picks: List<SpiderServerPick>,
    lockedNote: String?,
    localProxyAddress: String?,
    showConnectionInfo: Boolean,
    animate: Boolean,
    contentPadding: PaddingValues,
    runMode: dev.spider.spider.domain.model.RunMode,
    onRunModeChange: (dev.spider.spider.domain.model.RunMode) -> Unit,
    routingMode: dev.spider.spider.domain.model.RoutingMode,
    onRoutingModeChange: (dev.spider.spider.domain.model.RoutingMode) -> Unit,
    shareLanActive: Boolean,
    shareLanPort: Int,
    onToggleShareLan: (Boolean) -> Unit,
    onToggleConnection: () -> Unit,
    onBrowseConfigs: () -> Unit,
    onPickServer: (String) -> Unit,
    onOpenSpeedTest: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    onRefreshInfo: () -> Unit,
    onSecretUnlocked: () -> Unit,
    onShowLogs: () -> Unit,
    onBurst: () -> Unit,
    onStatusTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val prefs = Spider.prefs
    val platform = LocalPlatform.current
    val appLayoutDirection = LocalLayoutDirection.current
    val live = ui.state == ConnectionState.Connected
    val desktopLayout = platform.supportsSystemProxy
    val scroll = rememberScrollState()

    Box(modifier.fillMaxSize().background(c.ground)) {
        DotBackdrop(Modifier.fillMaxSize())

        ModeRail(
            runMode = runMode,
            tunAvailable = platform.supportsTun,
            onModeChange = onRunModeChange,
            modifier = Modifier
                .align(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Alignment.TopEnd else Alignment.TopStart)
                .zIndex(2f)
                .padding(start = 28.dp, end = 28.dp, top = 24.dp),
        )
        PanelPromo(
            onClick = { platform.openUri("https://spiderpanel.xfdscscsa.de5.net") },
            onSecretUnlocked = onSecretUnlocked,
            onBurst = onBurst,
            modifier = Modifier
                .align(if (LocalLayoutDirection.current == LayoutDirection.Rtl) Alignment.TopStart else Alignment.TopEnd)
                .zIndex(2f)
                .padding(horizontal = if (desktopLayout) 40.dp else 20.dp, vertical = 34.dp),
        )

        if (desktopLayout) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(start = 58.dp, end = 58.dp, top = 106.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(42.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                        HomeHero(
                            ui = ui,
                            info = info,
                            active = active,
                            live = live,
                            runMode = runMode,
                            tunAvailable = platform.supportsTun,
                            animate = animate,
                            showSpeed = prefs.homeSpeed,
                            showSession = prefs.homeSession,
                            onToggleConnection = onToggleConnection,
                            onStatusTap = onStatusTap,
                            modifier = Modifier.weight(1f).tourTarget(TourTargets.CORE),
                            globeSize = 286.dp,
                        )
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                        HomeDetails(
                            ui = ui,
                            info = info,
                            active = active,
                            picks = picks,
                            lockedNote = lockedNote,
                            localProxyAddress = localProxyAddress,
                            live = live,
                            showConnectionInfo = showConnectionInfo,
                            showSession = prefs.homeSession,
                            showChips = prefs.homeChips,
                            onToggleConnection = onToggleConnection,
                            onBrowseConfigs = onBrowseConfigs,
                            onPickServer = onPickServer,
                            onOpenSpeedTest = onOpenSpeedTest,
                            onOpenMap = onOpenMap,
                            routingMode = routingMode,
                            onRoutingModeChange = onRoutingModeChange,
                            shareLanActive = shareLanActive,
                            shareLanPort = shareLanPort,
                            onToggleShareLan = onToggleShareLan,
                            onRefreshInfo = onRefreshInfo,
                            onShowLogs = onShowLogs,
                            modifier = Modifier
                                .weight(1.08f)
                                .fillMaxHeight()
                                .appVerticalScroll(scroll)
                                .padding(vertical = 12.dp)
                                .widthIn(max = 600.dp),
                        )
                    }
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .appVerticalScroll(scroll)
                        .padding(start = SpiderMetrics.gutter, end = SpiderMetrics.gutter, top = 98.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    HomeHero(
                        ui = ui,
                        info = info,
                        active = active,
                        live = live,
                        runMode = runMode,
                        tunAvailable = platform.supportsTun,
                        animate = animate,
                        showSpeed = prefs.homeSpeed,
                        showSession = prefs.homeSession,
                        onToggleConnection = onToggleConnection,
                        onStatusTap = onStatusTap,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).tourTarget(TourTargets.CORE),
                        globeSize = 222.dp,
                    )
                    HomeDetails(
                        ui = ui,
                        info = info,
                        active = active,
                        picks = picks,
                        lockedNote = lockedNote,
                        localProxyAddress = localProxyAddress,
                        live = live,
                        showConnectionInfo = showConnectionInfo,
                        showSession = prefs.homeSession,
                        showChips = prefs.homeChips,
                        onToggleConnection = onToggleConnection,
                        onBrowseConfigs = onBrowseConfigs,
                        onPickServer = onPickServer,
                        onOpenSpeedTest = onOpenSpeedTest,
                        onOpenMap = onOpenMap,
                        routingMode = routingMode,
                        onRoutingModeChange = onRoutingModeChange,
                        shareLanActive = shareLanActive,
                        shareLanPort = shareLanPort,
                        onToggleShareLan = onToggleShareLan,
                        onRefreshInfo = onRefreshInfo,
                        onShowLogs = onShowLogs,
                        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHero(
    ui: ConnectionUiState,
    info: NetworkInfo,
    active: SpiderActiveServer?,
    live: Boolean,
    runMode: dev.spider.spider.domain.model.RunMode,
    tunAvailable: Boolean,
    animate: Boolean,
    showSpeed: Boolean,
    showSession: Boolean,
    onToggleConnection: () -> Unit,
    onStatusTap: () -> Unit,
    modifier: Modifier = Modifier,
    globeSize: androidx.compose.ui.unit.Dp,
) {
    val c = Spider.colors
    val connecting = ui.state == ConnectionState.Connecting || ui.state == ConnectionState.Reconnecting
    val stateColor = when {
        live -> c.good
        connecting -> c.mid
        else -> c.bad
    }
    val status = when (ui.state) {
        ConnectionState.Connected -> stringResource(Res.string.state_connected)
        ConnectionState.Connecting, ConnectionState.Reconnecting -> stringResource(Res.string.state_connecting)
        ConnectionState.Error -> stringResource(Res.string.state_error)
        ConnectionState.Disconnecting, ConnectionState.Idle -> stringResource(Res.string.state_idle)
    }
    val modeLabel = if (runMode == dev.spider.spider.domain.model.RunMode.Vpn && tunAvailable) "T · TUN" else "P · PROXY"

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(role = Role.Button, onClick = onStatusTap)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(8.dp).shadow(5.dp, CircleShape, ambientColor = stateColor, spotColor = stateColor).clip(CircleShape).background(stateColor))
                Text(status, style = SpiderType.body(), color = c.ink, textAlign = TextAlign.End)
                Text(active?.name ?: stringResource(Res.string.spider_no_server), style = SpiderType.body(), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
            }
        }
        SpiderGlobeButton(
            state = ui.state,
            animate = animate,
            onClick = onToggleConnection,
            modifier = Modifier.size(globeSize),
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(modeLabel, style = SpiderType.number(11.sp, FontWeight.Medium).copy(textDirection = TextDirection.Ltr), color = Color(0xFF3B82D4))
            Box(Modifier.width(22.dp).height(1.dp).background(c.line))
            Text(
                if (live && info.pingMs != null) ltrIsolate("${info.pingMs} ms") else "— ms",
                style = SpiderType.number(11.sp).copy(textDirection = TextDirection.Ltr),
                color = c.muted,
            )
            }
        }
        if (showSpeed || showSession) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                if (showSpeed) {
                    HomeMetric(stringResource(Res.string.home_download), ui.downloadBps, live, Res.drawable.ic_download)
                    HomeMetricDivider()
                    HomeMetric(stringResource(Res.string.home_upload), ui.uploadBps, live, Res.drawable.ic_upload)
                }
                if (showSession) {
                    if (showSpeed) HomeMetricDivider()
                    HomeTimeMetric(formatElapsed(if (live) ui.elapsedSeconds else 0))
                }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(label: String, bps: Long, live: Boolean, icon: org.jetbrains.compose.resources.DrawableResource) {
    val c = Spider.colors
    val value = if (live) "%.1f".format((bps * 8f) / 1_000_000f) else "—"
    Row(
        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = c.muted, modifier = Modifier.size(14.dp))
        Text(value, style = SpiderType.number(13.sp, FontWeight.Medium).copy(textDirection = TextDirection.Ltr), color = c.ink)
        Text("Mbps", style = SpiderType.number(10.sp).copy(textDirection = TextDirection.Ltr), color = c.muted)
    }
}

@Composable
private fun HomeTimeMetric(value: String) {
    val c = Spider.colors
    Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = SpiderType.number(13.sp, FontWeight.Medium).copy(textDirection = TextDirection.Ltr), color = c.ink)
        Text(stringResource(Res.string.spider_connected_for), style = SpiderType.caption(), color = c.muted)
    }
}

@Composable
private fun HomeMetricDivider() {
    Box(Modifier.width(1.dp).height(18.dp).background(Spider.colors.line))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeDetails(
    ui: ConnectionUiState,
    info: NetworkInfo,
    active: SpiderActiveServer?,
    picks: List<SpiderServerPick>,
    lockedNote: String?,
    localProxyAddress: String?,
    live: Boolean,
    showConnectionInfo: Boolean,
    showSession: Boolean,
    showChips: Boolean,
    onToggleConnection: () -> Unit,
    onBrowseConfigs: () -> Unit,
    onPickServer: (String) -> Unit,
    onOpenSpeedTest: (() -> Unit)?,
    onOpenMap: (() -> Unit)?,
    routingMode: dev.spider.spider.domain.model.RoutingMode,
    onRoutingModeChange: (dev.spider.spider.domain.model.RoutingMode) -> Unit,
    shareLanActive: Boolean,
    shareLanPort: Int,
    onToggleShareLan: (Boolean) -> Unit,
    onRefreshInfo: () -> Unit,
    onShowLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val platform = LocalPlatform.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val health = if (live) info.fault else null
        AnimatedVisibility(visible = health != null) {
            health?.let { SpiderFaultStrip(it, onShowLogs) }
        }
        AnimatedVisibility(visible = ui.state == ConnectionState.Error) {
            SpiderErrorCard(ui.error, onRetry = onToggleConnection, onOtherServer = onBrowseConfigs, onShowLogs = onShowLogs)
        }
        ServerCard(
            active = active,
            info = info,
            live = live,
            showInfo = showConnectionInfo,
            onClick = onBrowseConfigs,
            onRefreshInfo = onRefreshInfo,
            modifier = Modifier.tourTarget(TourTargets.CONFIG_CARD),
        )
        active?.subscription?.let { sub -> SpiderSubscriptionStrip(sub, onClick = onBrowseConfigs) }
        if (showSession) {
            FactTile(
                label = stringResource(Res.string.spider_data_session),
                value = formatBytes(ui.totalDownload + ui.totalUpload),
                tone = c.cyan.takeIf { c.colorful },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        run {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).tourTarget(TourTargets.HOME_TOOLS),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showChips && onOpenSpeedTest != null && live) ActionChip(stringResource(Res.string.title_speedtest), Res.drawable.ic_fill_speed, onOpenSpeedTest, c.pink.takeIf { c.colorful })
                if (showChips && onOpenMap != null) ActionChip(stringResource(Res.string.map_title), Res.drawable.ic_fill_map, onOpenMap, c.violet.takeIf { c.colorful })
                ShareLanChip(shareLanActive, shareLanPort, onToggleShareLan)
                RuleModeChip(routingMode, onRoutingModeChange)
            }
        }
        if (localProxyAddress != null && live) {
            val copied = stringResource(Res.string.copied)
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surface)
                    .border(1.dp, c.line, RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button) {
                        platform.copyToClipboard(localProxyAddress)
                        platform.toast(copied)
                    }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Text(stringResource(Res.string.home_local_proxy, localProxyAddress), style = SpiderType.number(12.sp, FontWeight.Medium), color = c.muted, maxLines = 1)
            }
        }
        if (lockedNote != null) {
            val noteScroll = rememberScrollState()
            Box(Modifier.fillMaxWidth().clip(SpiderShapes.small()).background(c.surface).border(1.dp, c.line, SpiderShapes.small())) {
                NoteText(
                    text = lockedNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).appVerticalScroll(noteScroll).padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        if (Spider.prefs.homeQuick && picks.isNotEmpty()) {
            SpiderCaption(stringResource(Res.string.spider_fastest))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                picks.forEachIndexed { i, pick -> PickRow(pick, i, picks.size) { onPickServer(pick.id) } }
            }
        }
    }
}

@Composable
private fun ShareLanChip(active: Boolean, port: Int, onToggle: (Boolean) -> Unit) {
    val platform = LocalPlatform.current
    var showInfo by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(50),
            color = Spider.colors.surface.copy(alpha = 0.82f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Spider.colors.line),
            modifier = Modifier.combinedClickable(onClick = { onToggle(!active) }, onLongClick = { showInfo = true }),
        ) {
            Text(
                stringResource(if (active) Res.string.map_share_lan_on else Res.string.map_share_lan),
                style = SpiderType.caption(),
                color = Spider.colors.ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        if (showInfo) {
            val addresses = platform.lanIpv4Addresses()
            val body = if (addresses.isEmpty()) stringResource(Res.string.map_share_lan_no_ipv4) else addresses.joinToString("\n") {
                "SOCKS5  $it:$port"
            }
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(stringResource(Res.string.map_share_lan)) },
                text = { Text(body) },
                confirmButton = { TextButton(onClick = { showInfo = false }) { Text(stringResource(Res.string.action_ok)) } },
            )
        }
    }
}

@Composable
private fun RuleModeChip(
    mode: dev.spider.spider.domain.model.RoutingMode,
    onSelect: (dev.spider.spider.domain.model.RoutingMode) -> Unit,
) {
    val c = Spider.colors
    var expanded by remember { mutableStateOf(false) }
    val modeLabel = when (mode) {
        dev.spider.spider.domain.model.RoutingMode.Global -> Res.string.home_rule_global
        dev.spider.spider.domain.model.RoutingMode.BypassIran,
        dev.spider.spider.domain.model.RoutingMode.BypassLanAndIran -> Res.string.home_rule_ir
        else -> null
    }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = c.surface.copy(alpha = 0.82f),
            border = androidx.compose.foundation.BorderStroke(1.dp, c.line),
        ) {
            Text(
                text = modeLabel?.let { "${stringResource(Res.string.home_rule)} · ${stringResource(it)}" }
                    ?: stringResource(Res.string.home_rule),
                style = SpiderType.caption(),
                color = c.ink,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(c.surface.copy(alpha = 0.96f), RoundedCornerShape(20.dp)),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_rule_global)) },
                onClick = { onSelect(dev.spider.spider.domain.model.RoutingMode.Global); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.home_rule_ir)) },
                onClick = { onSelect(dev.spider.spider.domain.model.RoutingMode.BypassIran); expanded = false },
            )
        }
    }
}

private val ModeRailEase = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
private val ModeRailHeight = 128.dp
private val ModeRailTrackWidth = 10.dp
private val ModeRailTrackHeight = 78.dp

@Composable
private fun ModeRail(
    runMode: dev.spider.spider.domain.model.RunMode,
    tunAvailable: Boolean,
    onModeChange: (dev.spider.spider.domain.model.RunMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val platform = LocalPlatform.current
    val proxyMode = if (platform.supportsSystemProxy) {
        dev.spider.spider.domain.model.RunMode.SystemProxy
    } else {
        dev.spider.spider.domain.model.RunMode.ProxyOnly
    }
    val proxySelected = runMode != dev.spider.spider.domain.model.RunMode.Vpn || !tunAvailable
    val density = LocalDensity.current
    val trackPx = with(density) { ModeRailTrackHeight.toPx() }
    val trackTopPx = with(density) { ((ModeRailHeight - ModeRailTrackHeight) / 2).toPx() }

    var dragging by remember { mutableStateOf(false) }
    var dragLevel by remember { mutableFloatStateOf(if (proxySelected) 0f else 1f) }
    val level by animateFloatAsState(
        targetValue = when {
            dragging -> dragLevel
            proxySelected -> 0f
            else -> 1f
        },
        animationSpec = if (dragging) snap() else tween(500, easing = ModeRailEase),
        label = "mode-rail-liquid",
    )

    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val faint = c.ink.copy(alpha = 0.42f)
    val lineTint by animateColorAsState(
        if (hovered && tunAvailable) faint else c.line,
        tween(140),
        label = "mode-rail-line",
    )
    val proxyTint by animateColorAsState(
        if (proxySelected) c.ink else faint,
        tween(140),
        label = "mode-rail-proxy",
    )
    val tunTint by animateColorAsState(
        if (!proxySelected) c.ink else faint.copy(alpha = if (tunAvailable) 1f else .35f),
        tween(140),
        label = "mode-rail-tun",
    )

    Box(
        modifier
            .width(30.dp)
            .height(ModeRailHeight)
            .verticalResizeCursor(tunAvailable)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, lineTint, CircleShape)
            .toggleable(
                value = !proxySelected,
                enabled = tunAvailable,
                role = Role.Switch,
                interactionSource = source,
                indication = ripple(color = c.ink),
                onValueChange = { tun ->
                    onModeChange(if (tun) dev.spider.spider.domain.model.RunMode.Vpn else proxyMode)
                },
            )
            .pointerInput(tunAvailable) {
                if (!tunAvailable) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { point ->
                        dragging = true
                        dragLevel = ((point.y - trackTopPx) / trackPx).coerceIn(0f, 1f)
                    },
                    onVerticalDrag = { change, amount ->
                        dragLevel = (dragLevel + amount / trackPx).coerceIn(0f, 1f)
                        change.consume()
                    },
                    onDragEnd = {
                        val chooseTun = dragLevel >= .5f
                        dragging = false
                        onModeChange(if (chooseTun) dev.spider.spider.domain.model.RunMode.Vpn else proxyMode)
                    },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .width(ModeRailTrackWidth)
                .height(ModeRailTrackHeight)
                .clip(CircleShape)
                .background(c.ground),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ModeRailTrackHeight)
                    .offset { IntOffset(0, (level * trackPx).roundToInt()) }
                    .background(c.muted),
            )
            Box(Modifier.matchParentSize().border(1.dp, c.line, CircleShape))
        }
        Text(
            "P",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            style = SpiderType.number(9.sp, FontWeight.SemiBold),
            color = proxyTint,
            textAlign = TextAlign.Center,
        )
        Text(
            "T",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            style = SpiderType.number(9.sp, FontWeight.SemiBold),
            color = tunTint,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PanelPromo(
    onClick: () -> Unit,
    onSecretUnlocked: () -> Unit,
    onBurst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(c.surface)
                .border(1.dp, c.line, RoundedCornerShape(18.dp))
                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = c.ink.copy(alpha = .08f), spotColor = c.ink.copy(alpha = .08f))
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.coral.container)
                .combinedClickable(
                    onClick = {
                        val now = dev.spider.spider.platform.currentTimeMillis()
                        taps = if (now - lastTap < TAP_WINDOW_MS) taps + 1 else 1
                        lastTap = now
                        if (taps >= TITLE_TAPS) { taps = 0; onSecretUnlocked() }
                        else if (taps == BURST_TAPS) onBurst()
                        else onClick()
                    },
                    onLongClick = onSecretUnlocked,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(Res.drawable.ic_spider_glyph), contentDescription = null, tint = c.bad, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(stringResource(Res.string.home_promo_title), style = SpiderType.rowTitle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
            Text(stringResource(Res.string.home_promo_body), style = SpiderType.caption(), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
        }
        Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = stringResource(Res.string.home_promo_action), tint = c.muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DotBackdrop(modifier: Modifier = Modifier) {
    val c = Spider.colors
    Canvas(modifier) {
        val step = 20.dp.toPx()
        var y = 8.dp.toPx()
        while (y < size.height) {
            var x = 8.dp.toPx()
            while (x < size.width) {
                drawCircle(c.muted.copy(alpha = .075f), 0.8.dp.toPx(), Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun SpiderGlobeButton(
    state: ConnectionState,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val connected = state == ConnectionState.Connected
    val connecting = state == ConnectionState.Connecting || state == ConnectionState.Reconnecting
    val pointColor = when {
        connected -> c.good
        connecting -> Color(0xFF6C9DD1)
        else -> c.bad
    }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (!animate) {
            rotation.snapTo(0f)
        } else {
            while (true) {
                rotation.animateTo(rotation.value + (2f * kotlin.math.PI.toFloat()), tween(28_000))
            }
        }
    }
    Box(
        modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * .40f
            val angle = rotation.value
            val cy = kotlin.math.cos(angle)
            val sy = kotlin.math.sin(angle)
            val goldenAngle = kotlin.math.PI.toFloat() * (3f - kotlin.math.sqrt(5f))
            val count = 520
            for (index in 0 until count) {
                val y = 1f - 2f * (index + .5f) / count
                val ring = kotlin.math.sqrt((1f - y * y).coerceAtLeast(0f))
                val theta = goldenAngle * index
                val x = kotlin.math.cos(theta) * ring
                val z = kotlin.math.sin(theta) * ring
                val rotatedX = x * cy + z * sy
                val depth = -x * sy + z * cy
                val px = center.x + rotatedX * radius
                val py = center.y + y * radius
                val opacity = if (depth > 0f) .35f + depth * .55f else .08f + (depth + 1f) * .12f
                val dotSize = (.65f + (depth.coerceAtLeast(0f) * .65f)).dp.toPx()
                drawCircle(pointColor.copy(alpha = opacity), dotSize, Offset(px, py))
            }
            drawCircle(pointColor.copy(alpha = .10f), radius * 1.08f, center, style = Stroke(1.dp.toPx()))
            drawOval(
                color = pointColor.copy(alpha = .16f),
                topLeft = Offset(center.x - radius * .42f, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * .84f, radius * 2f),
                style = Stroke(1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun ServerCard(
    active: SpiderActiveServer?,
    info: NetworkInfo,
    live: Boolean,
    showInfo: Boolean,
    onClick: () -> Unit,
    onRefreshInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spider.colors
    val tone = c.stageTone
    SpiderCard(
        tone = tone,
        modifier = modifier.fillMaxWidth(),
        shape = SpiderShapes.card(),
        onClick = onClick,
        motif = null,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(stringResource(Res.string.spider_current_server).uppercase(), style = SpiderType.caption(), color = tone.content.copy(alpha = 0.8f))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val country = (if (live && showInfo) info.countryCode else null) ?: active?.country
            if (country != null || active == null) {
                SpiderFlag(
                    countryCode = country,
                    size = 50.dp,
                    corner = 17.dp,
                    container = c.chipOnStage,
                    content = tone.content,
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    active?.name ?: stringResource(Res.string.spider_no_server),
                    style = SpiderType.title(27.sp),
                    color = tone.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
                Text(
                    active?.detail ?: if (active == null) stringResource(Res.string.spider_no_server_hint) else "",
                    style = SpiderType.body(),
                    color = tone.content.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                )
            }
            if (active?.locked == true) {
                Icon(painterResource(Res.drawable.ic_lock), contentDescription = null, tint = tone.content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = tone.content,
                modifier = Modifier.size(22.dp),
            )
        }
        }
        if (active != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (live && showInfo) {
                    val ping = info.pingMs
                    Box(Modifier.clip(RoundedCornerShape(15.dp)).clickable(role = Role.Button, onClick = onRefreshInfo)) {
                        SpiderPill(
                            text = ping?.let { ltrIsolate("%d ms".format(it)) } ?: "…",
                            container = c.chipOnStage,
                            content = tone.content,
                            dot = when {
                                ping == null -> tone.content.copy(alpha = 0.4f)
                                ping < 300 -> c.goodOnStage
                                ping < 800 -> c.mid
                                else -> c.bad
                            },
                            numeric = true,
                        )
                    }
                }
                SpiderPill(active.engine, c.lime, c.onLime)
                val ip = if (live && showInfo) info.ipv4 ?: info.ipv6 else null
                if (ip != null) {
                    SpiderPill(ip, c.chipOnStage, tone.content, numeric = true)
                }
            }
        }
    }
}

@Composable
private fun SpeedTile(
    label: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    bps: Long,
    history: List<Float>,
    tone: SpiderTone,
    live: Boolean,
    modifier: Modifier = Modifier,
) {
    val (value, unit) = formatRate(bps)
    Column(
        modifier
            .height(122.dp)
            .clip(SpiderShapes.block())
            .background(tone.container)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = tone.content, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = tone.content, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                value,
                style = SpiderType.number(36.sp),
                color = tone.content.copy(alpha = if (live) 1f else 0.6f),
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                unit,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = tone.content.copy(alpha = 0.8f),
                modifier = Modifier.alignByBaseline(),
            )
        }
        Sparkline(history, tone.content, Modifier.fillMaxWidth().height(22.dp))
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val path = remember { Path() }
    val fill = remember { Path() }
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = values.max()
        val min = values.min()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val pad = 3.dp.toPx()
        path.reset()
        fill.reset()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = if (max <= 0f) size.height - pad else size.height - pad - (v - min) / span * (size.height - pad * 2)
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        scale(if (rtl) -1f else 1f, 1f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0f))))
            drawPath(path, color.copy(alpha = 0.6f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun FactTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tone: SpiderTone? = null,
) {
    val c = Spider.colors
    Column(
        modifier
            .height(70.dp)
            .clip(SpiderShapes.small())
            .background(tone?.container ?: c.surface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = SpiderType.body().copy(fontSize = 12.5.sp), color = tone?.content?.copy(alpha = 0.8f) ?: c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(ltrIsolate(value), style = SpiderType.number(20.sp), color = tone?.content ?: c.ink, maxLines = 1)
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    onClick: () -> Unit,
    tone: SpiderTone? = null,
) {
    val c = Spider.colors
    val ink = tone?.content ?: c.ink
    Row(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tone?.container ?: c.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1)
    }
}

@Composable
private fun PickRow(pick: SpiderServerPick, index: Int, count: Int, onClick: () -> Unit) {
    val c = Spider.colors
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
        if (pick.country != null) {
            SpiderFlag(pick.country, size = 38.dp, corner = 13.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(pick.name, style = SpiderType.rowTitle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pick.detail, style = SpiderType.body().copy(fontSize = 12.5.sp), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        pick.pingMs?.let { ms ->
            Spacer(Modifier.width(8.dp))
            Text(if (ms > 0) ltrIsolate("%d ms".format(ms)) else "—", style = SpiderType.number(13.5.sp), color = c.pingColor(ms))
        }
    }
}

@Composable
private fun SpiderFaultStrip(fault: ConnectionFault, onShowLogs: () -> Unit) {
    val c = Spider.colors
    val known = fault.cause != ConnectionFault.Cause.Unknown
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SpiderShapes.small())
            .background(c.coral.container)
            .clickable(role = Role.Button, onClick = onShowLogs)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (known) faultTitle(fault.cause) else Res.string.state_error),
                style = SpiderType.rowTitle(),
                color = c.coral.content,
            )
            Text(
                stringResource(if (known) faultHint(fault.cause) else Res.string.fault_unknown),
                style = SpiderType.body(),
                color = c.coral.content.copy(alpha = 0.85f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(painterResource(Res.drawable.ic_description), contentDescription = stringResource(Res.string.fault_open_logs), tint = c.coral.content, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SpiderErrorCard(
    error: String?,
    onRetry: () -> Unit,
    onOtherServer: () -> Unit,
    onShowLogs: () -> Unit,
) {
    val c = Spider.colors
    val fault = error?.takeIf { it.isNotBlank() }?.let { ConnectionFault.classify(it) }
    val known = fault != null && fault.cause != ConnectionFault.Cause.Unknown
    val shake = remember { Animatable(0f) }
    var rawOpen by remember(error) { mutableStateOf(false) }
    LaunchedEffect(error) { for (x in listOf(10f, -8f, 6f, -3f, 1.5f, 0f)) shake.animateTo(x, tween(55)) }
    val density = LocalDensity.current
    SpiderCard(
        tone = c.coral,
        modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value * density.density },
        shape = SpiderShapes.hero(),
        motif = null,
        motifSize = 150.dp,
        motifRotation = -12f,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(if (known) faultTitle(fault!!.cause) else Res.string.state_error),
            style = SpiderType.title(24.sp),
            color = c.coral.content,
        )
        Text(
            stringResource(if (known) faultHint(fault!!.cause) else Res.string.fault_unknown),
            style = SpiderType.body(),
            color = c.coral.content.copy(alpha = 0.85f),
        )
        if (fault != null && fault.raw.isNotBlank()) {
            Text(
                fault.raw,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, textDirection = TextDirection.Ltr),
                color = c.coral.content.copy(alpha = 0.7f),
                maxLines = if (rawOpen) 10 else 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier = Modifier.clickable { rawOpen = !rawOpen },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(stringResource(Res.string.action_retry), c.lime, c.onLime, onRetry)
            PillButton(stringResource(Res.string.pixel_other_server), Color.Transparent, c.coral.content, onOtherServer, outline = c.coral.content.copy(alpha = 0.4f))
            Spacer(Modifier.weight(1f))
            SpiderIconButton(
                Res.drawable.ic_description,
                stringResource(Res.string.fault_open_logs),
                onShowLogs,
                container = Color.Transparent,
                content = c.coral.content,
                size = 40.dp,
            )
        }
    }
}

@Composable
private fun PillButton(label: String, container: Color, content: Color, onClick: () -> Unit, outline: Color? = null) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(container)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}
