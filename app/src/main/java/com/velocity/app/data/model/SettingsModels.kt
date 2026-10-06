package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VaultTreeItem(
    val path: String = "",
    val name: String = "",
    val title: String? = null,
    val category: String? = null,
    val type: String = "file"
)

@Serializable
data class ScheduledRoutine(
    val id: String = "",
    val name: String = "",
    @SerialName("event_type") val eventType: String = "recurring",
    val prompt: String = "",
    @SerialName("cron_expression") val cronExpression: String? = null,
    @SerialName("run_at") val runAt: String? = null,
    val status: String = "active",
    val timezone: String = "Asia/Kolkata",
    @SerialName("skill_id") val skillId: String? = null,
    @SerialName("next_run_at") val nextRunAt: String? = null
)

@Serializable
data class SkillRecord(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val instructions: String = "",
    val enabled: Boolean = true
)

@Serializable
data class GoogleWorkspaceStatus(
    @SerialName("google_connected") val connected: Boolean = false,
    @SerialName("google_user_email") val email: String? = null
)
