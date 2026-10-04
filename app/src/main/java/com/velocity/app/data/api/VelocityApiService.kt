package com.velocity.app.data.api

import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.ThreadItem
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface VelocityApiService {

    @GET("api/sessions/main/messages")
    suspend fun getMainMessages(): Response<List<ChatMessage>>

    @GET("api/threads")
    suspend fun getThreads(): Response<List<ThreadItem>>

    @POST("api/threads")
    suspend fun createThread(@Body payload: CreateThreadRequest): Response<ThreadItem>

    @GET("api/threads/{id}/messages")
    suspend fun getThreadMessages(@Path("id") threadId: String): Response<List<ChatMessage>>

    @POST("api/threads/{id}/conclude")
    suspend fun concludeThread(@Path("id") threadId: String): Response<Map<String, String>>

    @GET("api/artifacts")
    suspend fun getArtifacts(): Response<List<ArtifactItem>>

    @GET("api/artifacts/{id}")
    suspend fun getArtifact(@Path("id") id: String): Response<ArtifactItem>

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
    val parent_session_id: String = "main",
    val originating_user_prompt: String? = null
)

@Serializable
data class ActionResponseRequest(
    val action: String // "confirm" or "decline"
)
