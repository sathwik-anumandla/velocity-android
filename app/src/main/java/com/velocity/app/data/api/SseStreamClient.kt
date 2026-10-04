package com.velocity.app.data.api

import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.StagedAction
import com.velocity.app.data.model.ThreadProposal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
}

@Serializable
data class ChatStreamPayload(
    val message: String,
    val session_id: String = "main",
    val model: String? = null,
    val thinking_effort: String = "medium",
    val verbosity: String = "medium"
)

class SseStreamClient(
    private val baseUrl: String,
    private val cfClientId: String = "",
    private val cfClientSecret: String = ""
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Indefinite for SSE
        .build()

    fun streamChat(payload: ChatStreamPayload): Flow<ChatStreamEvent> = callbackFlow {
        val requestBody = json.encodeToString(ChatStreamPayload.serializer(), payload)
            .toRequestBody("application/json".toMediaType())

        val cleanUrl = baseUrl.trimEnd('/')
        val requestBuilder = Request.Builder()
            .url("$cleanUrl/chat/stream")
            .post(requestBody)
            .header("Accept", "text/event-stream")

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
                            val map = json.decodeFromString<Map<String, String?>>(data)
                            val text = map["text"] ?: ""
                            val msgId = map["assistant_message_id"]
                            trySend(ChatStreamEvent.Complete(text, msgId))
                            close()
                        }
                        "error" -> {
                            val map = json.decodeFromString<Map<String, String>>(data)
                            trySend(ChatStreamEvent.Error(map["error"] ?: "Unknown stream error"))
                            close()
                        }
                    }
                } catch (e: Exception) {
                    trySend(ChatStreamEvent.Error("Parse error: ${e.message}"))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorMsg = when {
                    response?.code == 403 -> "Cloudflare Access Forbidden (Check Client ID and Secret)"
                    response?.code == 404 -> "Endpoint not found on server"
                    response != null -> "Server error HTTP ${response.code}"
                    else -> t?.message ?: "Stream connection failed"
                }
                trySend(ChatStreamEvent.Error(errorMsg))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
