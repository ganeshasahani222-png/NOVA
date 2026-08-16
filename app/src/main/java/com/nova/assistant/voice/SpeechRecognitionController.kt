package com.nova.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechRecognitionController(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(listener: VoiceListener) {
        if (!isAvailable) {
            listener.onError("Speech recognition is not available on this device.")
            return
        }

        stopListening()

        val recognizerInstance = SpeechRecognizer.createSpeechRecognizer(context).also {
            recognizer = it
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Give the recognizer much more time before it decides
            // the user has stopped speaking, so longer sentences
            // aren't cut off mid-way.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000)
        }

        recognizerInstance.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = listener.onListeningStarted()

            override fun onPartialResults(partialResults: Bundle?) {
                extractBestText(partialResults)?.let(listener::onPartialResult)
            }

            override fun onResults(results: Bundle?) {
                val text = extractBestText(results)
                if (text != null) listener.onFinalResult(text)
                else listener.onError("No speech recognized.")
                listener.onListeningEnded()
            }

            override fun onError(error: Int) {
                listener.onError("Speech recognizer error code $error")
                listener.onListeningEnded()
            }

            override fun onEndOfSpeech() = listener.onListeningEnded()

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizerInstance.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    private fun extractBestText(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}
