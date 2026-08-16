package com.nova.assistant.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.nova.assistant.NovaApplication
import com.nova.assistant.R
import com.nova.assistant.ai.AiResult
import com.nova.assistant.intents.AlarmHelper
import com.nova.assistant.intents.SystemActionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class NovaListeningService : Service() {

    private lateinit var speechRecognitionController: SpeechRecognitionController
    private lateinit var geminiTtsHelper: GeminiTtsHelper
    private lateinit var alarmHelper: AlarmHelper
    private lateinit var systemActionDispatcher: SystemActionDispatcher
    private lateinit var container: com.nova.assistant.core.NovaContainer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = (application as NovaApplication).container
        speechRecognitionController = container.speechRecognitionController
        geminiTtsHelper = container.geminiTtsHelper
        alarmHelper = container.alarmHelper
        systemActionDispatcher = container.systemActionDispatcher

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        listenOnce(isFollowUpCommand = false)
    }

    private fun speak(text: String) {
        serviceScope.launch {
            geminiTtsHelper.speak(text)
        }
    }

    private fun listenOnce(isFollowUpCommand: Boolean) {
        speechRecognitionController.startListening(object : VoiceListener {
            override fun onListeningStarted() {}

            override fun onPartialResult(text: String) {}

            override fun onFinalResult(text: String) {
                val lowerText = text.trim().lowercase()

                if (!isFollowUpCommand && lowerText.contains("nova")) {
                    speak("Ji sir, boliye. Kya kaam hai, kis kaam se yaad kiya?")
                    listenOnce(isFollowUpCommand = true)
                } else if (isFollowUpCommand) {
                    handleCommand(text)
                    listenOnce(isFollowUpCommand = false)
                } else {
                    listenOnce(isFollowUpCommand = false)
                }
            }

            override fun onListeningEnded() {}

            override fun onError(message: String) {
                Handler(Looper.getMainLooper()).postDelayed({
                    listenOnce(isFollowUpCommand = false)
                }, 1200)
            }
        })
    }

    private fun handleCommand(text: String) {
        val lower = text.trim().lowercase()

        if (tryHandleAlarm(lower)) return
        if (tryHandleTimer(lower)) return
        if (tryHandleSystem(lower)) return

        serviceScope.launch {
            when (val result = container.aiEngine.generateResponse(text, emptyList())) {
                is AiResult.Success -> geminiTtsHelper.speak(result.text)
                is AiResult.Failure -> geminiTtsHelper.speak("Sorry, kuch problem hui.")
            }
        }
    }

    private fun tryHandleAlarm(lower: String): Boolean {
        if (!lower.contains("alarm")) return false

        val timePattern = Pattern.compile("(\\d{1,2})[:.]?(\\d{2})?\\s*(am|pm)?")
        val matcher = timePattern.matcher(lower)

        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: return false
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val meridiem = matcher.group(3)

            if (meridiem == "pm" && hour < 12) hour += 12
            if (meridiem == "am" && hour == 12) hour = 0

            val success = alarmHelper.setAlarm(hour, minute, "Nova Alarm")
            speak(if (success) "Alarm set." else "Sorry, alarm set nahi ho paya.")
            return true
        }
        return false
    }

    private fun tryHandleTimer(lower: String): Boolean {
        if (!lower.contains("timer")) return false

        val minutePattern = Pattern.compile("(\\d+)\\s*(minute|min)")
        val secondPattern = Pattern.compile("(\\d+)\\s*(second|sec)")

        val minuteMatcher = minutePattern.matcher(lower)
        val secondMatcher = secondPattern.matcher(lower)

        val totalSeconds = when {
            minuteMatcher.find() -> (minuteMatcher.group(1)?.toIntOrNull() ?: 0) * 60
            secondMatcher.find() -> secondMatcher.group(1)?.toIntOrNull() ?: 0
            else -> 0
        }

        if (totalSeconds <= 0) {
            speak("Kitni der ka timer chahiye, bataiye.")
            return true
        }

        val success = alarmHelper.setTimer(totalSeconds, "Nova Timer")
        speak(if (success) "Timer start ho gaya." else "Sorry, timer start nahi ho paya.")
        return true
    }

    private fun tryHandleSystem(lower: String): Boolean {
        val hasVolumeWord = lower.contains("volume") || lower.contains("aawaz") || lower.contains("awaz")
        val hasIncreaseWord = lower.contains("up") || lower.contains("increase") || lower.contains("badha") || lower.contains("tez") || lower.contains("tej")
        val hasDecreaseWord = lower.contains("down") || lower.contains("decrease") || lower.contains("kam") || lower.contains("ghata")

        when {
            hasVolumeWord && hasIncreaseWord -> {
                systemActionDispatcher.increaseVolume()
                speak("Volume badha diya.")
                return true
            }
            hasVolumeWord && hasDecreaseWord -> {
                systemActionDispatcher.decreaseVolume()
                speak("Volume kam kar diya.")
                return true
            }
            lower.contains("mute") || lower.contains("chup") -> {
                systemActionDispatcher.muteVolume()
                speak("Mute kar diya.")
                return true
            }
            lower.contains("wifi") || lower.contains("wi-fi") -> {
                systemActionDispatcher.openWifiSettings()
                speak("WiFi settings khol raha hoon.")
                return true
            }
            lower.contains("bluetooth") -> {
                systemActionDispatcher.openBluetoothSettings()
                speak("Bluetooth settings khol raha hoon.")
                return true
            }
        }
        return false
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
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "nova_listening_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
