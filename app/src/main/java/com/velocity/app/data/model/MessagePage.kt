package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessagePage(
    val messages: List<ChatMessage> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("oldest_cursor") val oldestCursor: String? = null,
    @SerialName("history_revision") val historyRevision: Long = 0
)

@Serializable
data class DeploymentVersion(
    val version: String = "unknown",
    @SerialName("backend_revision") val backendRevision: String = "unknown",
    @SerialName("frontend_revision") val frontendRevision: String = "unknown",
    val matches: Boolean? = null
)
