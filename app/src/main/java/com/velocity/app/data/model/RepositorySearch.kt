package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositorySearchResult(
    val id: String,
    val kind: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("message_id") val messageId: String? = null,
    val title: String? = null,
    val content: String = "",
    val snippet: String = "",
    val role: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class RepositorySearchPage(
    val results: List<RepositorySearchResult> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_offset") val nextOffset: Int? = null
)
