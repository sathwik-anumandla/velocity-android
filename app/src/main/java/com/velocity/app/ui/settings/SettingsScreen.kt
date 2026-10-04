package com.velocity.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class SettingsSubpage {
    ROOT,
    MODEL_REASONING,
    MEMORY_VAULT,
    GOOGLE_WORKSPACE,
    ROUTINES,
    SKILLS,
    TELEMETRY,
    SERVER_PAIRING
}

@Composable
fun SettingsScreen(
    repository: ChatRepository,
    serverConfig: ServerConfig,
    onClose: () -> Unit,
    onDisconnect: () -> Unit
) {
    val context = LocalContext.current
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.ROOT) }
    var modelConfig by remember { mutableStateOf(ServerConfigManager.loadModelConfig(context)) }

    // Intercept back button if on a nested page
    BackHandler(enabled = currentSubpage != SettingsSubpage.ROOT) {
        currentSubpage = SettingsSubpage.ROOT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelocityColors.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentSubpage,
            transitionSpec = {
                if (targetState == SettingsSubpage.ROOT) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                }
            },
            label = "settings_nav"
        ) { page ->
            when (page) {
                SettingsSubpage.ROOT -> SettingsRootView(
                    serverConfig = serverConfig,
                    modelConfig = modelConfig,
                    repository = repository,
                    onNavigate = {
                        VelocityHaptics.lightClick(context)
                        currentSubpage = it
                    },
                    onClose = onClose
                )
                SettingsSubpage.MODEL_REASONING -> ModelReasoningSubpage(
                    config = modelConfig,
                    onConfigChange = {
                        modelConfig = it
                        ServerConfigManager.saveModelConfig(context, it)
                    },
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.MEMORY_VAULT -> MemoryVaultSubpage(
                    repository = repository,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.GOOGLE_WORKSPACE -> GoogleWorkspaceSubpage(
                    repository = repository,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.ROUTINES -> RoutinesSubpage(
                    repository = repository,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.SKILLS -> SkillsSubpage(
                    repository = repository,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.TELEMETRY -> TelemetrySubpage(
                    repository = repository,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
                SettingsSubpage.SERVER_PAIRING -> ServerPairingSubpage(
                    serverConfig = serverConfig,
                    onDisconnect = onDisconnect,
                    onBack = { currentSubpage = SettingsSubpage.ROOT }
                )
            }
        }
    }
}

@Composable
private fun SettingsRootView(
    serverConfig: ServerConfig,
    modelConfig: ModelConfig,
    repository: ChatRepository,
    onNavigate: (SettingsSubpage) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isHealthy by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val res = withContext(Dispatchers.IO) { repository.testConnection() }
        isHealthy = res.isSuccess
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = VelocityTypography.headlineLarge,
                color = VelocityColors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C1C1E))
                    .clickable {
                        VelocityHaptics.lightClick(context)
                        onClose()
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

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Group 1: AI & Inference
            item {
                SettingsSectionHeader(title = "AI & INFERENCE")
                SettingsGroupCard {
                    SettingsRow(
                        title = "Model & Reasoning",
                        subtitle = "${modelConfig.model} · ${modelConfig.thinkingEffort} effort",
                        icon = LucideIcons.Sliders,
                        onClick = { onNavigate(SettingsSubpage.MODEL_REASONING) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Memory Vault",
                        subtitle = "Hindsight Memory Engine · ${modelConfig.recallBudget} recall",
                        icon = LucideIcons.History,
                        onClick = { onNavigate(SettingsSubpage.MEMORY_VAULT) }
                    )
                }
            }

            // Group 2: Integrations & Capabilities
            item {
                SettingsSectionHeader(title = "INTEGRATIONS & CAPABILITIES")
                SettingsGroupCard {
                    SettingsRow(
                        title = "Google Workspace",
                        subtitle = "Gmail, Calendar & Tasks Integration",
                        icon = LucideIcons.Compass,
                        onClick = { onNavigate(SettingsSubpage.GOOGLE_WORKSPACE) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Autonomous Routines",
                        subtitle = "Morning Briefing & Evening Reflection",
                        icon = LucideIcons.Chronology,
                        onClick = { onNavigate(SettingsSubpage.ROUTINES) }
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = "Skills Registry",
                        subtitle = "Autonomous capabilities & custom skills",
                        icon = LucideIcons.Documents,
                        onClick = { onNavigate(SettingsSubpage.SKILLS) }
                    )
                }
            }

            // Group 3: System Telemetry (Moved from main page!)
            item {
                SettingsSectionHeader(title = "SYSTEM & TELEMETRY")
                SettingsGroupCard {
                    SettingsRow(
                        title = "System Telemetry & Health",
                        subtitle = if (isHealthy) "All Systems Operational" else "Degraded Connectivity",
                        icon = LucideIcons.Settings,
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isHealthy) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                        },
                        onClick = { onNavigate(SettingsSubpage.TELEMETRY) }
                    )
                }
            }

            // Group 4: Server & Pairing
            item {
                SettingsSectionHeader(title = "SERVER & CONNECTION")
                SettingsGroupCard {
                    SettingsRow(
                        title = "Server Pairing",
                        subtitle = serverConfig.normalizedUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                        icon = LucideIcons.Qr,
                        onClick = { onNavigate(SettingsSubpage.SERVER_PAIRING) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// Nested Subpages
// -------------------------------------------------------------------------

@Composable
private fun ModelReasoningSubpage(
    config: ModelConfig,
    onConfigChange: (ModelConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val supportedModels = listOf(
        "gpt-5" to "Flagship intelligence & reasoning",
        "o3-mini" to "High-speed reasoning co-pilot",
        "o1" to "Exhaustive deep architectural reasoning",
        "gpt-4.5" to "Dynamic creative technical partner"
    )
    val effortLevels = listOf("low", "medium", "high")
    val verbosityLevels = listOf("concise", "medium", "exhaustive")
    val recallLevels = listOf("low", "medium", "high")

    SubpageScaffold(title = "Model & Reasoning", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Models
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeader(title = "PRIMARY INFERENCE MODEL")
                SettingsGroupCard {
                    supportedModels.forEachIndexed { index, (modelId, desc) ->
                        val isSelected = config.model.equals(modelId, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    VelocityHaptics.subtleTick(context)
                                    onConfigChange(config.copy(model = modelId))
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
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
                        if (index < supportedModels.size - 1) SettingsDivider()
                    }
                }
            }

            // Thinking Effort
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeader(title = "DEFAULT THINKING EFFORT")
                SettingsGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effortLevels.forEach { effort ->
                            val isSelected = config.thinkingEffort.equals(effort, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF282830) else Color.Transparent)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        onConfigChange(config.copy(thinkingEffort = effort))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = effort.replaceFirstChar { it.uppercase() },
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Verbosity
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionHeader(title = "RESPONSE VERBOSITY")
                SettingsGroupCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        verbosityLevels.forEach { verb ->
                            val isSelected = config.verbosity.equals(verb, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF282830) else Color.Transparent)
                                    .clickable {
                                        VelocityHaptics.subtleTick(context)
                                        onConfigChange(config.copy(verbosity = verb))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = verb.replaceFirstChar { it.uppercase() },
                                    fontFamily = SatoshiFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryVaultSubpage(repository: ChatRepository, onBack: () -> Unit) {
    SubpageScaffold(title = "Memory Vault", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionHeader(title = "DETERMINISTIC MEMORY VAULT")
            SettingsGroupCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hindsight Cognitive Memory",
                        style = VelocityTypography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Deterministic bi-directional memory synced with core user profiles, active context dossiers, and nightly synthesis routines.",
                        style = VelocityTypography.bodyMedium,
                        color = VelocityColors.TextSecondary
                    )
                }
            }

            SettingsSectionHeader(title = "CORE CONTEXT DOSSIERS")
            SettingsGroupCard {
                SettingsRow(title = "profile.md", subtitle = "User identity, key principles, and coding standards", icon = LucideIcons.Documents)
                SettingsDivider()
                SettingsRow(title = "active_context.md", subtitle = "Current active priorities, workstreams, and technical state", icon = LucideIcons.Documents)
            }
        }
    }
}

@Composable
private fun GoogleWorkspaceSubpage(repository: ChatRepository, onBack: () -> Unit) {
    var status by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        status = withContext(Dispatchers.IO) { repository.fetchIntegrationStatus() }
        isLoading = false
    }

    SubpageScaffold(title = "Google Workspace", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionHeader(title = "CONNECTED SERVICES")
            SettingsGroupCard {
                SettingsRow(
                    title = "Google Calendar",
                    subtitle = if (status["calendar"] == true) "Connected" else "Not connected",
                    icon = LucideIcons.Compass,
                    trailingContent = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (status["calendar"] == true) Color(0xFF10B981) else Color(0xFF71717A)))
                    }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Google Tasks",
                    subtitle = if (status["tasks"] == true) "Connected" else "Not connected",
                    icon = LucideIcons.Chronology,
                    trailingContent = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (status["tasks"] == true) Color(0xFF10B981) else Color(0xFF71717A)))
                    }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Gmail Drafts & Inbox",
                    subtitle = if (status["gmail"] == true) "Connected" else "Not connected",
                    icon = LucideIcons.Documents,
                    trailingContent = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (status["gmail"] == true) Color(0xFF10B981) else Color(0xFF71717A)))
                    }
                )
            }
        }
    }
}

