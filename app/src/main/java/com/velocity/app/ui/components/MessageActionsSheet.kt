package com.velocity.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.velocity.app.data.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsSheet(message: ChatMessage, canModify: Boolean, canRegenerate: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit, onRegenerate: () -> Unit, onBranch: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (message.role == "user") "Your message" else "Response", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Velocity message", message.content))
                onDismiss()
            }) { Text("Copy text") }
            TextButton(onClick = {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message.content) }, "Share message"))
                onDismiss()
            }) { Text("Share") }
            if (message.role == "user") TextButton(onClick = onEdit, enabled = canModify) { Text("Edit message") }
            if (message.role == "assistant") TextButton(onClick = onRegenerate, enabled = canModify && canRegenerate) { Text("Regenerate response") }
            TextButton(onClick = onBranch, enabled = canModify) { Text("Branch into a thread") }
            if (!canModify) Text("Connect to the server and stop the active response to edit or branch.")
            if (message.role == "assistant" && !canRegenerate) Text("Load older messages to include the original prompt before regenerating.")
        }
    }
}
