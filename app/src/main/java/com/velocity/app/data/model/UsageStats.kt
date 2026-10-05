package com.velocity.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenPrices(
    val input: Double,
    @SerialName("cached_input") val cachedInput: Double,
    val output: Double
)

@Serializable
data class UsagePricing(
    val prices: Map<String, TokenPrices>
)

@Serializable
data class UsageSettings(
    val prices: Map<String, TokenPrices> = emptyMap()
)

@Serializable
data class UsagePeriod(
    val model: String? = null,
    val source: String? = null,
    val calls: Long = 0,
    @SerialName("input_tokens") val inputTokens: Long = 0,
    @SerialName("output_tokens") val outputTokens: Long = 0,
    @SerialName("cached_tokens") val cachedTokens: Long = 0,
    @SerialName("cache_write_tokens") val cacheWriteTokens: Long = 0,
    @SerialName("reasoning_tokens") val reasoningTokens: Long = 0,
    @SerialName("reasoning_reported") val reasoningReported: Long = 0,
    @SerialName("cache_reported") val cacheReported: Long = 0,
    @SerialName("cost_usd") val costUsd: Double = 0.0,
    @SerialName("reserved_usd") val reservedUsd: Double = 0.0,
    @SerialName("cache_hit_rate") val cacheHitRate: Double = 0.0,
    @SerialName("cache_savings_usd") val cacheSavingsUsd: Double = 0.0,
    @SerialName("cache_hit_calls") val cacheHitCalls: Long = 0,
    @SerialName("reasoning_share") val reasoningShare: Double = 0.0,
    @SerialName("unpriced_calls") val unpricedCalls: Long = 0,
    @SerialName("unreported_calls") val unreportedCalls: Long = 0
)

@Serializable
data class DailyUsage(
    val date: String,
    val calls: Long = 0,
    @SerialName("total_tokens") val totalTokens: Long = 0,
    @SerialName("cost_usd") val costUsd: Double = 0.0,
    @SerialName("unpriced_calls") val unpricedCalls: Long = 0,
    @SerialName("unreported_calls") val unreportedCalls: Long = 0
)

@Serializable
data class UsageBreakdown(
    @SerialName("by_model") val byModel: List<UsagePeriod> = emptyList(),
    @SerialName("by_source") val bySource: List<UsagePeriod> = emptyList()
)

@Serializable
data class UsageStats(
    val settings: UsageSettings,
    val today: UsagePeriod,
    val month: UsagePeriod,
    @SerialName("all_time") val allTime: UsagePeriod,
    @SerialName("by_model") val byModel: List<UsagePeriod> = emptyList(),
    @SerialName("by_source") val bySource: List<UsagePeriod> = emptyList(),
    val breakdowns: Map<String, UsageBreakdown> = emptyMap(),
    val daily: List<DailyUsage> = emptyList(),
    val coverage: String = ""
)
