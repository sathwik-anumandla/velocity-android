package com.velocity.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors

@Composable
fun ChatCapsule(
    message: ChatMessage,
    onOpenThread: ((String) -> Unit)? = null,
    onOpenArtifact: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"

    // Detect routine label
    val routineType = when {
        message.content.startsWith("# Morning Briefing", ignoreCase = true) ||
        message.content.startsWith("Morning Briefing:", ignoreCase = true) -> RoutineType.BRIEFING
        message.content.startsWith("# Evening Reflection", ignoreCase = true) ||
        message.content.startsWith("Evening Reflection:", ignoreCase = true) -> RoutineType.REFLECTION
        message.content.startsWith("# Reminder", ignoreCase = true) ||
        message.content.startsWith("Reminder:", ignoreCase = true) -> RoutineType.REMINDER
        else -> null
    }

    val displayContent = if (routineType != null) {
        message.content
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?morning briefing\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?evening reflection\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?reminder\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .trim()
    } else {
        message.content
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Thread Pill (if proposal accepted)
        message.threadProposal?.let { prop ->
            if (prop.status == "accepted" && prop.threadId != null) {
                ThreadPill(
                    title = prop.title,
                    onClick = { onOpenThread?.invoke(prop.threadId) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 6.dp)
                )
            }
        }

        // Artifact Pill (if artifact produced)
        message.artifact?.let { art ->
            ArtifactPill(
                title = art.title,
                onClick = { onOpenArtifact?.invoke(art.id) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 6.dp)
            )
        }

        // Message Capsule
        if (displayContent.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isUser) Color(0xFF1E1E22) else Color(0xFF141416))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    if (routineType != null) {
                        GlowingRoutineLabel(type = routineType)
                    }

                    Text(
                        text = displayContent,
                        fontFamily = SatoshiFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        color = if (isUser) Color.White else Color(0xFFD4D4D8)
                    )
                }
            }
        } else if (message.isStreaming && !isUser) {
            // Typing Indicator Bubble while awaiting first token
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BouncingDotsIndicator()
            }
        }
    }
}

@Composable
fun BouncingDotsIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")

    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )

    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )

    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFA1A1AA))
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFA1A1AA))
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFFA1A1AA))
        )
    }
}
