# Velocity Android — Design System, Architecture & UI Specification

> **Single Source of Truth**: This document defines the design language, interaction rules, layout patterns, color tokens, and Jetpack Compose component specifications for the native Android Velocity client.

---

## 1. Core Visual Principles & Non-Negotiables

### 1.1 Strict Zero Borders
- **Zero Stroke / Outline Borders**: Do NOT use `Modifier.border()`, outline strokes, or line dividers.
- **Surface Elevation via Tone**: Separation and depth are achieved strictly through layered monochrome background tones (e.g. Canvas `#000000` -> Card `#141416` -> Capsule `#1C1C1E` -> Active/Elevated `#252528`).
- **Completely Flat Design**: All cards, capsules, buttons, bottom sheets, and flyouts have flat zero-elevation surfaces with rounded geometry.

### 1.2 Strict Zero Emojis
- Never use emojis anywhere in the codebase, UI copy, commit messages, status labels, or default prompts.
- Use clean, minimal vector iconography (Lucide / Material Symbols converted to Compose `ImageVector`).

### 1.3 Minimalist Monochrome with Functional Accents
- The interface is predominantly dark monochrome (OLED `#000000` with subtle zinc/neutral grays).
- Color is reserved strictly for functional semantic cues:
  - **Thread Pill**: Soft Violet (`#8B5CF6`) with squiggle icon.
  - **Artifact / Document Pill**: Emerald (`#10B981`) with document icon.
  - **Morning Briefing Label**: Amber glow (`#F59E0B`).
  - **Evening Reflection Label**: Cozy Indigo glow (`#6366F1`).
  - **Scheduled Reminders Label**: Sky Blue glow (`#0EA5E9`).
  - **Status Indicator**: Online (`#10B981`), Degraded (`#F59E0B`), Offline (`#EF4444`).

### 1.4 Branding & Header Identity
- The application header displays the brand title **velocity** centered in lowercase/normal casing with bold typography (`FontWeight.W700` or `W800`).
- No taglines, secondary labels, or clutter in the top bar.

---

## 2. Color Palette & Jetpack Compose Tokens

```kotlin
package com.velocity.app.ui.theme

import androidx.compose.ui.graphics.Color

object VelocityColors {
    // Canvas & Surfaces (AMOLED Dark)
    val Canvas = Color(0xFF000000)          // Pure AMOLED black background
    val SurfaceCard = Color(0xFF141416)     // Flyouts, bottom sheets, cards
    val SurfaceCapsule = Color(0xFF1C1C1E)  // User chat capsules, floating docks
    val SurfaceElevated = Color(0xFF252528) // Interactive hover/active states, pills
    val SurfaceInput = Color(0xFF141416)    // Bottom text input container
    val SurfaceCode = Color(0xFF0D0D10)     // Code block background
    val SurfaceCodeHeader = Color(0xFF141418)

    // Typography
    val TextPrimary = Color(0xFFFFFFFF)     // High-emphasis white
    val TextSecondary = Color(0xFFD4D4D8)   // Medium-emphasis readable prose
    val TextMuted = Color(0xFFA1A1AA)       // Metadata, timestamps, placeholders
    val TextDim = Color(0xFF71717A)         // De-emphasized details, subtle icons

    // Semantic Accents (Used Sparingly)
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
```

---

## 3. Typography & Hierarchy

Font Family: **Satoshi** (fallback to `Inter` / System Sans-Serif).

| Style | Size | Weight | Line Height | Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Brand Title** | 20sp | Bold (`W700`) | 24sp | Top app bar centered "velocity" |
| **Headline / Section** | 17sp | SemiBold (`W600`) | 22sp | Flyout headers, card titles |
| **Body (Timeline)** | 15sp | Medium (`W500`) | 22sp | Conversational bubbles, assistant prose |
| **Body (Thread)** | 15sp | Regular (`W400`) | 24sp | Long-form technical deep dives |
| **Routine Header** | 11sp | Bold (`W700`) | 14sp | Glowing uppercase label (BRIEFING, REFLECTION) |
| **Caption / Meta** | 12sp | Medium (`W500`) | 16sp | Timestamps, subtitles, status tags |
| **Code / Monospace** | 13.5sp | Regular (`W400`) | 20sp | Inline chips and syntax code blocks |

---

## 4. Dual Timeline Layout Architecture

Velocity operates on two distinct UI modes depending on the context:

### 4.1 Lifelong Main Timeline (Peer-to-Peer Capsule Mode)
- **Role**: Everyday thinking companion, high-signal conversational turns, daily briefings.
- **Capsule Structure**:
  - **User Turn**: Right-aligned capsule (`max-width: 82%`), rounded corners (`20dp`), background `#1E1E22`, white text.
  - **Assistant Turn**: Left-aligned capsule (`max-width: 82%`), rounded corners (`20dp`), background `#121214`, text `#D4D4D8`.
  - **Tight Vertical Rhythm**: Reduced spacing between request and response (`6dp` between turn pairs, `12dp` between distinct conversational blocks).
- **Turn Arrival**: Delivered atomically. While streaming, display a subtle 3-dot pulsing typing indicator inside a compact capsule before the complete response lands.

### 4.2 Dedicated Thread Workspace (Deep Engineering Mode)
- **Role**: Multi-step debugging, architectural RFC generation, long code implementations.
- **Layout**: Full-width prose, edge-to-edge reading canvas without bubble constraints.
- **Streaming**: Real-time incremental token rendering, live reasoning status indicator, full syntax-highlighted code blocks with copy action.

---

## 5. Proactive Routines & Glowing Text Shaders

Autonomous routines (Morning Briefings, Evening Reflections, Timed Reminders) render as clean, borderless text preceded by a subtle glowing uppercase label:

