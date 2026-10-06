package com.velocity.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.velocity.app.ui.components.LucideIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.velocity.app.data.model.DailyUsage
import com.velocity.app.data.model.UsagePeriod
import com.velocity.app.ui.theme.VelocityColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun usageUsd(value: Double): String {
    val precision = when { value == 0.0 || value >= 1 -> 2; value >= 0.01 -> 4; else -> 6 }
    return "$" + String.format(Locale.US, "%.${precision}f", value)
}

internal fun usageCount(value: Long) = NumberFormat.getIntegerInstance().format(value)

internal fun usageCompact(value: Long): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> usageCount(value)
}

internal fun usagePercent(value: Double) = String.format(Locale.US, "%.1f%%", value * 100)

private fun usageDate(value: String) = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}.getOrDefault(value)

internal fun usageSource(value: String) = when (value) {
    "chat" -> "Conversations"
    "scheduled" -> "Scheduled routines"
    "title" -> "Chat titles"
    "summary" -> "Context summaries"
    "rollup" -> "Thread summaries"
    "synthesis" -> "Memory synthesis"
    else -> value.replace('_', ' ')
}

@Composable
internal fun UsagePanel(title: String, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextPrimary)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted) }
        }
        content()
    }
}

@Composable
internal fun UsageSegments(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            Box(
                Modifier.weight(1f).background(if (index == selectedIndex) VelocityColors.SurfaceElevated else Color.Transparent, RoundedCornerShape(10.dp))
                    .semantics { selected = index == selectedIndex }
                    .clickable(role = Role.Tab) { onSelect(index) }.heightIn(min = 44.dp).padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (index == selectedIndex) VelocityColors.TextPrimary else VelocityColors.TextMuted) }
        }
    }
}

@Composable
internal fun UsageOverview(value: UsagePeriod) {
    UsagePanel("Estimated spending") {
        Text(if (value.unpricedCalls > 0 && value.costUsd == 0.0) "Unpriced" else usageUsd(value.costUsd), style = MaterialTheme.typography.displaySmall, color = VelocityColors.TextPrimary)
        Text("USD · priced model calls only", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
        androidx.compose.material3.HorizontalDivider(color = VelocityColors.SurfaceElevated)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Total tokens" to usageCount(value.inputTokens + value.outputTokens), "API calls" to usageCount(value.calls), "Cache savings" to usageUsd(value.cacheSavingsUsd)).forEach { (label, count) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                    Text(count, style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextPrimary)
                }
            }
        }
    }
}

