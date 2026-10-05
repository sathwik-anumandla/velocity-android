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
    val isLoadingOlder: Boolean = false,
    val hasMoreHistory: Boolean = false,
    val historyCursor: String? = null,
    val historyRevision: Long = 0,
    val isStreaming: Boolean = false,
    val streamingStatus: String? = null,
    val isBackendOnline: Boolean = true,
    val activeProposal: ThreadProposal? = null,
    val activeProposalMessageId: String? = null,
    val isRespondingToProposal: Boolean = false,
    val pendingAction: StagedAction? = null,
    val isRespondingToAction: Boolean = false,
    val error: String? = null
)

class TimelineViewModel : ViewModel() {

    private var repository: ChatRepository? = null
    private var streamJob: kotlinx.coroutines.Job? = null
    private var loadJob: kotlinx.coroutines.Job? = null
    private var cacheJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    fun initRepository(repo: ChatRepository) {
        this.repository = repo
        loadMessages()
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
            hasMoreHistory = false,
            historyCursor = null,
            isLoadingOlder = false,
            pendingAction = null,
            isRespondingToAction = false,
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
        loadJob?.cancel()
        streamJob?.cancel()
        cacheJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = true, isStreaming = false, streamingStatus = null)
        loadJob = viewModelScope.launch {
            try {
                repo.cachedHistory(sessionId)?.let { applyPage(it, sessionId, offline = true, loading = true) }
                val fresh = repo.fetchHistory(sessionId)
                val combined = repo.cachedHistory(sessionId) ?: fresh
                applyPage(combined, sessionId, offline = false, loading = false)
                if (_uiState.value.currentSessionId == sessionId) combined.messages.lastOrNull { it.role == "assistant" && it.turnStatus == "running" }?.let { resumeTurn(it, sessionId) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (_uiState.value.currentSessionId == sessionId) _uiState.value = _uiState.value.copy(isLoading = false, isBackendOnline = false, error = "Showing saved history. ${failure.message}")
            }
        }
    }

    private fun applyPage(page: com.velocity.app.data.model.MessagePage, sessionId: String, offline: Boolean, loading: Boolean) {
        if (_uiState.value.currentSessionId != sessionId) return
        val title = _uiState.value.sessionTitle
        repository?.let { repo -> viewModelScope.launch { repo.registerVisitedThread(sessionId, title) } }
        val proposalMessage = page.messages.findLast { it.threadProposal?.status == "pending" }
        _uiState.value = _uiState.value.copy(
            messages = page.messages, hasMoreHistory = page.hasMore, historyCursor = page.oldestCursor,
            historyRevision = page.historyRevision, isLoading = loading, isBackendOnline = !offline,
            pendingAction = page.messages.findLast { it.stagedAction?.status == "pending" }?.stagedAction,
            activeProposal = proposalMessage?.threadProposal, activeProposalMessageId = proposalMessage?.id,
            error = if (offline) "Saved history · connecting…" else page.messages.lastOrNull { it.role == "assistant" }?.turnStatus?.takeIf { it in setOf("cancelled", "failed", "interrupted") }?.let { "Response $it. Partial text is saved." }
        )
    }

    fun loadOlderMessages() {
        val repo = repository ?: return
        val state = _uiState.value
        val cursor = state.historyCursor ?: return
        if (state.isLoading || state.isLoadingOlder || !state.hasMoreHistory || !state.isBackendOnline) return
        _uiState.value = state.copy(isLoadingOlder = true)
        viewModelScope.launch {
            try {
                val page = repo.fetchHistory(state.currentSessionId, cursor)
                if (_uiState.value.currentSessionId != state.currentSessionId) return@launch
                if (page.historyRevision != state.historyRevision) { loadMessages(); return@launch }
                _uiState.value = _uiState.value.copy(messages = (page.messages + _uiState.value.messages).distinctBy { it.id }, hasMoreHistory = page.hasMore, historyCursor = page.oldestCursor, isLoadingOlder = false)
            } catch (failure: Exception) {
                if (_uiState.value.currentSessionId == state.currentSessionId) _uiState.value = _uiState.value.copy(isLoadingOlder = false, error = "Could not load older messages. Reload if history changed: ${failure.message}")
            }
        }
    }

