package com.velocity.app.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object ThemePreference {
    var mode by mutableStateOf("oled")
        private set

    fun load(context: Context) {
        mode = context.getSharedPreferences("velocity_appearance", Context.MODE_PRIVATE).getString("theme", "oled") ?: "oled"
    }

    fun select(context: Context, value: String) {
        mode = value
        context.getSharedPreferences("velocity_appearance", Context.MODE_PRIVATE).edit().putString("theme", value).apply()
    }
}

@Composable
fun VelocityTheme(content: @Composable () -> Unit) {
    val scheme = if (VelocityColors.isLight) lightColorScheme() else darkColorScheme()
    val colors = scheme.copy(
        primary = VelocityColors.TextPrimary,
        onPrimary = VelocityColors.Canvas,
        primaryContainer = VelocityColors.SurfaceCapsule,
        onPrimaryContainer = VelocityColors.TextPrimary,
        secondaryContainer = VelocityColors.SurfaceCapsule,
        onSecondaryContainer = VelocityColors.TextPrimary,
        background = VelocityColors.Canvas,
        onBackground = VelocityColors.TextPrimary,
        surface = VelocityColors.SurfaceCard,
        onSurface = VelocityColors.TextPrimary,
        surfaceVariant = VelocityColors.SurfaceCapsule,
        onSurfaceVariant = VelocityColors.TextSecondary,
        surfaceContainer = VelocityColors.SurfaceCard,
        surfaceContainerHigh = VelocityColors.SurfaceElevated,
        secondary = VelocityColors.Accent,
        tertiary = VelocityColors.Accent,
        error = VelocityColors.Accent,
        onError = VelocityColors.Canvas,
        errorContainer = VelocityColors.AccentBg,
        onErrorContainer = VelocityColors.TextPrimary,
        outline = VelocityColors.TextDim
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = VelocityColors.isLight
            controller.isAppearanceLightNavigationBars = VelocityColors.isLight
        }
    }
    MaterialTheme(colorScheme = colors, typography = VelocityTypography, content = content)
}
