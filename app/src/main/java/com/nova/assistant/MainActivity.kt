package com.nova.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.assistant.ui.chat.ChatScreen
import com.nova.assistant.ui.chat.ChatViewModelFactory
import com.nova.assistant.ui.theme.NovaTheme
import com.nova.assistant.voice.NovaListeningService

class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)
    private var isServiceRunning by mutableStateOf(false)

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as NovaApplication).container

        setContent {
            NovaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = {
                                if (hasMicPermission) {
                                    if (isServiceRunning) {
                                        stopService(Intent(this@MainActivity, NovaListeningService::class.java))
                                        isServiceRunning = false
                                    } else {
                                        val intent = Intent(this@MainActivity, NovaListeningService::class.java)
                                        ContextCompat.startForegroundService(this@MainActivity, intent)
                                        isServiceRunning = true
                                    }
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(if (isServiceRunning) "Stop Background Listening" else "Start Background Listening")
                        }

                        val viewModel = viewModel<com.nova.assistant.ui.chat.ChatViewModel>(factory = ChatViewModelFactory(container))
                        ChatScreen(
                            viewModel = viewModel,
                            hasMicPermission = hasMicPermission,
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    }
                }
            }
        }
    }
}