@Composable
private fun RoutinesSubpage(repository: ChatRepository, onBack: () -> Unit) {
    SubpageScaffold(title = "Autonomous Routines", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionHeader(title = "PROACTIVE SCHEDULED ROUTINES")
            SettingsGroupCard {
                SettingsRow(
                    title = "Morning Briefing",
                    subtitle = "Runs daily at 08:00 · Calendar, tasks & priorities",
                    icon = LucideIcons.Chronology,
                    trailingContent = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Evening Reflection",
                    subtitle = "Runs daily at 21:00 · Retrospective & vault memory retain",
                    icon = LucideIcons.Chronology,
                    trailingContent = {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6366F1)))
                    }
                )
            }
        }
    }
}

@Composable
private fun SkillsSubpage(repository: ChatRepository, onBack: () -> Unit) {
    var skills by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        skills = withContext(Dispatchers.IO) { repository.fetchSkills() }
    }

    SubpageScaffold(title = "Skills Registry", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionHeader(title = "ACTIVE AGENT SKILLS")
            SettingsGroupCard {
                if (skills.isEmpty()) {
                    SettingsRow(title = "System Skills Active", subtitle = "Coding, Web Research, Vault Memory, Side Chats", icon = LucideIcons.Documents)
                } else {
                    skills.forEachIndexed { idx, sk ->
                        SettingsRow(title = sk["name"] ?: "Skill", subtitle = sk["description"] ?: "Active", icon = LucideIcons.Documents)
                        if (idx < skills.size - 1) SettingsDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetrySubpage(repository: ChatRepository, onBack: () -> Unit) {
    var health by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pingTime by remember { mutableStateOf<Long?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun runPing() {
        coroutineScope.launch {
            val start = System.currentTimeMillis()
            health = withContext(Dispatchers.IO) { repository.fetchHealthDetails() }
            pingTime = System.currentTimeMillis() - start
        }
    }

    LaunchedEffect(Unit) {
        runPing()
    }

    SubpageScaffold(title = "Telemetry & Health", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSectionHeader(title = "SYSTEM STATUS (APPLE METRICS)")
            SettingsGroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Backend Server", style = VelocityTypography.titleMedium, color = Color.White)
                        Text(
                            text = if (health["status"] == "ok") "Operational" else "Offline",
                            style = VelocityTypography.bodySmall,
                            color = if (health["status"] == "ok") Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF26262B))
                            .clickable { runPing() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (pingTime != null) "${pingTime}ms" else "Ping",
                            style = MonoTextStyle,
                            color = Color.White
                        )
                    }
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Database & FTS5", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                    Text(health["database"] ?: "Connected", style = MonoTextStyle, color = Color.White)
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hindsight Engine", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                    Text(health["hindsight"] ?: "Operational", style = MonoTextStyle, color = Color.White)
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Velocity Core Version", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                    Text("v2.2-hybrid", style = MonoTextStyle, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ServerPairingSubpage(
    serverConfig: ServerConfig,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    SubpageScaffold(title = "Server & Pairing", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSectionHeader(title = "CURRENT PAIRING")
            SettingsGroupCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Host URL", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                    Text(serverConfig.normalizedUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'), style = MonoTextStyle, color = Color.White)
                }
                SettingsDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cloudflare Access", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                    Text(if (serverConfig.cfClientId.isNotBlank()) "Service Token" else "Direct", style = MonoTextStyle, color = Color.White)
                }
            }

            // Destructive Action: Disconnect & Re-pair
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2C1515))
                    .clickable {
                        VelocityHaptics.error(context)
                        onDisconnect()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Disconnect & Re-pair Device",
                    style = VelocityTypography.titleSmall,
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// Apple Components
// -------------------------------------------------------------------------

@Composable
private fun SubpageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        VelocityHaptics.lightClick(context)
                        onBack()
                    }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "‹ Settings",
                    fontFamily = SatoshiFontFamily,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = title,
                style = VelocityTypography.titleMedium,
                color = VelocityColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Box(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = SatoshiFontFamily,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = Color(0xFF71717A),
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E)),
        content = content
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: Int? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = SatoshiFontFamily,
                        fontSize = 12.5.sp,
                        color = Color(0xFF8E8E93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = Color(0xFF52525B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 46.dp),
        thickness = 0.5.dp,
        color = Color(0xFF28282C)
    )
}
