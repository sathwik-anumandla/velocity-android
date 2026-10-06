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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtifactScreen(repository: ChatRepository, artifactId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artifact by remember(artifactId) { mutableStateOf<ArtifactItem?>(null) }
    var error by remember(artifactId) { mutableStateOf<String?>(null) }
    var busy by remember(artifactId) { mutableStateOf(false) }
    var exporting by remember(artifactId) { mutableStateOf(false) }
    var copied by remember(artifactId) { mutableStateOf(false) }
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

    BackHandler(onBack = onBack)
    LaunchedEffect(artifactId) { refresh() }
    LaunchedEffect(copied) { if (copied) { kotlinx.coroutines.delay(2000); copied = false } }
    Column(Modifier.fillMaxSize().background(VelocityColors.Canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(painterResource(LucideIcons.ChevronLeft), "Back to chat", tint = VelocityColors.TextPrimary) }
            Text("Documents", color = VelocityColors.TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { refresh() }, enabled = !busy) { Icon(painterResource(LucideIcons.Refresh), "Refresh document", tint = VelocityColors.TextMuted) }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp)); if (artifact == null) TextButton(onClick = { refresh() }, enabled = !busy) { Text("Try again") } }
        artifact?.let { document ->
            val words = remember(document.content) { document.content.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
            val paper = when (document.theme) {
                "midnight" -> androidx.compose.ui.graphics.Color(0xFF141416)
                "technical" -> androidx.compose.ui.graphics.Color(0xFFF8F8FA)
                else -> androidx.compose.ui.graphics.Color.White
            }
            val ink = if (document.theme == "midnight") androidx.compose.ui.graphics.Color(0xFFE4E4E7) else androidx.compose.ui.graphics.Color(0xFF202023)
            val paperAccent = if (document.theme == "midnight") androidx.compose.ui.graphics.Color(0xFF9AA3D0) else androidx.compose.ui.graphics.Color(0xFF575F9F)
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(enabled = !savingAppearance && !exporting, onClick = { appearanceOpen = true }) { Text(if (savingAppearance) "Saving…" else "Appearance: ${document.theme.replaceFirstChar { it.uppercase() }}") }
                    DropdownMenu(expanded = appearanceOpen, onDismissRequest = { appearanceOpen = false }) {
                        listOf("editorial", "clean", "technical", "midnight").forEach { theme ->
                            DropdownMenuItem(text = { Text(theme.replaceFirstChar { it.uppercase() }) }, onClick = {
                                appearanceOpen = false
                                savingAppearance = true
                                scope.launch {
                                    try { artifact = repository.updateArtifactTheme(artifactId, theme); error = null }
                                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                    catch (failure: Exception) { error = failure.message }
                                    finally { savingAppearance = false }
                                }
                            })
                        }
                    }
                }
                Text("Used for PDF", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
            }
            FlowRow(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(document.title, document.content))
                    copied = true
                }) { Icon(painterResource(LucideIcons.Copy), null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (copied) "Copied" else "Copy") }
                FilledTonalButton(onClick = {
                    try {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, document.title)
                            putExtra(Intent.EXTRA_TEXT, document.content)
                        }, "Share document"))
                    } catch (failure: Exception) { error = failure.message }
                }) { Icon(painterResource(LucideIcons.Share), null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Share") }
                FilledTonalButton(enabled = !exporting && !savingAppearance, onClick = {
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
                }) { Icon(painterResource(LucideIcons.Download), null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(if (exporting) "Exporting…" else "PDF") }
            }
            Column(Modifier.weight(1f).fillMaxWidth().background(VelocityColors.SurfaceCard).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.fillMaxWidth().background(paper, RoundedCornerShape(20.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(document.artifactType.replace('_', ' ').uppercase(), color = paperAccent, style = MaterialTheme.typography.labelSmall)
                    Text(document.title, color = ink, fontFamily = if (document.theme == "editorial") androidx.compose.ui.text.font.FontFamily.Serif else com.velocity.app.ui.theme.SatoshiFontFamily, style = MaterialTheme.typography.headlineLarge)
                    Text("$words words · ${document.language} · v${document.version}", color = ink.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    document.summary?.takeIf { it.isNotBlank() }?.let { Text(it, color = ink, style = MaterialTheme.typography.bodyMedium) }
                }
                Column(Modifier.fillMaxWidth().background(paper, RoundedCornerShape(20.dp)).padding(20.dp)) {
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
