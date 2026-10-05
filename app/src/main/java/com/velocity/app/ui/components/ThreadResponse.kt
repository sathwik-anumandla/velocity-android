package com.velocity.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.ui.theme.JetBrainsMonoFontFamily
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.util.VelocityHaptics

@Composable
fun ThreadResponse(message: ChatMessage, streamingStatus: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var reasoningExpanded by remember(message.id) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (message.isStreaming && message.content.isEmpty()) {
            ThreadStatusText(streamingStatus ?: "Thinking")
        }

        val reasoning = message.reasoning
        if (!reasoning.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        reasoningExpanded = !reasoningExpanded
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.ChevronRight),
                    contentDescription = if (reasoningExpanded) "Collapse reasoning" else "Expand reasoning",
                    tint = VelocityColors.TextMuted,
                    modifier = Modifier.size(14.dp).rotate(if (reasoningExpanded) 90f else 0f)
                )
                Text(
                    text = "Thinking Process",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VelocityColors.TextMuted
                )
            }
            AnimatedVisibility(visible = reasoningExpanded) {
                Text(
                    text = reasoning,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 13.sp,
                    color = VelocityColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF18181B))
                        .padding(12.dp)
                )
            }
        }

        if (message.content.isNotEmpty()) {
            FormattedMarkdownText(
                content = message.content,
                isUser = false,
                isThread = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ThreadStatusText(text: String) {
    var textWidth by remember { mutableIntStateOf(1) }
    val transition = rememberInfiniteTransition(label = "threadStatus")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Restart),
        label = "shimmer"
    )
    val left = -textWidth.toFloat() + textWidth * 2f * progress
    Text(
        text = text,
        style = TextStyle(
            fontFamily = SatoshiFontFamily,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.3).sp,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF71717A), Color(0xFF71717A), Color.White, Color(0xFF71717A), Color(0xFF71717A)),
                start = Offset(left, 0f),
                end = Offset(left + textWidth * 1.6f, 0f)
            )
        ),
        modifier = Modifier.padding(vertical = 6.dp).onSizeChanged { textWidth = it.width.coerceAtLeast(1) }
    )
}
