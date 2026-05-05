package com.example.pillmate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.data.local.dao.ProfileDao
import com.example.pillmate.data.local.entity.ChatMessageEntity
import com.example.pillmate.data.local.entity.ChatSessionEntity
import com.example.pillmate.data.repository.AIChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AIChatUiState(
    val profileId: String = "",
    val currentSessionId: String = "",
    val inputText: String = "",
    val isThinking: Boolean = false,
    val error: String? = null
)

class AIChatViewModel(
    private val repository: AIChatRepository,
    private val profileDao: ProfileDao,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(AIChatUiState())
    val uiState: StateFlow<AIChatUiState> = _uiState.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = _uiState
        .flatMapLatest { state ->
            if (state.profileId.isBlank()) flowOf(emptyList()) else repository.getSessions(state.profileId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<ChatMessageEntity>> = _uiState
        .flatMapLatest { state ->
            if (state.profileId.isBlank() || state.currentSessionId.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.getMessages(state.profileId, state.currentSessionId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val profileId = resolveProfileId()
            if (profileId.isBlank()) {
                _uiState.update { it.copy(error = "No active profile found.") }
                return@launch
            }

            _uiState.update { it.copy(profileId = profileId) }
            runCatching { repository.refreshFromRemote(profileId) }

            val sessionId = repository.ensureSession(profileId)
            _uiState.update { it.copy(currentSessionId = sessionId) }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val state = _uiState.value
            val profileId = state.profileId
            val currentSessionId = state.currentSessionId
            if (profileId.isBlank() || currentSessionId.isBlank()) return@launch

            val currentMessageCount = repository.getMessageCount(currentSessionId)
            val reusableBlank = repository.findAnotherEmptySession(profileId, currentSessionId)

            val targetSessionId = when {
                currentMessageCount == 0 && reusableBlank != null -> reusableBlank.id
                currentMessageCount == 0 -> currentSessionId
                reusableBlank != null -> reusableBlank.id
                else -> repository.createSession(profileId, "New Chat")
            }

            _uiState.update { it.copy(currentSessionId = targetSessionId, error = null) }
        }
    }

    fun selectSession(sessionId: String) {
        _uiState.update { it.copy(currentSessionId = sessionId) }
    }

    fun sendMessage() {
        viewModelScope.launch {
            val state = _uiState.value
            val profileId = state.profileId
            val sessionId = state.currentSessionId
            val textToSend = state.inputText.trim()

            if (profileId.isBlank() || sessionId.isBlank() || textToSend.isBlank() || state.isThinking) return@launch

            _uiState.update { it.copy(isThinking = true, inputText = "", error = null) }
            val now = System.currentTimeMillis()
            repository.addMessage(
                profileId = profileId,
                sessionId = sessionId,
                text = textToSend,
                isBot = false,
                createdAt = now
            )

            if (messages.value.count { !it.isBot } <= 1) {
                repository.updateSessionTitle(profileId, sessionId, textToSend.take(60))
            }

            runCatching { repository.askAssistant(textToSend) }
                .onSuccess { reply ->
                    repository.addMessage(
                        profileId = profileId,
                        sessionId = sessionId,
                        text = reply,
                        isBot = true
                    )
                    _uiState.update { it.copy(isThinking = false) }
                }
                .onFailure { err ->
                    repository.addMessage(
                        profileId = profileId,
                        sessionId = sessionId,
                        text = "Sorry, I encountered an error: ${err.message}",
                        isBot = true
                    )
                    _uiState.update { it.copy(isThinking = false, error = err.message) }
                }
        }
    }

    fun deleteChat(sessionId: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val profileId = state.profileId
            if (profileId.isBlank()) return@launch

            repository.deleteSession(profileId, sessionId)
            val latest = repository.ensureSession(profileId)
            _uiState.update { it.copy(currentSessionId = latest) }
        }
    }

    private suspend fun resolveProfileId(): String {
        val active = profileDao.getActiveProfile()?.id?.takeIf { it.isNotBlank() }
        if (active != null) return active
        return firebaseAuth.currentUser?.uid ?: ""
    }
}
