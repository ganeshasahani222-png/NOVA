package com.nova.assistant.voice

/**
 * Callback surface used by voice input implementations. Kept separate
 * from Android's SpeechRecognizer.RecognitionListener so UI code
 * depends on a small, app-specific contract rather than the platform
 * API directly — makes it easy to later swap in wake-word detection
 * (e.g. Porcupine) or an on-device model without touching call sites.
 */
interface VoiceListener {
    fun onListeningStarted()
    fun onPartialResult(text: String)
    fun onFinalResult(text: String)
    fun onListeningEnded()
    fun onError(message: String)
}
