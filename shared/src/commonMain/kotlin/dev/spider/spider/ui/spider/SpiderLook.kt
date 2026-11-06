package dev.spider.spider.ui.spider

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spider.spider.domain.model.AppSettings
import dev.spider.spider.domain.model.SpiderConnect
import dev.spider.spider.domain.model.SpiderCorners
import dev.spider.spider.domain.model.SpiderDensity
import dev.spider.spider.domain.model.SpiderPalette
import dev.spider.spider.domain.model.SpiderPrefs
import dev.spider.spider.domain.model.SpiderTitle
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.spider_connect_button
import dev.spider.spider.shared.resources.spider_connect_hold
import dev.spider.spider.shared.resources.spider_connect_morph
import dev.spider.spider.shared.resources.spider_connect_rail
import dev.spider.spider.shared.resources.spider_connect_tile
import dev.spider.spider.shared.resources.spider_connect_slide
import dev.spider.spider.shared.resources.spider_connect_title
import dev.spider.spider.shared.resources.spider_corners_leaf
import dev.spider.spider.shared.resources.spider_corners_round
import dev.spider.spider.shared.resources.spider_corners_soft
import dev.spider.spider.shared.resources.spider_corners_title
import dev.spider.spider.shared.resources.spider_density_comfortable
import dev.spider.spider.shared.resources.spider_density_compact
import dev.spider.spider.shared.resources.spider_density_title
import dev.spider.spider.shared.resources.spider_group_board
import dev.spider.spider.shared.resources.spider_group_home
import dev.spider.spider.shared.resources.spider_group_look
import dev.spider.spider.shared.resources.spider_home_chips
import dev.spider.spider.shared.resources.spider_home_quick
import dev.spider.spider.shared.resources.spider_home_session
import dev.spider.spider.shared.resources.spider_home_speed
import dev.spider.spider.shared.resources.spider_indexes
import dev.spider.spider.shared.resources.spider_indexes_sum
import dev.spider.spider.shared.resources.spider_live_tiles
import dev.spider.spider.shared.resources.spider_live_tiles_sum
import dev.spider.spider.shared.resources.spider_motifs
import dev.spider.spider.shared.resources.spider_motifs_sum
import dev.spider.spider.shared.resources.spider_palette_accent
import dev.spider.spider.shared.resources.spider_palette_brand
import dev.spider.spider.shared.resources.spider_palette_mono
import dev.spider.spider.shared.resources.spider_palette_citrus
import dev.spider.spider.shared.resources.spider_palette_matcha
import dev.spider.spider.shared.resources.spider_palette_neon
import dev.spider.spider.shared.resources.spider_palette_pistachio
import dev.spider.spider.shared.resources.spider_palette_ocean
import dev.spider.spider.shared.resources.spider_palette_sunset
import dev.spider.spider.shared.resources.spider_palette_berry
import dev.spider.spider.shared.resources.spider_palette_pastel
import dev.spider.spider.shared.resources.spider_palette_title
import dev.spider.spider.shared.resources.spider_palette_vivid
import dev.spider.spider.shared.resources.spider_playful
import dev.spider.spider.shared.resources.spider_playful_sum
import dev.spider.spider.shared.resources.spider_reset_look
import dev.spider.spider.shared.resources.spider_reset_tiles
import dev.spider.spider.shared.resources.spider_title_compact
import dev.spider.spider.shared.resources.spider_title_huge
import dev.spider.spider.shared.resources.spider_title_large
import dev.spider.spider.shared.resources.spider_title_title
import dev.spider.spider.shared.resources.spider_wave
import dev.spider.spider.shared.resources.spider_wave_sum
import dev.spider.spider.shared.resources.ic_check
import dev.spider.spider.ui.settings.SettingsActionRow
import dev.spider.spider.ui.settings.SettingsChoiceRow
import dev.spider.spider.ui.settings.SettingsListRow
import dev.spider.spider.ui.settings.SettingsGroup
import dev.spider.spider.ui.settings.SettingsSegment
import dev.spider.spider.ui.settings.SettingsSwitchRow
import dev.spider.spider.ui.theme.accentSeed
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SpiderLookSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val prefs = settings.spider
    fun edit(change: (SpiderPrefs) -> SpiderPrefs) = onUpdate { it.copy(spider = change(it.spider)) }

    SettingsGroup(stringResource(Res.string.spider_group_look)) {
        SettingsChoiceRow(
            title = stringResource(Res.string.spider_corners_title),
            summary = null,
            options = listOf(
                SpiderCorners.Leaf to stringResource(Res.string.spider_corners_leaf),
                SpiderCorners.Round to stringResource(Res.string.spider_corners_round),
                SpiderCorners.Soft to stringResource(Res.string.spider_corners_soft),
            ),
            selected = prefs.corners,
            onSelected = { v -> edit { it.copy(corners = v) } },
        )
        SettingsChoiceRow(
            title = stringResource(Res.string.spider_title_title),
            summary = null,
            options = listOf(
                SpiderTitle.Huge to stringResource(Res.string.spider_title_huge),
                SpiderTitle.Large to stringResource(Res.string.spider_title_large),
                SpiderTitle.Compact to stringResource(Res.string.spider_title_compact),
            ),
            selected = prefs.title,
            onSelected = { v -> edit { it.copy(title = v) } },
        )
        SettingsChoiceRow(
            title = stringResource(Res.string.spider_density_title),
            summary = null,
            options = listOf(
                SpiderDensity.Comfortable to stringResource(Res.string.spider_density_comfortable),
                SpiderDensity.Compact to stringResource(Res.string.spider_density_compact),
            ),
            selected = prefs.density,
            onSelected = { v -> edit { it.copy(density = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_motifs),
            summary = stringResource(Res.string.spider_motifs_sum),
            checked = prefs.motifs,
            onCheckedChange = { v -> edit { it.copy(motifs = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_playful),
            summary = stringResource(Res.string.spider_playful_sum),
            checked = prefs.playfulTouch,
            onCheckedChange = { v -> edit { it.copy(playfulTouch = v) } },
        )
    }

    SettingsGroup(stringResource(Res.string.spider_group_board)) {
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_indexes),
            summary = stringResource(Res.string.spider_indexes_sum),
            checked = prefs.indexes,
            onCheckedChange = { v -> edit { it.copy(indexes = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_live_tiles),
            summary = stringResource(Res.string.spider_live_tiles_sum),
            checked = prefs.liveTiles,
            onCheckedChange = { v -> edit { it.copy(liveTiles = v) } },
        )
        SettingsActionRow(title = stringResource(Res.string.spider_reset_tiles)) { edit { it.copy(tiles = "") } }
    }

    SettingsGroup(stringResource(Res.string.spider_group_home)) {
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_home_speed),
            checked = prefs.homeSpeed,
            onCheckedChange = { v -> edit { it.copy(homeSpeed = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_home_session),
            checked = prefs.homeSession,
            onCheckedChange = { v -> edit { it.copy(homeSession = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_home_quick),
            checked = prefs.homeQuick,
            onCheckedChange = { v -> edit { it.copy(homeQuick = v) } },
        )
        SettingsSwitchRow(
            title = stringResource(Res.string.spider_home_chips),
            checked = prefs.homeChips,
            onCheckedChange = { v -> edit { it.copy(homeChips = v) } },
        )
        SettingsActionRow(title = stringResource(Res.string.spider_reset_look)) {
            edit { SpiderPrefs(tiles = it.tiles) }
        }
    }
}

@Composable
internal fun PalettePicker(settings: AppSettings, selected: SpiderPalette, onSelect: (SpiderPalette) -> Unit) {
    val dark = Spider.colors.dark
    val seed = accentSeed(settings.accentColor)
    val options = listOf(
        SpiderPalette.Brand to stringResource(Res.string.spider_palette_brand),
        SpiderPalette.Accent to stringResource(Res.string.spider_palette_accent),
        SpiderPalette.Pastel to stringResource(Res.string.spider_palette_pastel),
        SpiderPalette.Vivid to stringResource(Res.string.spider_palette_vivid),
        SpiderPalette.Mono to stringResource(Res.string.spider_palette_mono),
        SpiderPalette.Citrus to stringResource(Res.string.spider_palette_citrus),
        SpiderPalette.Matcha to stringResource(Res.string.spider_palette_matcha),
        SpiderPalette.Neon to stringResource(Res.string.spider_palette_neon),
        SpiderPalette.Pistachio to stringResource(Res.string.spider_palette_pistachio),
        SpiderPalette.Ocean to stringResource(Res.string.spider_palette_ocean),
        SpiderPalette.Sunset to stringResource(Res.string.spider_palette_sunset),
        SpiderPalette.Berry to stringResource(Res.string.spider_palette_berry),
    )
    SettingsSegment {
        Column(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp)) {
            Text(
                stringResource(Res.string.spider_palette_title),
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(options, key = { it.first.name }) { (palette, label) ->
                    val colors = remember(palette, dark, seed) { SpiderPalettes.of(palette, dark, seed, amoled = false) }
                    PaletteSwatch(label, colors, palette == selected) { onSelect(palette) }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(label: String, colors: SpiderColors, selected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .width(92.dp)
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, ring, shape) else Modifier)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(colors.ground)
                .padding(5.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(8.dp)).background(colors.stage))
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(colors.lime))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(colors.cyan.container, colors.pink.container, colors.blue.container).forEach { tone ->
                        Box(Modifier.weight(1f).height(23.dp).clip(RoundedCornerShape(7.dp)).background(tone))
                    }
                }
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(ring),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(Res.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SpiderDesignPreview() {
    val c = Spider.colors
    Column(
        Modifier.fillMaxWidth().padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(width = 44.dp, height = 9.dp).clip(RoundedCornerShape(4.dp)).background(c.ink.copy(alpha = 0.85f)))
        Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(10.dp)).background(c.stage))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(8.dp)).background(c.blue.container))
            Box(Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(8.dp)).background(c.violet.container))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(c.surface))
            Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(c.surface))
        }
        Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(8.dp)).background(c.lime))
        Box(
            Modifier.align(Alignment.CenterHorizontally).size(width = 54.dp, height = 12.dp).clip(RoundedCornerShape(6.dp)).background(c.dock),
        ) {
            Box(Modifier.align(Alignment.CenterStart).padding(2.dp).size(width = 18.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(c.lime))
        }
    }
}
