package com.velocity.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.util.VelocityHaptics

@Composable
fun InputCapsule(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String = "Message velocity...",
    onOptionsClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isStreaming: Boolean = false,
    onStop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF161618))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Options Plus Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF222226))
                .clickable {
                    VelocityHaptics.lightClick(context)
                    onOptionsClick?.invoke()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(LucideIcons.Plus),
                contentDescription = "Options",
                tint = VelocityColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Text Field Container
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = VelocityColors.TextMuted,
                    fontSize = 15.sp,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = VelocityColors.TextPrimary,
                    fontSize = 15.sp,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(VelocityColors.AccentSky),
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Send Button
        AnimatedVisibility(
            visible = value.isNotBlank() || isStreaming,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (enabled || isStreaming) Color.White else Color(0xFF71717A))
                    .clickable(enabled = enabled || isStreaming) {
                        VelocityHaptics.lightClick(context)
                        if (isStreaming) onStop() else onSend()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isStreaming) Box(Modifier.size(12.dp).background(Color.Black, RoundedCornerShape(2.dp))) else Icon(
                    painter = painterResource(LucideIcons.ArrowUp),
                    contentDescription = "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
