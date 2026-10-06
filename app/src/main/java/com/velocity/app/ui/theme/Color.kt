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

    // Semantic Accents (Strictly Functional)
    val AccentViolet: Color get() = if (isLight) Color(0xFF7C3AED) else Color(0xFFA78BFA)    // Thread resource pills
    val AccentVioletBg = Color(0x268B5CF6)  // 15% opacity container
    val AccentEmerald: Color get() = if (isLight) Color(0xFF047857) else Color(0xFF34D399)   // Artifact / Document pills
    val AccentEmeraldBg = Color(0x2610B981) // 15% opacity container
    val AccentAmber: Color get() = if (isLight) Color(0xFFB45309) else Color(0xFFF59E0B)     // Morning Briefings, warnings
    val AccentAmberBg = Color(0x26F59E0B)
    val AccentIndigo = Color(0xFF6366F1)    // Evening Reflections
    val AccentIndigoBg = Color(0x266366F1)
    val AccentSky: Color get() = if (isLight) Color(0xFF0369A1) else Color(0xFF38BDF8)       // Scheduled Reminders, inline code
    val AccentSkyBg = Color(0x260EA5E9)

    // System Status Dots
    val StatusOnline = Color(0xFF10B981)
    val StatusDegraded = Color(0xFFF59E0B)
    val StatusOffline = Color(0xFFEF4444)
}
