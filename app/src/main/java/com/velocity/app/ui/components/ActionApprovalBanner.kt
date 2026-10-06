package com.velocity.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.StagedAction
import com.velocity.app.ui.theme.MonoTextStyle
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
fun ActionApprovalCard(
    action: StagedAction,
    onDecline: () -> Unit,
    onApprove: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val to = action.parameters["to"]?.jsonPrimitive?.contentOrNull ?: "Recipient"
    val subject = action.parameters["subject"]?.jsonPrimitive?.contentOrNull ?: "(No Subject)"
    val body = action.parameters["body"]?.jsonPrimitive?.contentOrNull ?: ""
    val sendAt = action.parameters["send_at"]?.jsonPrimitive?.contentOrNull
    var expandedBody by remember(action.id) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VelocityColors.SurfaceCard)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x26F59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Mail),
                        contentDescription = null,
                        tint = VelocityColors.AccentAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "CONTROLLED ACTION APPROVAL",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = VelocityColors.AccentAmber
                    )
                    Text(
                        text = action.actionType.replace('_', ' ').uppercase(),
                        fontFamily = SatoshiFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VelocityColors.TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x26F59E0B))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Requires Approval",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = VelocityColors.AccentAmber
                )
            }
        }

        // Details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.SurfaceCapsule)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row {
                Text(text = "To: ", style = MonoTextStyle, fontSize = 11.sp, color = VelocityColors.TextMuted)
                Text(text = to, style = MonoTextStyle, fontSize = 11.sp, color = VelocityColors.TextPrimary)
            }
            for ((field, label) in listOf("cc" to "CC", "bcc" to "BCC", "send_at" to "Send at")) {
                action.parameters[field]?.jsonPrimitive?.contentOrNull?.let { value ->
                    Text(text = "$label: $value", style = MonoTextStyle, fontSize = 11.sp, color = VelocityColors.TextPrimary)
                }
            }
            Row {
                Text(text = "Subject: ", style = MonoTextStyle, fontSize = 11.sp, color = VelocityColors.TextMuted)
                Text(text = subject, style = MonoTextStyle, fontSize = 11.sp, color = VelocityColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (body.isNotEmpty()) {
                Text(
                    text = body,
                    style = MonoTextStyle,
                    fontSize = 11.sp,
                    color = VelocityColors.TextSecondary,
                    maxLines = if (expandedBody) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(text = if (expandedBody) "Collapse body" else "Read full body", fontSize = 11.sp, color = VelocityColors.TextPrimary, modifier = Modifier.clickable { expandedBody = !expandedBody })
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onDecline() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Decline",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = VelocityColors.TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(VelocityColors.TextPrimary)
                    .clickable(enabled = enabled) { onApprove() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (sendAt == null) "Approve & Send" else "Approve & Schedule",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.Canvas
                )
            }
        }
    }
}

@Composable
fun ProposalApprovalCard(
    title: String,
    reason: String,
    onDecline: () -> Unit,
    onApprove: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VelocityColors.SurfaceCapsule)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x26A78BFA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Threads),
                    contentDescription = null,
                    tint = VelocityColors.AccentViolet,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VelocityColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = reason.ifEmpty { "Branch into dedicated Side Chat" },
                    fontFamily = SatoshiFontFamily,
                    fontSize = 11.sp,
                    color = VelocityColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onDecline() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Continue Here",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = VelocityColors.TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) VelocityColors.TextPrimary else VelocityColors.TextDim)
                    .clickable(enabled = enabled) { onApprove() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Approve",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.Canvas
                )
            }
        }
    }
}
