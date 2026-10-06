package com.velocity.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.ui.theme.JetBrainsMonoFontFamily
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownPadding

data class ThreadRollupData(
    val type: String,
    val title: String,
    val summary: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatCapsule(
    message: ChatMessage,
    onOpenThread: ((String) -> Unit)? = null,
    onOpenArtifact: ((String) -> Unit)? = null,
    isThread: Boolean = false,
    streamingStatus: String? = null,
    onLongPress: (() -> Unit)? = null,
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
            .then(if (onLongPress != null) Modifier.combinedClickable(onClick = {}, onLongClickLabel = "Message actions", onLongClick = onLongPress) else Modifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 1. Thread Rollup Card
        if (rollupData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(VelocityColors.SurfaceCard)
                    .clickable {
                        val threadId = message.threadProposal?.threadId
                        if (threadId != null) onOpenThread?.invoke(threadId)
                    }
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
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
                                    .background(VelocityColors.AccentBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(LucideIcons.Threads),
                                    contentDescription = null,
                                    tint = VelocityColors.AccentViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = rollupData.title,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelocityColors.TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(VelocityColors.AccentBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rollupData.type.uppercase(),
                                fontSize = 10.sp,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = VelocityColors.AccentViolet
                            )
                        }
                    }

                    if (rollupData.summary.isNotEmpty()) {
                        Text(
                            text = rollupData.summary,
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = VelocityColors.TextSecondary
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
                            color = VelocityColors.AccentViolet
                        )
                        Icon(
                            painter = painterResource(LucideIcons.ChevronRight),
                            contentDescription = null,
                            tint = VelocityColors.AccentViolet,
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
            ThreadResponse(message = message, streamingStatus = streamingStatus, selectable = onLongPress == null)
        } else if (displayContent.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .widthIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - 32.dp) * 0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isUser) VelocityColors.SurfaceCapsule else VelocityColors.SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    if (routineType != null) {
                        GlowingRoutineLabel(type = routineType)
                    }

                    if (isUser) {
                        val userText: @Composable () -> Unit = {
                            Text(displayContent, color = VelocityColors.TextPrimary, fontFamily = SatoshiFontFamily, fontSize = 15.sp, lineHeight = 22.sp)
                        }
                        if (onLongPress == null) SelectionContainer { userText() } else userText()
                    } else {
                        FormattedMarkdownText(content = displayContent, isUser = false, selectable = onLongPress == null)
                    }
                }
            }
        } else if (message.isStreaming && !isUser) {
            // Typing Indicator Bubble while awaiting first token
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(VelocityColors.SurfaceCard)
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
    selectable: Boolean = true,
    modifier: Modifier = Modifier,
    documentTheme: String? = null
) {
    val textColor = when (documentTheme) {
        "midnight", "technical-dark" -> Color(0xFFE4E4E7)
        null -> if (isUser) VelocityColors.TextPrimary else VelocityColors.TextSecondary
        else -> Color(0xFF202023)
    }
    val accentColor = if (documentTheme in setOf("midnight", "technical-dark")) Color(0xFF54E6D4) else if (documentTheme != null) Color(0xFF087F73) else VelocityColors.Accent
    val bodyStyle = TextStyle(
        fontFamily = if (documentTheme == "editorial") androidx.compose.ui.text.font.FontFamily.Serif else SatoshiFontFamily,
        fontSize = if (documentTheme?.startsWith("technical") == true) 14.sp else if (isThread) 15.5.sp else 15.sp,
        lineHeight = if (isThread) 24.sp else 22.sp,
        fontWeight = FontWeight.Medium,
        color = textColor
    )
    val headingStyle = bodyStyle.copy(fontWeight = FontWeight.Bold)
    val codeStyle = bodyStyle.copy(
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = accentColor
    )
    val codeBackground = when (documentTheme) {
        "midnight", "technical-dark" -> Color(0xFF0D0D10)
        null -> if (isThread) Color.Transparent else VelocityColors.SurfaceCode
        else -> Color(0xFFF0F0F3)
    }
    val hasBlocks = Regex("(?m)^\\s*(?:[-*+] |\\d+[.)] |```|\\|)").containsMatchIn(content)

    val rendered: @Composable () -> Unit = {
        Markdown(
            content = content,
            modifier = if (hasBlocks || documentTheme != null) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
            padding = markdownPadding(block = 8.dp, list = 4.dp, listItemBottom = 3.dp, indentList = 16.dp),
            colors = markdownColor(
                text = textColor,
                codeText = accentColor,
                inlineCodeText = accentColor,
                linkText = accentColor,
                codeBackground = codeBackground,
                inlineCodeBackground = codeBackground,
                dividerColor = VelocityColors.SurfaceElevated,
                tableText = textColor,
                tableBackground = Color.Transparent
            ),
            typography = markdownTypography(
                h1 = headingStyle.copy(fontSize = 20.sp, lineHeight = 28.sp),
                h2 = headingStyle.copy(fontSize = 18.sp, lineHeight = 26.sp),
                h3 = headingStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
                h4 = headingStyle,
                h5 = headingStyle,
                h6 = headingStyle,
                text = bodyStyle,
                paragraph = bodyStyle,
                ordered = bodyStyle,
                bullet = bodyStyle,
                list = bodyStyle,
                code = codeStyle,
                inlineCode = codeStyle,
                quote = bodyStyle.copy(fontSize = 14.sp, fontStyle = FontStyle.Italic),
                link = bodyStyle.copy(color = accentColor, textDecoration = TextDecoration.Underline)
            ),
            animations = markdownAnimations(animateTextSize = { this })
        )
    }
    if (selectable) SelectionContainer(modifier = modifier) { rendered() } else Box(modifier) { rendered() }
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
                .background(VelocityColors.TextMuted)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(VelocityColors.TextMuted)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(VelocityColors.TextMuted)
        )
    }
}
