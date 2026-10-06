package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtifactItem(
    val id: String = "",
    val title: String = "",
    @SerialName("artifact_type") val artifactType: String = "document",
    val language: String = "markdown",
    val content: String = "",
    val summary: String? = null,
    val theme: String = "editorial",
    val version: Int = 1,
    @SerialName("session_id") val sessionId: String = "main",
    @SerialName("created_at") val createdAt: String? = null
)
