package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.auth.SessionManager
import com.example.data.db.PulseDatabase
import com.example.data.network.PulseWebSocketService
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PulseApplication : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var database: PulseDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var webSocketService: PulseWebSocketService
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var authRepository: com.example.data.repository.AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            com.example.util.FirebaseDiagnostics.runDiagnostics(this)
        } catch (e: Exception) {
            android.util.Log.e("PulseApplication", "Failed to initialize FirebaseApp: ${e.message}")
        }
        instance = this

        database = PulseDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        webSocketService = PulseWebSocketService(database)
        chatRepository = ChatRepository(database, webSocketService)
        authRepository = com.example.data.repository.AuthRepository(this)

        createNotificationChannels()

        applicationScope.launch {
            chatRepository.populateSeedDataIfEmpty()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Pulse Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time message notifications from Pulse Chat"
                enableVibration(true)
            }

            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Pulse Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video call alerts"
                enableVibration(true)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(callsChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "pulse_messages_channel"
        const val CHANNEL_CALLS = "pulse_calls_channel"

        lateinit var instance: PulseApplication
            private set
    }
}
