package dev.spider.spider.domain.model

enum class NavTab { Home, Servers, Chains, Subscriptions, Vault, SpeedTest, Map, Logs, Routing, Apps, Settings }

object NavTabs {
    const val MAX = 5

    // Keep the everyday destinations in the compact mobile dock. Remaining pages
    // stay available through Home shortcuts, Settings, and the NavBar picker.
    val DEFAULT: List<NavTab> = listOf(
        NavTab.Home,
        NavTab.Servers,
        NavTab.Chains,
        NavTab.Logs,
        NavTab.Settings,
    )

    val FIXED: Set<NavTab> = setOf(NavTab.Home, NavTab.Settings)

    fun parse(code: String, available: Set<NavTab> = NavTab.entries.toSet()): List<NavTab> {
        val normalizedCode = code.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val previousDefault = listOf(NavTab.Home, NavTab.Servers, NavTab.Vault, NavTab.Settings).map { it.name }
        if (normalizedCode == previousDefault) return normalize(DEFAULT.filter { it in available })
        val previousRedesignDefault = listOf(NavTab.Home, NavTab.Servers, NavTab.Subscriptions, NavTab.Logs, NavTab.Settings).map { it.name }
        if (normalizedCode == previousRedesignDefault) return normalize(DEFAULT.filter { it in available })
        val picked = code.split(',')
            .mapNotNull { name -> NavTab.entries.firstOrNull { it.name == name.trim() } }
            .filter { it in available }
            .distinct()
        return normalize(if (picked.isEmpty()) DEFAULT.filter { it in available } else picked)
    }

    fun encode(tabs: List<NavTab>): String = normalize(tabs).joinToString(",") { it.name }

    fun normalize(tabs: List<NavTab>): List<NavTab> {
        val unique = tabs.distinct().toMutableList()
        if (NavTab.Home !in unique) unique.add(0, NavTab.Home)
        if (NavTab.Settings !in unique) unique.add(NavTab.Settings)
        while (unique.size > MAX) {
            val drop = unique.indexOfLast { it !in FIXED }
            if (drop < 0) break
            unique.removeAt(drop)
        }
        return unique
    }

    fun canAdd(tabs: List<NavTab>): Boolean = tabs.size < MAX

    fun canRemove(tab: NavTab): Boolean = tab !in FIXED
}
