package com.nova.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nova.assistant.ui.chat.ChatScreen
import com.nova.assistant.ui.chat.ChatViewModelFactory
import com.nova.assistant.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val container = (application as NovaApplication).container

        setContent {
            NovaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
