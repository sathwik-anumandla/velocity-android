package com.velocity.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.velocity.app.data.model.TokenPrices
import com.velocity.app.data.model.UsagePricing
import com.velocity.app.data.model.UsageStats
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.ui.components.LucideIcons
import com.velocity.app.ui.theme.VelocityColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class PriceDraft(val input: String, val cachedInput: String, val output: String)

private fun priceDrafts(rates: Map<String, TokenPrices>) = rates.mapValues { (_, rate) -> PriceDraft(rate.input.toString(), rate.cachedInput.toString(), rate.output.toString()) }

@Composable
fun UsageScreen(repository: ChatRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember(repository) { mutableStateOf<UsageStats?>(null) }
    var error by remember(repository) { mutableStateOf<String?>(null) }
    var busy by remember(repository) { mutableStateOf(true) }
    var prices by remember(repository) { mutableStateOf<Map<String, PriceDraft>>(emptyMap()) }
    var priceDirty by remember(repository) { mutableStateOf(false) }
    var pricingError by remember(repository) { mutableStateOf<String?>(null) }
    var showPrices by rememberSaveable { mutableStateOf(false) }
    var range by rememberSaveable { mutableIntStateOf(0) }
    var modelName by remember(repository) { mutableStateOf("") }
    var updated by remember(repository) { mutableStateOf<String?>(null) }
    var saved by remember(repository) { mutableStateOf(false) }

    fun applyStats(value: UsageStats) {
        stats = value
        if (!priceDirty) prices = priceDrafts(value.settings.prices)
        updated = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        error = null
    }

    LaunchedEffect(repository) {
        try {
            applyStats(repository.fetchUsage())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Usage unavailable"
        } finally {
            busy = false
        }
    }

    fun refresh() {
        if (busy || priceDirty) return
        busy = true
        saved = false
        scope.launch {
            try {
                applyStats(repository.fetchUsage())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Usage unavailable"
            } finally {
                busy = false
            }
        }
    }

    val period = stats?.let { when (range) { 1 -> it.month; 2 -> it.allTime; else -> it.today } }
    val rangeKey = listOf("today", "month", "all_time")[range]
    val rangeLabel = listOf("Today", "This month", "All time")[range]
    val breakdown = stats?.breakdowns?.get(rangeKey)
    val breakdownNote = if (breakdown != null) rangeLabel else "All time · update backend for period filtering"

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Usage", style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextPrimary)
            IconButton(enabled = !busy && !priceDirty, onClick = { refresh() }) {
                Icon(painterResource(LucideIcons.Refresh), contentDescription = "Refresh usage", tint = if (!busy && !priceDirty) VelocityColors.TextPrimary else VelocityColors.TextDim, modifier = Modifier.size(20.dp))
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Your AI activity, at a glance", style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextPrimary)
                Text("USD estimates · periods use UTC" + (updated?.let { " · updated $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted, modifier = Modifier.padding(top = 5.dp))
            }
            item { UsageSegments(listOf("Today", "This month", "All time"), range) { range = it } }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = VelocityColors.TextSecondary, trackColor = VelocityColors.SurfaceElevated) }
            error?.let { failure ->
                item {
                    Column(Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text((if (stats != null) "Refresh failed. Showing the last loaded data. " else "") + failure, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        if (stats == null) TextButton(enabled = !busy, onClick = { refresh() }) { Text("Try again") }
                    }
                }
            }
            if (busy && stats == null) {
                items(2) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(2) { Box(Modifier.weight(1f).height(116.dp).background(VelocityColors.SurfaceCard, RoundedCornerShape(18.dp))) }
                    }
                }
            }
            stats?.let { value ->
                if (value.allTime.calls == 0L) item {
                    UsagePanel("Your first insights are on the way") {
                        Icon(painterResource(LucideIcons.Sparkles), null, tint = VelocityColors.AccentIndigo, modifier = Modifier.size(24.dp))
                        Text("Send a message to start tracking tokens, reasoning and cache savings. Only calls recorded since usage tracking was enabled appear here.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                    }
                }
                period?.let { summary ->
                    item {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UsageStat("Estimated cost", if (summary.unpricedCalls > 0 && summary.costUsd == 0.0) "Unpriced" else usageUsd(summary.costUsd), "Priced model calls only", Modifier.weight(1f).fillMaxHeight())
                            UsageStat("Total tokens", usageCompact(summary.inputTokens + summary.outputTokens), "${usageCount(summary.inputTokens + summary.outputTokens)} input + output", Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            UsageStat("API calls", usageCount(summary.calls), "Includes model/tool hops", Modifier.weight(1f).fillMaxHeight())
                            UsageStat("Cache savings", usageUsd(summary.cacheSavingsUsd), "Estimated vs. uncached input", Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    if (summary.unpricedCalls > 0 || summary.unreportedCalls > 0 || summary.reservedUsd > 0) item {
                        UsagePanel("Some costs are incomplete") {
                            Text("${summary.unpricedCalls} unpriced calls · ${summary.unreportedCalls} calls without reported usage.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.AccentAmber)
                            if (summary.reservedUsd > 0) Text("Pending / unknown upper estimate: ${usageUsd(summary.reservedUsd)}. Not added to the cost above.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                        }
                    }
                    item { UsageDailyChart(value.daily) }
                    item { UsageTokenMix(summary) }
                    item { UsageInsight("Cache effectiveness", summary.cacheHitRate, summary.cacheReported > 0, if (summary.cacheReported > 0) "${usageCount(summary.cachedTokens)} cached input tokens · ${usageCount(summary.cacheHitCalls)} cache-hit calls. Rate uses input from ${usageCount(summary.cacheReported)} reporting calls." else "The provider has not reported cache details for this period.", VelocityColors.AccentEmerald) }
                    item { UsageInsight("Reasoning share", summary.reasoningShare, summary.reasoningReported > 0, if (summary.reasoningReported > 0) "${usageCount(summary.reasoningTokens)} reasoning tokens, already included in output. Share uses output from ${usageCount(summary.reasoningReported)} reporting calls." else "The provider has not reported reasoning details for this period.", VelocityColors.AccentViolet) }
                }
                item { UsageBreakdownChart("Models", breakdownNote, breakdown?.byModel ?: value.byModel, true) }
                item { UsageBreakdownChart("Activity", breakdownNote, breakdown?.bySource ?: value.bySource, false) }
                item {
                    Column(Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(20.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { showPrices = !showPrices }.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Model pricing", style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextPrimary)
                            Text(if (showPrices) "Hide" else "Edit", style = MaterialTheme.typography.labelMedium, color = VelocityColors.TextMuted)
                        }
                        if (showPrices) {
                            Text("Optional provider rates in USD per million tokens. Changes affect future calls, not past estimates. Reset/remove restores a default rate when available. No spending limits are enforced.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                            prices.forEach { (model, draft) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(model, style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextSecondary, modifier = Modifier.weight(1f))
                                        TextButton(enabled = !busy, onClick = { prices = prices.filterKeys { it != model }; priceDirty = true; saved = false }) { Text("Reset/remove", style = MaterialTheme.typography.labelSmall) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Input" to draft.input, "Cached" to draft.cachedInput, "Output" to draft.output).forEachIndexed { index, (label, text) ->
                                            OutlinedTextField(
                                                text, { next ->
                                                    prices = prices + (model to when (index) { 0 -> draft.copy(input = next); 1 -> draft.copy(cachedInput = next); else -> draft.copy(output = next) })
                                                    priceDirty = true
                                                    saved = false
                                                }, modifier = Modifier.weight(1f), enabled = !busy, label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                                singleLine = true, textStyle = MaterialTheme.typography.bodySmall, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(modelName, { modelName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Custom model ID") }, singleLine = true, enabled = !busy)
                            TextButton(enabled = !busy && modelName.trim().isNotEmpty() && modelName.trim() !in prices, onClick = {
                                prices = prices + (modelName.trim() to PriceDraft("", "", ""))
                                modelName = ""
                                priceDirty = true
                                saved = false
                            }) { Text("Add model") }
                            pricingError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = !busy, onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            fun parse(raw: String): Double {
                                                val amount = raw.trim().replace(',', '.').toDoubleOrNull()
                                                require(amount != null && amount.isFinite() && amount >= 0) { "Enter a nonnegative number for every price." }
                                                return amount
                                            }
                                            val prepared = prices.mapValues { (_, draft) -> TokenPrices(parse(draft.input), parse(draft.cachedInput), parse(draft.output)) }
                                            val response = repository.saveUsagePrices(UsagePricing(prepared))
                                            priceDirty = false
                                            applyStats(response)
                                            pricingError = null
                                            saved = true
                                        } catch (cancelled: CancellationException) {
                                            throw cancelled
                                        } catch (failure: Exception) {
                                            pricingError = failure.message ?: "Could not save rates"
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }) { Text("Save rates") }
                                TextButton(enabled = !busy, onClick = { prices = priceDrafts(value.settings.prices); priceDirty = false; pricingError = null }) { Text("Discard edits") }
                            }
                        }
                    }
                }
                if (priceDirty) item { Text("Unsaved pricing edits · save or discard them to refresh.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.AccentAmber) }
                if (saved) item { Text("Rates saved. Future calls use these prices.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.AccentEmerald) }
                item {
                    var expanded by rememberSaveable { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Text("What is included?", style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextSecondary, modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 8.dp))
                        if (expanded) Text(value.coverage, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
    }
}
