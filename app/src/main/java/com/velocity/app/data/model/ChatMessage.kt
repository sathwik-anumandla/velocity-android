package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessage(
    val id: String,
    val role: String, // "user", "assistant", "system"
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_streaming") val isStreaming: Boolean = false,
    @SerialName("thread_proposal") val threadProposal: ThreadProposal? = null,
    val artifact: ArtifactItem? = null,
    @SerialName("staged_action") val stagedAction: StagedAction? = null,
    val reasoning: String? = null
)

@Serializable
data class ThreadProposal(
    val title: String,
    val reason: String,
    @SerialName("suggested_first_turn") val suggestedFirstTurn: String? = null,
    val status: String = "pending", // "pending", "accepted", "declined"
    @SerialName("thread_id") val threadId: String? = null
)

@Serializable
data class StagedAction(
    val id: String,
    @SerialName("action_type") val actionType: String,
    val parameters: Map<String, JsonElement> = emptyMap(),
    val status: String = "pending"
)

