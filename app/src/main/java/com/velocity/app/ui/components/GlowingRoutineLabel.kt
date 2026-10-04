package com.velocity.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.ui.theme.VelocityColors

enum class RoutineType {
    BRIEFING,
    REFLECTION,
    REMINDER
}

@Composable
fun GlowingRoutineLabel(
    type: RoutineType,
    modifier: Modifier = Modifier
) {
    val (label, glowColor) = when (type) {
        RoutineType.BRIEFING -> "MORNING BRIEFING" to VelocityColors.AccentAmber
        RoutineType.REFLECTION -> "EVENING REFLECTION" to VelocityColors.AccentIndigo
        RoutineType.REMINDER -> "SCHEDULED REMINDER" to VelocityColors.AccentSky
    }

    Text(
        text = label,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = glowColor,
            shadow = Shadow(
                color = glowColor.copy(alpha = 0.65f),
                offset = Offset(0f, 0f),
                blurRadius = 14f
            )
        ),
        modifier = modifier.padding(bottom = 6.dp)
    )
}
