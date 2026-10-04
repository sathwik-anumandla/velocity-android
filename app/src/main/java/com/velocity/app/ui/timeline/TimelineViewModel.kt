package com.velocity.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velocity.app.data.api.ChatStreamEvent
import com.velocity.app.data.model.ChatMessage
import com.velocity.app.data.model.StagedAction
import com.velocity.app.data.model.ThreadProposal
import com.velocity.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class TimelineUiState(
    val currentSessionId: String = "main",
    val sessionTitle: String = "velocity",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingStatus: String? = null,
    val isBackendOnline: Boolean = true,
    val activeProposal: ThreadProposal? = null,
    val pendingAction: StagedAction? = null,
    val error: String? = null
)

class TimelineViewModel : ViewModel() {

    private var repository: ChatRepository? = null

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    fun initRepository(repo: ChatRepository) {
        this.repository = repo
        loadMessages()
        checkHealth()
    }

    fun checkHealth() {
        val repo = repository ?: return
        viewModelScope.launch {
            val result = repo.testConnection()
            _uiState.value = _uiState.value.copy(isBackendOnline = result.isSuccess)
        }
    }

    fun switchSession(sessionId: String, title: String? = null) {
        val newTitle = if (sessionId == "main") "velocity" else (title ?: "Thread")
        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            sessionTitle = newTitle,
            messages = emptyList(),
            isLoading = true
        )
        loadMessages()
    }

    fun loadMessages() {
        val repo = repository ?: return
        val sessionId = _uiState.value.currentSessionId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val msgs = if (sessionId == "main") {
                    repo.fetchMainMessages()
                } else {
                    repo.fetchThreadMessages(sessionId)
                }
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    isLoading = false,
                    isBackendOnline = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false,
                    isBackendOnline = false
                )
            }
        }
    }

    fun sendMessage(
        text: String,
        model: String? = null,
        thinkingEffort: String = "medium",
        verbosity: String = "medium"
    ) {
        if (text.isBlank()) return
        val repo = repository ?: return
        val sessionId = _uiState.value.currentSessionId

        val userMsgId = UUID.randomUUID().toString()
        val asstMsgId = UUID.randomUUID().toString()

        val userMsg = ChatMessage(id = userMsgId, role = "user", content = text)
        val asstMsg = ChatMessage(id = asstMsgId, role = "assistant", content = "", isStreaming = true)

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + listOf(userMsg, asstMsg),
            isStreaming = true,
            streamingStatus = "Thinking..."
        )

        viewModelScope.launch {
            try {
                repo.streamTurn(
                    message = text,
                    sessionId = sessionId,
                    model = model,
                    thinkingEffort = thinkingEffort,
                    verbosity = verbosity
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Status -> {
                            _uiState.value = _uiState.value.copy(streamingStatus = event.text)
                        }
                        is ChatStreamEvent.Delta -> {
                            updateAssistantMessage(asstMsgId) { it.copy(content = it.content + event.text) }
                        }
                        is ChatStreamEvent.Proposal -> {
                            _uiState.value = _uiState.value.copy(activeProposal = event.proposal)
                            updateAssistantMessage(asstMsgId) { it.copy(threadProposal = event.proposal) }
                        }
                        is ChatStreamEvent.ArtifactCreated -> {
                            updateAssistantMessage(asstMsgId) { it.copy(artifact = event.artifact) }
                        }
                        is ChatStreamEvent.ActionProposal -> {
                            _uiState.value = _uiState.value.copy(pendingAction = event.action)
                            updateAssistantMessage(asstMsgId) { it.copy(stagedAction = event.action) }
                        }
                        is ChatStreamEvent.Complete -> {
                            updateAssistantMessage(asstMsgId) {
                                it.copy(
                                    id = event.messageId ?: it.id,
                                    content = if (event.text.isNotEmpty()) event.text else it.content,
                                    isStreaming = false
                                )
                            }
                            _uiState.value = _uiState.value.copy(
                                isStreaming = false,
                                streamingStatus = null
                            )
                        }
                        is ChatStreamEvent.Error -> {
                            updateAssistantMessage(asstMsgId) {
                                it.copy(content = it.content.ifEmpty { "[Error]: ${event.message}" }, isStreaming = false)
                            }
                            _uiState.value = _uiState.value.copy(
                                isStreaming = false,
                                streamingStatus = null,
                                error = event.message
                            )
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isStreaming = false, error = e.message)
            }
        }
    }

    fun respondAction(confirm: Boolean) {
        val repo = repository ?: return
        val action = _uiState.value.pendingAction ?: return
        viewModelScope.launch {
            try {
                repo.respondAction(action.id, confirm)
                _uiState.value = _uiState.value.copy(pendingAction = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun updateAssistantMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.map { if (it.id == id) transform(it) else it }
        )
    }
}
