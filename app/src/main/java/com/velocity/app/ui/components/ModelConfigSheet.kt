package com.velocity.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.repository.ModelConfig
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTypography
import com.velocity.app.ui.util.VelocityHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigSheet(
    isOpen: Boolean,
    currentConfig: ModelConfig,
    onConfigChange: (ModelConfig) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val supportedModels = listOf(
        "gpt-5.4-mini" to "Fast, lightweight daily driver",
        "gpt-5.4" to "Flagship deep intelligence"
    )

    val effortLevels = listOf("none", "low", "medium", "high", "max")
    val verbosityLevels = listOf("low" to "Concise", "medium" to "Balanced", "high" to "Comprehensive")
    val recallLevels = listOf("low" to "Low", "medium" to "Balanced", "high" to "Deep")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF323236))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model & Reasoning",
                    style = VelocityTypography.titleLarge,
                    color = VelocityColors.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222226))
                        .clickable {
                            VelocityHaptics.lightClick(context)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Close),
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 1. Model Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACTIVE MODEL",
                    style = VelocityTypography.labelSmall,
                    color = VelocityColors.TextMuted
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1E)),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    supportedModels.forEachIndexed { index, (modelId, desc) ->
                        val isSelected = currentConfig.model.equals(modelId, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0xFF24242A) else Color(0xFF1A1A1E))
                                .clickable {
                                    VelocityHaptics.subtleTick(context)
                                    onConfigChange(currentConfig.copy(model = modelId))
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = modelId,
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else VelocityColors.TextSecondary
                                )
                                Text(
                                    text = desc,
                                    style = VelocityTypography.bodySmall,
                                    color = VelocityColors.TextDim
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Thinking Effort
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "THINKING EFFORT",
                    style = VelocityTypography.labelSmall,
                    color = VelocityColors.TextMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    effortLevels.forEach { effort ->
                        val isSelected = currentConfig.thinkingEffort.equals(effort, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
                                .clickable {
                                    VelocityHaptics.subtleTick(context)
                                    onConfigChange(currentConfig.copy(thinkingEffort = effort))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = effort.replaceFirstChar { it.uppercase() },
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }

            // 3. Verbosity
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VERBOSITY",
                    style = VelocityTypography.labelSmall,
                    color = VelocityColors.TextMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    verbosityLevels.forEach { (verbKey, verbLabel) ->
                        val isSelected = currentConfig.verbosity.equals(verbKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
                                .clickable {
                                    VelocityHaptics.subtleTick(context)
                                    onConfigChange(currentConfig.copy(verbosity = verbKey))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = verbLabel,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }

            // 4. Memory Recall Budget
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "MEMORY RECALL BUDGET",
                    style = VelocityTypography.labelSmall,
                    color = VelocityColors.TextMuted
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1A1A1E))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recallLevels.forEach { (recKey, recLabel) ->
                        val isSelected = currentConfig.recallBudget.equals(recKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
                                .clickable {
                                    VelocityHaptics.subtleTick(context)
                                    onConfigChange(currentConfig.copy(recallBudget = recKey))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = recLabel,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

