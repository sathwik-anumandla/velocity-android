package com.velocity.app.ui.theme

import androidx.compose.ui.graphics.Color

object VelocityColors {
    // Canvas & Layered Surfaces (Strict Flat Design, Zero Borders)
    val Canvas = Color(0xFF000000)          // Pure AMOLED black canvas
    val SurfaceCard = Color(0xFF141416)     // Containers, sheets, flyouts
    val SurfaceCapsule = Color(0xFF1C1C1E)  // User chat capsule, floating docks
    val SurfaceElevated = Color(0xFF252528) // Hover / active touch states
    val SurfaceInput = Color(0xFF141416)    // Bottom message bar container
    val SurfaceCode = Color(0xFF0D0D10)     // Code block background
    val SurfaceCodeHeader = Color(0xFF141418)

    // Typography
    val TextPrimary = Color(0xFFFFFFFF)     // High-emphasis white
    val TextSecondary = Color(0xFFD4D4D8)   // Medium-emphasis readable prose
    val TextMuted = Color(0xFFA1A1AA)       // Metadata, timestamps, placeholders
    val TextDim = Color(0xFF71717A)         // De-emphasized details, subtle icons

    // Semantic Accents (Strictly Functional)
    val AccentViolet = Color(0xFF8B5CF6)    // Thread resource pills
    val AccentVioletBg = Color(0x268B5CF6)  // 15% opacity container
    val AccentEmerald = Color(0xFF10B981)   // Artifact / Document pills
    val AccentEmeraldBg = Color(0x2610B981) // 15% opacity container
    val AccentAmber = Color(0xFFF59E0B)     // Morning Briefings, warnings
    val AccentAmberBg = Color(0x26F59E0B)
    val AccentIndigo = Color(0xFF6366F1)    // Evening Reflections
    val AccentIndigoBg = Color(0x266366F1)
    val AccentSky = Color(0xFF0EA5E9)       // Scheduled Reminders, inline code
    val AccentSkyBg = Color(0x260EA5E9)

    // System Status Dots
    val StatusOnline = Color(0xFF10B981)
    val StatusDegraded = Color(0xFFF59E0B)
    val StatusOffline = Color(0xFFEF4444)
}
