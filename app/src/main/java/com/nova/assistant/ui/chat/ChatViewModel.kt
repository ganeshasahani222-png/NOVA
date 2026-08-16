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
import com.nova.assistant.intents.SystemActionDispatcher
import com.nova.assistant.voice.GeminiTtsHelper
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

class ChatViewModel(
    private val aiEngine: AiEngine,
    private val speechRecognitionController: SpeechRecognitionController,
    private val textToSpeechHelper: TextToSpeechHelper,
    private val geminiTtsHelper: GeminiTtsHelper,
    private val alarmHelper: AlarmHelper,
    private val systemActionDispatcher: SystemActionDispatcher
) : ViewModel() {

    var uiState by mutableStateOf(ChatUiState())
        private set

    private fun speak(text: String) {
        viewModelScope.launch {
            val success = geminiTtsHelper.speak(text)
            if (!success) {
                textToSpeechHelper.speak(text)
            }
        }
    }

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
        listenOnce(isFollowUpCommand = false)
    }

    private fun listenOnce(isFollowUpCommand: Boolean) {
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

                if (!isFollowUpCommand && lowerText.contains("nova")) {
                    speak("Ji sir, boliye. Kya kaam hai, kis kaam se yaad kiya?")
                    listenOnce(isFollowUpCommand = true)
                } else {
                    handleIncomingText(text)
                }
            }

            override fun onListeningEnded() {
                uiState = uiState.copy(isListening = false)
            }

            override fun onError(message: String) {
                uiState = uiState.copy(isListening = false)
                if (isFollowUpCommand) return
                appendMessage(ChatMessage(sender = Sender.SYSTEM, text = message, isError = true))
            }
        })
    }

    fun stopVoiceInput() {
        speechRecognitionController.stopListening()
        uiState = uiState.copy(isListening = false)
    }

    private fun handleIncomingText(text: String) {
        try {
            val lower = text.trim().lowercase(Locale.getDefault())

            if (tryHandleAlarmCommand(lower, text)) return
            if (tryHandleTimerCommand(lower, text)) return
            if (tryHandleSystemCommand(lower, text)) return

            sendMessage(text)
        } catch (e: Exception) {
            appendMessage(
                ChatMessage(
                    sender = Sender.SYSTEM,
                    text = "Error: ${e.message ?: e.toString()}",
                    isError = true
                )
            )
        }
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
            speak(reply)
            return true
        }
        return fallbackAlarm(originalText)
    }

    private fun fallbackAlarm(originalText: String): Boolean {
        appendMessage(ChatMessage(sender = Sender.USER, text = originalText))
        val reply = "Please tell me a specific time, like 'set alarm at 7:30 am'."
        appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
        speak(reply)
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
            speak(reply)
            return true
        }

        val success = alarmHelper.setTimer(totalSeconds, "Nova Timer")
        val reply = if (success) {
            "Timer started for $totalSeconds seconds."
        } else {
            "Sorry, I couldn't start the timer. No clock app found."
        }
        appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
        speak(reply)
        return true
    }

    private fun tryHandleSystemCommand(lower: String, originalText: String): Boolean {
        val hasVolumeWord = lower.contains("volume") || lower.contains("aawaz") || lower.contains("awaz")
        val hasIncreaseWord = lower.contains("up") || lower.contains("increase") || lower.contains("badha") || lower.contains("tez") || lower.contains("tej")
        val hasDecreaseWord = lower.contains("down") || lower.contains("decrease") || lower.contains("kam") || lower.contains("ghata")

        val reply: String? = when {
            hasVolumeWord && hasIncreaseWord -> {
                systemActionDispatcher.increaseVolume()
                "Volume increased."
            }
            hasVolumeWord && hasDecreaseWord -> {
                systemActionDispatcher.decreaseVolume()
                "Volume decreased."
            }
            lower.contains("mute") || lower.contains("chup") -> {
                systemActionDispatcher.muteVolume()
                "Muted."
            }
            lower.contains("brightness") || lower.contains("roshni") -> {
                systemActionDispatcher.openDisplaySettings()
                "Opening display settings — you can adjust brightness there."
            }
            lower.contains("wifi") || lower.contains("wi-fi") -> {
                systemActionDispatcher.openWifiSettings()
                "Opening WiFi settings."
            }
            lower.contains("bluetooth") -> {
                systemActionDispatcher.openBluetoothSettings()
                "Opening Bluetooth settings."
            }
            else -> null
        }

        if (reply == null) return false

        appendMessage(ChatMessage(sender = Sender.USER, text = originalText))
        appendMessage(ChatMessage(sender = Sender.NOVA, text = reply))
        speak(reply)
        return true
    }

    private fun sendMessage(text: String) {
        appendMessage(ChatMessage(sender = Sender.USER, text = text))
        uiState = uiState.copy(isWaitingForResponse = true)

        viewModelScope.launch {
            val history = uiState.messages.map { it.text }
            when (val result = aiEngine.generateResponse(text, history)) {
                is AiResult.Success -> {
                    appendMessage(ChatMessage(sender = Sender.NOVA, text = result.text))
                    speak(result.text)
                }
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
