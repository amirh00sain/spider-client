package dev.spider.spider.domain.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SoarProfile(
    val zone: String,
    val serverKey: String,
    val resolvers: List<String> = emptyList(),
) {
    fun validationError(): String? = when {
        zone.isBlank() || !zone.contains('.') -> "A valid Soar zone is required"
        serverKey.isBlank() -> "Soar server key is required"
        resolvers.none { it.isNotBlank() } -> "At least one DNS resolver is required"
        else -> null
    }

    companion object {
        private val parser = Json { ignoreUnknownKeys = true }
        fun fromPastedJson(text: String): SoarProfile? = runCatching {
            parser.decodeFromString(serializer(), text.trim())
        }.getOrNull()?.takeIf { it.validationError() == null }
    }
}
