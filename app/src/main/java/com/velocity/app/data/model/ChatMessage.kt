package com.velocity.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@Serializable
data class ChatMessage(
    val id: String = "",
    val role: String = "assistant", // "user", "assistant", "system"
    val content: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_streaming") val isStreaming: Boolean = false,
    @Serializable(with = ThreadProposalFlexibleSerializer::class)
    @SerialName("thread_proposal") val threadProposal: ThreadProposal? = null,
    @Serializable(with = ArtifactItemFlexibleSerializer::class)
    val artifact: ArtifactItem? = null,
    @Serializable(with = StagedActionFlexibleSerializer::class)
    @SerialName("staged_action") val stagedAction: StagedAction? = null,
    val reasoning: String? = null,
    @SerialName("turn_id") val turnId: String? = null,
    @SerialName("turn_status") val turnStatus: String? = null
)

@kotlinx.serialization.ExperimentalSerializationApi
object ThreadProposalFlexibleSerializer : KSerializer<ThreadProposal?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThreadProposalFlexible")

    override fun serialize(encoder: Encoder, value: ThreadProposal?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            ThreadProposal.serializer().serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): ThreadProposal? {
        val input = decoder as? JsonDecoder ?: return null
        val element = input.decodeJsonElement()
        if (element is JsonNull) return null
        return try {
            if (element is JsonObject) {
                input.json.decodeFromJsonElement(ThreadProposal.serializer(), element)
            } else if (element is JsonPrimitive && element.isString) {
                input.json.decodeFromString(ThreadProposal.serializer(), element.content)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

@kotlinx.serialization.ExperimentalSerializationApi
object ArtifactItemFlexibleSerializer : KSerializer<ArtifactItem?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArtifactItemFlexible")

    override fun serialize(encoder: Encoder, value: ArtifactItem?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            ArtifactItem.serializer().serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): ArtifactItem? {
        val input = decoder as? JsonDecoder ?: return null
        val element = input.decodeJsonElement()
        if (element is JsonNull) return null
        return try {
            if (element is JsonObject) {
                input.json.decodeFromJsonElement(ArtifactItem.serializer(), element)
            } else if (element is JsonPrimitive && element.isString) {
                input.json.decodeFromString(ArtifactItem.serializer(), element.content)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

@kotlinx.serialization.ExperimentalSerializationApi
object StagedActionFlexibleSerializer : KSerializer<StagedAction?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StagedActionFlexible")

    override fun serialize(encoder: Encoder, value: StagedAction?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            StagedAction.serializer().serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): StagedAction? {
        val input = decoder as? JsonDecoder ?: return null
        val element = input.decodeJsonElement()
        if (element is JsonNull) return null
        return try {
            if (element is JsonObject) {
                input.json.decodeFromJsonElement(StagedAction.serializer(), element)
            } else if (element is JsonPrimitive && element.isString) {
                input.json.decodeFromString(StagedAction.serializer(), element.content)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class ThreadProposal(
    val title: String = "",
    val reason: String = "",
    @SerialName("suggested_first_turn") val suggestedFirstTurn: String? = null,
    val status: String = "pending", // "pending", "accepted", "declined"
    @SerialName("thread_id") val threadId: String? = null
)

@Serializable
data class StagedAction(
    val id: String = "",
    @SerialName("action_type") val actionType: String = "",
    val parameters: Map<String, JsonElement> = emptyMap(),
    val status: String = "pending"
)
