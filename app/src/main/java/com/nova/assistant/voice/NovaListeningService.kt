package com.nova.assistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nova.assistant.NovaApplication
import com.nova.assistant.R

/**
 * Foreground service that keeps the microphone listening even while
 * Nova is in the background. Android requires a visible, ongoing
 * notification for any background microphone use — this is a system
 * rule that cannot be bypassed, even by the app's own developer.
 */
class NovaListeningService : Service() {

    private lateinit var speechRecognitionController: SpeechRecognitionController
    private lateinit var textToSpeechHelper: TextToSpeechHelper

    override fun onCreate() {
        super.onCreate()
        val container = (application as NovaApplication).container
        speechRecognitionController = container.speechRecognitionController
        textToSpeechHelper = container.textToSpeechHelper

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startListeningLoop()
    }

    private fun startListeningLoop() {
        speechRecognitionController.startListening(object : VoiceListener {
            override fun onListeningStarted() {}

            override fun onPartialResult(text: String) {}

            override fun onFinalResult(text: String) {
                val lowerText = text.trim().lowercase()
                if (lowerText.contains("nova")) {
                    textToSpeechHelper.speak(
                        "Ji sir, boliye. Kya kaam hai, kis kaam se yaad kiya?"
                    )
                }
                // Restart listening for the next phrase
                startListeningLoop()
            }

            override fun onListeningEnded() {}

            override fun onError(message: String) {
                // Wait briefly before restarting to avoid a rapid on/off loop
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    listenOnce(isFollowUpCommand = false)
                }, 1200)
            }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova Listening",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Nova is listening for the wake word"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nova is listening")
            .setContentText("Say \"Nova\" to wake me up")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionController.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "nova_listening_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
