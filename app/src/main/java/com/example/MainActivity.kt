package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.HistoryScreen
import com.example.ui.HomeScreen
import com.example.ui.NudgeViewModel
import com.example.ui.theme.NudgeTheme
import com.example.ui.theme.PureBlack

enum class Screen {
    HOME,
    HISTORY
}

class MainActivity : ComponentActivity() {

    private val viewModel: NudgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialScreen = if (intent.getStringExtra("navigate_to") == "history") {
            Screen.HISTORY
        } else {
            Screen.HOME
        }

        setContent {
            NudgeTheme {
                var currentScreen by remember { mutableStateOf(initialScreen) }

                // Notification permission launcher for Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* Handled */ }
                    )

                    LaunchedEffect(Unit) {
                        val status = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (status != PackageManager.PERMISSION_GRANTED) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureBlack)
                ) {
                    when (currentScreen) {
                        Screen.HOME -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToHistory = {
                                    currentScreen = Screen.HISTORY
                                }
                            )
                        }

                        Screen.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = Screen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

