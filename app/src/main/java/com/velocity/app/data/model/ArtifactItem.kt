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
    val summary: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
