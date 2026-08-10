package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.screens.MainScreen
import com.example.ui.theme.PulseChatTheme
import com.example.ui.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by mainViewModel.themeMode.collectAsState()
            PulseChatTheme(themeMode = themeMode) {
                MainScreen(viewModel = mainViewModel)
            }
        }
    }
}
