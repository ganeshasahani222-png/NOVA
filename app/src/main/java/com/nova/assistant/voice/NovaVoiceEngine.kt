package com.nova.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class NovaVoiceEngine(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))

            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                setNaturalFemaleVoice()
            }
        }
    }

    private fun setNaturalFemaleVoice() {
        val voices = tts?.voices ?: return

        val femaleVoice = voices.find { voice ->
            (voice.locale.language == "hi" &&
                voice.name.contains("female", ignoreCase = true)) ||
                voice.name.contains("hi-in-x-hie-local", ignoreCase = true)
        }

        if (femaleVoice != null) {
            tts?.voice = femaleVoice
        }

        tts?.setPitch(1.05f)
        tts?.setSpeechRate(0.98f)
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NovaSpeechID")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
