com.nova.assistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nova.assistant.core.NovaContainer

class ChatViewModelFactory(private val container: NovaContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java))
        return ChatViewModel(
            aiEngine = container.aiEngine,
            speechRecognitionController = container.speechRecognitionController,
            textToSpeechHelper = container.textToSpeechHelper,
            geminiTtsHelper = container.geminiTtsHelper,
            alarmHelper = container.alarmHelper,
            systemActionDispatcher = container.systemActionDispatcher
        ) as T
