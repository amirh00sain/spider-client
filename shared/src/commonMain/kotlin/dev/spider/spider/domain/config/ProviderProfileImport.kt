package dev.spider.spider.domain.config

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.*

/** Import format shared with WhiteDNS Desktop: whitedns.profile v1 in a scheme URL. */
sealed interface ImportedProviderProfile {
    val name: String
    data class Soar(override val name: String, val settings: SoarProfile) : ImportedProviderProfile
    data class MasterDns(override val name: String, val settings: MasterDnsProfile) : ImportedProviderProfile
}

object ProviderProfileImport {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): ImportedProviderProfile? = runCatching {
        val input = raw.trim().lineSequence().first().trim()
        val scheme = input.substringBefore("://").lowercase()
        if (scheme != "soar" && scheme != "masterdns") return null
        val encoded = input.substringAfter("://", "").substringBefore('#').trim()
        if (encoded.isEmpty() || encoded.any { it.isWhitespace() }) return null
        val payloadText = decode(encoded)
        val root = json.parseToJsonElement(payloadText).jsonObject
        if (root["schema"]?.jsonPrimitive?.content != "whitedns.profile" ||
            root["version"]?.jsonPrimitive?.intOrNull != 1) return null
        val profile = root["profile"]?.jsonObject ?: return null
        val declaredType = root["import_type"]?.jsonPrimitive?.contentOrNull?.lowercase()
        if (scheme == "soar") {
            if (declaredType != null && declaredType != "soar") return null
            val server = profile["server"]?.jsonObject ?: return null
            val zone = server["domain"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val key = server["public_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
                .ifBlank { server["encryption_key"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            val resolvers = profile["resolvers"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty().ifEmpty { listOf("1.1.1.1") }
            val settings = SoarProfile(zone, key, resolvers)
            if (settings.validationError() != null) return null
            ImportedProviderProfile.Soar(profile["name"]?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: zone, settings)
        } else {
            if (declaredType != null && declaredType != "masterdns") return null
            val server = profile["server"]?.jsonObject ?: return null
            val domains = buildList {
                server["domain"]?.jsonPrimitive?.contentOrNull?.let(::add)
                server["domains"]?.jsonArray?.mapNotNullTo(this) { it.jsonPrimitive.contentOrNull }
            }.distinct().joinToString("\n")
            val key = server["encryption_key"]?.jsonPrimitive?.contentOrNull.orEmpty()
                .ifBlank { server["encryptionKey"]?.jsonPrimitive?.contentOrNull.orEmpty() }
            val method = server["encryption_method"]?.jsonPrimitive?.intOrNull
                ?: server["encryptionMethod"]?.jsonPrimitive?.intOrNull ?: MasterDnsProfile.ENC_XOR
            val resolvers = profile["resolvers"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty().ifEmpty { listOf("1.1.1.1:53") }
            val settings = MasterDnsProfile(domains = domains, encryptionKey = key, encryptionMethod = method, resolvers = resolvers.joinToString("\n"))
            if (!settings.isValid || method !in 0..5 || MasterDnsProfile.invalidResolvers(settings.resolvers).isNotEmpty()) return null
            ImportedProviderProfile.MasterDns(profile["name"]?.jsonPrimitive?.contentOrNull?.ifBlank { null } ?: domains.lineSequence().first(), settings)
        }
    }.getOrNull()

    private fun decode(value: String): String {
        val normalized = value.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.Default.decode(padded).decodeToString()
    }
}