@Composable
internal fun UsageTokenMix(value: UsagePeriod) {
    UsagePanel("Tokens", "Cached input and reasoning are subsets, not additional tokens.") {
        TokenCategory("Input tokens", value.inputTokens, "Cached input", value.cachedTokens, value.cacheReported, value.cacheHitRate, "of reported input served from cache", VelocityColors.AccentSky)
        TokenCategory("Output tokens", value.outputTokens, "Reasoning", value.reasoningTokens, value.reasoningReported, value.reasoningShare, "of reported output used for reasoning", VelocityColors.AccentViolet)
        if (value.cacheReported > 0 && value.cacheWriteTokens > 0) Text("Cache writes: ${usageCount(value.cacheWriteTokens)} tokens · reported separately", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
    }
}

@Composable
private fun TokenCategory(label: String, count: Long, subset: String, subsetCount: Long, reported: Long, share: Double, note: String, color: Color) {
    Column(Modifier.fillMaxWidth().background(VelocityColors.SurfaceCapsule, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).background(color, RoundedCornerShape(7.dp)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = VelocityColors.TextMuted)
        }
        Text(usageCount(count), style = MaterialTheme.typography.headlineLarge, color = VelocityColors.TextPrimary)
        androidx.compose.material3.HorizontalDivider(color = VelocityColors.SurfaceElevated)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(subset, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
            Text(if (reported > 0) usageCount(subsetCount) else "Not reported", style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextPrimary)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(VelocityColors.SurfaceElevated, RoundedCornerShape(4.dp))) {
            if (reported > 0 && share > 0) Box(Modifier.fillMaxWidth(share.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(4.dp)))
        }
        Text(if (reported > 0) "${usagePercent(share)} $note · ${usageCount(reported)} reporting calls" else "Your provider has not reported this breakdown.", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
    }
}

@Composable
internal fun UsageDailyChart(days: List<DailyUsage>) {
    var metric by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    val selectedIndex = if (selectedDate == null) days.lastIndex else days.indexOfFirst { it.date == selectedDate }.coerceAtLeast(0)
    val selected = days.getOrNull(selectedIndex)
    fun amount(day: DailyUsage) = when (metric) { 1 -> day.costUsd; 2 -> day.calls.toDouble(); else -> day.totalTokens.toDouble() }
    fun display(value: Double) = if (metric == 1) usageUsd(value) else usageCount(value.toLong())
    val maximum = days.maxOfOrNull { amount(it) } ?: 0.0
    val metricLabel = listOf("tokens", "cost", "calls")[metric]
    UsagePanel("Daily activity", "Last 30 days · UTC · tracked usage only") {
        UsageSegments(listOf("Tokens", "Cost", "Calls"), metric) { metric = it }
        Text("Peak: ${display(maximum)}", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
        if (selected == null) {
            Text("Update the backend to enable daily history.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
        } else {
            Canvas(
                Modifier.fillMaxWidth().height(132.dp).semantics {
                    contentDescription = "Daily $metricLabel chart. Peak ${display(maximum)}. Use previous and next day buttons to explore all days."
                }.pointerInput(days, metric) {
                    detectTapGestures { offset -> if (size.width > 0) selectedDate = days[(offset.x / size.width * days.size).toInt().coerceIn(0, days.lastIndex)].date }
                }
            ) {
                val slot = size.width / days.size
                val gap = 3.dp.toPx().coerceAtMost(slot / 3)
                for (fraction in listOf(0f, 0.5f, 1f)) drawLine(VelocityColors.SurfaceElevated, Offset(0f, size.height * fraction), Offset(size.width, size.height * fraction), 1.dp.toPx())
                days.forEachIndexed { index, day ->
                    val height = if (maximum > 0) (amount(day) / maximum * size.height).toFloat().coerceAtLeast(2.dp.toPx()) else 2.dp.toPx()
                    drawRect(if (index == selectedIndex) VelocityColors.AccentIndigo else VelocityColors.TextDim, Offset(index * slot + gap / 2, size.height - height), Size(slot - gap, height))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(usageDate(days.first().date), style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                Text(usageDate(days.last().date), style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
            }
            Row(
                Modifier.fillMaxWidth().background(VelocityColors.SurfaceCapsule, RoundedCornerShape(12.dp)).padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(enabled = selectedIndex > 0, onClick = { selectedDate = days[selectedIndex - 1].date }) { Icon(painterResource(LucideIcons.ChevronLeft), "Previous day", tint = if (selectedIndex > 0) VelocityColors.TextPrimary else VelocityColors.TextDim, modifier = Modifier.size(20.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${usageDate(selected.date)} · ${display(amount(selected))} ${if (metric == 1) "estimated" else metricLabel}", style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextPrimary)
                Text("${usageCount(selected.calls)} calls" + (if (selected.unpricedCalls > 0) " · ${selected.unpricedCalls} unpriced" else "") + (if (selected.unreportedCalls > 0) " · ${selected.unreportedCalls} missing usage" else ""), style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                }
                IconButton(enabled = selectedIndex < days.lastIndex, onClick = { selectedDate = days[selectedIndex + 1].date }) { Icon(painterResource(LucideIcons.ChevronRight), "Next day", tint = if (selectedIndex < days.lastIndex) VelocityColors.TextPrimary else VelocityColors.TextDim, modifier = Modifier.size(20.dp)) }
            }
            if (maximum == 0.0) Text(if (metric == 1) "No priced spending recorded. Unpriced calls are not free calls." else "No tracked activity for this metric in this window.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
        }
    }
}

@Composable
internal fun UsageBreakdownChart(title: String, note: String, rows: List<UsagePeriod>, models: Boolean) {
    var metric by remember { mutableIntStateOf(0) }
    val sorted = if (metric == 0) rows.sortedByDescending { it.calls } else rows.sortedByDescending { it.costUsd }
    val maximum = rows.maxOfOrNull { if (metric == 0) it.calls.toDouble() else it.costUsd } ?: 0.0
    UsagePanel(title, note) {
        UsageSegments(listOf("Calls", "Cost"), metric) { metric = it }
        if (rows.isEmpty()) Text("No tracked calls in this period.", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
        sorted.forEach { row ->
            val label = if (models) row.model ?: "Unknown model" else usageSource(row.source ?: "Unknown")
            val amount = if (metric == 0) row.calls.toDouble() else row.costUsd
            val description = if (metric == 0) "${usageCount(row.calls)} calls" else usageUsd(row.costUsd)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextSecondary, modifier = Modifier.weight(1f))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextPrimary)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).background(VelocityColors.SurfaceElevated, RoundedCornerShape(6.dp)).semantics { contentDescription = "$label: $description" }) {
                    if (maximum > 0 && amount > 0) Box(Modifier.fillMaxWidth((amount / maximum).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(VelocityColors.TextMuted, RoundedCornerShape(6.dp)))
                }
                if (row.unpricedCalls > 0) Text("${row.unpricedCalls} calls have no configured price", style = MaterialTheme.typography.labelSmall, color = VelocityColors.AccentAmber)
            }
        }
    }
}
