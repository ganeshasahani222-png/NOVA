package com.nova.assistant.core

import android.content.Context
import com.nova.assistant.admin.DeviceAdminManager
import com.nova.assistant.ai.AiEngine
import com.nova.assistant.ai.RemoteAiEngine
import com.nova.assistant.intents.AlarmHelper
import com.nova.assistant.intents.SystemActionDispatcher
import com.nova.assistant.voice.SpeechRecognitionController
import com.nova.assistant.voice.TextToSpeechHelper

class NovaContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val aiEngine: AiEngine = RemoteAiEngine()

    val speechRecognitionController = SpeechRecognitionController(appContext)
    val textToSpeechHelper = TextToSpeechHelper(appContext)
    val systemActionDispatcher = SystemActionDispatcher(appContext)
    val deviceAdminManager = DeviceAdminManager(appContext)
    val alarmHelper = AlarmHelper(appContext)
}
