package dev.spider.spider.domain.config

import kotlinx.serialization.Serializable

/** Spider's editable settings for an Aether tunnel. Values are core option tokens. */
@Serializable
data class AetherProfile(
    val protocol: String = "wg",
    val transport: String = "h3",
    val peer: String = "",
    val outerPeer: String = "",
    val innerPeer: String = "",
    val scan: String = "balanced",
    val obfuscation: String = "auto",
    val ipVersion: String = "v4",
    val masqueSni: String = "",
    val dns: String = "",
    val exitLocation: String = "",
    val ech: Boolean = false,
    val fragment: Boolean = false,
) {
    /** PattN/PattNG compatible share link. */
    fun toLink(name: String): String {
        val query = linkedMapOf(
            "protocol" to protocol,
            "scan" to scan,
            "ip" to ipVersion,
        )
        if (protocol in setOf("masque", "mim", "wg-over-masque")) {
            query["transport"] = transport
            masqueSni.trim().takeIf { it.isNotEmpty() && it != "engage.cloudflareclient.com" }?.let { query["sni"] = it }
            if (ech) query["ech"] = "1"
            if (fragment) query["fragment"] = "1"
        }
        if (obfuscation != "auto" && !(protocol in setOf("masque", "mim", "wg-over-masque") && transport == "h2")) query["noize"] = obfuscation
        dns.trim().takeIf(String::isNotEmpty)?.let { query["dns"] = it }
        exitLocation.trim().takeIf(String::isNotEmpty)?.let { query["exit_loc"] = it }
        if (protocol in setOf("gool", "mim", "wg-over-masque")) {
            outerPeer.trim().takeIf(String::isNotEmpty)?.let { query["outer"] = it }
            innerPeer.trim().takeIf(String::isNotEmpty)?.let { query["inner"] = it }
        }
        val endpoint = if (protocol in setOf("gool", "mim", "wg-over-masque")) "" else peer.trim()
        val encodedQuery = query.entries.joinToString("&") { "${it.key}=${it.value.percentEncode()}" }
        return "aether://$endpoint?$encodedQuery#${name.percentEncode()}"
    }

    fun arguments(socksPort: Int): List<String> = buildList {
        val coreProtocol = if (protocol == "wg-over-masque") "gool" else protocol
        addAll(listOf("--bind", "127.0.0.1:$socksPort", "--protocol", coreProtocol, "--scan", scan, "--ip", ipVersion))
        if (protocol == "gool") add("--gool-classic")
        dns.trim().takeIf(String::isNotEmpty)?.let { addAll(listOf("--dns", it)) }
        exitLocation.trim().takeIf(String::isNotEmpty)?.let { addAll(listOf("--exit-loc", it)) }
        if (protocol in setOf("masque", "mim", "wg-over-masque")) {
            addAll(listOf("--masque-sni", masqueSni.trim().ifEmpty { "engage.cloudflareclient.com" }))
            if (transport == "h2") {
                add("--h2")
                if (fragment) add("--fragment")
            }
            if (ech) addAll(listOf("--ech", "auto"))
        }
        when (protocol) {
            "mim" -> {
                if (outerPeer.isNotBlank()) addAll(listOf("--mim-outer", outerPeer.trim()))
                if (innerPeer.isNotBlank()) addAll(listOf("--mim-inner", innerPeer.trim()))
                if (outerPeer.isBlank() && innerPeer.isBlank()) add("--mim-scan")
            }
            "gool" -> {
                if (outerPeer.isNotBlank()) addAll(listOf("--wiw-outer", outerPeer.trim()))
                if (innerPeer.isNotBlank()) addAll(listOf("--wiw-inner", innerPeer.trim()))
                if (outerPeer.isBlank() && innerPeer.isBlank()) add("--wiw-scan")
            }
            "wg-over-masque" -> {
                if (outerPeer.isNotBlank()) addAll(listOf("--peer", outerPeer.trim()))
                if (innerPeer.isNotBlank()) addAll(listOf("--gool-peer", innerPeer.trim()))
            }
            else -> if (peer.isNotBlank()) addAll(listOf("--peer", peer.trim()))
        }
        if (obfuscation != "auto" && !(protocol in setOf("masque", "mim", "wg-over-masque") && transport == "h2")) {
            addAll(listOf("--noize", obfuscation))
        }
        add("--quick-reconnect")
    }

    fun validationError(): String? {
        if (protocol !in PROTOCOLS) return "Unsupported Aether protocol"
        if (transport !in TRANSPORTS) return "Unsupported Aether transport"
        if (scan !in SCANS) return "Unsupported Aether scan mode"
        if (obfuscation !in OBFUSCATIONS) return "Unsupported Aether obfuscation mode"
        if (ipVersion !in IP_VERSIONS) return "Unsupported IP version"
        if (masqueSni.isNotBlank() && !masqueSni.trim().matches(Regex("[A-Za-z0-9.-]{1,253}"))) return "Invalid MASQUE server name"
        if (dns.isNotBlank() && dns.any(Char::isWhitespace)) return "DNS servers must be comma separated without spaces"
        if (exitLocation.isNotBlank() && !exitLocation.trim().matches(Regex("!?[A-Za-z]{2}(,!?[A-Za-z]{2})*"))) return "Use comma-separated two-letter country codes"
        if (protocol in TWO_HOP_PROTOCOLS) {
            if (outerPeer.isNotBlank() && !validEndpoint(outerPeer)) return "Invalid outer endpoint"
            if (innerPeer.isNotBlank() && !validEndpoint(innerPeer)) return "Invalid inner endpoint"
        } else if (peer.isNotBlank() && !validEndpoint(peer)) return "Invalid endpoint"
        return null
    }

    companion object {
        val PROTOCOLS = setOf("wg", "masque", "gool", "mim", "wg-over-masque")
        val TRANSPORTS = setOf("h3", "h2")
        val SCANS = setOf("turbo", "balanced", "thorough", "verified", "ironclad")
        val OBFUSCATIONS = setOf("auto", "off", "light", "firewall", "balanced", "gfw", "aggressive")
        val IP_VERSIONS = setOf("v4", "v6", "both")
        private val TWO_HOP_PROTOCOLS = setOf("gool", "mim", "wg-over-masque")

        private fun validEndpoint(value: String): Boolean =
            value.trim().matches(Regex("(?:[A-Za-z0-9.-]+|\\[[0-9A-Fa-f:]+]):[0-9]{1,5}")) &&
                value.substringAfterLast(':').toIntOrNull()?.let { it in 1..65535 } == true
    }
}

private fun String.percentEncode(): String = buildString {
    encodeToByteArray().forEach { byte ->
        val value = byte.toInt() and 0xff
        if (value in 'a'.code..'z'.code || value in 'A'.code..'Z'.code || value in '0'.code..'9'.code || value.toChar() in "-._~") {
            append(value.toChar())
        } else {
            append('%')
            append("0123456789ABCDEF"[value ushr 4])
            append("0123456789ABCDEF"[value and 15])
        }
    }
}
