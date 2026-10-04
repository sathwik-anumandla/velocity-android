package com.velocity.app.data.api

import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.ThreadItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VelocityApiService {

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

    @GET("api/threads/{id}/messages")
    suspend fun getThreadMessages(@Path("id") threadId: String): Response<List<ChatMessage>>

    @POST("api/threads/{id}/conclude")
    suspend fun concludeThread(@Path("id") threadId: String): Response<Map<String, String>>

    @GET("api/artifacts")
    suspend fun getArtifacts(): Response<ResponseBody>

    @GET("api/artifacts/{id}")
    suspend fun getArtifact(@Path("id") id: String): Response<ArtifactItem>

    @GET("api/navigation/chronology")
    suspend fun getChronology(@Query("limit") limit: Int = 50): Response<ResponseBody>

    @GET("search")
    suspend fun searchMessages(@Query("q") query: String): Response<List<SearchResultItem>>

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
    ): Response<Map<String, String>>

    @GET("health")
    suspend fun getHealth(): Response<Map<String, String>>
}

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
    val metadata: Map<String, String>? = null
)
