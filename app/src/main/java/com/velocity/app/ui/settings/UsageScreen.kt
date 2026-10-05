package com.velocity.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velocity.app.data.model.TokenPrices
import com.velocity.app.data.model.UsagePricing
import com.velocity.app.data.model.UsagePeriod
import com.velocity.app.data.model.UsageStats
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.ui.theme.VelocityColors
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
fun UsageScreen(repository: ChatRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UsageStats?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var prices by remember { mutableStateOf("") }
    var showPrices by remember { mutableStateOf(false) }

    fun applyStats(value: UsageStats) {
        stats = value
        prices = Json.encodeToString(value.settings.prices)
    }

    fun refresh() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                applyStats(repository.fetchUsage())
                error = null
            } catch (failure: Exception) {
                error = failure.message
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(repository) { refresh() }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Usage", style = MaterialTheme.typography.headlineSmall, color = VelocityColors.TextPrimary)
                TextButton(onClick = { refresh() }, enabled = !busy) { Text("Refresh") }
            }
            Text("USD estimates · UTC periods · tracked since this upgrade", color = VelocityColors.TextMuted)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        stats?.let { value ->
            item { UsagePeriodCard("Today", value.today) }
            item { UsagePeriodCard("This month", value.month) }
            item { UsagePeriodCard("All time", value.allTime) }
            item {
                TextButton(onClick = { showPrices = !showPrices }) { Text("Model prices / custom provider") }
                if (showPrices) {
                    Text("USD per million tokens. JSON model → input, cached_input, output. Configure actual provider rates to estimate costs. No spending limits are enforced.", color = VelocityColors.TextMuted)
                    OutlinedTextField(prices, { prices = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model price JSON") }, minLines = 4)
                }
                if (showPrices) Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val modelPrices = Json.decodeFromString<Map<String, TokenPrices>>(prices)
                            require(modelPrices.values.all { it.input.isFinite() && it.cachedInput.isFinite() && it.output.isFinite() && it.input >= 0 && it.cachedInput >= 0 && it.output >= 0 }) { "Prices must be nonnegative finite numbers" }
                            applyStats(repository.saveUsagePrices(UsagePricing(modelPrices)))
                            error = null
                        } catch (failure: Exception) {
                            error = failure.message
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("Save prices") }
            }
            item {
                Text("By model · all time", style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextPrimary)
                value.byModel.forEach { UsagePeriodCard(it.model.orEmpty(), it) }
            }
            item {
                Text("By activity · all time", style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextPrimary)
                value.bySource.forEach { Text("${it.source}: ${it.calls} calls · ${usd(it.costUsd)}", color = VelocityColors.TextSecondary) }
            }
            item { Text(value.coverage, color = VelocityColors.TextMuted) }
        }
    }
}

private fun usd(value: Double) = String.format(Locale.US, "$%.6f", value)

@Composable
private fun UsagePeriodCard(title: String, value: UsagePeriod) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("${usd(value.costUsd)} estimated · ${value.calls} API calls")
            Text("Input: ${value.inputTokens} · Output: ${value.outputTokens}")
            Text(if (value.reasoningReported > 0) "Reasoning: ${value.reasoningTokens} tokens · ${String.format(Locale.US, "%.1f", value.reasoningShare * 100)}% of reporting calls’ output" else "Reasoning: provider data unavailable")
            Text("Reasoning tokens are included in output, not billed twice.")
            Text("Reasoning reported by ${value.reasoningReported} calls")
            Text("Cache reads: ${value.cachedTokens} · Writes: ${value.cacheWriteTokens}")
            Text(if (value.cacheReported > 0) "Cache hit rate: ${String.format(Locale.US, "%.1f", value.cacheHitRate * 100)}% of reported input · ${value.cacheReported} calls" else "Cache hit rate: provider data unavailable")
            Text("Cache hits: ${value.cacheHitCalls} calls · Estimated savings: ${usd(value.cacheSavingsUsd)}")
            if (value.reservedUsd > 0) Text("Pending / unknown upper estimate: ${usd(value.reservedUsd)}")
            if (value.unpricedCalls > 0 || value.unreportedCalls > 0) Text("Unpriced: ${value.unpricedCalls} · Usage unavailable: ${value.unreportedCalls}")
        }
    }
}
