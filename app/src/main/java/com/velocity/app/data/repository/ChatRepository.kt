package com.velocity.app.data.repository

import com.velocity.app.data.api.ActionResponseRequest
import com.velocity.app.data.api.ChatStreamEvent
import com.velocity.app.data.api.ChatStreamPayload
import com.velocity.app.data.api.ChronologyItem
import com.velocity.app.data.api.SearchResultItem
import com.velocity.app.data.api.SseStreamClient
import com.velocity.app.data.api.VelocityApiService
import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.ThreadItem
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ChatRepository(private val config: ServerConfig) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
            if (config.cfClientId.isNotBlank() && config.cfClientSecret.isNotBlank()) {
                builder.addHeader("CF-Access-Client-Id", config.cfClientId.trim())
                builder.addHeader("CF-Access-Client-Secret", config.cfClientSecret.trim())
            }
            chain.proceed(builder.build())
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(config.normalizedUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: VelocityApiService = retrofit.create(VelocityApiService::class.java)

    private val sseClient = SseStreamClient(
        baseUrl = config.normalizedUrl,
        cfClientId = config.cfClientId,
        cfClientSecret = config.cfClientSecret
    )

    /**
     * Tests connectivity to the server. Returns Result.success(true) or failure with message.
     */
    suspend fun testConnection(): Result<Boolean> {
        return try {
            val res = api.getHealth()
            if (res.isSuccessful) {
                Result.success(true)
            } else if (res.code() == 403) {
                Result.failure(Exception("Cloudflare Access Denied (HTTP 403). Please verify Service Token credentials."))
            } else {
                val fallback = api.getMainSessionRaw()
                if (fallback.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Server responded with HTTP ${res.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message ?: "Server unreachable"}"))
        }
    }

    suspend fun fetchHealthDetails(): Map<String, String> {
        return try {
            val res = api.getHealth()
            if (res.isSuccessful) res.body().orEmpty() else emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun fetchMainMessages(): List<ChatMessage> {
        return fetchSessionMessages("main")
    }

    suspend fun fetchThreadMessages(threadId: String): List<ChatMessage> {
        return fetchSessionMessages(threadId)
    }

    private suspend fun fetchSessionMessages(sessionId: String): List<ChatMessage> {
        return try {
            val res = api.getSessionRaw(sessionId)
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)

            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("messages")) {
                json.decodeFromJsonElement<List<ChatMessage>>(element.jsonObject["messages"]!!)
            } else if (element is kotlinx.serialization.json.JsonArray) {
                json.decodeFromJsonElement<List<ChatMessage>>(element)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchThreads(): List<ThreadItem> {
        return try {
            val res = api.getThreads()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("threads")) {
                json.decodeFromJsonElement<List<ThreadItem>>(element.jsonObject["threads"]!!)
            } else if (element is kotlinx.serialization.json.JsonArray) {
                json.decodeFromJsonElement<List<ThreadItem>>(element)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchArtifacts(): List<ArtifactItem> {
        return try {
            val res = api.getArtifacts()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("artifacts")) {
                json.decodeFromJsonElement<List<ArtifactItem>>(element.jsonObject["artifacts"]!!)
            } else if (element is kotlinx.serialization.json.JsonArray) {
                json.decodeFromJsonElement<List<ArtifactItem>>(element)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchChronology(): List<ChronologyItem> {
        return try {
            val res = api.getChronology()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("events")) {
                json.decodeFromJsonElement<List<ChronologyItem>>(element.jsonObject["events"]!!)
            } else if (element is kotlinx.serialization.json.JsonArray) {
                json.decodeFromJsonElement<List<ChronologyItem>>(element)
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun searchMessages(query: String): List<SearchResultItem> {
        if (query.isBlank()) return emptyList()
        return try {
            val res = api.searchMessages(query.trim())
            if (res.isSuccessful) res.body().orEmpty() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchIntegrationStatus(): Map<String, Boolean> {
        return try {
            val res = api.getIntegrationStatus()
            if (!res.isSuccessful) return emptyMap()
            val raw = res.body()?.string() ?: return emptyMap()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonObject) {
                element.jsonObject.mapValues { (_, v) ->
                    v.jsonPrimitive.content.toBooleanStrictOrNull() ?: false
                }
            } else emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun fetchSchedules(): List<Map<String, String>> {
        return try {
            val res = api.getSchedules()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonArray) {
                element.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
                    .map { obj -> obj.mapValues { it.value.jsonPrimitive.content } }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun fetchSkills(): List<Map<String, String>> {
        return try {
            val res = api.getSkills()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonArray) {
                element.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
                    .map { obj -> obj.mapValues { it.value.jsonPrimitive.content } }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun respondAction(actionId: String, confirm: Boolean): Boolean {
        val res = api.respondAction(
            actionId,
            ActionResponseRequest(if (confirm) "confirm" else "decline")
        )
        return res.isSuccessful
    }

    fun streamTurn(
        message: String,
        sessionId: String = "main",
        model: String? = null,
        thinkingEffort: String = "medium",
        verbosity: String = "medium"
    ): Flow<ChatStreamEvent> {
        return sseClient.streamChat(
            ChatStreamPayload(
                message = message,
                session_id = sessionId,
                model = model,
                thinking_effort = thinkingEffort,
                verbosity = verbosity
            )
        )
    }
}
