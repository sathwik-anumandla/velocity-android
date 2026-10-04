package com.velocity.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.data.repository.ModelConfig
import com.velocity.app.data.repository.ServerConfig
import com.velocity.app.data.repository.ServerConfigManager
import com.velocity.app.ui.components.*
import com.velocity.app.ui.settings.SettingsScreen
import com.velocity.app.ui.sheets.NavigationSheetHost
import com.velocity.app.ui.sheets.SheetType
import com.velocity.app.ui.theme.SatoshiFontFamily
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTypography
import com.velocity.app.ui.util.VelocityHaptics

@Composable
fun MainTimelineScreen(
    repository: ChatRepository,
    serverConfig: ServerConfig,
    viewModel: TimelineViewModel,
    onDisconnect: () -> Unit,
    onOpenArtifactDetail: ((ArtifactItem) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isMenuOpen by remember { mutableStateOf(false) }
    var isModelSheetOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    var modelConfig by remember { mutableStateOf(ServerConfigManager.loadModelConfig(context)) }

    // Initialize repository on first launch
    LaunchedEffect(repository) {
        viewModel.initRepository(repository)
    }

    // Auto scroll to bottom when new messages arrive or when streaming starts
    LaunchedEffect(uiState.messages.size, uiState.isStreaming) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    if (isSettingsOpen) {
        SettingsScreen(
            repository = repository,
            serverConfig = serverConfig,
            onClose = {
                // Reload model preferences in case they were updated in settings
                modelConfig = ServerConfigManager.loadModelConfig(context)
                isSettingsOpen = false
            },
            onDisconnect = onDisconnect
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelocityColors.Canvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Apple-Style Header (Centered Brand, Left Back if in Thread, Right Hamburger Menu)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left Side: Thread Back Button (Only shown when not in main session; Health dot removed)
                if (uiState.currentSessionId != "main") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1C1E))
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                viewModel.switchSession("main", "velocity")
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "‹ Main",
                            fontFamily = SatoshiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = VelocityColors.TextPrimary
                        )
                    }
                }

                // Center Title
                Text(
                    text = uiState.sessionTitle,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextPrimary,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 60.dp)
                )

                // Right Side: Apple-Style Hamburger Menu Button
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Menu),
                            contentDescription = "Menu",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Apple-Style Dropdown Menu
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        // 1. Threads
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Threads",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Threads),
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                activeSheet = SheetType.THREADS
                            }
                        )

                        // 2. Documents
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Documents",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Documents),
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                activeSheet = SheetType.DOCUMENTS
                            }
                        )

                        // 3. Search
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Search",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Search),
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                activeSheet = SheetType.SEARCH
                            }
                        )

                        // 4. Chronology
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Chronology",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Chronology),
                                    contentDescription = null,
                                    tint = Color(0xFFC084FC),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                activeSheet = SheetType.CHRONOLOGY
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFF2E2E32)
                        )

                        // 5. Settings (Standalone Apple System Settings)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Settings",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Settings),
                                    contentDescription = null,
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                isSettingsOpen = true
                            }
                        )
                    }
                }
            }

            // Chat Message Timeline
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatCapsule(
                        message = msg,
                        onOpenThread = { threadId ->
                            viewModel.switchSession(threadId)
                        },
                        onOpenArtifact = { _ ->
                            activeSheet = SheetType.DOCUMENTS
                        }
                    )
                }

                // Live Streaming Status Pulse
                if (uiState.isStreaming && uiState.streamingStatus != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(VelocityColors.AccentSky)
                            )
                            Text(
                                text = uiState.streamingStatus ?: "Thinking...",
                                fontSize = 12.sp,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }

            // Bottom Section: Action Approval + Single Floating Input Capsule (Elevates with IME)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pending Staged Action Banner
                AnimatedVisibility(
                    visible = uiState.pendingAction != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.pendingAction?.let { action ->
                        ActionApprovalBanner(
                            title = "${action.actionType.replace('_', ' ').uppercase()}: ${action.parameters["subject"] ?: action.parameters["to"] ?: "Approval"}",
                            onDecline = {
                                VelocityHaptics.error(context)
                                viewModel.respondAction(confirm = false)
                            },
                            onApprove = {
                                VelocityHaptics.success(context)
                                viewModel.respondAction(confirm = true)
                            }
                        )
                    }
                }

                // Pure Floating Input Capsule
                InputCapsule(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(
                            text = text,
                            model = modelConfig.model,
                            thinkingEffort = modelConfig.thinkingEffort,
                            verbosity = modelConfig.verbosity
                        )
                    },
                    onOptionsClick = {
                        // Opens model & reasoning configuration sheet
                        VelocityHaptics.lightClick(context)
                        isModelSheetOpen = true
                    }
                )
            }
        }

        // Apple Bottom Sheet Host for Threads, Documents, Search, Chronology
        NavigationSheetHost(
            activeSheet = activeSheet,
            onDismiss = { activeSheet = SheetType.NONE },
            repository = repository,
            currentServerConfig = serverConfig,
            onSelectThread = { threadId ->
                viewModel.switchSession(threadId)
            },
            onOpenArtifact = { artifact ->
                onOpenArtifactDetail?.invoke(artifact)
            },
            onDisconnect = onDisconnect
        )

        // Model Configuration Bottom Sheet (Triggered by + button)
        ModelConfigSheet(
            isOpen = isModelSheetOpen,
            currentConfig = modelConfig,
            onConfigChange = {
                modelConfig = it
                ServerConfigManager.saveModelConfig(context, it)
            },
            onDismiss = { isModelSheetOpen = false }
        )
    }
}
