private fun handleIncomingText(text: String) {
        try {
            val lower = text.trim().lowercase(Locale.getDefault())

            val alarmHandled = tryHandleAlarmCommand(lower, text)
            if (alarmHandled) return

            val timerHandled = tryHandleTimerCommand(lower, text)
            if (timerHandled) return

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
}        textToSpeechHelper.speak(reply)
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
