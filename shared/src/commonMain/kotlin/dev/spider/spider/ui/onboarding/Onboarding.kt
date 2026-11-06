@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.spider.spider.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spider.spider.domain.model.AppLanguage
import dev.spider.spider.domain.model.AppSettings
import dev.spider.spider.domain.model.SpiderConnect
import dev.spider.spider.domain.model.RoutingPresets
import dev.spider.spider.domain.model.RulesetItem
import dev.spider.spider.domain.model.ThemeMode
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.*
import dev.spider.spider.ui.spider.Spider
import dev.spider.spider.ui.spider.SpiderBadge
import dev.spider.spider.ui.spider.SpiderCard
import dev.spider.spider.ui.spider.SpiderCaption
import dev.spider.spider.ui.spider.SpiderFlag
import dev.spider.spider.ui.spider.SpiderPill
import dev.spider.spider.ui.spider.SpiderSearchField
import dev.spider.spider.ui.spider.SpiderShapes
import dev.spider.spider.ui.spider.SpiderType
import dev.spider.spider.ui.spider.PalettePicker
import dev.spider.spider.ui.components.MorphingBlob
import dev.spider.spider.ui.platform.appVerticalScroll
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val ONBOARDING_VERSION = 2

private const val LAST_STEP = 4

private enum class RegionSource { Sim, MobileNetwork, Network, Language }

private data class RegionGuess(val code: String, val source: RegionSource)

private fun localGuesses(platform: dev.spider.spider.ui.platform.Platform): List<RegionGuess> = buildList {
    platform.simCountry?.let { add(RegionGuess(it, RegionSource.Sim)) }
    platform.mobileNetworkCountry?.let { add(RegionGuess(it, RegionSource.MobileNetwork)) }
    java.util.Locale.getDefault().country.uppercase()
        .takeIf { it.length == 2 && it.all { ch -> ch in 'A'..'Z' } }
        ?.let { add(RegionGuess(it, RegionSource.Language)) }
}

@Composable
private fun rememberRegionGuesses(): List<RegionGuess> {
    val platform = dev.spider.spider.ui.platform.LocalPlatform.current
    var guesses by remember { mutableStateOf(localGuesses(platform)) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val viaVpn = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            dev.spider.spider.core.VpnManager.deviceVpnActive()
        }
        if (viaVpn) return@LaunchedEffect
        val info = kotlinx.coroutines.withTimeoutOrNull(NETWORK_LOOKUP_MS) {
            runCatching { dev.spider.spider.data.net.NetworkInfoRepository().fetchInfo(force = true) }.getOrNull()
        } ?: return@LaunchedEffect
        if (info.underVpn) return@LaunchedEffect
        val code = info.countryCode?.trim()?.uppercase()?.takeIf { it.length == 2 } ?: return@LaunchedEffect
        guesses = guesses + RegionGuess(code, RegionSource.Network)
    }
    return guesses
}

private const val NETWORK_LOOKUP_MS = 8_000L

@Composable
fun OnboardingFlow(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onStartTour: () -> Unit,
    onFinish: () -> Unit,
    replay: Boolean = false,
) {
    val c = Spider.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var region by rememberSaveable { mutableStateOf(settings.regionCode) }
    var picked by rememberSaveable { mutableStateOf(settings.regionCode.isNotBlank()) }
    val guesses = rememberRegionGuesses()
    val best = guesses.minByOrNull { it.source.ordinal }
    androidx.compose.runtime.LaunchedEffect(best) {
        if (!picked && best != null) region = best.code
    }
    var directLocal by rememberSaveable { mutableStateOf(true) }
    var blockAds by rememberSaveable { mutableStateOf(true) }
    var blockQuic by rememberSaveable { mutableStateOf(true) }
    var directTorrent by rememberSaveable { mutableStateOf(true) }
    var proxyAll by rememberSaveable { mutableStateOf(false) }
    var applyRules by rememberSaveable { mutableStateOf(!replay) }

    val options = RoutingPresets.RegionOptions(directLocal, blockAds, blockQuic, directTorrent, proxyAll)
    val preview = remember(region, options) { RoutingPresets.forRegion(region, options) }

    fun commit() {
        onUpdate {
            val next = it.copy(regionCode = region)
            if (!applyRules) next
            else next.copy(
                rulesets = RoutingPresets.apply(it.rulesets, RoutingPresets.forRegion(region, options)),
                routingSeeded = true,
                routingMigrated = true,
            )
        }
    }

    Box(Modifier.fillMaxSize().background(c.ground)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 12.dp, bottom = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                StepTrail(step, LAST_STEP + 1, Modifier.weight(1f))
                if (step < LAST_STEP) {
                    Text(
                        stringResource(Res.string.onb_skip_tour),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.muted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button, onClick = onFinish)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { w -> if (forward) w / 5 else -w / 5 } + fadeIn()) togetherWith
                        (slideOutHorizontally { w -> if (forward) -w / 5 else w / 5 } + fadeOut())
                },
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(settings, onLanguage)
                    1 -> RegionStep(
                        region = region,
                        detectedBy = if (picked) null else guesses.filter { it.code == region }.minByOrNull { it.source.ordinal }?.source,
                        guesses = guesses,
                    ) {
                        region = it
                        picked = true
                    }
                    2 -> RulesStep(
                        region = region,
                        preview = preview,
                        replay = replay,
                        applyRules = applyRules,
                        onApplyRules = { applyRules = it },
                        directLocal = directLocal,
                        onDirectLocal = { directLocal = it },
                        blockAds = blockAds,
                        onBlockAds = { blockAds = it },
                        blockQuic = blockQuic,
                        onBlockQuic = { blockQuic = it },
                        directTorrent = directTorrent,
                        onDirectTorrent = { directTorrent = it },
                        proxyAll = proxyAll,
                        onProxyAll = { proxyAll = it },
                    )
                    3 -> LookStep(settings, onUpdate)
                    else -> ReadyStep(region, applyRules, preview.size)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step > 0) {
                    FlowButton(stringResource(Res.string.onb_back), primary = false) { step-- }
                }
                Spacer(Modifier.weight(1f))
                if (step == LAST_STEP) {
                    FlowButton(stringResource(Res.string.onb_finish), primary = false) {
                        commit()
                        onFinish()
                    }
                    FlowButton(stringResource(Res.string.onb_start_tour), primary = true) {
                        commit()
                        onStartTour()
                    }
                } else {
                    FlowButton(stringResource(Res.string.onb_next), primary = true, enabled = step != 1 || region.isNotBlank()) { step++ }
                }
            }
        }
    }
}

