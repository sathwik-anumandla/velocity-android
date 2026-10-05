package com.velocity.app.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import com.velocity.app.data.model.ChatMessage
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val isThread = uiState.currentSessionId != "main"
    var inputText by androidx.compose.runtime.saveable.rememberSaveable(uiState.currentSessionId) { mutableStateOf("") }
    var activeArtifactId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val scrollPositions = remember { mutableMapOf<String, Triple<Int, Int, Boolean>>() }
    val savedPosition = scrollPositions[uiState.currentSessionId]
    val listState = key(uiState.currentSessionId) { rememberLazyListState(savedPosition?.first ?: 0, savedPosition?.second ?: 0) }
    var followLatest by remember(uiState.currentSessionId) { mutableStateOf(savedPosition?.third ?: true) }
    var selectedMessage by remember(uiState.currentSessionId) { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember(uiState.currentSessionId) { mutableStateOf<ChatMessage?>(null) }
    var confirmEdit by remember { mutableStateOf(false) }
    var regenerateMessage by remember { mutableStateOf<ChatMessage?>(null) }

    var isMenuOpen by remember { mutableStateOf(false) }
    var isModelSheetOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    var modelConfig by remember { mutableStateOf(ServerConfigManager.loadModelConfig(context)) }

    // Initialize repository on first launch
    LaunchedEffect(repository) {
        viewModel.initRepository(repository)
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val nearBottom by remember(listState) {
        derivedStateOf {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()
            last == null || (last.index == layout.totalItemsCount - 1 && last.offset + last.size <= layout.viewportEndOffset + 80)
        }
    }
    LaunchedEffect(isDragged) {
        if (isDragged) followLatest = false else if (nearBottom && uiState.messages.isNotEmpty()) followLatest = true
    }
    LaunchedEffect(uiState.currentSessionId, uiState.messages.isNotEmpty()) {
        if (uiState.messages.isNotEmpty() && savedPosition != null && !followLatest) {
            listState.scrollToItem(savedPosition.first.coerceAtMost(uiState.messages.lastIndex), savedPosition.second)
        }
    }
    LaunchedEffect(uiState.messages.lastOrNull()?.id, uiState.messages.lastOrNull()?.content?.length, uiState.isStreaming, imeBottom) {
        if (followLatest && uiState.messages.isNotEmpty()) {
            val lastIndex = uiState.messages.lastIndex
            listState.scrollToItem(lastIndex)
            val layout = listState.layoutInfo
            layout.visibleItemsInfo.lastOrNull { it.index == lastIndex }?.let { last ->
                val overflow = last.offset + last.size - layout.viewportEndOffset
                if (overflow > 0) listState.scrollBy(overflow.toFloat())
            }
        }
    }
    DisposableEffect(listState, uiState.currentSessionId) {
        val sessionId = uiState.currentSessionId
        onDispose { scrollPositions[sessionId] = Triple(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, followLatest) }
    }

    activeArtifactId?.let { artifactId ->
        com.velocity.app.ui.components.ArtifactScreen(repository, artifactId) { activeArtifactId = null }
        return
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
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Apple-Style Header (Centered Brand, Left Back if in Thread, Right Hamburger Menu)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Left Side: Thread Back Button OR Reload Button when on Main
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
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                viewModel.loadMessages()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.Refresh),
                            contentDescription = "Reload",
                            tint = if (uiState.isLoading) Color(0xFFA1A1AA) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Center Title (Tapping triggers reload)
                Text(
                    text = uiState.sessionTitle,
                    fontFamily = SatoshiFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextPrimary,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            VelocityHaptics.lightClick(context)
                            viewModel.loadMessages()
                        }
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
                            .width(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1C1C1E))
                    ) {
                        // Return to Main (If currently in a side thread)
                        if (uiState.currentSessionId != "main") {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Main Timeline",
                                        fontFamily = SatoshiFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(LucideIcons.Home),
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    VelocityHaptics.lightClick(context)
                                    isMenuOpen = false
                                    viewModel.switchSession("main", "velocity")
                                }
                            )
                        }

                        // Reload Conversation
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (uiState.isLoading) "Reloading..." else "Reload Conversation",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(LucideIcons.Refresh),
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                VelocityHaptics.lightClick(context)
                                isMenuOpen = false
                                viewModel.loadMessages()
                            }
                        )
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
            if (uiState.hasMoreHistory) TextButton(onClick = { viewModel.loadOlderMessages() }, enabled = !uiState.isLoadingOlder && uiState.isBackendOnline) {
                Text(if (uiState.isLoadingOlder) "Loading older messages…" else "Load older messages")
            }
            if (!nearBottom && uiState.messages.isNotEmpty()) TextButton(onClick = {
                followLatest = true
                coroutineScope.launch { listState.animateScrollToItem(uiState.messages.lastIndex) }
            }) { Text("Jump to latest") }
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
                        onLongPress = { selectedMessage = msg },
                        isThread = isThread,
                        streamingStatus = if (msg.isStreaming) uiState.streamingStatus else null,
                        onOpenThread = { threadId ->
                            VelocityHaptics.lightClick(context)
                            viewModel.switchSession(threadId)
                        },
                        onOpenArtifact = { artifact ->
                            VelocityHaptics.lightClick(context)
                            activeArtifactId = artifact
                        }
                    )
                }

            }

            // Bottom Section: Action Approval + Single Floating Input Capsule (Elevates with IME)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Thread Proposal Banner
                AnimatedVisibility(
                    visible = uiState.activeProposal != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.activeProposal?.let { prop ->
                        ProposalApprovalCard(
                            title = prop.title,
                            reason = prop.reason,
                            enabled = !uiState.isRespondingToProposal && !uiState.isStreaming && !uiState.isLoading && uiState.isBackendOnline,
                            onDecline = {
                                viewModel.respondProposal(
                                    accept = false,
                                    onSuccess = { VelocityHaptics.lightClick(context) },
                                    onError = { VelocityHaptics.error(context) }
                                )
                            },
                            onApprove = {
                                viewModel.respondProposal(
                                    accept = true,
                                    onSuccess = { VelocityHaptics.success(context) },
                                    onError = { VelocityHaptics.error(context) }
                                )
                            }
                        )
                    }
                }

                // 2. Pending Staged Action Banner (Gmail)
                AnimatedVisibility(
                    visible = uiState.pendingAction != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.pendingAction?.let { action ->
                        ActionApprovalCard(
                            action = action,
                            enabled = !uiState.isRespondingToAction && !uiState.isStreaming && !uiState.isLoading && uiState.isBackendOnline,
                            onDecline = {
                                viewModel.respondAction(
                                    confirm = false,
                                    onSuccess = { VelocityHaptics.lightClick(context) },
                                    onError = { VelocityHaptics.error(context) }
                                )
                            },
                            onApprove = {
                                viewModel.respondAction(
                                    confirm = true,
                                    onSuccess = { VelocityHaptics.success(context) },
                                    onError = { VelocityHaptics.error(context) }
                                )
                            }
                        )
                    }
                }


                // Pure Floating Input Capsule
                editingMessage?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Editing earlier prompt", color = VelocityColors.TextMuted, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editingMessage = null; inputText = "" }) { Text("Cancel edit") }
                    }
                }
                uiState.error?.let { error ->
                    Text(error, color = VelocityColors.TextMuted, fontSize = 12.sp)
                    Row {
                        TextButton(onClick = { viewModel.loadMessages() }, enabled = !uiState.isLoading) { Text("Recover / reload") }
                        TextButton(onClick = {
                            inputText = uiState.messages.lastOrNull { it.role == "user" }?.content.orEmpty()
                            viewModel.clearError()
                        }) { Text("Restore message") }
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                }
                InputCapsule(
                    value = inputText,
                    enabled = !uiState.isStreaming && !uiState.isLoading && uiState.isBackendOnline,
                    isStreaming = uiState.isStreaming,
                    onStop = { viewModel.stopResponse() },
                    onValueChange = { inputText = it },
                    onSend = {
                        if (editingMessage != null) {
                            confirmEdit = true
                            return@InputCapsule
                        }
                        followLatest = true
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(
                            text = text,
                            model = modelConfig.model,
                            thinkingEffort = modelConfig.thinkingEffort,
                            verbosity = modelConfig.verbosity,
                            recallBudget = modelConfig.recallBudget
                        )
                    },
                    onOptionsClick = {
                        // Opens model & reasoning configuration sheet
                        isModelSheetOpen = true
                    }
                )
            }
        }

        selectedMessage?.let { message ->
            val prompt = uiState.messages.takeWhile { it.id != message.id }.lastOrNull { it.role == "user" }
            com.velocity.app.ui.components.MessageActionsSheet(
                message, canModify = !uiState.isStreaming && !uiState.isLoading && uiState.isBackendOnline,
                canRegenerate = prompt != null, onDismiss = { selectedMessage = null },
                onEdit = { editingMessage = message; inputText = message.content; selectedMessage = null },
                onRegenerate = { regenerateMessage = message; selectedMessage = null },
                onBranch = { selectedMessage = null; viewModel.branchMessage(message) }
            )
        }
        if (confirmEdit && editingMessage != null) AlertDialog(
            onDismissRequest = { confirmEdit = false }, title = { Text("Replace this prompt?") },
            text = { Text("This replaces the selected prompt and removes later replies from this conversation. Completed external actions are not undone.") },
            confirmButton = { TextButton(onClick = {
                val message = editingMessage ?: return@TextButton
                confirmEdit = false; editingMessage = null; followLatest = true
                viewModel.editAndSend(message.id, inputText, modelConfig.model, modelConfig.thinkingEffort, modelConfig.verbosity, modelConfig.recallBudget)
                inputText = ""
            }) { Text("Replace and send") } },
            dismissButton = { TextButton(onClick = { confirmEdit = false }) { Text("Cancel") } }
        )
        regenerateMessage?.let { message ->
            val prompt = uiState.messages.takeWhile { it.id != message.id }.lastOrNull { it.role == "user" }
            AlertDialog(onDismissRequest = { regenerateMessage = null }, title = { Text("Regenerate this response?") },
                text = { Text("The original prompt and later replies will be replaced. Existing Calendar, Tasks and Gmail actions will not be repeated or undone; regeneration uses read-only tools.") },
                confirmButton = { TextButton(enabled = prompt != null, onClick = {
                    prompt ?: return@TextButton
                    regenerateMessage = null; followLatest = true
                    viewModel.editAndSend(prompt.id, prompt.content, modelConfig.model, modelConfig.thinkingEffort, modelConfig.verbosity, modelConfig.recallBudget, message.content.take(29000))
                }) { Text("Regenerate") } },
                dismissButton = { TextButton(onClick = { regenerateMessage = null }) { Text("Cancel") } }
            )
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
                activeSheet = SheetType.NONE
                if (onOpenArtifactDetail != null) onOpenArtifactDetail(artifact) else activeArtifactId = artifact.id
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
