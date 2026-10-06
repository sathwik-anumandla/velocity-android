package com.velocity.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.GoogleWorkspaceStatus
import com.velocity.app.data.model.ScheduledRoutine
import com.velocity.app.data.model.SkillRecord
import com.velocity.app.data.model.VaultTreeItem
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.data.repository.ModelConfig
import com.velocity.app.data.repository.ServerConfig
import com.velocity.app.data.repository.ServerConfigManager
import com.velocity.app.ui.components.LucideIcons
import com.velocity.app.ui.theme.MonoTextStyle
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTypography
import com.velocity.app.ui.util.VelocityHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SettingsTab {
    ROOT,
    GENERAL,
    MEMORY,
    PLUGINS,
    SCHEDULES,
    SKILLS,
    USAGE
}

@Composable
fun SettingsScreen(
    repository: ChatRepository,
    serverConfig: ServerConfig,
    onClose: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(SettingsTab.ROOT) }
    var modelConfig by remember { mutableStateOf(ServerConfigManager.loadModelConfig(context)) }

    BackHandler {
        if (currentTab == SettingsTab.ROOT) onClose() else currentTab = SettingsTab.ROOT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelocityColors.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                if (targetState == SettingsTab.ROOT) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                }
            },
            label = "settings_tab_nav"
        ) { tab ->
            when (tab) {
                SettingsTab.USAGE -> UsageScreen(repository) { currentTab = SettingsTab.ROOT }
                SettingsTab.ROOT -> SettingsRootView(
                    serverConfig = serverConfig,
                    modelConfig = modelConfig,
                    onNavigate = {
                        VelocityHaptics.lightClick(context)
                        currentTab = it
                    },
                    onClose = onClose
                )

                SettingsTab.GENERAL -> GeneralTabSubpage(
                    serverConfig = serverConfig,
                    config = modelConfig,
                    repository = repository,
                    onConfigChange = {
                        modelConfig = it
                        ServerConfigManager.saveModelConfig(context, it)
                    },
                    onBack = {
                        VelocityHaptics.lightClick(context)
                        currentTab = SettingsTab.ROOT
                    },
                    onDisconnect = onDisconnect
                )

                SettingsTab.MEMORY -> MemoryTabSubpage(
                    repository = repository,
                    onBack = {
                        VelocityHaptics.lightClick(context)
                        currentTab = SettingsTab.ROOT
                    }
                )

                SettingsTab.PLUGINS -> PluginsTabSubpage(
                    repository = repository,
                    onBack = {
                        VelocityHaptics.lightClick(context)
                        currentTab = SettingsTab.ROOT
                    }
                )

                SettingsTab.SCHEDULES -> SchedulesTabSubpage(
                    repository = repository,
                    onBack = {
                        VelocityHaptics.lightClick(context)
                        currentTab = SettingsTab.ROOT
                    }
                )

                SettingsTab.SKILLS -> SkillsTabSubpage(
                    repository = repository,
                    onBack = {
                        VelocityHaptics.lightClick(context)
                        currentTab = SettingsTab.ROOT
                    }
                )
            }
        }
    }
}

