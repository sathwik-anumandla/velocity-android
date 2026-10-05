package com.velocity.app.data.repository

import com.velocity.app.data.api.ActionResponseRequest
import com.velocity.app.data.api.ChatStreamEvent
import com.velocity.app.data.api.ChatStreamPayload
import com.velocity.app.data.api.ChronologyItem
import com.velocity.app.data.api.CreateScheduleRequest
import com.velocity.app.data.api.ProposalRespondRequest
import com.velocity.app.data.api.ProposalRespondResult
import com.velocity.app.data.api.SaveDocRequest
import com.velocity.app.data.api.SearchResultItem
import com.velocity.app.data.api.SseStreamClient
import com.velocity.app.data.api.UpdateScheduleRequest
import com.velocity.app.data.api.UpdateSkillRequest
import com.velocity.app.data.api.VelocityApiService
import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.GoogleWorkspaceStatus
import com.velocity.app.data.model.ScheduledRoutine
import com.velocity.app.data.model.SkillRecord
import com.velocity.app.data.model.ThreadItem
import com.velocity.app.data.model.VaultTreeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
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

            val msgArray = when {
                element is kotlinx.serialization.json.JsonObject && element.containsKey("messages") -> element.jsonObject["messages"]?.jsonArray
                element is kotlinx.serialization.json.JsonArray -> element
                else -> null
            } ?: return emptyList()

            msgArray.mapNotNull { msgElem ->
                try {
                    json.decodeFromJsonElement<ChatMessage>(msgElem)
                } catch (e: Exception) {
                    try {
                        val obj = msgElem.jsonObject
                        val id = obj["id"]?.jsonPrimitive?.content ?: java.util.UUID.randomUUID().toString()
                        val role = obj["role"]?.jsonPrimitive?.content ?: "assistant"
                        val content = obj["content"]?.jsonPrimitive?.content ?: ""
                        val createdAt = obj["created_at"]?.jsonPrimitive?.content
                        ChatMessage(id = id, role = role, content = content, createdAt = createdAt)
                    } catch (_: Exception) {
                        null
                    }
                }
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
        val status = fetchGoogleStatus()
        return mapOf(
            "google_calendar" to status.connected,
            "google_tasks" to status.connected,
            "gmail" to status.connected
        )
    }

    suspend fun fetchGoogleStatus(): GoogleWorkspaceStatus {
        return try {
            val res = api.getIntegrationStatus()
            if (!res.isSuccessful) return GoogleWorkspaceStatus()
            val raw = res.body()?.string() ?: return GoogleWorkspaceStatus()
            json.decodeFromString<GoogleWorkspaceStatus>(raw)
        } catch (_: Exception) {
            GoogleWorkspaceStatus()
        }
    }

    suspend fun disconnectGoogle(): Boolean {
        return try {
            val res = api.disconnectGoogle()
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchVaultTree(): List<VaultTreeItem> {
        return try {
            val res = api.getVaultTree()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            val array = when {
                element is kotlinx.serialization.json.JsonObject && element.containsKey("tree") -> element.jsonObject["tree"]?.jsonArray
                element is kotlinx.serialization.json.JsonArray -> element
                else -> null
            } ?: return emptyList()

            array.mapNotNull { itemElem ->
                try {
                    json.decodeFromJsonElement<VaultTreeItem>(itemElem)
                } catch (_: Exception) {
                    try {
                        val obj = itemElem.jsonObject
                        val path = obj["path"]?.jsonPrimitive?.content ?: ""
                        val name = obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: path
                        val title = obj["title"]?.jsonPrimitive?.content
                        val category = obj["category"]?.jsonPrimitive?.content
                        if (path.isNotBlank()) VaultTreeItem(path = path, name = name, title = title, category = category) else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchVaultDoc(path: String): String {
        return try {
            val res = api.getMemoryDoc(path)
            if (!res.isSuccessful) return ""
            val raw = res.body()?.string() ?: return ""
            val element = json.parseToJsonElement(raw)
            if (element is kotlinx.serialization.json.JsonObject && element.containsKey("content")) {
                element.jsonObject["content"]?.jsonPrimitive?.content ?: ""
            } else raw
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun saveVaultDoc(path: String, content: String): Boolean {
        return try {
            val res = api.saveMemoryDoc(SaveDocRequest(path = path, content = content))
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchSchedules(): List<ScheduledRoutine> {
        return try {
            val res = api.getSchedules()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            val array = if (element is kotlinx.serialization.json.JsonArray) element else return emptyList()
            array.mapNotNull { itemElem ->
                try {
                    json.decodeFromJsonElement<ScheduledRoutine>(itemElem)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun createSchedule(
        name: String,
        eventType: String,
        prompt: String,
        cronExpression: String? = null,
        runAt: String? = null
    ): Boolean {
        return try {
            val res = api.createSchedule(
                CreateScheduleRequest(
                    name = name,
                    eventType = eventType,
                    prompt = prompt,
                    cronExpression = cronExpression,
                    runAt = runAt,
                    sessionId = "main"
                )
            )
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleSchedule(id: String, active: Boolean): Boolean {
        return try {
            val nextStatus = if (active) "active" else "paused"
            val res = api.updateSchedule(id, UpdateScheduleRequest(status = nextStatus))
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteSchedule(id: String): Boolean {
        return try {
            val res = api.deleteSchedule(id)
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchSkills(): List<SkillRecord> {
        return try {
            val res = api.getSkills()
            if (!res.isSuccessful) return emptyList()
            val raw = res.body()?.string() ?: return emptyList()
            val element = json.parseToJsonElement(raw)
            val array = if (element is kotlinx.serialization.json.JsonArray) element else return emptyList()
            array.mapNotNull { itemElem ->
                try {
                    json.decodeFromJsonElement<SkillRecord>(itemElem)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun toggleSkill(id: String, enabled: Boolean): Boolean {
        return try {
            val res = api.updateSkill(id, UpdateSkillRequest(enabled = enabled))
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateSkillInstructions(id: String, instructions: String): Boolean {
        return try {
            val res = api.updateSkill(id, UpdateSkillRequest(instructions = instructions))
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun respondProposal(messageId: String, accept: Boolean): ProposalRespondResult {
        val res = api.respondThreadProposal(
            messageId = messageId,
            payload = ProposalRespondRequest(if (accept) "accept" else "decline")
        )
        check(res.isSuccessful) { "Thread proposal failed (HTTP ${res.code()})" }
        return checkNotNull(res.body()) { "Thread proposal response is empty" }
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
        verbosity: String = "low"
    ): Flow<ChatStreamEvent> {
        val cleanModel = when (model?.lowercase()?.trim()) {
            "gpt-5.4", "flagship", "gpt-5-full" -> "gpt-5.4"
            else -> "gpt-5.4-mini"
        }
        val cleanVerbosity = when (verbosity.lowercase().trim()) {
            "low", "medium", "high" -> verbosity.lowercase().trim()
            "concise" -> "low"
            "exhaustive" -> "high"
            else -> "low"
        }
        val cleanEffort = when (thinkingEffort.lowercase().trim()) {
            "none", "low", "medium", "high", "max" -> thinkingEffort.lowercase().trim()
            else -> "medium"
        }

        return sseClient.streamChat(
            ChatStreamPayload(
                message = message,
                session_id = sessionId,
                model = cleanModel,
                thinking_effort = cleanEffort,
                verbosity = cleanVerbosity
            )
        )
    }
}
