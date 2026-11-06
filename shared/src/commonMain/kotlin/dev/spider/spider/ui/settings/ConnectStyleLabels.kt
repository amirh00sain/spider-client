package dev.spider.spider.ui.settings

import dev.spider.spider.domain.model.ConnectButtonStyle
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.connect_style_bar
import dev.spider.spider.shared.resources.connect_style_hero
import dev.spider.spider.shared.resources.connect_style_icon
import dev.spider.spider.shared.resources.connect_style_pill
import dev.spider.spider.shared.resources.connect_style_ring
import dev.spider.spider.shared.resources.connect_style_switch
import org.jetbrains.compose.resources.StringResource

fun ConnectButtonStyle.labelRes(): StringResource = when (this) {
    ConnectButtonStyle.Pill -> Res.string.connect_style_pill
    ConnectButtonStyle.Hero -> Res.string.connect_style_hero
    ConnectButtonStyle.Ring -> Res.string.connect_style_ring
    ConnectButtonStyle.Bar -> Res.string.connect_style_bar
    ConnectButtonStyle.Icon -> Res.string.connect_style_icon
    ConnectButtonStyle.Switch -> Res.string.connect_style_switch
}
