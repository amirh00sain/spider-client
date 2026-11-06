package dev.spider.spider.ui.navigation

import dev.spider.spider.domain.model.NavTab
import dev.spider.spider.shared.resources.Res
import dev.spider.spider.shared.resources.ic_add_link
import dev.spider.spider.shared.resources.ic_description
import dev.spider.spider.shared.resources.ic_dns
import dev.spider.spider.shared.resources.ic_encrypted
import dev.spider.spider.shared.resources.ic_fill_alt_route
import dev.spider.spider.shared.resources.ic_fill_apps
import dev.spider.spider.shared.resources.ic_home
import dev.spider.spider.shared.resources.ic_map
import dev.spider.spider.shared.resources.ic_spider_settings
import dev.spider.spider.shared.resources.ic_speed
import dev.spider.spider.shared.resources.ic_sync
import dev.spider.spider.shared.resources.nav_apps
import dev.spider.spider.shared.resources.nav_chains
import dev.spider.spider.shared.resources.nav_home
import dev.spider.spider.shared.resources.nav_locked
import dev.spider.spider.shared.resources.nav_logs
import dev.spider.spider.shared.resources.nav_map
import dev.spider.spider.shared.resources.nav_routing
import dev.spider.spider.shared.resources.nav_servers
import dev.spider.spider.shared.resources.nav_settings
import dev.spider.spider.shared.resources.nav_speed
import dev.spider.spider.shared.resources.nav_subs
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class TopDestination(
    val labelRes: StringResource,
    val icon: DrawableResource,
    val tab: NavTab,
) {
    Home(Res.string.nav_home, Res.drawable.ic_home, NavTab.Home),
    Servers(Res.string.nav_servers, Res.drawable.ic_dns, NavTab.Servers),
    Chains(Res.string.nav_chains, Res.drawable.ic_add_link, NavTab.Chains),
    Subscriptions(Res.string.nav_subs, Res.drawable.ic_sync, NavTab.Subscriptions),
    Vault(Res.string.nav_locked, Res.drawable.ic_encrypted, NavTab.Vault),
    SpeedTest(Res.string.nav_speed, Res.drawable.ic_speed, NavTab.SpeedTest),
    Map(Res.string.nav_map, Res.drawable.ic_map, NavTab.Map),
    Logs(Res.string.nav_logs, Res.drawable.ic_description, NavTab.Logs),
    Routing(Res.string.nav_routing, Res.drawable.ic_fill_alt_route, NavTab.Routing),
    Apps(Res.string.nav_apps, Res.drawable.ic_fill_apps, NavTab.Apps),
    Settings(Res.string.nav_settings, Res.drawable.ic_spider_settings, NavTab.Settings);

    companion object {
        fun of(tab: NavTab): TopDestination = entries.first { it.tab == tab }
    }
}
