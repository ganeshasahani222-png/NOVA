package com.nova.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper around Android's built-in TextToSpeech engine.
 * No extra permissions or libraries needed.
 */
class TextToSpeechHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                selectFemaleVoice()
                tts?.setPitch(1.15f)
                tts?.setSpeechRate(1.0f)
                isReady = true
            }
        }
    }

    private fun selectFemaleVoice() {
        val engine = tts ?: return
        val voices = engine.voices ?: return

        val femaleVoice = voices.firstOrNull { voice ->
            voice.name.contains("female", ignoreCase = true) &&
                voice.locale.language == Locale.getDefault().language
        } ?: voices.firstOrNull { voice ->
            voice.name.contains("female", ignoreCase = true)
        }

        if (femaleVoice != null) {
            engine.voice = femaleVoice
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
