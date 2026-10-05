package com.velocity.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.velocity.app.data.model.DeploymentVersion
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.ui.theme.VelocityColors

@Composable
fun DeploymentInfo(repository: ChatRepository) {
    var version by remember { mutableStateOf<DeploymentVersion?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(repository) {
        try { version = repository.fetchVersion() } catch (failure: Exception) { error = failure.message }
    }
    Column {
        Text("Android ${com.velocity.app.BuildConfig.VERSION_NAME} · ${com.velocity.app.BuildConfig.GIT_REVISION}", color = VelocityColors.TextSecondary)
        version?.let {
            Text("Server ${it.version} · ${it.backendRevision.take(12)}", color = VelocityColors.TextSecondary)
            Text("Web ${it.frontendRevision.take(12)}", color = VelocityColors.TextSecondary)
            if (it.matches == false) Text("Backend/web versions differ. Rebuild the VPS deployment.", color = VelocityColors.AccentSky)
        }
        error?.let { Text(it, color = VelocityColors.TextMuted) }
    }
}
