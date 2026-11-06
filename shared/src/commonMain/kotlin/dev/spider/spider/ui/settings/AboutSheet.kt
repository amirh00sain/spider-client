@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.spider.spider.ui.settings

import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.*
import dev.spider.spider.platform.AppInfo
import dev.spider.spider.ui.platform.LocalPlatform
import dev.spider.spider.ui.components.MorphingBlob
import dev.spider.spider.ui.theme.SpiderGradients
import dev.spider.spider.ui.platform.rememberAppSheetState

private const val SPIDER_PANEL_URL = "https://spiderpanel.xfdscscsa.de5.net"
private const val SPIDER_TELEGRAM_URL = "https://t.me/SPiDER_VPN1"
private const val SPIDER_GITHUB_URL = "https://github.com/amirh00sain/spider-client"

@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val platform = LocalPlatform.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberAppSheetState()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MorphingBlob(
                progress = 0.5f,
                brush = SpiderGradients.connected,
                modifier = Modifier.size(96.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_spider_glyph),
                    contentDescription = stringResource(Res.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(70.dp),
                )
            }
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.about_version, AppInfo.versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.about_built_on),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(4.dp))

            Text(
                stringResource(Res.string.about_spider_support),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LinkRow(
                iconRes = Res.drawable.ic_add_link,
                title = stringResource(Res.string.about_spider_panel),
                subtitle = "spiderpanel.xfdscscsa.de5.net",
                onClick = { platform.openUri(SPIDER_PANEL_URL) },
            )
            LinkRow(
                iconRes = Res.drawable.ic_add_link,
                title = stringResource(Res.string.about_spider_telegram),
                subtitle = "t.me/SPiDER_VPN1",
                onClick = { platform.openUri(SPIDER_TELEGRAM_URL) },
            )
            LinkRow(
                iconRes = Res.drawable.ic_description,
                title = stringResource(Res.string.about_spider_github),
                subtitle = "github.com/amirh00sain/spider-client",
                onClick = { platform.openUri(SPIDER_GITHUB_URL) },
            )
        }
    }
}

@Composable
private fun LinkRow(iconRes: DrawableResource, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(iconRes), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(painterResource(Res.drawable.ic_chevron_right), null)
        }
    }
}
