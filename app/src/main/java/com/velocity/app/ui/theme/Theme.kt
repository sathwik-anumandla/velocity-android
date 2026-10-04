package com.velocity.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VelocityColorScheme = darkColorScheme(
    primary = VelocityColors.TextPrimary,
    onPrimary = VelocityColors.Canvas,
    primaryContainer = VelocityColors.SurfaceCapsule,
    onPrimaryContainer = VelocityColors.TextPrimary,
    background = VelocityColors.Canvas,
    onBackground = VelocityColors.TextPrimary,
    surface = VelocityColors.SurfaceCard,
    onSurface = VelocityColors.TextPrimary,
    surfaceVariant = VelocityColors.SurfaceCapsule,
    onSurfaceVariant = VelocityColors.TextSecondary,
)

@Composable
fun VelocityTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VelocityColors.Canvas.toArgb()
            window.navigationBarColor = VelocityColors.Canvas.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = VelocityColorScheme,
        typography = VelocityTypography,
        content = content
    )
}
