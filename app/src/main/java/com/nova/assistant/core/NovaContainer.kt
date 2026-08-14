package com.nova.assistant.core

import android.content.Context
import com.nova.assistant.admin.DeviceAdminManager
import com.nova.assistant.ai.AiEngine
import com.nova.assistant.ai.StubAiEngine
import com.nova.assistant.intents.SystemActionDispatcher
import com.nova.assistant.voice.SpeechRecognitionController

/**
 * Minimal, dependency-injection-framework-free composition root.
 * Swap [aiEngine] for a RemoteAiEngine instance once you have a real
 * backend/API key configured — everything downstream (ChatViewModel,
 * UI) depends only on the AiEngine interface, so nothing else changes.
 *
 * If the project grows, this is the natural place to migrate to
 * Hilt/Koin without touching feature code.
 */
class NovaContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val aiEngine: AiEngine = StubAiEngine()

    val speechRecognitionController = SpeechRecognitionController(appContext)
    val textToSpeechHelper = TextToSpeechHelper(appContext)
    val systemActionDispatcher = SystemActionDispatcher(appContext)
    val deviceAdminManager = DeviceAdminManager(appContext)
}
