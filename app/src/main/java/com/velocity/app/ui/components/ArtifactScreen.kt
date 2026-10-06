package com.velocity.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.velocity.app.data.model.ArtifactItem
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.ui.theme.VelocityColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ArtifactScreen(repository: ChatRepository, artifactId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artifact by remember(artifactId) { mutableStateOf<ArtifactItem?>(null) }
    var error by remember(artifactId) { mutableStateOf<String?>(null) }
    var busy by remember(artifactId) { mutableStateOf(false) }
    var exporting by remember(artifactId) { mutableStateOf(false) }
    var copied by remember(artifactId) { mutableStateOf(false) }
    var menuOpen by remember(artifactId) { mutableStateOf(false) }
    var appearanceOpen by remember(artifactId) { mutableStateOf(false) }
    var savingAppearance by remember(artifactId) { mutableStateOf(false) }

    fun refresh() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                artifact = repository.fetchArtifact(artifactId)
                error = null
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message
            } finally {
                busy = false
            }
        }
    }

    BackHandler { if (menuOpen) { menuOpen = false; appearanceOpen = false } else onBack() }
    LaunchedEffect(artifactId) { refresh() }
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(2000); copied = false } }
    Column(Modifier.fillMaxSize().background(VelocityColors.Canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(painterResource(LucideIcons.ChevronLeft), "Back to chat", tint = VelocityColors.TextPrimary) }
            Text("Documents", color = VelocityColors.TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true; appearanceOpen = false }) { Icon(painterResource(LucideIcons.Menu), "Document options", tint = VelocityColors.TextMuted) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false; appearanceOpen = false }) {
                    if (appearanceOpen) {
                        DropdownMenuItem(text = { Text("Back to options") }, leadingIcon = { Icon(painterResource(LucideIcons.ChevronLeft), null) }, onClick = { appearanceOpen = false })
                        Text("Also applies to PDF exports", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        listOf("midnight" to "Midnight", "editorial" to "Editorial", "clean" to "Clean", "technical" to "Technical Light", "technical-dark" to "Technical Dark").forEach { (theme, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = if (artifact?.theme == theme) VelocityColors.Accent else VelocityColors.TextPrimary) },
                                enabled = artifact != null && !savingAppearance && !exporting,
                                onClick = {
                                    menuOpen = false
                                    appearanceOpen = false
                                    savingAppearance = true
                                    scope.launch {
                                        try { artifact = repository.updateArtifactTheme(artifactId, theme); error = null }
                                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                        catch (failure: Exception) { error = failure.message }
                                        finally { savingAppearance = false }
                                    }
                                }
                            )
                        }
                    } else {
                        DropdownMenuItem(text = { Text("Appearance") }, leadingIcon = { Icon(painterResource(LucideIcons.Sliders), null) }, enabled = artifact != null, onClick = { appearanceOpen = true })
                        artifact?.let { document ->
                            DropdownMenuItem(text = { Text("Copy Markdown") }, leadingIcon = { Icon(painterResource(LucideIcons.Copy), null) }, onClick = {
                                menuOpen = false
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(document.title, document.content))
                                copied = true
                            })
                            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(painterResource(LucideIcons.Share), null) }, onClick = {
                                menuOpen = false
                                try {
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, document.title)
                                        putExtra(Intent.EXTRA_TEXT, document.content)
                                    }, "Share document"))
                                } catch (failure: Exception) { error = failure.message }
                            })
                            DropdownMenuItem(text = { Text("Export PDF") }, leadingIcon = { Icon(painterResource(LucideIcons.Download), null) }, enabled = !exporting && !savingAppearance, onClick = {
                                menuOpen = false
                                exporting = true
                                scope.launch {
                                    try {
                                        val bytes = repository.exportArtifactPdf(artifactId)
                                        val file = withContext(Dispatchers.IO) {
                                            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
                                            File(directory, "velocity-${artifactId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.pdf").apply { writeBytes(bytes) }
                                        }
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            clipData = ClipData.newRawUri(document.title, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }, "Save or share PDF"))
                                        error = null
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                    catch (failure: Exception) { error = failure.message }
                                    finally { exporting = false }
                                }
                            })
                        }
                        DropdownMenuItem(text = { Text("Refresh") }, leadingIcon = { Icon(painterResource(LucideIcons.Refresh), null) }, enabled = !busy && !savingAppearance && !exporting, onClick = { menuOpen = false; refresh() })
                    }
                }
            }
        }
        if (copied || exporting || savingAppearance) Text(if (exporting) "Exporting PDF…" else if (savingAppearance) "Saving appearance…" else "Document copied", color = VelocityColors.TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)); if (artifact == null) TextButton(onClick = { refresh() }, enabled = !busy) { Text("Try again") } }
        artifact?.let { document ->
            val words = remember(document.content) { document.content.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
            val paper = when (document.theme) {
                "midnight", "technical-dark" -> androidx.compose.ui.graphics.Color(0xFF141416)
                "technical" -> androidx.compose.ui.graphics.Color(0xFFF8F8FA)
                else -> androidx.compose.ui.graphics.Color.White
            }
            val ink = if (document.theme in setOf("midnight", "technical-dark")) androidx.compose.ui.graphics.Color(0xFFE4E4E7) else androidx.compose.ui.graphics.Color(0xFF202023)
            val paperAccent = if (document.theme in setOf("midnight", "technical-dark")) androidx.compose.ui.graphics.Color(0xFF54E6D4) else androidx.compose.ui.graphics.Color(0xFF087F73)
            Column(Modifier.weight(1f).fillMaxWidth().background(VelocityColors.SurfaceCard).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.fillMaxWidth().background(paper, RoundedCornerShape(20.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(document.artifactType.replace('_', ' ').uppercase(), color = paperAccent, style = MaterialTheme.typography.labelSmall)
                    Text(document.title, color = ink, fontFamily = if (document.theme == "editorial") androidx.compose.ui.text.font.FontFamily.Serif else com.velocity.app.ui.theme.SatoshiFontFamily, style = MaterialTheme.typography.headlineLarge)
                    Text("$words words · ${document.language} · v${document.version}", color = ink.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    document.summary?.takeIf { it.isNotBlank() }?.let { Text(it, color = ink, style = MaterialTheme.typography.bodyMedium) }
                    HorizontalDivider(color = ink.copy(alpha = 0.12f))
                    if (document.language == "markdown") {
                        FormattedMarkdownText(document.content, isUser = false, isThread = true, documentTheme = document.theme)
                    } else {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(document.content, color = ink, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