// ==============================================================================
// 1. ROOT VIEW: Exactly the 5 Categories matching Web Version
// ==============================================================================
@Composable
private fun SettingsRootView(
    serverConfig: ServerConfig,
    modelConfig: ModelConfig,
    onNavigate: (SettingsTab) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = VelocityTypography.titleLarge,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VelocityColors.TextPrimary
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.lightClick(context)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Grouped Category Cards
            item {
                Text(
                    text = "PREFERENCES & TOOLS",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(VelocityColors.SurfaceCard)
                ) {
                    // 1. General
                    SettingsRowItem(
                        icon = LucideIcons.Sliders,
                        iconTint = VelocityColors.TextMuted,
                        title = "General",
                        subtitle = "Model ${modelConfig.model} · ${modelConfig.thinkingEffort.replaceFirstChar { it.uppercase() }} reasoning",
                        onClick = { onNavigate(SettingsTab.GENERAL) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated, modifier = Modifier.padding(horizontal = 16.dp))

                    // 2. Memory
                    SettingsRowItem(
                        icon = LucideIcons.Brain,
                        iconTint = VelocityColors.AccentSky,
                        title = "Memory",
                        subtitle = "Browse and edit saved memories",
                        onClick = { onNavigate(SettingsTab.MEMORY) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated, modifier = Modifier.padding(horizontal = 16.dp))

                    // 3. Plugins
                    SettingsRowItem(
                        icon = LucideIcons.Puzzle,
                        iconTint = VelocityColors.AccentAmber,
                        title = "Plugins",
                        subtitle = "Calendar, Tasks and Gmail",
                        onClick = { onNavigate(SettingsTab.PLUGINS) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated, modifier = Modifier.padding(horizontal = 16.dp))

                    // 4. Schedules
                    SettingsRowItem(
                        icon = LucideIcons.Clock,
                        iconTint = VelocityColors.AccentViolet,
                        title = "Schedules",
                        subtitle = "Create and manage routines",
                        onClick = { onNavigate(SettingsTab.SCHEDULES) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated, modifier = Modifier.padding(horizontal = 16.dp))

                    // 5. Skills
                    SettingsRowItem(
                        icon = LucideIcons.Sparkles,
                        iconTint = VelocityColors.AccentEmerald,
                        title = "Skills",
                        subtitle = "Customize skills and instructions",
                        onClick = { onNavigate(SettingsTab.SKILLS) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated)
                    SettingsRowItem(
                        icon = LucideIcons.Chart,
                        iconTint = VelocityColors.AccentSky,
                        title = "Usage",
                        subtitle = "Spending, tokens and cache insights",
                        onClick = { onNavigate(SettingsTab.USAGE) }
                    )
                }
            }

            // Connection Summary Inset Card
            item {
                Text(
                    text = "ACTIVE PAIRING",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(VelocityColors.SurfaceCard)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Server Endpoint",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = serverConfig.baseUrl.ifEmpty { "Connected" },
                            style = MonoTextStyle,
                            fontSize = 12.sp,
                            color = VelocityColors.TextPrimary
                        )
                    }

                    if (serverConfig.cfClientId.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cloudflare Access",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = VelocityColors.TextMuted
                            )
                            Text(
                                text = "Service Token Active",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VelocityColors.AccentEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. GENERAL TAB: Models, Reasoning Effort, Verbosity, Recall, Server Diagnostics
// ==============================================================================
@Composable
private fun GeneralTabSubpage(
    serverConfig: ServerConfig,
    config: ModelConfig,
    repository: ChatRepository,
    onConfigChange: (ModelConfig) -> Unit,
    onBack: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    val supportedModels = listOf(
        "gpt-5.4-mini" to "Fast, lightweight daily driver",
        "gpt-5.4" to "Flagship deep intelligence"
    )
    val effortLevels = listOf("none", "low", "medium", "high", "max")
    val verbosityLevels = listOf("low" to "Concise", "medium" to "Balanced", "high" to "Comprehensive")
    val recallLevels = listOf("low" to "Low", "medium" to "Balanced", "high" to "Deep")

    LaunchedEffect(Unit) {
        isPinging = true
        val start = System.currentTimeMillis()
        repository.testConnection()
        latencyMs = System.currentTimeMillis() - start
        isPinging = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SettingsSubpageHeader(title = "General", onBack = onBack)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Primary Model
            item { DeploymentInfo(repository) }
            item {
                SectionCard(title = "Appearance", subtitle = "Choose a theme for every screen.", icon = LucideIcons.Sun) {
                    val options = listOf("Light", "Dark", "OLED")
                    val modes = listOf("light", "dark", "oled")
                    UsageSegments(options, modes.indexOf(com.velocity.app.ui.theme.ThemePreference.mode).coerceAtLeast(0)) { index ->
                        com.velocity.app.ui.theme.ThemePreference.select(context, modes[index])
                    }
                }
            }
            item {
                SectionCard(title = "Model", subtitle = "Balance speed and reasoning depth.", icon = LucideIcons.Sparkles) {
                    supportedModels.forEach { (mId, mDesc) ->
                        val isSelected = config.model.equals(mId, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) VelocityColors.SurfaceElevated else Color.Transparent)
                                .clickable {
                                    VelocityHaptics.lightClick(context)
                                    onConfigChange(config.copy(model = mId))
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (mId == "gpt-5.4-mini") "GPT-5.4 Mini" else "GPT-5.4 Flagship",
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VelocityColors.TextPrimary
                                )
                                Text(
                                    text = mDesc,
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 12.sp,
                                    color = VelocityColors.TextMuted
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(VelocityColors.TextPrimary)
                                )
                            }
                        }
                    }
                }
            }

            // Thinking Effort
            item {
                SectionCard(title = "Reasoning", subtitle = "Choose how deeply the model thinks.", icon = LucideIcons.Brain) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        effortLevels.forEach { effort ->
                            val isSelected = config.thinkingEffort.equals(effort, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VelocityColors.SurfaceElevated else Color.Transparent)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        onConfigChange(config.copy(thinkingEffort = effort))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = effort.replaceFirstChar { it.uppercase() },
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) VelocityColors.TextPrimary else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Verbosity
            item {
                SectionCard(title = "Response length", subtitle = "Choose concise or detailed answers.", icon = LucideIcons.Documents) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        verbosityLevels.forEach { (vKey, vLabel) ->
                            val isSelected = config.verbosity.equals(vKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VelocityColors.SurfaceElevated else Color.Transparent)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        onConfigChange(config.copy(verbosity = vKey))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = vLabel,
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) VelocityColors.TextPrimary else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Recall Budget
            item {
                SectionCard(title = "Memory recall", subtitle = "Set how much memory to search.", icon = LucideIcons.History) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recallLevels.forEach { (rKey, rLabel) ->
                            val isSelected = config.recallBudget.equals(rKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) VelocityColors.SurfaceElevated else Color.Transparent)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        onConfigChange(config.copy(recallBudget = rKey))
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rLabel,
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) VelocityColors.TextPrimary else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Server Diagnostics & Pairing
            item {
                SectionCard(title = "Connection", icon = LucideIcons.Qr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Round-Trip Latency",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = if (isPinging) "Measuring..." else "${latencyMs ?: 0} ms",
                            style = MonoTextStyle,
                            fontSize = 12.sp,
                            color = VelocityColors.AccentEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                onDisconnect()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Disconnect & Re-pair Device",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. MEMORY TAB: Browse & Inspect ALL Vault Documents with Editor
// ==============================================================================
@Composable
private fun MemoryTabSubpage(
    repository: ChatRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var vaultTree by remember { mutableStateOf<List<VaultTreeItem>>(emptyList()) }
    var isLoadingTree by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Active document viewer state
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var docContent by remember { mutableStateOf("") }
    var isEditingDoc by remember { mutableStateOf(false) }
    var editDraft by remember { mutableStateOf("") }
    var isLoadingDoc by remember { mutableStateOf(false) }
    var isSavingDoc by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoadingTree = true
        vaultTree = withContext(Dispatchers.IO) { repository.fetchVaultTree() }
        isLoadingTree = false
    }

    fun openDoc(path: String) {
        selectedPath = path
        isEditingDoc = false
        isLoadingDoc = true
        coroutineScope.launch {
            val content = withContext(Dispatchers.IO) { repository.fetchVaultDoc(path) }
            docContent = content
            editDraft = content
            isLoadingDoc = false
        }
    }

    fun saveDoc() {
        val path = selectedPath ?: return
        isSavingDoc = true
        coroutineScope.launch {
            val success = withContext(Dispatchers.IO) { repository.saveVaultDoc(path, editDraft) }
            if (success) {
                docContent = editDraft
                isEditingDoc = false
                VelocityHaptics.success(context)
            } else {
                VelocityHaptics.error(context)
            }
            isSavingDoc = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SettingsSubpageHeader(
            title = if (selectedPath != null) "Document Viewer" else "Memory Vault",
            onBack = {
                if (selectedPath != null) {
                    selectedPath = null
                } else {
                    onBack()
                }
            }
        )

        if (selectedPath != null) {
            // Document View / Edit Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VelocityColors.SurfaceCard)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPath ?: "",
                        style = MonoTextStyle,
                        fontSize = 12.sp,
                        color = VelocityColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isEditingDoc) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VelocityColors.SurfaceElevated)
                                    .clickable { isEditingDoc = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, color = VelocityColors.TextMuted)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VelocityColors.TextPrimary)
                                    .clickable { saveDoc() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isSavingDoc) "Saving..." else "Save",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VelocityColors.Canvas
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VelocityColors.SurfaceElevated)
                                    .clickable {
                                        editDraft = docContent
                                        isEditingDoc = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(LucideIcons.Edit),
                                        contentDescription = "Edit",
                                        tint = VelocityColors.TextPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("Edit", fontSize = 12.sp, color = VelocityColors.TextPrimary)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated)

                if (isLoadingDoc) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp))
                    }
                } else if (isEditingDoc) {
                    TextField(
                        value = editDraft,
                        onValueChange = { editDraft = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = VelocityColors.TextPrimary,
                            unfocusedTextColor = VelocityColors.TextSecondary,
                            cursorColor = VelocityColors.TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MonoTextStyle.copy(fontSize = 13.sp, lineHeight = 20.sp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        item {
                            Text(
                                text = docContent.ifEmpty { "Empty document" },
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                                lineHeight = 21.sp,
                                color = VelocityColors.TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memory documents...", fontSize = 13.sp, color = VelocityColors.TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(VelocityColors.SurfaceCard),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = VelocityColors.TextPrimary,
                    unfocusedTextColor = VelocityColors.TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (isLoadingTree) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp))
                }
            } else {
                val filtered = vaultTree.filter {
                    searchQuery.isBlank() || it.path.contains(searchQuery, ignoreCase = true) || it.name.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No memory files found", color = VelocityColors.TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filtered, key = { it.path }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VelocityColors.SurfaceCard)
                                    .clickable {
                                        VelocityHaptics.lightClick(context)
                                        openDoc(item.path)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(LucideIcons.Documents),
                                        contentDescription = null,
                                        tint = VelocityColors.AccentSky,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontFamily = SatoshiFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = VelocityColors.TextPrimary
                                        )
                                        Text(
                                            text = item.path,
                                            style = MonoTextStyle,
                                            fontSize = 11.sp,
                                            color = VelocityColors.TextMuted
                                        )
                                    }
                                }

                                Icon(
                                    painter = painterResource(LucideIcons.ChevronRight),
                                    contentDescription = null,
                                    tint = VelocityColors.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 4. PLUGINS TAB: Google Workspace Integration Status
// ==============================================================================
@Composable
private fun PluginsTabSubpage(
    repository: ChatRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var status by remember { mutableStateOf(GoogleWorkspaceStatus()) }
    var isLoading by remember { mutableStateOf(true) }
    var isDisconnecting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        status = withContext(Dispatchers.IO) { repository.fetchGoogleStatus() }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SettingsSubpageHeader(title = "Plugins", onBack = onBack)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VelocityColors.SurfaceCard)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x26F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(LucideIcons.Puzzle),
                                contentDescription = null,
                                tint = VelocityColors.AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Google Workspace",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelocityColors.TextPrimary
                            )
                            Text(
                                text = if (status.connected) "Connected: ${status.email ?: "Account linked"}" else "Not Connected",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = if (status.connected) VelocityColors.AccentEmerald else VelocityColors.TextMuted
                            )
                        }
                    }

                    if (status.connected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .clickable {
                                    isDisconnecting = true
                                    coroutineScope.launch {
                                        val ok = withContext(Dispatchers.IO) { repository.disconnectGoogle() }
                                        if (ok) {
                                            status = GoogleWorkspaceStatus()
                                            VelocityHaptics.success(context)
                                        }
                                        isDisconnecting = false
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isDisconnecting) "Disconnecting..." else "Disconnect",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated)

                Text(
                    text = "Integrated Services:",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VelocityColors.TextMuted
                )

                PluginServiceRow(name = "Google Calendar", active = status.connected, desc = "Proactive daily agenda & event scheduling")
                PluginServiceRow(name = "Google Tasks", active = status.connected, desc = "Task synchronization & automated completion")
                PluginServiceRow(name = "Gmail", active = status.connected, desc = "Email synthesis & human-in-the-loop drafting")
            }
        }
    }
}

@Composable
private fun PluginServiceRow(name: String, active: Boolean, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontFamily = SatoshiFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = VelocityColors.TextPrimary)
            Text(text = desc, fontFamily = SatoshiFontFamily, fontSize = 11.sp, color = VelocityColors.TextMuted)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) Color(0x2634D399) else VelocityColors.SurfaceCapsule)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (active) "Active" else "Inactive",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) VelocityColors.AccentEmerald else VelocityColors.TextMuted
            )
        }
    }
}

