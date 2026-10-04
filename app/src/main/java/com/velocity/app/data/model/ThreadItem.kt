package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThreadItem(
    val id: String,
    val name: String,
    @SerialName("parent_session_id") val parentSessionId: String? = "main",
    val status: String = "active", // "active", "concluded"
    @SerialName("rollup_summary") val rollupSummary: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