@Composable
private fun StepTrail(current: Int, total: Int, modifier: Modifier) {
    val c = Spider.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val width by animateDpAsState(if (i == current) 34.dp else 10.dp, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow), label = "trail-width")
            val color by animateColorAsState(if (i <= current) c.lime else c.surface2, label = "trail-color")
            Box(Modifier.height(10.dp).width(width).clip(RoundedCornerShape(5.dp)).background(color))
        }
    }
}

@Composable
private fun FlowButton(label: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Spider.colors
    val container by animateColorAsState(
        when {
            !enabled -> c.surface2
            primary -> c.lime
            else -> c.surface
        },
        label = "flow-button-bg",
    )
    Box(
        Modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(container)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !enabled -> c.muted
                primary -> c.onLime
                else -> c.ink
            },
        )
    }
}

@Composable
private fun StepTitle(title: String, body: String) {
    val c = Spider.colors
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = SpiderType.title(34.sp), color = c.ink)
        Text(body, style = SpiderType.body().copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.muted)
    }
}

@Composable
private fun WelcomeStep(settings: AppSettings, onLanguage: (AppLanguage) -> Unit) {
    val c = Spider.colors
    Column(
        Modifier.fillMaxSize().appVerticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpiderCard(
            tone = c.stageTone,
            shape = SpiderShapes.card(),
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MorphingBlob(
                progress = 0.35f,
                brush = androidx.compose.ui.graphics.SolidColor(c.lime),
                startShape = MaterialShapes.Cookie9Sided,
                endShape = MaterialShapes.SoftBurst,
                modifier = Modifier.size(76.dp),
            ) {
                Image(painterResource(Res.drawable.ic_spider_glyph), contentDescription = null, modifier = Modifier.size(34.dp))
            }
            Text(stringResource(Res.string.app_name), style = SpiderType.title(40.sp), color = c.onStage)
            Text(stringResource(Res.string.onb_welcome_body), style = SpiderType.body().copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.onStage.copy(alpha = 0.88f))
        }
        SpiderCaption(stringResource(Res.string.onb_lang_title))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
            listOf(
                AppLanguage.System to stringResource(Res.string.settings_language_system),
                AppLanguage.Persian to "فارسی",
                AppLanguage.English to "English",
                AppLanguage.Chinese to "中文",
                AppLanguage.Russian to "Русский",
                AppLanguage.Spanish to "Español",
            ).forEach { (lang, label) ->
                ChoiceTile(label, settings.language == lang, Modifier.weight(1f)) {
                    if (settings.language != lang) onLanguage(lang)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChoiceTile(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Spider.colors
    val container by animateColorAsState(if (selected) c.lime else c.surface, label = "choice-bg")
    val content by animateColorAsState(if (selected) c.onLime else c.ink, label = "choice-fg")
    Box(
        modifier
            .heightIn(min = 58.dp)
            .clip(SpiderShapes.small())
            .background(container)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun regionSourceLabel(source: RegionSource): String = stringResource(
    when (source) {
        RegionSource.Sim -> Res.string.onb_region_from_sim
        RegionSource.MobileNetwork -> Res.string.onb_region_from_mobile
        RegionSource.Network -> Res.string.onb_region_from_network
        RegionSource.Language -> Res.string.onb_region_from_language
    },
)

@Composable
private fun RegionStep(region: String, detectedBy: RegionSource?, guesses: List<RegionGuess>, onPick: (String) -> Unit) {
    val c = Spider.colors
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { java.util.Locale.getISOCountries().toList() }
    val named = remember(all) { all.map { it to dev.spider.spider.ui.servers.countryName(it) }.sortedBy { it.second.lowercase() } }
    val needle = query.trim().lowercase()
    val shown = if (needle.isEmpty()) named else named.filter { (code, name) -> name.lowercase().contains(needle) || code.lowercase() == needle }
    Column(Modifier.fillMaxSize()) {
        StepTitle(stringResource(Res.string.onb_region_title), stringResource(Res.string.onb_region_body))
        Spacer(Modifier.height(12.dp))
        if (region.isBlank()) {
            Text(
                stringResource(Res.string.onb_region_needed),
                style = SpiderType.body(),
                color = c.lime,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
        } else {
            SpiderCard(
                tone = c.stageTone,
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = SpiderShapes.small(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpiderFlag(region, size = 46.dp, corner = 15.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            detectedBy?.let { stringResource(Res.string.onb_region_detected, regionSourceLabel(it)) }
                                ?: stringResource(Res.string.onb_region_picked),
                            style = SpiderType.caption(),
                            color = c.onStage.copy(alpha = 0.8f),
                        )
                        Text(dev.spider.spider.ui.servers.countryName(region), style = SpiderType.title(22.sp), color = c.onStage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (detectedBy != null) {
                            Text(stringResource(Res.string.onb_region_change_hint), style = SpiderType.body().copy(fontSize = 12.5.sp), color = c.onStage.copy(alpha = 0.75f))
                        }
                    }
                    SpiderPill(region, c.lime, c.onLime, numeric = true)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        val others = guesses.distinctBy { it.code }.filter { it.code != region }
        if (others.isNotEmpty()) {
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                others.forEach { guess ->
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surface)
                            .clickable(role = Role.Button) { onPick(guess.code) }
                            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SpiderFlag(guess.code, size = 26.dp, corner = 9.dp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(dev.spider.spider.ui.servers.countryName(guess.code), style = MaterialTheme.typography.labelLarge, color = c.ink, maxLines = 1)
                            Text(regionSourceLabel(guess.source), style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        SpiderSearchField(query, { query = it }, stringResource(Res.string.onb_region_search), Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        val list = rememberLazyListState()
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(shown, key = { it.first }) { (code, name) ->
                val active = code == region
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(SpiderShapes.small())
                        .background(if (active) c.selected else c.surface)
                        .semantics { selected = active }
                        .clickable(role = Role.RadioButton) { onPick(code) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpiderFlag(code, size = 34.dp, corner = 11.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(code, style = SpiderType.number(13.sp), color = c.muted)
                }
            }
        }
    }
}

@Composable
private fun RulesStep(
    region: String,
    preview: List<RulesetItem>,
    replay: Boolean,
    applyRules: Boolean,
    onApplyRules: (Boolean) -> Unit,
    directLocal: Boolean,
    onDirectLocal: (Boolean) -> Unit,
    blockAds: Boolean,
    onBlockAds: (Boolean) -> Unit,
    blockQuic: Boolean,
    onBlockQuic: (Boolean) -> Unit,
    directTorrent: Boolean,
    onDirectTorrent: (Boolean) -> Unit,
    proxyAll: Boolean,
    onProxyAll: (Boolean) -> Unit,
) {
    val c = Spider.colors
    val country = if (region.isBlank()) "" else dev.spider.spider.ui.servers.countryName(region)
    Column(
        Modifier.fillMaxSize().appVerticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StepTitle(stringResource(Res.string.onb_rules_title), stringResource(Res.string.onb_rules_body))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (replay) {
                RuleSwitch(stringResource(Res.string.onb_rules_apply), stringResource(Res.string.onb_rules_apply_sum), applyRules, true, onApplyRules)
            }
            val live = applyRules
            RuleSwitch(
                stringResource(Res.string.onb_rules_proxy_all),
                stringResource(Res.string.onb_rules_proxy_all_sum),
                proxyAll,
                live,
                onProxyAll,
            )
            if (region.isNotBlank()) {
                RuleSwitch(
                    stringResource(Res.string.onb_rules_local, country),
                    stringResource(Res.string.onb_rules_local_sum),
                    directLocal && !proxyAll,
                    live && !proxyAll,
                    onDirectLocal,
                    flag = region,
                )
            }
            RuleSwitch(stringResource(Res.string.onb_rules_ads), stringResource(Res.string.onb_rules_ads_sum), blockAds, live, onBlockAds)
            RuleSwitch(stringResource(Res.string.onb_rules_quic), stringResource(Res.string.onb_rules_quic_sum), blockQuic, live, onBlockQuic)
            RuleSwitch(stringResource(Res.string.onb_rules_torrent), stringResource(Res.string.onb_rules_torrent_sum), directTorrent && !proxyAll, live && !proxyAll, onDirectTorrent)
            if (live) {
                SpiderCard(tone = c.surfaceTone, shape = SpiderShapes.small(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.onb_rules_preview, preview.size), style = SpiderType.caption(), color = c.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        preview.forEach { rule ->
                            val (bg, fg) = when (rule.outboundTag) {
                                RulesetItem.OUTBOUND_BLOCK -> c.pink.container to c.pink.content
                                RulesetItem.OUTBOUND_DIRECT -> c.cyan.container to c.cyan.content
                                else -> c.violet.container to c.violet.content
                            }
                            SpiderPill(rule.remarks, bg, fg)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RuleSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    flag: String? = null,
) {
    val c = Spider.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(SpiderShapes.small())
            .background(c.surface)
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flag != null) {
            SpiderFlag(flag, size = 36.dp, corner = 12.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLargeEmphasized, color = if (enabled) c.ink else c.muted)
            Text(summary, style = SpiderType.body().copy(fontSize = 12.5.sp), color = c.muted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = c.lime, checkedThumbColor = c.onLime),
        )
    }
}

@Composable
private fun LookStep(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    Column(
        Modifier.fillMaxSize().appVerticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepTitle(stringResource(Res.string.onb_look_title), stringResource(Res.string.onb_look_body))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SpiderCaption(stringResource(Res.string.onb_theme_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ThemeMode.System to stringResource(Res.string.settings_theme_system),
                    ThemeMode.Light to stringResource(Res.string.settings_theme_light),
                    ThemeMode.Dark to stringResource(Res.string.settings_theme_dark),
                ).forEach { (mode, label) ->
                    ChoiceTile(label, settings.themeMode == mode, Modifier.weight(1f)) { onUpdate { it.copy(themeMode = mode) } }
                }
            }
        }
        Box(Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(22.dp))) {
            PalettePicker(settings, settings.spider.palette) { palette -> onUpdate { it.copy(spider = it.spider.copy(palette = palette)) } }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SpiderCaption(stringResource(Res.string.spider_connect_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                listOf(
                    SpiderConnect.Slide to stringResource(Res.string.spider_connect_slide),
                    SpiderConnect.Morph to stringResource(Res.string.spider_connect_morph),
                    SpiderConnect.Rail to stringResource(Res.string.spider_connect_rail),
                    SpiderConnect.Hold to stringResource(Res.string.spider_connect_hold),
                    SpiderConnect.Tile to stringResource(Res.string.spider_connect_tile),
                    SpiderConnect.Button to stringResource(Res.string.spider_connect_button),
                ).forEach { (style, label) ->
                    ChoiceTile(label, settings.spider.connect == style, Modifier.weight(1f)) {
                        onUpdate { it.copy(spider = it.spider.copy(connect = style)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ReadyStep(region: String, applyRules: Boolean, rules: Int) {
    val c = Spider.colors
    Column(
        Modifier.fillMaxSize().appVerticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpiderCard(
            tone = c.limeTone,
            shape = SpiderShapes.card(),
            motif = MaterialShapes.Sunny,
            motifSize = 210.dp,
            contentPadding = PaddingValues(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SpiderBadge(
                shape = MaterialShapes.Cookie6Sided,
                container = c.onLime,
                content = c.lime,
                icon = Res.drawable.ic_check,
                size = 58.dp,
            )
            Text(stringResource(Res.string.onb_ready_title), style = SpiderType.title(34.sp), color = c.onLime)
            Text(stringResource(Res.string.onb_tour_body), style = SpiderType.body().copy(fontSize = 15.sp, lineHeight = 22.sp), color = c.onLime.copy(alpha = 0.85f))
        }
        if (region.isNotBlank()) {
            SpiderCard(tone = c.surfaceTone, shape = SpiderShapes.small(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SpiderFlag(region, size = 40.dp, corner = 13.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(dev.spider.spider.ui.servers.countryName(region), style = MaterialTheme.typography.titleMedium, color = c.ink)
                        Text(
                            if (applyRules) stringResource(Res.string.onb_ready_rules, rules) else stringResource(Res.string.onb_ready_kept),
                            style = SpiderType.body(),
                            color = c.muted,
                        )
                    }
                }
            }
        }
        Text(stringResource(Res.string.onb_tour_note), style = SpiderType.body(), color = c.muted)
    }
}
