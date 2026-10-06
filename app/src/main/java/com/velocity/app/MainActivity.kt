package com.velocity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velocity.app.data.repository.ChatRepository
import com.velocity.app.data.repository.ServerConfig
import com.velocity.app.data.repository.ServerConfigManager
import com.velocity.app.ui.pairing.PairingScreen
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTheme
import com.velocity.app.ui.timeline.MainTimelineScreen
import com.velocity.app.ui.timeline.TimelineViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.velocity.app.ui.theme.ThemePreference.load(this)
        setContent {
            VelocityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VelocityColors.Canvas
                ) {
                    var currentConfig by remember {
                        mutableStateOf(
                            if (ServerConfigManager.isPaired(this@MainActivity)) {
                                ServerConfigManager.loadConfig(this@MainActivity)
                            } else {
                                null
                            }
                        )
                    }

                    val timelineViewModel: TimelineViewModel = viewModel()

                    Crossfade(
                        targetState = currentConfig,
                        label = "screen_transition"
                    ) { config ->
                        if (config != null && config.isPaired) {
                            val repository = remember(config) { ChatRepository(config, applicationContext) }

                            MainTimelineScreen(
                                repository = repository,
                                serverConfig = config,
                                viewModel = timelineViewModel,
                                onDisconnect = {
                                    ServerConfigManager.clearConfig(this@MainActivity)
                                    currentConfig = null
                                }
                            )
                        } else {
                            PairingScreen(
                                initialConfig = ServerConfigManager.loadConfig(this@MainActivity),
                                onPairingSuccess = { newConfig ->
                                    currentConfig = newConfig
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
