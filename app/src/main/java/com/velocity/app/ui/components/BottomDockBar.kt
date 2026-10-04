package com.velocity.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.velocity.app.ui.theme.VelocityColors

enum class DockTab {
    THREADS,
    DOCUMENTS,
    SEARCH,
    CHRONOLOGY,
    SETTINGS
}

@Composable
fun BottomDockBar(
    selectedTab: DockTab?,
    onSelectTab: (DockTab) -> Unit,
    isHealthy: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(VelocityColors.SurfaceCapsule)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DockIcon(
            icon = Icons.Default.AltRoute,
            isSelected = selectedTab == DockTab.THREADS,
            onClick = { onSelectTab(DockTab.THREADS) }
        )
        DockIcon(
            icon = Icons.Default.Description,
            isSelected = selectedTab == DockTab.DOCUMENTS,
            onClick = { onSelectTab(DockTab.DOCUMENTS) }
        )
        DockIcon(
            icon = Icons.Default.Search,
            isSelected = selectedTab == DockTab.SEARCH,
            onClick = { onSelectTab(DockTab.SEARCH) }
        )
        DockIcon(
            icon = Icons.Default.Schedule,
            isSelected = selectedTab == DockTab.CHRONOLOGY,
            onClick = { onSelectTab(DockTab.CHRONOLOGY) }
        )
        DockIcon(
            icon = Icons.Default.Tune,
            isSelected = selectedTab == DockTab.SETTINGS,
            onClick = { onSelectTab(DockTab.SETTINGS) }
        )

        // Health Dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isHealthy) VelocityColors.StatusOnline else VelocityColors.StatusDegraded)
        )
    }
}

@Composable
private fun DockIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) VelocityColors.SurfaceElevated else VelocityColors.SurfaceCapsule)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) VelocityColors.TextPrimary else VelocityColors.TextDim,
            modifier = Modifier.size(18.dp)
        )
    }
}
