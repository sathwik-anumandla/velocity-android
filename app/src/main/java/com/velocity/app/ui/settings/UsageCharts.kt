package com.velocity.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
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
internal fun UsageStat(label: String, value: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(VelocityColors.SurfaceCard, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = VelocityColors.TextMuted)
        Text(value, style = MaterialTheme.typography.headlineLarge, color = VelocityColors.TextPrimary)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
    }
}

@Composable
internal fun UsageInsight(title: String, value: Double, available: Boolean, detail: String, color: Color) {
    UsagePanel(title) {
        Text(if (available) usagePercent(value) else "—", style = MaterialTheme.typography.headlineLarge, color = VelocityColors.TextPrimary)
        Box(
            Modifier.fillMaxWidth().height(8.dp).background(VelocityColors.SurfaceElevated, RoundedCornerShape(8.dp))
                .semantics { contentDescription = if (available) "$title: ${usagePercent(value)}" else "$title: provider data unavailable" }
        ) {
            if (available && value > 0) Box(Modifier.fillMaxWidth(value.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(8.dp)))
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
    }
}

@Composable
internal fun UsageTokenMix(value: UsagePeriod) {
    val total = value.inputTokens + value.outputTokens
    UsagePanel("Token breakdown", "Input + output = total. Reasoning is already included in output.") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().semantics { contentDescription = "${usageCount(value.inputTokens)} input tokens and ${usageCount(value.outputTokens)} output tokens" }) {
                    val stroke = 8.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val origin = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    drawArc(if (total > 0) Color(0xFF818CF8) else VelocityColors.SurfaceElevated, -90f, 360f, false, origin, Size(diameter, diameter), style = Stroke(stroke))
                    if (total > 0) drawArc(VelocityColors.TextSecondary, -90f, (value.inputTokens.toDouble() / total * 360).toFloat(), false, origin, Size(diameter, diameter), style = Stroke(stroke))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics {}) {
                    Text(usageCompact(total), style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextPrimary)
                    Text("total tokens", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TokenLegend("Input", usageCount(value.inputTokens), VelocityColors.TextSecondary)
                TokenLegend("Output", usageCount(value.outputTokens), Color(0xFF818CF8))
            }
        }
        Text("Cached input: ${if (value.cacheReported > 0) usageCount(value.cachedTokens) else "not reported"} · Cache writes: ${if (value.cacheReported > 0) usageCount(value.cacheWriteTokens) else "not reported"}", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
        Text("Reasoning: ${if (value.reasoningReported > 0) usageCount(value.reasoningTokens) else "not reported"}", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
    }
}

@Composable
private fun TokenLegend(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).background(color, RoundedCornerShape(7.dp)))
            Text(label, style = MaterialTheme.typography.labelMedium, color = VelocityColors.TextMuted)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextPrimary)
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
                    drawRect(if (index == selectedIndex) Color(0xFF818CF8) else VelocityColors.TextDim, Offset(index * slot + gap / 2, size.height - height), Size(slot - gap, height))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(usageDate(days.first().date), style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                Text(usageDate(days.last().date), style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
            }
            Column(
                Modifier.fillMaxWidth().background(VelocityColors.SurfaceCapsule, RoundedCornerShape(12.dp)).padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("${usageDate(selected.date)} · ${display(amount(selected))} ${if (metric == 1) "estimated" else metricLabel}", style = MaterialTheme.typography.titleSmall, color = VelocityColors.TextPrimary)
                Text("${usageCount(selected.calls)} calls" + (if (selected.unpricedCalls > 0) " · ${selected.unpricedCalls} unpriced" else "") + (if (selected.unreportedCalls > 0) " · ${selected.unreportedCalls} missing usage" else ""), style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(enabled = selectedIndex > 0, onClick = { selectedDate = days[selectedIndex - 1].date }) { Text("Previous day", color = if (selectedIndex > 0) VelocityColors.TextPrimary else VelocityColors.TextDim) }
                    TextButton(enabled = selectedIndex < days.lastIndex, onClick = { selectedDate = days[selectedIndex + 1].date }) { Text("Next day", color = if (selectedIndex < days.lastIndex) VelocityColors.TextPrimary else VelocityColors.TextDim) }
                }
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