    fun branchMessage(message: ChatMessage) {
        val repo = repository ?: return
        if (_uiState.value.isStreaming || !_uiState.value.isBackendOnline) return
        val sessionId = _uiState.value.currentSessionId
        viewModelScope.launch {
            try {
                val thread = repo.branchMessage(sessionId, message.id)
                if (_uiState.value.currentSessionId == sessionId) switchSession(thread.id, thread.name)
            } catch (failure: Exception) { _uiState.value = _uiState.value.copy(error = failure.message) }
        }
    }

    fun editAndSend(messageId: String, text: String, model: String, thinkingEffort: String, verbosity: String, recallBudget: String, regenerationContext: String? = null) {
        val repo = repository ?: return
        if (_uiState.value.isStreaming || _uiState.value.isLoading || !_uiState.value.isBackendOnline) return
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            try {
                repo.truncateHistory(state.currentSessionId, messageId)
                if (_uiState.value.currentSessionId != state.currentSessionId) return@launch
                val page = repo.fetchHistory(state.currentSessionId)
                applyPage(page, state.currentSessionId, offline = false, loading = false)
                sendMessage(text, model, thinkingEffort, verbosity, recallBudget, regenerationContext)
            } catch (failure: Exception) { if (_uiState.value.currentSessionId == state.currentSessionId) _uiState.value = _uiState.value.copy(isLoading = false, error = failure.message) }
        }
    }

    private fun persistHistory() {
        val repo = repository ?: return
        val state = _uiState.value
        cacheJob?.cancel()
        cacheJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            repo.cacheMessages(state.currentSessionId, state.messages.takeLast(2))
        }
    }

    fun sendMessage(
        text: String,
        model: String? = "gpt-5.4-mini",
        thinkingEffort: String = "medium",
        verbosity: String = "low",
        recallBudget: String = "medium",
        regenerationContext: String? = null
    ) {
        if (text.isBlank() || _uiState.value.isStreaming || _uiState.value.isLoading || !_uiState.value.isBackendOnline) return
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
        persistHistory()
        collectTurn(repo.streamTurn(text, sessionId, model, thinkingEffort, verbosity, turnId, recallBudget, regenerationContext), sessionId, assistantId)
    }

    private fun resumeTurn(message: ChatMessage, sessionId: String) {
        val repo = repository ?: return
        val turnId = message.turnId ?: return
        updateAssistantMessage(message.id) { it.copy(content = "", reasoning = null, isStreaming = true) }
        _uiState.value = _uiState.value.copy(isStreaming = true, streamingStatus = "Reconnecting…")
        collectTurn(repo.resumeTurn(turnId), sessionId, message.id)
    }

    private fun collectTurn(events: kotlinx.coroutines.flow.Flow<ChatStreamEvent>, sessionId: String, assistantId: String) {
        val repo = repository ?: return
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
                    persistHistory()
                }
                if (_uiState.value.currentSessionId == sessionId && !_uiState.value.isStreaming) {
                    cacheJob?.cancel()
                    try {
                        val fresh = repo.fetchHistory(sessionId)
                        applyPage(repo.cachedHistory(sessionId) ?: fresh, sessionId, offline = false, loading = false)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        persistHistory()
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_uiState.value.currentSessionId == sessionId) {
                    updateAssistantMessage(assistantId) { it.copy(isStreaming = false, turnStatus = "interrupted") }
                    _uiState.value = _uiState.value.copy(isStreaming = false, streamingStatus = null, error = "Connection lost. Reload to recover the response: ${error.message}")
                    persistHistory()
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
        if (_uiState.value.isRespondingToAction || _uiState.value.isStreaming || _uiState.value.isLoading || !_uiState.value.isBackendOnline) return
        val repo = repository ?: return
        val action = _uiState.value.pendingAction ?: return
        val sessionId = _uiState.value.currentSessionId
        _uiState.value = _uiState.value.copy(isRespondingToAction = true)
        viewModelScope.launch {
            try {
                check(repo.respondAction(action.id, confirm)) { "Action response failed" }
                if (_uiState.value.currentSessionId != sessionId) return@launch
                _uiState.value = _uiState.value.copy(pendingAction = null, isRespondingToAction = false)
                onSuccess()
                loadMessages()
            } catch (e: Exception) {
                if (_uiState.value.currentSessionId != sessionId) return@launch
                _uiState.value = _uiState.value.copy(error = e.message, isRespondingToAction = false)
                onError()
            }
        }
    }

    fun respondProposal(accept: Boolean, onSuccess: () -> Unit = {}, onError: () -> Unit = {}) {
        if (_uiState.value.isRespondingToProposal || _uiState.value.isStreaming || _uiState.value.isLoading || !_uiState.value.isBackendOnline) return
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
