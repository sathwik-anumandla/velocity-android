package com.velocity.app.data.api

import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.StagedAction
import com.velocity.app.data.model.ThreadProposal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

sealed class ChatStreamEvent {
    data class Status(val text: String) : ChatStreamEvent()
    data class ReasoningDelta(val text: String) : ChatStreamEvent()
    data class Delta(val text: String) : ChatStreamEvent()
    data class Proposal(val proposal: ThreadProposal) : ChatStreamEvent()
    data class ArtifactCreated(val artifact: ArtifactItem) : ChatStreamEvent()
    data class ActionProposal(val action: StagedAction) : ChatStreamEvent()
    data class Complete(val text: String, val messageId: String?) : ChatStreamEvent()
    data class Error(val message: String) : ChatStreamEvent()
    data object Cancelled : ChatStreamEvent()
}

@Serializable
data class ChatStreamPayload(
    val message: String,
    val session_id: String,
    val model: String = "gpt-5.4-mini",
    val thinking_effort: String = "medium",
    val verbosity: String = "low",
    val message_id: String,
    val recall_budget: String = "medium"
)

@Serializable
data class ChatStreamCompletion(
    val text: String = "",
    val assistant_message_id: String? = null
)

class SseStreamClient(
    private val baseUrl: String,
    private val cfClientId: String = "",
    private val cfClientSecret: String = ""
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for SSE
        .build()

    fun streamChat(payload: ChatStreamPayload): Flow<ChatStreamEvent> = recoveringStream(payload, payload.message_id)

    fun resumeChat(turnId: String): Flow<ChatStreamEvent> = recoveringStream(null, turnId)

    private fun recoveringStream(payload: ChatStreamPayload?, turnId: String): Flow<ChatStreamEvent> = flow {
        val cursor = AtomicReference<String?>(null)
        var attempts = 0
        while (true) {
            try {
                connection(payload, turnId, cursor).collect { emit(it) }
                break
            } catch (error: IOException) {
                if (attempts++ >= 4) throw error
                emit(ChatStreamEvent.Status("Reconnecting…"))
                delay(1000L * attempts)
            }
        }
    }

    private fun connection(payload: ChatStreamPayload?, turnId: String, cursor: AtomicReference<String?>): Flow<ChatStreamEvent> = callbackFlow {
        var terminal = false
        val cleanUrl = baseUrl.trimEnd('/')
        val requestBuilder = Request.Builder()
            .url(if (payload == null) "$cleanUrl/api/chat/turns/$turnId/events" else "$cleanUrl/chat/stream")
            .header("Accept", "text/event-stream")
        if (payload != null) {
            requestBuilder.post(json.encodeToString(ChatStreamPayload.serializer(), payload).toRequestBody("application/json".toMediaType()))
        }
        cursor.get()?.let { requestBuilder.header("Last-Event-ID", it) }

        if (cfClientId.isNotBlank() && cfClientSecret.isNotBlank()) {
            requestBuilder.header("CF-Access-Client-Id", cfClientId.trim())
            requestBuilder.header("CF-Access-Client-Secret", cfClientSecret.trim())
        }

        val request = requestBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    when (type) {
                        "status" -> {
                            val map = json.decodeFromString<Map<String, String>>(data)
                            map["text"]?.let { trySend(ChatStreamEvent.Status(it)) }
                        }
                        "reasoning_delta" -> {
                            val map = json.decodeFromString<Map<String, String>>(data)
                            map["text"]?.let { trySend(ChatStreamEvent.ReasoningDelta(it)) }
                        }
                        "delta" -> {
                            val map = json.decodeFromString<Map<String, String>>(data)
                            map["text"]?.let { trySend(ChatStreamEvent.Delta(it)) }
                        }
                        "thread_proposal" -> {
                            val proposal = json.decodeFromString<ThreadProposal>(data)
                            trySend(ChatStreamEvent.Proposal(proposal))
                        }
                        "artifact_created" -> {
                            val artifact = json.decodeFromString<ArtifactItem>(data)
                            trySend(ChatStreamEvent.ArtifactCreated(artifact))
                        }
                        "action_proposal" -> {
                            val action = json.decodeFromString<StagedAction>(data)
                            trySend(ChatStreamEvent.ActionProposal(action))
                        }
                        "complete" -> {
                            val completion = json.decodeFromString<ChatStreamCompletion>(data)
                            trySend(ChatStreamEvent.Complete(completion.text, completion.assistant_message_id))
                            terminal = true
                            close()
                        }
                        "cancelled" -> {
                            trySend(ChatStreamEvent.Cancelled)
                            terminal = true
                            close()
                        }
                        "error" -> {
                            val map = json.decodeFromString<Map<String, String>>(data)
                            trySend(ChatStreamEvent.Error(map["error"] ?: "Unknown stream error"))
                            terminal = true
                            close()
                        }
                    }
                    if (id != null) cursor.set(id)
                } catch (e: Exception) {
                    terminal = true
                    trySend(ChatStreamEvent.Error("Invalid server event: ${e.message}"))
                    close()
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (terminal) return
                if (response == null || response.code >= 500) {
                    close(IOException("Stream interrupted", t))
                    return
                }
                val errorBody = try {
                    response?.body?.string()
                } catch (_: Exception) {
                    null
                }
                val errorMsg = when {
                    response?.code == 403 -> "Cloudflare Access Forbidden (HTTP 403). Check Service Token Client ID & Secret."
                    response?.code == 404 -> "Endpoint not found on server (HTTP 404)"
                    response?.code == 422 -> {
                        if (!errorBody.isNullOrBlank()) "Validation error (HTTP 422): $errorBody"
                        else "Invalid payload (HTTP 422)"
                    }
                    else -> {
                        if (!errorBody.isNullOrBlank()) "Server error HTTP ${response.code}: $errorBody"
                        else "Server error HTTP ${response.code}"
                    }
                }
                trySend(ChatStreamEvent.Error(errorMsg))
                terminal = true
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (!terminal) close(IOException("Connection ended before completion")) else close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }.buffer(Channel.UNLIMITED)
}
