package com.nova.assistant.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.nova.assistant.R
import com.nova.assistant.ui.chat.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaListeningService : Service() {

    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListeningForWakeWord = true
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "nova_listening_channel"
        const val NOTIFICATION_ID = 101
        const val WAKE_WORD = "nova"
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        startWakeWordListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startForegroundServiceWithNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Nova Background Listening", NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nova is listening")
            .setContentText("Bolo 'Nova' aur apna command do")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun freshIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

    private fun startWakeWordListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val delay = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500L else 600L
                handler.postDelayed({ restartListening() }, delay)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""

                if (isListeningForWakeWord) {
                    if (text.lowercase().contains(WAKE_WORD)) {
                        onWakeWordDetected()
                    } else {
                        restartListening()
                    }
                } else {
                    isListeningForWakeWord = true
                    handleSpokenCommand(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(freshIntent())
    }

    private fun restartListening() {
        handler.post {
            speechRecognizer?.cancel()
            speechRecognizer?.startListening(freshIntent())
        }
    }

    private fun onWakeWordDetected() {
        isListeningForWakeWord = false
        serviceScope.launch {
            GeminiTtsHelper(applicationContext).speak("Ji sir, boliye")
        }
        handler.postDelayed({ restartListening() }, 1800L)
    }

    private fun handleSpokenCommand(command: String) {
        if (command.isNotBlank()) {
            val vm = ChatViewModel.activeInstance
            if (vm != null) {
                vm.onInputTextChanged(command)
                vm.sendCurrentInput()
            } else {
                serviceScope.launch {
                    GeminiTtsHelper(applicationContext)
                        .speak("Pehle Nova app ko ek baar khol lo, phir background se command chalega")
                }
            }
        }
        restartListening()
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
