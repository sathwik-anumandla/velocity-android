package com.velocity.app.ui.theme

import androidx.compose.ui.graphics.Color

object VelocityColors {
    val isLight: Boolean get() = ThemePreference.mode == "light"
    // Canvas & Layered Surfaces (Strict Flat Design, Zero Borders)
    val Canvas: Color get() = if (isLight) Color(0xFFFFFFFF) else if (ThemePreference.mode == "dark") Color(0xFF101012) else Color(0xFF000000)          // Pure AMOLED black canvas
    val SurfaceCard: Color get() = if (isLight) Color(0xFFF4F4F5) else Color(0xFF141416)     // Containers, sheets, flyouts
    val SurfaceCapsule: Color get() = if (isLight) Color(0xFFEAEAED) else Color(0xFF1C1C1E)  // User chat capsule, floating docks
    val SurfaceElevated: Color get() = if (isLight) Color(0xFFDFDFE4) else Color(0xFF252528) // Hover / active touch states
    val SurfaceInput: Color get() = if (isLight) Color(0xFFF4F4F5) else Color(0xFF141416)    // Bottom message bar container
    val SurfaceCode: Color get() = if (isLight) Color(0xFFF8F8FA) else Color(0xFF0D0D10)     // Code block background
    val SurfaceCodeHeader: Color get() = if (isLight) Color(0xFFF0F0F3) else Color(0xFF141418)

    // Typography
    val TextPrimary: Color get() = if (isLight) Color(0xFF151518) else Color(0xFFFFFFFF)     // High-emphasis white
    val TextSecondary: Color get() = if (isLight) Color(0xFF34343A) else Color(0xFFD4D4D8)   // Medium-emphasis readable prose
    val TextMuted: Color get() = if (isLight) Color(0xFF62626C) else Color(0xFFA1A1AA)       // Metadata, timestamps, placeholders
    val TextDim: Color get() = if (isLight) Color(0xFF777782) else Color(0xFF71717A)         // De-emphasized details, subtle icons

    val Accent: Color get() = if (isLight) Color(0xFF087F73) else Color(0xFF54E6D4)
    val AccentBg: Color get() = Accent.copy(alpha = 0.15f)
    val AccentViolet: Color get() = Accent
    val AccentVioletBg: Color get() = AccentBg
    val AccentEmerald: Color get() = Accent
    val AccentEmeraldBg: Color get() = AccentBg
    val AccentAmber: Color get() = Accent
    val AccentAmberBg: Color get() = AccentBg
    val AccentIndigo: Color get() = Accent
    val AccentIndigoBg: Color get() = AccentBg
    val AccentSky: Color get() = Accent
    val AccentSkyBg: Color get() = AccentBg
    val StatusOnline: Color get() = Accent
    val StatusDegraded: Color get() = TextMuted
    val StatusOffline: Color get() = TextDim
}
