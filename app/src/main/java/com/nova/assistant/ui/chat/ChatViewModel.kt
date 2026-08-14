package com.nova.assistant.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.assistant.ai.AiEngine
import com.nova.assistant.ai.AiResult
import com.nova.assistant.data.ChatMessage
import com.nova.assistant.data.Sender
import com.nova.assistant.voice.SpeechRecognitionController
import com.nova.assistant.voice.VoiceListener
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isListening: Boolean = false,
    val isWaitingForResponse: Boolean = false
)

/**
 * Owns chat state and coordinates the voice and AI modules. Neither
 * module knows about the other, or about Compose — this class is the
 * only place that wires them together, which keeps each module
 * independently testable/replaceable.
 */
class ChatViewModel(
    private val aiEngine: AiEngine,
    private val speechRecognitionController: SpeechRecognitionController
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    fun onInputTextChanged(text: String) {
        uiState = uiState.copy(inputText = text)
    }

    fun sendCurrentInput() {
        val text = uiState.inputText.trim()
        if (text.isEmpty()) return
        uiState = uiState.copy(inputText = "")
        sendMessage(text)
    }

    fun startVoiceInput() {
        speechRecognitionController.startListening(object : VoiceListener {
            override fun onListeningStarted() {
                uiState = uiState.copy(isListening = true)
            }

            override fun onPartialResult(text: String) {
                uiState = uiState.copy(inputText = text)
            }

            override fun onFinalResult(text: String) {
                uiState = uiState.copy(inputText = "")
                sendMessage(text)
            }

            override fun onListeningEnded() {
                uiState = uiState.copy(isListening = false)
            }

            override fun onError(message: String) {
                uiState = uiState.copy(isListening = false)
                appendMessage(ChatMessage(sender = Sender.SYSTEM, text = message, isError = true))
            }
        })
    }

    fun stopVoiceInput() {
        speechRecognitionController.stopListening()
        uiState = uiState.copy(isListening = false)
    }

    private fun sendMessage(text: String) {
        appendMessage(ChatMessage(sender = Sender.USER, text = text))
        uiState = uiState.copy(isWaitingForResponse = true)

        viewModelScope.launch {
            val history = uiState.messages.map { it.text }
            when (val result = aiEngine.generateResponse(text, history)) {
                is AiResult.Success -> appendMessage(ChatMessage(sender = Sender.NOVA, text = result.text))
                is AiResult.Failure -> appendMessage(
                    ChatMessage(sender = Sender.SYSTEM, text = result.message, isError = true)
                )
            }
            uiState = uiState.copy(isWaitingForResponse = false)
        }
    }

    private fun appendMessage(message: ChatMessage) {
        uiState = uiState.copy(messages = uiState.messages + message)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionController.stopListening()
    }
}
