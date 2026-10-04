package com.velocity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.velocity.app.ui.theme.VelocityColors
import com.velocity.app.ui.theme.VelocityTheme
import com.velocity.app.ui.timeline.MainTimelineScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VelocityTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VelocityColors.Canvas
                ) {
                    MainTimelineScreen(
                        onOpenThread = { threadId ->
                            // Slide into thread workspace
                        },
                        onOpenArtifact = { artifactId ->
                            // Slide into artifact viewer
                        },
                        onOpenSheet = { tab ->
                            // Open bottom sheet for Threads, Documents, Search, Chronology, Settings
                        }
                    )
                }
            }
        }
    }
}
