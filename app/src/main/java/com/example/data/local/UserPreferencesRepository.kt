package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AssistantTone
import com.example.domain.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jarvis_preferences"
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ASSISTANT_TONE = stringPreferencesKey("assistant_tone")
        val VOICE_TTS_ENABLED = booleanPreferencesKey("voice_tts_enabled")
        val AUTO_REMEMBER_ENABLED = booleanPreferencesKey("auto_remember_enabled")
        val INJECT_MEMORY_CONTEXT = booleanPreferencesKey("inject_memory_context")
        val DIRECT_CALL_PREFERRED = booleanPreferencesKey("direct_call_preferred")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val preferencesFlow: Flow<UserPreferences> = context.jarvisPrefsDataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME].orEmpty(),
            assistantTone = AssistantTone.fromString(prefs[Keys.ASSISTANT_TONE].orEmpty()),
            voiceTtsEnabled = prefs[Keys.VOICE_TTS_ENABLED] ?: false,
            autoRememberEnabled = prefs[Keys.AUTO_REMEMBER_ENABLED] ?: true,
            injectMemoryContext = prefs[Keys.INJECT_MEMORY_CONTEXT] ?: true,
            directCallPreferred = prefs[Keys.DIRECT_CALL_PREFERRED] ?: true,
            darkTheme = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun setUserName(name: String) {
        context.jarvisPrefsDataStore.edit { it[Keys.USER_NAME] = name.trim() }
    }

    suspend fun setAssistantTone(tone: AssistantTone) {
        context.jarvisPrefsDataStore.edit { it[Keys.ASSISTANT_TONE] = tone.name }
    }

    suspend fun setVoiceTtsEnabled(enabled: Boolean) {
        context.jarvisPrefsDataStore.edit { it[Keys.VOICE_TTS_ENABLED] = enabled }
    }

    suspend fun setAutoRememberEnabled(enabled: Boolean) {
        context.jarvisPrefsDataStore.edit { it[Keys.AUTO_REMEMBER_ENABLED] = enabled }
    }

    suspend fun setInjectMemoryContext(enabled: Boolean) {
        context.jarvisPrefsDataStore.edit { it[Keys.INJECT_MEMORY_CONTEXT] = enabled }
    }

    suspend fun setDirectCallPreferred(enabled: Boolean) {
        context.jarvisPrefsDataStore.edit { it[Keys.DIRECT_CALL_PREFERRED] = enabled }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.jarvisPrefsDataStore.edit { it[Keys.DARK_THEME] = enabled }
    }
}
