package com.velocity.app.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.app.data.api.ChronologyItem
import com.velocity.app.data.api.SearchResultItem
import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.model.ThreadItem
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.data.repository.ServerConfig
import com.velocity.app.ui.components.LucideIcons
import com.velocity.app.ui.theme.MonoTextStyle
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTypography
import com.velocity.app.ui.util.VelocityHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SheetType {
    NONE,
    THREADS,
    DOCUMENTS,
    SEARCH,
    CHRONOLOGY,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationSheetHost(
    activeSheet: SheetType,
    onDismiss: () -> Unit,
    repository: ChatRepository,
    currentServerConfig: ServerConfig,
    onSelectThread: (String) -> Unit,
    onOpenArtifact: (ArtifactItem) -> Unit,
    onDisconnect: () -> Unit
) {
    if (activeSheet == SheetType.NONE) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VelocityColors.SurfaceCard,
        scrimColor = Color(0x99000000),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceElevated)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            when (activeSheet) {
                SheetType.THREADS -> ThreadsSheetContent(
                    repository = repository,
                    onSelectThread = {
                        onSelectThread(it)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
                SheetType.DOCUMENTS -> DocumentsSheetContent(
                    repository = repository,
                    onOpenArtifact = {
                        onOpenArtifact(it)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
                SheetType.SEARCH -> SearchSheetContent(
                    repository = repository,
                    onSelectMessage = { sessionId ->
                        onSelectThread(sessionId)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
                SheetType.CHRONOLOGY -> ChronologySheetContent(
                    repository = repository,
                    onSelectThread = {
                        onSelectThread(it)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
                SheetType.SETTINGS -> SettingsSheetContent(
                    serverConfig = currentServerConfig,
                    repository = repository,
                    onDisconnect = {
                        onDisconnect()
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
                SheetType.NONE -> {}
            }
        }
    }
}

@Composable
private fun ThreadsSheetContent(
    repository: ChatRepository,
    onSelectThread: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var threads by remember { mutableStateOf<List<ThreadItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        threads = repository.cachedThreads()
        isLoading = threads.isEmpty()
        threads = withContext(Dispatchers.IO) { repository.fetchThreads() }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Threads),
                    contentDescription = null,
                    tint = VelocityColors.AccentViolet,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Threads",
                    style = VelocityTypography.headlineMedium,
                    color = VelocityColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = VelocityColors.TextPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        } else if (threads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(LucideIcons.Threads),
                        contentDescription = null,
                        tint = VelocityColors.SurfaceElevated,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No side threads yet",
                        style = VelocityTypography.bodyMedium,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = "Propose a thread in chat to dive deeper into technical tasks",
                        style = VelocityTypography.bodySmall,
                        color = VelocityColors.TextDim
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(threads, key = { it.id }) { thread ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                onSelectThread(thread.id)
                            }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = thread.name,
                                    style = VelocityTypography.titleMedium,
                                    color = VelocityColors.TextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    painter = painterResource(LucideIcons.ChevronRight),
                                    contentDescription = null,
                                    tint = VelocityColors.TextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (!thread.rollupSummary.isNullOrBlank()) {
                                Text(
                                    text = thread.rollupSummary,
                                    style = VelocityTypography.bodySmall,
                                    color = VelocityColors.TextSecondary,
                                    maxLines = 2
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
private fun DocumentsSheetContent(
    repository: ChatRepository,
    onOpenArtifact: (ArtifactItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var artifacts by remember { mutableStateOf<List<ArtifactItem>>(emptyList()) }
    var filterQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        artifacts = withContext(Dispatchers.IO) { repository.fetchArtifacts() }
        isLoading = false
    }

    val filtered = if (filterQuery.isBlank()) artifacts else {
        artifacts.filter {
            it.title.contains(filterQuery, ignoreCase = true) ||
            it.summary.contains(filterQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Documents),
                    contentDescription = null,
                    tint = VelocityColors.AccentEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Documents",
                    style = VelocityTypography.headlineMedium,
                    color = VelocityColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.SurfaceCapsule)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Search),
                    contentDescription = null,
                    tint = VelocityColors.TextDim,
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    textStyle = TextStyle(color = VelocityColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(VelocityColors.AccentSky),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (filterQuery.isEmpty()) {
                            Text("Filter documents...", style = VelocityTypography.bodyMedium, color = VelocityColors.TextDim)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (filterQuery.isBlank()) "No documents generated yet" else "No matching documents",
                    style = VelocityTypography.bodyMedium,
                    color = VelocityColors.TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.id }) { doc ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.SurfaceCard)
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                onOpenArtifact(doc)
                            }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = doc.title,
                                    style = VelocityTypography.titleMedium,
                                    color = VelocityColors.TextPrimary,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(VelocityColors.SurfaceElevated)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = doc.artifactType.replace('_', ' '),
                                        style = MonoTextStyle.copy(fontSize = 10.sp),
                                        color = VelocityColors.AccentSky
                                    )
                                }
                            }
                            if (doc.summary.isNotBlank()) {
                                Text(
                                    text = doc.summary,
                                    style = VelocityTypography.bodySmall,
                                    color = VelocityColors.TextSecondary,
                                    maxLines = 2
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
private fun SearchSheetContent(
    repository: ChatRepository,
    onSelectMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        isSearching = true
        results = withContext(Dispatchers.IO) { repository.searchMessages(query) }
        isSearching = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Search),
                    contentDescription = null,
                    tint = VelocityColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Search History",
                    style = VelocityTypography.headlineMedium,
                    color = VelocityColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.SurfaceCapsule)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Search),
                    contentDescription = null,
                    tint = VelocityColors.TextDim,
                    modifier = Modifier.size(16.dp)
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    textStyle = TextStyle(color = VelocityColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(VelocityColors.AccentSky),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text("Search messages, code, memory...", style = VelocityTypography.bodyMedium, color = VelocityColors.TextDim)
                        }
                        innerTextField()
                    }
                )
                if (query.isNotEmpty()) {
                    Icon(
                        painter = painterResource(LucideIcons.Close),
                        contentDescription = "Clear",
                        tint = VelocityColors.TextDim,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { query = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isBlank()) "Type to search messages" else "No matching messages found",
                    style = VelocityTypography.bodyMedium,
                    color = VelocityColors.TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(results) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .clickable {
                                VelocityHaptics.lightClick(context)
                                onSelectMessage(item.sessionId)
                            }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.sessionName ?: "Main Timeline",
                                style = VelocityTypography.titleSmall,
                                color = VelocityColors.AccentSky
                            )
                            Text(
                                text = item.snippet ?: item.content,
                                style = VelocityTypography.bodySmall,
                                color = VelocityColors.TextPrimary,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChronologySheetContent(
    repository: ChatRepository,
    onSelectThread: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<ChronologyItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        events = withContext(Dispatchers.IO) { repository.fetchChronology() }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Chronology),
                    contentDescription = null,
                    tint = VelocityColors.AccentViolet,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Chronology",
                    style = VelocityTypography.headlineMedium,
                    color = VelocityColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VelocityColors.TextPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chronology events recorded yet",
                    style = VelocityTypography.bodyMedium,
                    color = VelocityColors.TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(events) { ev ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.SurfaceCapsule)
                            .clickable {
                                (ev.metadata?.get("thread_id") as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { threadId ->
                                    VelocityHaptics.lightClick(context)
                                    onSelectThread(threadId)
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ev.type.uppercase(),
                                    style = MonoTextStyle.copy(fontSize = 10.sp),
                                    color = VelocityColors.AccentViolet
                                )
                                ev.timestamp?.let {
                                    Text(
                                        text = it.take(10),
                                        style = MonoTextStyle.copy(fontSize = 10.sp),
                                        color = VelocityColors.TextDim
                                    )
                                }
                            }
                            Text(
                                text = ev.title,
                                style = VelocityTypography.titleSmall,
                                color = VelocityColors.TextPrimary
                            )
                            ev.description?.let {
                                Text(
                                    text = it,
                                    style = VelocityTypography.bodySmall,
                                    color = VelocityColors.TextSecondary
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
private fun SettingsSheetContent(
    serverConfig: ServerConfig,
    repository: ChatRepository,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pingStatus by remember { mutableStateOf<String?>("Connected") }
    var isChecking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Settings),
                    contentDescription = null,
                    tint = VelocityColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Settings",
                    style = VelocityTypography.headlineMedium,
                    color = VelocityColors.TextPrimary
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.SurfaceCapsule)
                    .clickable {
                        VelocityHaptics.subtleTick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Close),
                    contentDescription = "Close",
                    tint = VelocityColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Server Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(VelocityColors.SurfaceCapsule)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Connected Backend",
                style = VelocityTypography.titleSmall,
                color = VelocityColors.TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Server URL", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                Text(
                    text = serverConfig.normalizedUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                    style = MonoTextStyle,
                    color = VelocityColors.TextPrimary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cloudflare Access", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                Text(
                    text = if (serverConfig.cfClientId.isNotBlank()) "Configured" else "Direct",
                    style = MonoTextStyle,
                    color = if (serverConfig.cfClientId.isNotBlank()) VelocityColors.AccentEmerald else VelocityColors.TextDim
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Health Check", style = VelocityTypography.bodyMedium, color = VelocityColors.TextSecondary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VelocityColors.SurfaceElevated)
                        .clickable {
                            if (!isChecking) {
                                isChecking = true
                                pingStatus = "Testing..."
                                coroutineScope.launch {
                                    val res = withContext(Dispatchers.IO) { repository.testConnection() }
                                    isChecking = false
                                    pingStatus = if (res.isSuccess) "Operational" else "Degraded"
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = pingStatus ?: "Test Ping",
                        style = MonoTextStyle.copy(fontSize = 11.sp),
                        color = if (pingStatus == "Operational") VelocityColors.AccentEmerald else VelocityColors.AccentAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Disconnect / Re-pair Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                .clickable {
                    VelocityHaptics.lightClick(context)
                    onDisconnect()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Disconnect & Re-pair",
                style = VelocityTypography.titleSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
