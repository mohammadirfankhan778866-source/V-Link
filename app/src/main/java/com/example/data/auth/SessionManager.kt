package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.UserEntity
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pulse_chat_session", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _jwtToken = MutableStateFlow<String?>(
        prefs.getString(KEY_JWT_TOKEN, null) ?: generateJwt("mohammadirfankhan778866@gmail.com")
    )
    val jwtToken: StateFlow<String?> = _jwtToken

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode

    fun performGoogleSignIn(
        email: String = "mohammadirfankhan778866@gmail.com",
        displayName: String = "Mohammad Irfan Khan",
        avatarUrl: String = "https://picsum.photos/seed/irfan/300/300"
    ): UserEntity {
        val userId = "usr_google_irfan_9075"
        val token = generateJwt(email)
        val username = "@" + email.substringBefore("@").lowercase()

        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, displayName)
            .apply()

        _jwtToken.value = token
        _isLoggedIn.value = true

        return UserEntity(
            id = userId,
            displayName = displayName,
            username = username,
            email = email,
            profilePictureUrl = avatarUrl,
            bio = "Hey there! I am using V-Link ⚡",
            onlineStatus = "ONLINE",
            lastSeenTimestamp = System.currentTimeMillis(),
            accountCreatedDate = "2026-01-15",
            isCurrentUser = true
        )
    }

    fun saveCustomUserSession(token: String, user: UserEntity) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_NAME, user.displayName)
            .apply()

        _jwtToken.value = token
        _isLoggedIn.value = true
    }

    fun updateUserName(displayName: String) {
        prefs.edit().putString(KEY_USER_NAME, displayName).apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_JWT_TOKEN)
            .apply()
        _isLoggedIn.value = false
        _jwtToken.value = null
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun generateJwt(email: String): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val payload = "eyJzdWIiOiIke2VtYWlsfSIsImF1ZCI6InB1bHNlY2hhdF9hcHAiLCJpYXQiOjE3NDIwMDAwMDB9"
        val signature = UUID.randomUUID().toString().replace("-", "").take(16)
        return "$header.$payload.$signature"
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
