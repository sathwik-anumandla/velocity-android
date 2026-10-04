package com.velocity.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.velocity.app.ui.components.*
import com.velocity.app.ui.theme.VelocityColors

@Composable
fun MainTimelineScreen(
    viewModel: TimelineViewModel = viewModel(),
    onOpenThread: (String) -> Unit = {},
    onOpenArtifact: (String) -> Unit = {},
    onOpenSheet: (DockTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
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
        ) {
            // Minimal Header (Centered "velocity", health dot)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Brand Name
                Text(
                    text = "velocity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )

                // Health Dot (Aligned Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(VelocityColors.StatusOnline)
                )
            }

            // Message Timeline
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatCapsule(
                        message = msg,
                        onOpenThread = onOpenThread,
                        onOpenArtifact = onOpenArtifact
                    )
                }

                // Streaming indicator
                if (uiState.isStreaming && uiState.streamingStatus != null) {
                    item {
                        Box(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                            Text(
                                text = uiState.streamingStatus ?: "Thinking...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }

            // Bottom Section: Approval Banner + Input Capsule + Centered Dock Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Floating Approval Banner
                uiState.pendingAction?.let { action ->
                    ActionApprovalBanner(
                        title = "${action.actionType.replace('_', ' ').uppercase()}: ${action.parameters["subject"] ?: action.parameters["to"] ?: "Approval"}",
                        onDecline = { viewModel.respondAction(confirm = false) },
                        onApprove = { viewModel.respondAction(confirm = true) }
                    )
                }

                // Input Capsule
                InputCapsule(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = {
                        val text = inputText
                        inputText = ""
                        viewModel.sendMessage(text)
                    }
                )

                // Centered Dock Bar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomDockBar(
                        selectedTab = null,
                        onSelectTab = { tab -> onOpenSheet(tab) }
                    )
                }
            }
        }
    }
}
