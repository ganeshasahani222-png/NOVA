package com.nova.assistant.voice

/**
 * Placeholder contract for future always-on wake-word detection
 * (e.g. "Hey Nova"), backed by a library such as Porcupine or Vosk,
 * or a custom on-device model.
 *
 * Not implemented yet — deliberately left as an interface so the
 * rest of the app can be wired against it ahead of time. A real
 * implementation should run as a foreground Service (Android
 * requires a visible notification for any persistent microphone use)
 * and call [SpeechRecognitionController.startListening] once the wake
 * word fires.
 */
interface WakeWordDetector {
    fun start(onWakeWordDetected: () -> Unit)
    fun stop()
}
