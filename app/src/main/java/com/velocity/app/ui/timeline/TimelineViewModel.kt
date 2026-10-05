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
    val activeProposalMessageId: String? = null,
    val isRespondingToProposal: Boolean = false,
    val pendingAction: StagedAction? = null,
    val error: String? = null
)

class TimelineViewModel : ViewModel() {

    private var repository: ChatRepository? = null
    private var streamJob: kotlinx.coroutines.Job? = null

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
        streamJob?.cancel()
        val newTitle = if (sessionId == "main") "velocity" else (title ?: "Thread")
        _uiState.value = _uiState.value.copy(
            currentSessionId = sessionId,
            sessionTitle = newTitle,
            messages = emptyList(),
            pendingAction = null,
            activeProposal = null,
            activeProposalMessageId = null,
            isLoading = true,
            isStreaming = false,
            streamingStatus = null,
            error = null
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

                val pendingAct = msgs.findLast { it.stagedAction != null && it.stagedAction.status == "pending" }?.stagedAction
                val pendingPropMsg = msgs.findLast { it.threadProposal != null && it.threadProposal.status == "pending" }

                if (_uiState.value.currentSessionId != sessionId) return@launch
                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    pendingAction = pendingAct,
                    activeProposal = pendingPropMsg?.threadProposal,
                    activeProposalMessageId = pendingPropMsg?.id,
                    isLoading = false,
                    isBackendOnline = true,
                    error = msgs.lastOrNull { it.role == "assistant" }?.turnStatus?.takeIf { it in setOf("cancelled", "failed", "interrupted") }?.let { "Response $it. Partial text is saved; restore your message to try again." }
                )
                msgs.lastOrNull { it.role == "assistant" && it.turnStatus == "running" }?.let { resumeTurn(it, sessionId) }
            } catch (e: Exception) {
                if (_uiState.value.currentSessionId != sessionId) return@launch
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
        model: String? = "gpt-5.4-mini",
        thinkingEffort: String = "medium",
        verbosity: String = "low",
        recallBudget: String = "medium"
    ) {
        if (text.isBlank() || _uiState.value.isStreaming || _uiState.value.isLoading) return
        val repo = repository ?: return
        val sessionId = _uiState.value.currentSessionId
        val turnId = UUID.randomUUID().toString()
        val assistantId = "$turnId:assistant"
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + listOf(
                ChatMessage(id = turnId, role = "user", content = text, turnId = turnId, turnStatus = "running"),
                ChatMessage(id = assistantId, role = "assistant", isStreaming = true, turnId = turnId, turnStatus = "running")
            ),
            isStreaming = true, streamingStatus = "Thinking…", error = null
        )
        collectTurn(repo.streamTurn(text, sessionId, model, thinkingEffort, verbosity, turnId, recallBudget), sessionId, assistantId)
    }

    private fun resumeTurn(message: ChatMessage, sessionId: String) {
        val repo = repository ?: return
        val turnId = message.turnId ?: return
        updateAssistantMessage(message.id) { it.copy(content = "", reasoning = null, isStreaming = true) }
        _uiState.value = _uiState.value.copy(isStreaming = true, streamingStatus = "Reconnecting…")
        collectTurn(repo.resumeTurn(turnId), sessionId, message.id)
    }

    private fun collectTurn(events: kotlinx.coroutines.flow.Flow<ChatStreamEvent>, sessionId: String, assistantId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                events.collect { event ->
                    if (_uiState.value.currentSessionId != sessionId) return@collect
                    when (event) {
                        is ChatStreamEvent.Status -> _uiState.value = _uiState.value.copy(streamingStatus = event.text)
                        is ChatStreamEvent.Delta -> updateAssistantMessage(assistantId) { it.copy(content = it.content + event.text) }
                        is ChatStreamEvent.ReasoningDelta -> updateAssistantMessage(assistantId) { it.copy(reasoning = it.reasoning.orEmpty() + event.text) }
                        is ChatStreamEvent.Proposal -> {
                            updateAssistantMessage(assistantId) { it.copy(threadProposal = event.proposal) }
                            _uiState.value = _uiState.value.copy(activeProposal = event.proposal, activeProposalMessageId = assistantId)
                        }
                        is ChatStreamEvent.ArtifactCreated -> updateAssistantMessage(assistantId) { it.copy(artifact = event.artifact) }
                        is ChatStreamEvent.ActionProposal -> {
                            updateAssistantMessage(assistantId) { it.copy(stagedAction = event.action) }
                            _uiState.value = _uiState.value.copy(pendingAction = event.action)
                        }
                        is ChatStreamEvent.Complete -> {
                            updateAssistantMessage(assistantId) { it.copy(id = event.messageId ?: assistantId, content = event.text, isStreaming = false, turnStatus = "completed") }
                            _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null, error = null)
                        }
                        is ChatStreamEvent.Cancelled -> {
                            updateAssistantMessage(assistantId) { it.copy(isStreaming = false, turnStatus = "cancelled") }
                            _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null, error = "Response stopped. Partial text is saved.")
                        }
                        is ChatStreamEvent.Error -> {
                            updateAssistantMessage(assistantId) { it.copy(isStreaming = false, turnStatus = "failed") }
                            _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null, error = event.message)
                        }
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_uiState.value.currentSessionId == sessionId) {
                    updateAssistantMessage(assistantId) { it.copy(isStreaming = false, turnStatus = "interrupted") }
                    _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null, error = "Connection lost. Reload to recover the response: ${error.message}")
                }
            }
        }
    }

    fun stopResponse() {
        val repo = repository ?: return
        val sessionId = _uiState.value.currentSessionId
        val turnId = _uiState.value.messages.lastOrNull { it.role == "assistant" && it.isStreaming }?.turnId ?: return
        viewModelScope.launch {
            try {
                repo.cancelTurn(turnId)
                if (_uiState.value.currentSessionId == sessionId) {
                    streamJob?.cancel()
                    _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null)
                    loadMessages()
                }
            } catch (error: Exception) {
                if (_uiState.value.currentSessionId == sessionId) _uiState.value = _uiState.value.copy(error = error.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun respondAction(confirm: Boolean, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        val repo = repository ?: return
        val action = _uiState.value.pendingAction ?: return
        viewModelScope.launch {
            try {
                check(repo.respondAction(action.id, confirm)) { "Action response failed" }
                _uiState.value = _uiState.value.copy(pendingAction = null)
                onSuccess()
                loadMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
                onError()
            }
        }
    }

    fun respondProposal(accept: Boolean, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        if (_uiState.value.isRespondingToProposal || _uiState.value.isStreaming) return
        val repo = repository ?: return
        val msgId = _uiState.value.activeProposalMessageId ?: return
        val proposal = _uiState.value.activeProposal ?: return
        _uiState.value = _uiState.value.copy(isRespondingToProposal = true)
        viewModelScope.launch {
            try {
                val result = repo.respondProposal(msgId, accept)
                val threadId = result.thread?.id ?: result.proposal?.threadId
                if (accept) check(!threadId.isNullOrBlank()) { "Accepted proposal did not return a thread" }
                _uiState.value = _uiState.value.copy(activeProposal = null, activeProposalMessageId = null, isRespondingToProposal = false)
                onSuccess()
                if (accept) {
                    switchSession(checkNotNull(threadId), result.thread?.name ?: proposal.title)
                } else {
                    loadMessages()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isRespondingToProposal = false)
                onError()
            }
        }
    }

    private fun updateAssistantMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.map { if (it.id == id) transform(it) else it }
        )
    }
}