// ==============================================================================
// 5. SCHEDULES TAB: List, Toggle, Delete & Add Scheduled Tasks
// ==============================================================================
@Composable
private fun SchedulesTabSubpage(
    repository: ChatRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var routines by remember { mutableStateOf<List<ScheduledRoutine>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ScheduledRoutine?>(null) }
    var deleting by remember { mutableStateOf<ScheduledRoutine?>(null) }
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var timing by remember { mutableStateOf("0 8 * * *") }
    var timezone by remember { mutableStateOf(java.time.ZoneId.systemDefault().id) }
    var oneShot by remember { mutableStateOf(false) }
    var advancedTiming by remember { mutableStateOf(false) }
    var frequency by remember { mutableIntStateOf(0) }
    var executionTime by remember { mutableStateOf("08:00") }
    var saving by remember { mutableStateOf(false) }
    var editorError by remember { mutableStateOf<String?>(null) }

    fun load() {
        loading = true
        scope.launch {
            try {
                routines = repository.fetchSchedules()
                error = null
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Could not load schedules."
            } finally { loading = false }
        }
    }

    fun openEditor(routine: ScheduledRoutine?) {
        editing = routine
        name = routine?.name ?: ""
        prompt = routine?.prompt ?: ""
        oneShot = routine?.eventType == "one_shot"
        timing = if (oneShot) routine?.runAt ?: "" else routine?.cronExpression ?: "0 8 * * *"
        val parts = timing.trim().split(Regex("\\s+"))
        val dayOptions = listOf("*", "1-5", "6,0")
        advancedTiming = !oneShot && (parts.size != 5 || parts.getOrNull(0)?.toIntOrNull() !in 0..59 || parts.getOrNull(1)?.toIntOrNull() !in 0..23 || parts.getOrNull(2) != "*" || parts.getOrNull(3) != "*" || parts.getOrNull(4) !in dayOptions)
        frequency = dayOptions.indexOf(parts.getOrNull(4)).coerceAtLeast(0)
        executionTime = if (!oneShot && !advancedTiming) "${parts[1].padStart(2, '0')}:${parts[0].padStart(2, '0')}" else "08:00"
        timezone = routine?.timezone ?: java.time.ZoneId.systemDefault().id
        editorError = null
        editorOpen = true
    }

    fun save() {
        if (saving) return
        editorError = null
        try {
            if (!oneShot && !advancedTiming) {
                val time = java.time.LocalTime.parse(executionTime.trim())
                timing = "${time.minute} ${time.hour} * * ${listOf("*", "1-5", "6,0")[frequency]}"
            }
            require(name.isNotBlank()) { "Enter a routine name." }
            require(prompt.isNotBlank() || editing?.skillId != null) { "Enter instructions for this routine." }
            java.time.ZoneId.of(timezone.trim())
            if (oneShot) {
                require(java.time.OffsetDateTime.parse(timing.trim()).toInstant().isAfter(java.time.Instant.now())) { "Choose a future date and time." }
            } else {
                require(timing.trim().split(Regex("\\s+")).size == 5) { "Use a five-field cron expression, for example 0 8 * * *." }
            }
        } catch (failure: Exception) {
            editorError = failure.message ?: "Check the time and time zone."
            return
        }
        saving = true
        scope.launch {
            try {
                val current = editing
                val ok = if (current != null) {
                    repository.editSchedule(current.id, name.trim(), prompt.trim(), if (oneShot) null else timing.trim(), if (oneShot) timing.trim() else null, timezone.trim())
                } else {
                    repository.createSchedule(name.trim(), if (oneShot) "one_shot" else "recurring", prompt.trim(), if (oneShot) null else timing.trim(), if (oneShot) timing.trim() else null, timezone.trim())
                }
                check(ok) { "Could not save this routine. Check the schedule format and connection." }
                editorOpen = false
                VelocityHaptics.success(context)
                load()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                editorError = failure.message ?: "Could not save the routine."
                VelocityHaptics.error(context)
            } finally { saving = false }
        }
    }

    BackHandler(enabled = editorOpen && !saving) { editorOpen = false }
    LaunchedEffect(repository) { load() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        SettingsSubpageHeader("Schedules", onBack)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Routines that work for you", style = VelocityTypography.bodyMedium, color = VelocityColors.TextMuted, modifier = Modifier.weight(1f))
            IconButton(onClick = { openEditor(null) }) {
                Icon(painterResource(LucideIcons.Plus), "New routine", tint = VelocityColors.TextPrimary)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { load() }) { Text("Try again") } }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            if (!loading && routines.isEmpty() && error == null) item {
                Text("No routines yet. Tap + to schedule your first one.", style = VelocityTypography.bodyMedium, color = VelocityColors.TextMuted, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(routines, key = { it.id }) { routine ->
                val active = routine.status == "active"
                Column(Modifier.fillMaxWidth().background(VelocityColors.SurfaceCard, RoundedCornerShape(18.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(LucideIcons.Clock), null, tint = VelocityColors.AccentViolet, modifier = Modifier.size(18.dp))
                        Text(routine.name, style = VelocityTypography.titleMedium, color = VelocityColors.TextPrimary, modifier = Modifier.weight(1f).padding(start = 10.dp))
                        IconButton(onClick = { openEditor(routine) }) { Icon(painterResource(LucideIcons.Edit), "Edit ${routine.name}", tint = VelocityColors.TextMuted, modifier = Modifier.size(18.dp)) }
                    }
                    Text(routine.prompt.ifBlank { "Runs an installed skill" }, style = VelocityTypography.bodySmall, color = VelocityColors.TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text("${routine.cronExpression ?: routine.runAt ?: "Time not set"} · ${routine.timezone}", style = VelocityTypography.labelSmall, color = VelocityColors.TextMuted)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(routine.status.replaceFirstChar { it.uppercase() }, style = VelocityTypography.labelMedium, color = if (active) VelocityColors.AccentEmerald else VelocityColors.TextMuted, modifier = Modifier.weight(1f))
                        if (routine.status == "active" || routine.status == "paused") IconButton(enabled = !loading, onClick = {
                            scope.launch {
                                if (repository.toggleSchedule(routine.id, !active)) load() else error = "Could not change this routine."
                            }
                        }) { Icon(painterResource(if (active) LucideIcons.Pause else LucideIcons.Play), if (active) "Pause routine" else "Resume routine", tint = VelocityColors.TextMuted, modifier = Modifier.size(18.dp)) }
                        IconButton(onClick = { deleting = routine }) { Icon(painterResource(LucideIcons.Trash), "Delete routine", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }
    if (editorOpen) AlertDialog(
        onDismissRequest = { if (!saving) editorOpen = false },
        title = { Text(if (editing == null) "New routine" else "Edit routine") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, enabled = !saving, singleLine = true)
                OutlinedTextField(prompt, { prompt = it }, label = { Text("Instructions") }, enabled = !saving, minLines = 3, maxLines = 6)
                if (editing == null) UsageSegments(listOf("Recurring", "One time"), if (oneShot) 1 else 0) {
                    oneShot = it == 1
                    timing = if (oneShot) "" else "0 8 * * *"
                }
                if (!oneShot && !advancedTiming) {
                    UsageSegments(listOf("Daily", "Weekdays", "Weekends"), frequency) { frequency = it }
                    OutlinedTextField(executionTime, { executionTime = it }, label = { Text("Time · HH:mm") }, enabled = !saving, singleLine = true)
                } else {
                    OutlinedTextField(timing, { timing = it }, label = { Text(if (oneShot) "Date & time with offset" else "Cron expression") }, enabled = !saving, singleLine = true)
                    Text(if (oneShot) "Example: 2026-12-01T08:00:00+05:30" else "Minute · hour · day · month · weekday\n0 8 * * * = every day at 08:00", style = VelocityTypography.bodySmall, color = VelocityColors.TextMuted)
                }
                if (!oneShot) TextButton(enabled = !saving, onClick = {
                    if (!advancedTiming) {
                        val time = runCatching { java.time.LocalTime.parse(executionTime.trim()) }.getOrNull()
                        if (time != null) timing = "${time.minute} ${time.hour} * * ${listOf("*", "1-5", "6,0")[frequency]}"
                    }
                    advancedTiming = !advancedTiming
                }) { Text(if (advancedTiming) "Use simple schedule" else "Custom cron schedule") }
                OutlinedTextField(timezone, { timezone = it }, label = { Text("Time zone") }, enabled = !saving, singleLine = true)
                Text("Use an IANA zone such as Asia/Kolkata or America/New_York. Existing skill and status are preserved.", style = VelocityTypography.bodySmall, color = VelocityColors.TextMuted)
                editorError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save() }) { Text(if (saving) "Saving…" else "Save routine") } },
        dismissButton = { TextButton(enabled = !saving, onClick = { editorOpen = false }) { Text("Cancel") } }
    )
    deleting?.let { routine ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete routine?") },
            text = { Text("“${routine.name}” will no longer run.") },
            confirmButton = { TextButton(onClick = {
                deleting = null
                scope.launch { if (repository.deleteSchedule(routine.id)) load() else error = "Could not delete this routine." }
            }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

// ==============================================================================
// 6. SKILLS TAB: Modular Technical Skills Registry & Prompt Editor
// ==============================================================================
@Composable
private fun SkillsTabSubpage(
    repository: ChatRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var skills by remember { mutableStateOf<List<SkillRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Selected skill for editing instructions
    var editingSkill by remember { mutableStateOf<SkillRecord?>(null) }
    var instructionsDraft by remember { mutableStateOf("") }
    var isSavingSkill by remember { mutableStateOf(false) }

    fun loadSkills() {
        isLoading = true
        coroutineScope.launch {
            skills = withContext(Dispatchers.IO) { repository.fetchSkills() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadSkills()
    }

    fun saveInstructions() {
        val skill = editingSkill ?: return
        isSavingSkill = true
        coroutineScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.updateSkillInstructions(skill.id, instructionsDraft) }
            if (ok) {
                editingSkill = null
                VelocityHaptics.success(context)
                loadSkills()
            } else {
                VelocityHaptics.error(context)
            }
            isSavingSkill = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        SettingsSubpageHeader(
            title = if (editingSkill != null) "Edit Skill" else "Skills Registry",
            onBack = {
                if (editingSkill != null) {
                    editingSkill = null
                } else {
                    onBack()
                }
            }
        )

        if (editingSkill != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VelocityColors.SurfaceCard)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = editingSkill?.name ?: "Instructions",
                        fontFamily = SatoshiFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VelocityColors.TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VelocityColors.TextPrimary)
                            .clickable { saveInstructions() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSavingSkill) "Saving..." else "Save",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VelocityColors.Canvas
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = VelocityColors.SurfaceElevated)

                TextField(
                    value = instructionsDraft,
                    onValueChange = { instructionsDraft = it },
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = VelocityColors.TextPrimary,
                        unfocusedTextColor = VelocityColors.TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MonoTextStyle.copy(fontSize = 13.sp, lineHeight = 20.sp)
                )
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp))
            }
        } else if (skills.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No modular skills installed", color = VelocityColors.TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(skills, key = { it.id }) { skill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.SurfaceCard)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skill.name,
                                fontFamily = SatoshiFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VelocityColors.TextPrimary
                            )
                            Text(
                                text = skill.description.ifEmpty { "Modular skill" },
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = VelocityColors.TextMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Edit instructions button
                            Text(
                                text = "Edit Instructions",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VelocityColors.AccentSky,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .clickable {
                                        editingSkill = skill
                                        instructionsDraft = skill.instructions
                                    }
                            )
                        }

                        Switch(
                            checked = skill.enabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) { repository.toggleSkill(skill.id, enabled) }
                                    loadSkills()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VelocityColors.TextPrimary,
                                checkedTrackColor = VelocityColors.AccentEmerald,
                                uncheckedThumbColor = VelocityColors.TextMuted,
                                uncheckedTrackColor = VelocityColors.SurfaceCapsule
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// Common Helper UI Components
// ==============================================================================
@Composable
private fun SettingsSubpageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(VelocityColors.SurfaceCapsule)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(LucideIcons.Close),
                contentDescription = "Back",
                tint = VelocityColors.TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        Text(
            text = title,
            style = VelocityTypography.titleLarge,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = VelocityColors.TextPrimary
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: Int = LucideIcons.Settings,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(VelocityColors.SurfaceCard)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(icon), null, tint = VelocityColors.TextMuted, modifier = Modifier.size(18.dp))
            Text(title, style = VelocityTypography.titleSmall, color = VelocityColors.TextPrimary)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = SatoshiFontFamily,
                fontSize = 12.sp,
                color = VelocityColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        content()
    }
}

@Composable
private fun SettingsRowItem(
    icon: Int,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VelocityColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    color = VelocityColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = VelocityColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
