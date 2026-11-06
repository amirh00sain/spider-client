package dev.spider.spider.domain.config

/** Parser for Aether share links used by PattN/PattNG and Spider. */
object AetherLink {
    fun isAetherLink(text: String): Boolean = text.trim().startsWith("aether://", ignoreCase = true)

    fun parse(text: String): Pair<String, AetherProfile>? {
        if (!isAetherLink(text)) return null
        val parts = ConfigParser.parseLink(text.trim().substringAfter("://"))
        val query = parts.query
        val protocol = query["protocol"]?.lowercase()?.takeIf { it in AetherProfile.PROTOCOLS } ?: "wg"
        fun endpoint(value: String?): String = value?.trim()?.takeIf { it.isNotEmpty() && validEndpoint(it) }.orEmpty()
        val twoHop = protocol in setOf("gool", "mim", "wg-over-masque")
        val peer = if (twoHop || parts.host.isEmpty() || parts.port == null) "" else endpoint("${bracket(parts.host)}:${parts.port}")
        val settings = AetherProfile(
            protocol = protocol,
            transport = query["transport"]?.lowercase()?.takeIf { it in AetherProfile.TRANSPORTS } ?: "h3",
            peer = peer,
            outerPeer = endpoint(query["outer"]),
            innerPeer = endpoint(query["inner"]),
            scan = query["scan"]?.lowercase()?.takeIf { it in AetherProfile.SCANS } ?: "balanced",
            obfuscation = query["noize"]?.lowercase()?.takeIf { it in AetherProfile.OBFUSCATIONS } ?: "auto",
            ipVersion = query["ip"]?.lowercase()?.takeIf { it in AetherProfile.IP_VERSIONS } ?: "v4",
            masqueSni = query["sni"]?.takeIf { it.matches(Regex("[A-Za-z0-9.-]{1,253}")) }.orEmpty(),
            dns = query["dns"].orEmpty(),
            exitLocation = query["exit_loc"].orEmpty(),
            ech = query["ech"] == "1",
            fragment = query["fragment"] == "1",
        )
        if (settings.validationError() != null) return null
        return (parts.fragment.ifBlank { "Aether" } to settings)
    }

    private fun bracket(host: String): String = if (':' in host && !host.startsWith('[')) "[$host]" else host

    private fun validEndpoint(value: String): Boolean =
        value.matches(Regex("(?:[A-Za-z0-9.-]+|\\[[0-9A-Fa-f:]+]):[0-9]{1,5}")) &&
            value.substringAfterLast(':').toIntOrNull()?.let { it in 1..65535 } == true
}
