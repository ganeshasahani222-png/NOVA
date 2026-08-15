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
import com.nova.assistant.intents.AlarmHelper
import com.nova.assistant.voice.SpeechRecognitionController
import com.nova.assistant.voice.TextToSpeechHelper
import com.nova.assistant.voice.VoiceListener
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern

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
    private val speechRecognitionController: SpeechRecognitionController,
    private val textToSpeechHelper: TextToSpeechHelper,
    private val alarmHelper: AlarmHelper
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
        handleIncomingText(text)
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
                val lowerText = text.trim().lowercase()
                if (lowerText.contains("nova")) {
                    textToSpeechHelper.speak("Ji sir, boliye. Kya kaam hai, kis kaam se yaad kiya?")
                } else {
                    handleIncomingText(text)
                }
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

    /**
     * Routes incoming text (from typing or voice) to a built-in
     * command handler first (alarms, timers). Falls through to the
     * AI engine for everything else.
     */
    private fun handleIncomingText(text: String) {
        val lower = text.trim().lowercase(Locale.getDefault())

        val alarmHandled = tryHandleAlarmCommand(lower, text)
        if (alarmHandled) return

        val timerHandled = tryHandleTimerCommand(lower, text)
        if (timerHandled) return

        sendMessage(text)
    }

    private fun tryHandleAlarmCommand(lower: String, originalText: String): Boolean {
        if (!lower.contains("alarm")) return false

        val timePattern = Pattern.compile("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?")
        val matcher = timePattern.matcher(lower)

        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: return fallbackAlarm(originalText)
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val meridiem = matcher.group(3)

            if (meridiem == "pm" && hour < 12) hour += 12
            if (meridiem == "am" && hour == 12) hour = 0

            val success = alarmHelper.setAlarm(hour, minute, "Nova Alarm")
            val reply = if (success) {
                "Alarm set for ${String.format("%02d:%02d", hour, minute)}."
            } else {
                "Sorry, I couldn't set the alarm. No clock app found."
            }
            appendMessage(ChatMessage(sender = Sender.USER, text = originalText))
            appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
            textToSpeechHelper.speak(reply)
            return true
        }
        return fallbackAlarm(originalText)
    }

    private fun fallbackAlarm(originalText: String): Boolean {
        appendMessage(ChatMessage(sender = Sender.USER, text = originalText))
        val reply = "Please tell me a specific time, like 'set alarm at 7:30 am'."
        appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
        textToSpeechHelper.speak(reply)
        return true
    }

    private fun tryHandleTimerCommand(lower: String, originalText: String): Boolean {
        if (!lower.contains("timer")) return false

        val minutePattern = Pattern.compile("(\\d+)\\s*(minute|min)")
        val secondPattern = Pattern.compile("(\\d+)\\s*(second|sec)")

        val minuteMatcher = minutePattern.matcher(lower)
        val secondMatcher = secondPattern.matcher(lower)

        val totalSeconds = when {
            minuteMatcher.find() -> (minuteMatcher.group(1)?.toIntOrNull() ?: 0) * 60
            secondMatcher.find() -> secondMatcher.group(1)?.toIntOrNull() ?: 0
            else -> 0
        }

        appendMessage(ChatMessage(sender = Sender.USER, text = originalText))

        if (totalSeconds <= 0) {
            val reply = "Please tell me how long, like 'set a timer for 5 minutes'."
            appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
            textToSpeechHelper.speak(reply)
            return true
        }

        val success = alarmHelper.setTimer(totalSeconds, "Nova Timer")
        val reply = if (success) {
            "Timer started for $totalSeconds seconds."
        } else {
            "Sorry, I couldn't start the timer. No clock app found."
        }
        appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
        textToSpeechHelper.speak(reply)
        return true
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