```kotlin
@Composable
fun GlowingRoutineLabel(
    text: String,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
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
```

- **Morning Briefing**: `glowColor = VelocityColors.AccentAmber`, label = `"MORNING BRIEFING"`.
- **Evening Reflection**: `glowColor = VelocityColors.AccentIndigo`, label = `"EVENING REFLECTION"`.
- **Scheduled Reminder**: `glowColor = VelocityColors.AccentSky`, label = `"REMINDER"`.

---

## 6. Component Specifications & Interaction Flows

### 6.1 Top Bar
- Height: `56dp`, background `VelocityColors.Canvas` (`#000000`).
- Left: System health dot (3.5dp emerald/amber circle).
- Center: Brand name `"velocity"` (`20sp`, Bold, `#FFFFFF`).
- Right: Minimal icon for Quick Action or Thread Conclude (when inside a thread).

### 6.2 Bottom Navigation Dock
- Floating, borderless container anchored above the system gesture navigation bar.
- Background: `VelocityColors.SurfaceCapsule` (`#1C1C1E`) with `32dp` pill rounding.
- 5 Centered Options:
  1. **Threads** (`LineSquiggle` icon) -> Opens Threads Sheet.
  2. **Documents** (`FileText` icon) -> Opens Artifacts & Vault Sheet.
  3. **Search** (`Search` icon) -> Opens Global Search Overlay.
  4. **Chronology** (`ClockHistory` icon) -> Opens Chronology Timeline.
  5. **Settings** (`Sliders` icon) -> Opens Two-Pane Settings Sheet.

### 6.3 Resource Pills (Threads & Artifacts)
Rendered as subtle, tactile pills inside the chat timeline when a thread is branched or a document is produced:

```
+-----------------------------------------------------------+
|  [Squiggle Icon]   Postgres WAL Tuning Benchmarks         |
|  (in violet sq)    Thread                                 |
+-----------------------------------------------------------+
```

- Height: `52dp`, padding: `12dp horizontal`, `8dp vertical`.
- Background: `VelocityColors.SurfaceCard` (`#141416`), active press: `#1E1E22`.
- Icon Container: `36dp x 36dp`, rounded `12dp` squircle with 15% tinted background.
- Tap Action: Seamlessly slides open the Thread detail view or Artifact Canvas.

### 6.4 Floating Action Approval Banner (Gmail Send / Controlled Tools)
When a staged action requires user confirmation:
- Displays floating directly above the bottom input capsule.
- Background: `VelocityColors.SurfaceCard` (`#18181B`), rounded `20dp`.
- Left: Amber spark icon with action summary (`"SEND EMAIL: Design Review Sync"`).
- Right: Two action buttons:
  - **Decline**: Subtle dark button (`#27272A`), white text.
  - **Approve**: High-contrast solid white pill, black bold text.

### 6.5 Input Capsule & Slash Command Palette
- **Capsule**: Rounded `26dp`, background `VelocityColors.SurfaceInput` (`#141416`).
- **Plus Button**: Toggles Effort, Model, Verbosity popover.
- **Text Field**: Auto-expanding `1` to `6` lines, placeholder `"Message velocity..."`.
- **Slash Autocomplete**: Typing `/` reveals a floating menu above the input listing skills (`/briefing`, `/reflection`, `/study`) with instant tab/tap selection.

---

## 7. Android Project Structure

```
velocity-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/java/com/velocity/app/
│       ├── data/
│       │   ├── api/
│       │   │   ├── VelocityApiService.kt       // Retrofit REST interface
│       │   │   └── SseStreamClient.kt          // OkHttp EventSource SSE client
│       │   ├── model/
│       │   │   ├── ChatMessage.kt              // Message, ThreadProposal, Artifact
│       │   │   ├── ThreadItem.kt               // Active & Concluded threads
│       │   │   ├── ArtifactItem.kt             // Document artifacts
│       │   │   └── ScheduledEvent.kt           // Proactive routines
│       │   └── repository/
│       │       ├── ChatRepository.kt           // Message store & streaming logic
│       │       └── ThreadRepository.kt         // Thread lifecycle & rollups
│       ├── ui/
│       │   ├── theme/
│       │   │   ├── Color.kt                    // Velocity color tokens
│       │   │   ├── Theme.kt                    // Jetpack Compose VelocityTheme
│       │   │   └── Typography.kt               // Satoshi / Inter font scale
│       │   ├── components/
│       │   │   ├── ChatCapsule.kt              // User and assistant message bubbles
│       │   │   ├── ThreadPill.kt               // Resource card for threads
│       │   │   ├── ArtifactPill.kt             // Resource card for documents
│       │   │   ├── ActionApprovalBanner.kt     // Floating approval banner
│       │   │   ├── GlowingRoutineLabel.kt      // Shader glowing routine label
│       │   │   ├── InputCapsule.kt             // Flat bottom message bar
│       │   │   └── BottomDockBar.kt            // 5-icon centered navigation dock
│       │   ├── timeline/
│       │   │   ├── MainTimelineScreen.kt       // Lifelong timeline view
│       │   │   └── TimelineViewModel.kt
│       │   ├── thread/
│       │   │   ├── ThreadWorkspaceScreen.kt    // Dedicated thread deep dive
│       │   │   └── ThreadViewModel.kt
│       │   ├── sheets/
│       │   │   ├── ThreadsListSheet.kt
│       │   │   ├── DocumentsListSheet.kt
│       │   │   ├── GlobalSearchSheet.kt
│       │   │   ├── ChronologySheet.kt
│       │   │   └── SettingsSheet.kt
│       │   └── MainActivity.kt
```
