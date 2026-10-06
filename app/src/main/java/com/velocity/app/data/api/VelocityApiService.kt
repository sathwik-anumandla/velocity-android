package com.velocity.app.data.api

import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.ThreadItem
import com.velocity.app.data.model.ThreadProposal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VelocityApiService {
    @GET("api/version")
    suspend fun getVersion(): Response<com.velocity.app.data.model.DeploymentVersion>

    @GET("api/sessions/{id}/history")
    suspend fun getHistory(@Path("id") id: String, @Query("before") before: String? = null, @Query("limit") limit: Int = 40): Response<com.velocity.app.data.model.MessagePage>

    @DELETE("api/sessions/{id}/messages")
    suspend fun truncateHistory(@Path("id") id: String, @Query("from_message_id") messageId: String): Response<ResponseBody>

    @POST("api/sessions/{id}/messages/{message}/branch")
    suspend fun branchMessage(@Path("id") id: String, @Path("message") messageId: String, @Body payload: Map<String, String>): Response<ThreadItem>

    @GET("sessions/main")
    suspend fun getMainSessionRaw(): Response<ResponseBody>

    @GET("sessions/{id}")
    suspend fun getSessionRaw(@Path("id") id: String): Response<ResponseBody>

    @GET("api/sessions/main/messages")
    suspend fun getMainMessagesDirect(): Response<List<ChatMessage>>

    @GET("api/threads")
    suspend fun getThreads(): Response<ResponseBody>

    @POST("api/threads")
    suspend fun createThread(@Body payload: CreateThreadRequest): Response<ThreadItem>

    @GET("api/sessions/{id}/messages")
    suspend fun getThreadMessages(@Path("id") threadId: String): Response<List<ChatMessage>>

    @POST("api/threads/{id}/rollup")
    suspend fun concludeThread(@Path("id") threadId: String, @Query("conclude") conclude: Boolean = true): Response<ResponseBody>

    @GET("api/artifacts")
    suspend fun getArtifacts(): Response<ResponseBody>

    @GET("api/artifacts/{id}")
    suspend fun getArtifact(@Path("id") id: String): Response<ArtifactItem>

    @PATCH("api/artifacts/{id}")
    suspend fun updateArtifactTheme(@Path("id") id: String, @Body payload: Map<String, String>): Response<ArtifactItem>

    @GET("api/navigation/chronology")
    suspend fun getChronology(@Query("limit") limit: Int = 50): Response<ResponseBody>

    @GET("search")
    suspend fun searchMessages(@Query("q") query: String): Response<List<SearchResultItem>>

    @GET("api/search")
    suspend fun searchRepository(@Query("q") query: String, @Query("kind") kind: String, @Query("session_id") sessionId: String?, @Query("role") role: String?, @Query("after") after: String?, @Query("before") before: String?, @Query("offset") offset: Int): Response<com.velocity.app.data.model.RepositorySearchPage>

    @GET("api/integrations/status")
    suspend fun getIntegrationStatus(): Response<ResponseBody>

    @GET("api/schedules")
    suspend fun getSchedules(): Response<ResponseBody>

    @GET("api/skills")
    suspend fun getSkills(): Response<ResponseBody>

    @GET("api/memory/tree")
    suspend fun getVaultTree(): Response<ResponseBody>

    @POST("api/actions/{id}/respond")
    suspend fun respondAction(
        @Path("id") actionId: String,
        @Body payload: ActionResponseRequest
    ): Response<com.velocity.app.data.model.StagedAction>

    @GET("api/memory/doc")
    suspend fun getMemoryDoc(@Query("path") path: String): Response<ResponseBody>

    @PUT("api/memory/doc")
    suspend fun saveMemoryDoc(@Body payload: SaveDocRequest): Response<ResponseBody>

    @POST("api/schedules")
    suspend fun createSchedule(@Body payload: CreateScheduleRequest): Response<ResponseBody>

    @PATCH("api/schedules/{id}")
    suspend fun updateSchedule(@Path("id") id: String, @Body payload: UpdateScheduleRequest): Response<ResponseBody>

    @DELETE("api/schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): Response<ResponseBody>

    @PATCH("api/skills/{id}")
    suspend fun updateSkill(@Path("id") id: String, @Body payload: UpdateSkillRequest): Response<ResponseBody>

    @POST("api/threads/proposals/{message_id}/respond")
    suspend fun respondThreadProposal(
        @Path("message_id") messageId: String,
        @Body payload: ProposalRespondRequest
    ): Response<ProposalRespondResult>

    @DELETE("api/integrations/google")
    suspend fun disconnectGoogle(): Response<ResponseBody>

    @GET("api/usage")
    suspend fun getUsage(): Response<com.velocity.app.data.model.UsageStats>

    @PUT("api/usage/prices")
    suspend fun saveUsagePrices(@Body pricing: com.velocity.app.data.model.UsagePricing): Response<com.velocity.app.data.model.UsageStats>

    @POST("api/chat/turns/{id}/cancel")
    suspend fun cancelTurn(@Path("id") id: String): Response<ResponseBody>

    @GET("api/artifacts/{id}/export/pdf")
    suspend fun exportArtifactPdf(@Path("id") id: String): Response<ResponseBody>

    @GET("health")
    suspend fun getHealth(): Response<Map<String, String>>
}

@Serializable
data class SaveDocRequest(val path: String, val content: String)

@Serializable
data class CreateScheduleRequest(
    val name: String,
    @SerialName("event_type") val eventType: String,
    val prompt: String,
    @SerialName("cron_expression") val cronExpression: String? = null,
    @SerialName("run_at") val runAt: String? = null,
    @SerialName("session_id") val sessionId: String = "main",
    val timezone: String = java.time.ZoneId.systemDefault().id
)

@Serializable
data class UpdateScheduleRequest(
    val status: String? = null,
    val name: String? = null,
    val prompt: String? = null,
    @SerialName("cron_expression") val cronExpression: String? = null,
    @SerialName("run_at") val runAt: String? = null,
    val timezone: String? = null
)

@Serializable
data class UpdateSkillRequest(
    val enabled: Boolean? = null,
    val instructions: String? = null
)

@Serializable
data class ProposalRespondRequest(
    val action: String // "accept" or "decline"
)

@Serializable
data class ProposalRespondResult(
    val status: String,
    val thread: ThreadItem? = null,
    val proposal: ThreadProposal? = null
)


@Serializable
data class CreateThreadRequest(
    val name: String,
    @SerialName("parent_session_id") val parentSessionId: String = "main",
    @SerialName("originating_user_prompt") val originatingUserPrompt: String? = null
)

@Serializable
data class ActionResponseRequest(
    val action: String // "confirm" or "decline"
)

@Serializable
data class SearchResultItem(
    @SerialName("session_id") val sessionId: String = "main",
    @SerialName("session_name") val sessionName: String? = null,
    val content: String = "",
    val snippet: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ChronologyItem(
    val type: String = "event",
    val title: String = "",
    val description: String? = null,
    val timestamp: String? = null,
    val metadata: Map<String, kotlinx.serialization.json.JsonElement>? = null
)
