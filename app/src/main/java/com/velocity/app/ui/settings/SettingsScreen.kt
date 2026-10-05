package com.velocity.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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

    BackHandler(enabled = currentTab != SettingsTab.ROOT) {
        currentTab = SettingsTab.ROOT
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
                    .background(Color(0xFF1E1E22))
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
                    text = "SYSTEM ARCHITECTURE",
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
                        .background(Color(0xFF141416))
                ) {
                    // 1. General
                    SettingsRowItem(
                        icon = LucideIcons.Sliders,
                        iconTint = Color(0xFFA1A1AA),
                        title = "General",
                        subtitle = "Model ${modelConfig.model} · ${modelConfig.thinkingEffort.replaceFirstChar { it.uppercase() }} reasoning",
                        onClick = { onNavigate(SettingsTab.GENERAL) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226), modifier = Modifier.padding(horizontal = 16.dp))

                    // 2. Memory
                    SettingsRowItem(
                        icon = LucideIcons.Brain,
                        iconTint = Color(0xFF38BDF8),
                        title = "Memory",
                        subtitle = "Deterministic vault documents, inspect & edit files",
                        onClick = { onNavigate(SettingsTab.MEMORY) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226), modifier = Modifier.padding(horizontal = 16.dp))

                    // 3. Plugins
                    SettingsRowItem(
                        icon = LucideIcons.Puzzle,
                        iconTint = Color(0xFFF59E0B),
                        title = "Plugins",
                        subtitle = "Google Workspace (Calendar, Tasks, Gmail)",
                        onClick = { onNavigate(SettingsTab.PLUGINS) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226), modifier = Modifier.padding(horizontal = 16.dp))

                    // 4. Schedules
                    SettingsRowItem(
                        icon = LucideIcons.Clock,
                        iconTint = Color(0xFFA78BFA),
                        title = "Schedules",
                        subtitle = "Autonomous routines, morning briefings & reminders",
                        onClick = { onNavigate(SettingsTab.SCHEDULES) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226), modifier = Modifier.padding(horizontal = 16.dp))

                    // 5. Skills
                    SettingsRowItem(
                        icon = LucideIcons.Sparkles,
                        iconTint = Color(0xFF34D399),
                        title = "Skills",
                        subtitle = "Modular technical skills & dynamic instructions",
                        onClick = { onNavigate(SettingsTab.SKILLS) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226))
                    SettingsRowItem(
                        icon = LucideIcons.Clock,
                        iconTint = VelocityColors.AccentSky,
                        title = "Usage",
                        subtitle = "Tokens, reasoning, cache effectiveness & cost estimates",
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
                        .background(Color(0xFF141416))
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
                            color = Color.White
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
                                color = Color(0xFF34D399)
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
            item {
                SectionCard(title = "PRIMARY INFERENCE MODEL", subtitle = "High-speed vs. deep architecture reasoning model.") {
                    supportedModels.forEach { (mId, mDesc) ->
                        val isSelected = config.model.equals(mId, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF222226) else Color.Transparent)
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
                                    color = Color.White
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
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // Thinking Effort
            item {
                SectionCard(title = "THINKING EFFORT", subtitle = "Depth of reasoning applied before response turns.") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1E))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        effortLevels.forEach { effort ->
                            val isSelected = config.thinkingEffort.equals(effort, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
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
                                    color = if (isSelected) Color.White else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Verbosity
            item {
                SectionCard(title = "RESPONSE VERBOSITY", subtitle = "Control paragraph length and response density.") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1E))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        verbosityLevels.forEach { (vKey, vLabel) ->
                            val isSelected = config.verbosity.equals(vKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
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
                                    color = if (isSelected) Color.White else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Recall Budget
            item {
                SectionCard(title = "MEMORY RECALL BUDGET", subtitle = "Depth of Hindsight memory search per turn.") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1E))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recallLevels.forEach { (rKey, rLabel) ->
                            val isSelected = config.recallBudget.equals(rKey, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2E2E34) else Color.Transparent)
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
                                    color = if (isSelected) Color.White else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Server Diagnostics & Pairing
            item {
                SectionCard(title = "SERVER DIAGNOSTICS & PAIRING") {
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
                            color = Color(0xFF34D399)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2B1214))
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
                    .background(Color(0xFF141416))
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
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isEditingDoc) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222226))
                                    .clickable { isEditingDoc = false }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, color = VelocityColors.TextMuted)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { saveDoc() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isSavingDoc) "Saving..." else "Save",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF222226))
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
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("Edit", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226))

                if (isLoadingDoc) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE4E4E7),
                            cursorColor = Color.White,
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
                                color = Color(0xFFE4E4E7)
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
                    .background(Color(0xFF141416)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            if (isLoadingTree) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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
                                    .background(Color(0xFF141416))
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
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontFamily = SatoshiFontFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
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
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
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
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Google Workspace",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (status.connected) "Connected: ${status.email ?: "Account linked"}" else "Not Connected",
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = if (status.connected) Color(0xFF34D399) else VelocityColors.TextMuted
                            )
                        }
                    }

                    if (status.connected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2B1214))
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

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226))

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
            Text(text = name, fontFamily = SatoshiFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(text = desc, fontFamily = SatoshiFontFamily, fontSize = 11.sp, color = VelocityColors.TextMuted)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (active) Color(0x2634D399) else Color(0xFF1E1E22))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (active) "Active" else "Inactive",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) Color(0xFF34D399) else VelocityColors.TextMuted
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
    val coroutineScope = rememberCoroutineScope()
    var routines by remember { mutableStateOf<List<ScheduledRoutine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog state to add routine
    var isAddingRoutine by remember { mutableStateOf(false) }
    var newRoutineName by remember { mutableStateOf("") }
    var newRoutineType by remember { mutableStateOf("recurring") } // "recurring" or "one_shot"
    var newRoutineFrequency by remember { mutableStateOf("daily") } // "daily", "weekdays", "weekends"
    var newRoutineTime by remember { mutableStateOf("08:00") }
    var newRoutinePrompt by remember { mutableStateOf("") }
    var isSavingRoutine by remember { mutableStateOf(false) }

    fun loadRoutines() {
        isLoading = true
        coroutineScope.launch {
            routines = withContext(Dispatchers.IO) { repository.fetchSchedules() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRoutines()
    }

    fun submitNewRoutine() {
        if (newRoutineName.isBlank() || newRoutinePrompt.isBlank()) return
        isSavingRoutine = true
        coroutineScope.launch {
            val cronExpr = if (newRoutineType == "recurring") {
                val parts = newRoutineTime.split(":").map { it.trim() }
                val h = parts.getOrNull(0) ?: "08"
                val m = parts.getOrNull(1) ?: "00"
                when (newRoutineFrequency) {
                    "weekdays" -> "$m $h * * 1-5"
                    "weekends" -> "$m $h * * 6,0"
                    else -> "$m $h * * *"
                }
            } else null

            val success = withContext(Dispatchers.IO) {
                repository.createSchedule(
                    name = newRoutineName.trim(),
                    eventType = newRoutineType,
                    prompt = newRoutinePrompt.trim(),
                    cronExpression = cronExpr,
                    runAt = null
                )
            }

            if (success) {
                VelocityHaptics.success(context)
                isAddingRoutine = false
                newRoutineName = ""
                newRoutinePrompt = ""
                loadRoutines()
            } else {
                VelocityHaptics.error(context)
            }
            isSavingRoutine = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E22))
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
                    text = "Schedules",
                    style = VelocityTypography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextPrimary
                )
            }

            // New Routine Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { isAddingRoutine = !isAddingRoutine }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(LucideIcons.Plus),
                        contentDescription = "Add",
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "New Routine",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // New Routine Form (Inline Accordion Card)
        if (isAddingRoutine) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141416))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SCHEDULE NEW ROUTINE",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                TextField(
                    value = newRoutineName,
                    onValueChange = { newRoutineName = it },
                    placeholder = { Text("Routine Name (e.g. Morning Briefing)", fontSize = 12.sp, color = VelocityColors.TextMuted) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1E)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Frequency pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("daily" to "Daily", "weekdays" to "Weekdays", "weekends" to "Weekends").forEach { (fKey, fLabel) ->
                        val isSelected = newRoutineFrequency == fKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2E2E34) else Color(0xFF1A1A1E))
                                .clickable { newRoutineFrequency = fKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else VelocityColors.TextMuted
                            )
                        }
                    }
                }

                // Time Input
                TextField(
                    value = newRoutineTime,
                    onValueChange = { newRoutineTime = it },
                    placeholder = { Text("Execution Time (e.g. 08:00)", fontSize = 12.sp, color = VelocityColors.TextMuted) },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1E)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Prompt
                TextField(
                    value = newRoutinePrompt,
                    onValueChange = { newRoutinePrompt = it },
                    placeholder = { Text("Prompt instructions (e.g. Synthesize today's calendar and urgent priorities)...", fontSize = 12.sp, color = VelocityColors.TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1E)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .clickable { submitNewRoutine() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isSavingRoutine) "Saving..." else "Save Routine",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        } else if (routines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scheduled routines configured", color = VelocityColors.TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(routines, key = { it.id }) { routine ->
                    val isActive = routine.status.equals("active", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141416))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = routine.name,
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isActive) Color(0x2634D399) else Color(0xFF1E1E22))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "Active" else "Paused",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isActive) Color(0xFF34D399) else VelocityColors.TextMuted
                                    )
                                }
                            }

                            Text(
                                text = routine.prompt.ifEmpty { "Autonomous routine" },
                                fontFamily = SatoshiFontFamily,
                                fontSize = 12.sp,
                                color = VelocityColors.TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Toggle pause/play
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E22))
                                    .clickable {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) { repository.toggleSchedule(routine.id, !isActive) }
                                            loadRoutines()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(if (isActive) LucideIcons.Pause else LucideIcons.Play),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }

                            // Delete
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B1214))
                                    .clickable {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) { repository.deleteSchedule(routine.id) }
                                            loadRoutines()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(LucideIcons.Trash),
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(12.dp)
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
                    .background(Color(0xFF141416))
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
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { saveInstructions() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSavingSkill) "Saving..." else "Save",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF222226))

                TextField(
                    value = instructionsDraft,
                    onValueChange = { instructionsDraft = it },
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MonoTextStyle.copy(fontSize = 13.sp, lineHeight = 20.sp)
                )
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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
                            .background(Color(0xFF141416))
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
                                color = Color.White
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
                                color = Color(0xFF38BDF8),
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
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34D399),
                                uncheckedThumbColor = Color(0xFFA1A1AA),
                                uncheckedTrackColor = Color(0xFF1E1E22)
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
                .background(Color(0xFF1E1E22))
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141416))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontFamily = SatoshiFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = VelocityColors.TextMuted
        )
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
                    color = Color.White
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
