package dev.spider.spider.ui.spider

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.spider.spider.ui.components.LocalPageBack
import dev.spider.spider.ui.settings.SettingsArt
import dev.spider.spider.ui.settings.sharedArt

@Composable
fun SpiderPageHeader(
    title: String,
    subtitle: String?,
    art: SettingsArt?,
    modifier: Modifier = Modifier,
    scroll: ScrollState? = null,
) {
    val c = Spider.colors
    val back = LocalPageBack.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = SpiderMetrics.gutter),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (back != null) SpiderBackButton(back)
        if (art == null) {
            Text(title, style = SpiderType.display(), color = c.ink, modifier = Modifier.padding(horizontal = 4.dp))
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = SpiderType.body(), color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
            return@Column
        }
        val prominent = SpiderTiles.isProminent(art.key)
        val tone = c.tone(SpiderTiles.toneOf(art.key))
        val accent = if (prominent) c.lime else tone.content
        val accentInk = if (prominent) c.onLime else tone.container
        val spin = (scroll?.value ?: 0) * 0.12f
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
                .clip(SpiderShapes.hero())
                .background(tone.container)
                .spiderMotif(art.shape, tone.content, 170.dp, 18f + spin, enabled = Spider.prefs.motifs, alpha = 0.1f, bleedX = 36.dp, bleedY = 44.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides tone.content) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        if (Spider.prefs.indexes) SpiderIndex(SpiderTiles.ordinalOf(art.key), tone.content)
                        Spacer(Modifier.weight(1f))
                        SpiderBadge(
                            shape = art.shape,
                            container = accent,
                            content = accentInk,
                            icon = art.icon,
                            size = 48.dp,
                            rotation = 12f + spin,
                            modifier = Modifier.sharedArt(art),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            title,
                            style = SpiderType.title(if (title.length > 18) 28.sp else 36.sp),
                            color = tone.content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                subtitle,
                                style = SpiderType.body(),
                                color = tone.content.copy(alpha = 0.82f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpiderGroupLabel(title: String, modifier: Modifier = Modifier) {
    SpiderCaption(title, modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 4.dp))
}
