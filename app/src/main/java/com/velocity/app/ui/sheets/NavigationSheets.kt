package com.velocity.app.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
    onDisconnect: () -> Unit,
    currentSessionId: String = "main",
    onSelectMessage: (String, String) -> Unit = { sessionId, _ -> onSelectThread(sessionId) }
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
                    currentSessionId = currentSessionId,
                    onOpenArtifact = { artifact -> onOpenArtifact(artifact); onDismiss() },
                    onSelectMessage = { sessionId, messageId ->
                        if (messageId != null) onSelectMessage(sessionId, messageId) else onSelectThread(sessionId)
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
            it.summary.orEmpty().contains(filterQuery, ignoreCase = true)
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
                            if (!doc.summary.isNullOrBlank()) {
                                Text(
                                    text = doc.summary.orEmpty(),
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
    currentSessionId: String,
    onSelectMessage: (String, String?) -> Unit,
    onOpenArtifact: (ArtifactItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("all") }
    var currentOnly by remember { mutableStateOf(false) }
    var author by remember { mutableStateOf("") }
    var after by remember { mutableStateOf("") }
    var before by remember { mutableStateOf("") }
    var filtersOpen by remember { mutableStateOf(false) }
    var offset by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf(com.velocity.app.data.model.RepositorySearchPage()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, kind, currentOnly, author, after, before) {
        offset = 0
        page = com.velocity.app.data.model.RepositorySearchPage()
    }
    LaunchedEffect(query, kind, currentOnly, author, after, before, offset) {
        if (query.isBlank()) { busy = false; return@LaunchedEffect }
        busy = true
        try {
            delay(250)
            val result = repository.searchRepository(query, kind, currentSessionId.takeIf { currentOnly }, author.takeIf { it.isNotBlank() }, after.takeIf { it.isNotBlank() }, before.takeIf { it.isNotBlank() }, offset)
            page = result.copy(results = if (offset == 0) result.results else (page.results + result.results).distinctBy { it.kind to it.id })
            error = null
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message
        } finally {
            busy = false
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Search", style = VelocityTypography.titleLarge, color = VelocityColors.TextPrimary)
            IconButton(onClick = onDismiss) { Icon(painterResource(LucideIcons.Close), "Close search", tint = VelocityColors.TextMuted) }
        }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search saved content…") })
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all", "messages", "threads", "documents").forEach { category ->
                FilterChip(selected = kind == category, onClick = { kind = category }, label = { Text(category.replaceFirstChar { it.uppercase() }) })
            }
        }
        TextButton(onClick = { filtersOpen = !filtersOpen }) { Text(if (filtersOpen) "Hide filters" else "Filters") }
        if (filtersOpen) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(currentOnly, { currentOnly = it })
                Text("Current conversation only", style = VelocityTypography.bodySmall, color = VelocityColors.TextMuted)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("" to "Any author", "user" to "You", "assistant" to "Velocity").forEach { (role, label) ->
                    FilterChip(selected = author == role, onClick = { author = role }, label = { Text(label) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(after, { after = it }, label = { Text("From YYYY-MM-DD") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(before, { before = it }, label = { Text("Until YYYY-MM-DD") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
        error?.let { Text(it, color = VelocityColors.Accent, style = VelocityTypography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)) }
        if (!busy && page.results.isEmpty()) Text(if (query.isBlank()) "Find messages, threads and documents. Use quotes for exact phrases." else "No matches. Try fewer words or wider filters.", color = VelocityColors.TextMuted, style = VelocityTypography.bodySmall, modifier = Modifier.padding(vertical = 16.dp))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("documents", "messages", "threads").forEach { category ->
                val results = page.results.filter { it.kind == category }
                if (results.isNotEmpty()) item(key = category) { Text(category.uppercase(), color = VelocityColors.TextDim, style = VelocityTypography.labelSmall, modifier = Modifier.padding(top = 12.dp)) }
                items(results, key = { it.kind + it.id }) { result ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VelocityColors.SurfaceCapsule).clickable {
                        VelocityHaptics.lightClick(context)
                        if (result.kind == "documents") onOpenArtifact(ArtifactItem(id = result.id, title = result.title.orEmpty()))
                        else onSelectMessage(result.sessionId, result.messageId)
                    }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(result.title ?: "Main timeline", color = VelocityColors.TextPrimary, style = VelocityTypography.titleSmall)
                        val excerpt = androidx.compose.ui.text.buildAnnotatedString {
                            var highlighted = false
                            (result.snippet.ifBlank { result.content }).forEach { character ->
                                when (character) {
                                    '\uE000' -> { highlighted = true; pushStyle(androidx.compose.ui.text.SpanStyle(color = VelocityColors.Accent, fontWeight = FontWeight.Bold)) }
                                    '\uE001' -> if (highlighted) { pop(); highlighted = false }
                                    else -> append(character)
                                }
                            }
                        }
                        Text(excerpt, color = VelocityColors.TextMuted, style = VelocityTypography.bodySmall, maxLines = 3)
                        Text(listOfNotNull(result.role, result.createdAt.take(10)).joinToString(" · "), color = VelocityColors.TextDim, style = VelocityTypography.labelSmall)
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = VelocityColors.Accent) }
            if (page.hasMore && !busy) item { TextButton(onClick = { offset = page.nextOffset ?: 0 }, modifier = Modifier.fillMaxWidth()) { Text("Load more results") } }
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
