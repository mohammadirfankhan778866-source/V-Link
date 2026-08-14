package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.chatDraftDataStore: DataStore<Preferences> by preferencesDataStore(name = "pulse_chat_drafts")

class ChatDraftDataStore(private val context: Context) {

    fun getDraftFlow(chatId: String): Flow<String> {
        val key = stringPreferencesKey("draft_$chatId")
        return context.chatDraftDataStore.data.map { preferences ->
            preferences[key] ?: ""
        }
    }

    suspend fun getDraft(chatId: String): String {
        val key = stringPreferencesKey("draft_$chatId")
        val preferences = context.chatDraftDataStore.data.first()
        return preferences[key] ?: ""
    }

    suspend fun saveDraft(chatId: String, draftText: String) {
        val key = stringPreferencesKey("draft_$chatId")
        context.chatDraftDataStore.edit { preferences ->
            if (draftText.isBlank()) {
                preferences.remove(key)
            } else {
                preferences[key] = draftText
            }
        }
    }

    suspend fun clearDraft(chatId: String) {
        val key = stringPreferencesKey("draft_$chatId")
        context.chatDraftDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
