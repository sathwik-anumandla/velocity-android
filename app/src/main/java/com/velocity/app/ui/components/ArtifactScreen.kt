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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }

    fun refresh() {
        if (busy) return
        busy = true
        scope.launch {
            try {
                artifact = repository.fetchArtifact(artifactId)
                error = null
            } catch (failure: Exception) {
                error = failure.message
            } finally {
                busy = false
            }
        }
    }

    BackHandler(onBack = onBack)
    LaunchedEffect(artifactId) { refresh() }
    Column(Modifier.fillMaxSize().background(VelocityColors.Canvas).statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back") }
            TextButton(onClick = { refresh() }, enabled = !busy) { Text("Refresh") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        artifact?.let { document ->
            Text(document.title, color = VelocityColors.TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Text("${document.artifactType} · ${document.language}", color = VelocityColors.TextMuted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(document.title, document.content))
                }) { Text("Copy") }
                TextButton(onClick = {
                    try {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, document.title)
                            putExtra(Intent.EXTRA_TEXT, document.content)
                        }, "Share document"))
                    } catch (failure: Exception) { error = failure.message }
                }) { Text("Share") }
                TextButton(enabled = !exporting, onClick = {
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
                        } catch (failure: Exception) { error = failure.message }
                        finally { exporting = false }
                    }
                }) { Text(if (exporting) "Exporting…" else "PDF") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
                if (document.language == "markdown") {
                    FormattedMarkdownText(document.content, isUser = false, isThread = true)
                } else {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(document.content, color = VelocityColors.TextPrimary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
