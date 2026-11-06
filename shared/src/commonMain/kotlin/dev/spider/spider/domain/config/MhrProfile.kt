package dev.spider.spider.domain.config

import kotlinx.serialization.Serializable

@Serializable
data class MhrProfile(
    val deploymentIds: List<String>,
    val auth: String,
) {
    fun validationError(): String? = when {
        deploymentIds.isEmpty() -> "Add at least one deployment ID"
        deploymentIds.any { it.isBlank() } -> "Deployment IDs cannot be empty"
        auth.isBlank() -> "Auth is required"
        else -> null
    }
}
