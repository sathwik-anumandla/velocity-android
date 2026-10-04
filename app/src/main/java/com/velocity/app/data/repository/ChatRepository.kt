package com.velocity.app.data.repository

import com.velocity.app.data.api.ChatStreamEvent
import com.velocity.app.data.api.ChatStreamPayload
import com.velocity.app.data.api.SseStreamClient
import com.velocity.app.data.api.VelocityApiService
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.ThreadItem
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class ChatRepository(private val baseUrl: String = "http://10.0.2.2:8000/") {

    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(VelocityApiService::class.java)
    private val sseClient = SseStreamClient(baseUrl)

    suspend fun fetchMainMessages(): List<ChatMessage> {
        val res = api.getMainMessages()
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }

    suspend fun fetchThreads(): List<ThreadItem> {
        val res = api.getThreads()
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }

    suspend fun fetchThreadMessages(threadId: String): List<ChatMessage> {
        val res = api.getThreadMessages(threadId)
        return if (res.isSuccessful) res.body().orEmpty() else emptyList()
    }

    suspend fun respondAction(actionId: String, confirm: Boolean): Boolean {
        val res = api.respondAction(
            actionId,
            com.velocity.app.data.api.ActionResponseRequest(if (confirm) "confirm" else "decline")
        )
        return res.isSuccessful
    }

    fun streamTurn(
        message: String,
        sessionId: String = "main",
        thinkingEffort: String = "medium",
        verbosity: String = "medium"
    ): Flow<ChatStreamEvent> {
        return sseClient.streamChat(
            ChatStreamPayload(
                message = message,
                session_id = sessionId,
                thinking_effort = thinkingEffort,
                verbosity = verbosity
            )
        )
    }
}
