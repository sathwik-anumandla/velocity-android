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
            pendingAction = null,
            activeProposal = null,
            activeProposalMessageId = null,
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

                val pendingAct = msgs.findLast { it.stagedAction != null && it.stagedAction.status == "pending" }?.stagedAction
                val pendingPropMsg = msgs.findLast { it.threadProposal != null && it.threadProposal.status == "pending" }

                _uiState.value = _uiState.value.copy(
                    messages = msgs,
                    pendingAction = pendingAct,
                    activeProposal = pendingPropMsg?.threadProposal,
                    activeProposalMessageId = pendingPropMsg?.id,
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
        model: String? = "gpt-5.4-mini",
        thinkingEffort: String = "medium",
        verbosity: String = "low"
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

        val cleanModel = when (model?.lowercase()?.trim()) {
            "gpt-5.4", "flagship", "gpt-5-full" -> "gpt-5.4"
            else -> "gpt-5.4-mini"
        }
        val cleanVerbosity = when (verbosity.lowercase().trim()) {
            "low", "medium", "high" -> verbosity.lowercase().trim()
            "concise" -> "low"
            "exhaustive" -> "high"
            else -> "low"
        }
        val cleanEffort = when (thinkingEffort.lowercase().trim()) {
            "none", "low", "medium", "high", "max" -> thinkingEffort.lowercase().trim()
            else -> "medium"
        }

        viewModelScope.launch {
            try {
                repo.streamTurn(
                    message = text,
                    sessionId = sessionId,
                    model = cleanModel,
                    thinkingEffort = cleanEffort,
                    verbosity = cleanVerbosity
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Status -> {
                            _uiState.value = _uiState.value.copy(streamingStatus = event.text)
                        }
                        is ChatStreamEvent.Delta -> {
                            updateAssistantMessage(asstMsgId) { it.copy(content = it.content + event.text) }
                        }
                        is ChatStreamEvent.Proposal -> {
                            _uiState.value = _uiState.value.copy(
                                activeProposal = event.proposal,
                                activeProposalMessageId = asstMsgId
                            )
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
                            val finalMsgId = event.messageId ?: asstMsgId
                            updateAssistantMessage(asstMsgId) {
                                it.copy(
                                    id = finalMsgId,
                                    content = if (event.text.isNotEmpty()) event.text else it.content,
                                    isStreaming = false
                                )
                            }
                            _uiState.value = _uiState.value.copy(
                                isStreaming = false,
                                streamingStatus = null,
                                activeProposalMessageId = if (_uiState.value.activeProposal != null) finalMsgId else _uiState.value.activeProposalMessageId
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
                updateAssistantMessage(asstMsgId) {
                    it.copy(content = it.content.ifEmpty { "[Connection Error]: ${e.message}" }, isStreaming = false)
                }
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    streamingStatus = null,
                    error = e.message
                )
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
                loadMessages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun respondProposal(accept: Boolean) {
        val repo = repository ?: return
        val msgId = _uiState.value.activeProposalMessageId ?: return
        viewModelScope.launch {
            try {
                repo.respondProposal(msgId, accept)
                _uiState.value = _uiState.value.copy(activeProposal = null, activeProposalMessageId = null)
                loadMessages()
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

