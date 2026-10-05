package com.velocity.app.ui.components

import android.util.TypedValue
import android.graphics.Typeface
import android.widget.TextView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.ChatMessage
import androidx.core.content.res.ResourcesCompat
import com.velocity.app.R
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableAwareMovementMethod
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.movement.MovementMethodPlugin

data class ThreadRollupData(
    val type: String,
    val title: String,
    val summary: String
)

@Composable
fun ChatCapsule(
    message: ChatMessage,
    onOpenThread: ((String) -> Unit)? = null,
    onOpenArtifact: ((String) -> Unit)? = null,
    isThread: Boolean = false,
    streamingStatus: String? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val trimmed = message.content.trim()
    val lower = trimmed.lowercase()

    // 1. Detect Routine (Briefing, Reflection, Reminder)
    val routineType = when {
        lower.startsWith("# morning briefing") || lower.startsWith("**morning briefing") || lower.startsWith("morning briefing:") -> RoutineType.BRIEFING
        lower.startsWith("# evening reflection") || lower.startsWith("**evening reflection") || lower.startsWith("evening reflection:") -> RoutineType.REFLECTION
        lower.startsWith("# reminder") || lower.startsWith("**reminder") || lower.startsWith("reminder:") -> RoutineType.REMINDER
        else -> null
    }

    val displayContent = if (routineType != null) {
        trimmed
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?morning briefing\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?evening reflection\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .replaceFirst(Regex("^(#*\\s*\\*?\\*?reminder\\*?\\*?:?\\s*\\n*)", RegexOption.IGNORE_CASE), "")
            .trim()
    } else {
        trimmed
    }

    // 2. Detect Thread Rollup Milestone Card
    val rollupRegex = Regex("^\\[(Thread Update|Thread Concluded):\\s*([^\\]]+)\\]\\s*\\n?([\\s\\S]*)$", RegexOption.IGNORE_CASE)
    val rollupMatch = if (!isUser) rollupRegex.find(trimmed) else null
    val rollupData = rollupMatch?.let {
        ThreadRollupData(
            type = it.groupValues[1].trim(),
            title = it.groupValues[2].trim(),
            summary = it.groupValues[3].trim()
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 1. Thread Rollup Card
        if (rollupData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF16161A))
                    .clickable {
                        val threadId = message.threadProposal?.threadId
                        if (threadId != null) onOpenThread?.invoke(threadId)
                    }
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x26A78BFA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(LucideIcons.Threads),
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = rollupData.title,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x26A78BFA))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rollupData.type.uppercase(),
                                fontSize = 10.sp,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    }

                    if (rollupData.summary.isNotEmpty()) {
                        Text(
                            text = rollupData.summary,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFD4D4D8)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Open Thread",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA78BFA)
                        )
                        Icon(
                            painter = painterResource(LucideIcons.ChevronRight),
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            return@Column
        }

        // 2. Standalone Thread Pill (if proposal accepted)
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

        // 3. Standalone Artifact Pill
        message.artifact?.let { art ->
            ArtifactPill(
                title = art.title,
                onClick = { onOpenArtifact?.invoke(art.id) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 6.dp)
            )
        }

        // 4. Message Capsule
        if (isThread && !isUser) {
            ThreadResponse(message = message, streamingStatus = streamingStatus)
        } else if (displayContent.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isUser) Color(0xFF1E1E22) else Color(0xFF141416))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    if (routineType != null) {
                        GlowingRoutineLabel(type = routineType)
                    }

                    FormattedMarkdownText(
                        content = displayContent,
                        isUser = isUser
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
fun FormattedMarkdownText(
    content: String,
    isUser: Boolean,
    isThread: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember(context, isThread) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(MovementMethodPlugin.create(TableAwareMovementMethod.create()))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .linkColor(Color(0xFF38BDF8).toArgb())
                        .codeTextColor(Color(0xFF38BDF8).toArgb())
                        .codeBackgroundColor((if (isThread) Color.Transparent else Color(0xFF202024)).toArgb())
                        .codeBlockBackgroundColor((if (isThread) Color.Transparent else Color(0xFF202024)).toArgb())
                        .codeTypeface(ResourcesCompat.getFont(context, R.font.jetbrainsmono_regular) ?: Typeface.MONOSPACE)
                    if (isThread) {
                        builder.headingTextSizeMultipliers(floatArrayOf(20f / 15.5f, 18f / 15.5f, 16f / 15.5f, 1f, 1f, 1f))
                    }
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isThread) 15.5f else 15f)
                typeface = ResourcesCompat.getFont(viewContext, R.font.satoshi_medium)
                includeFontPadding = false
                setLineSpacing(0f, if (isThread) 1.55f else 22f / 15f)
            }
        },
        update = { textView ->
            textView.setTextColor((if (isUser) Color.White else Color(0xFFD4D4D8)).toArgb())
            markwon.setMarkdown(textView, content)
        }
    )
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
