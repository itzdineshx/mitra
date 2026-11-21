package com.mitra.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.mitraDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val language: String = "en-IN",
    val wakeWordEnabled: Boolean = false,
    val speakReplies: Boolean = true,
    val wifiOnly: Boolean = true,
    val cloudHelpEnabled: Boolean = false,
    val firstRunCompleted: Boolean = false,
    val noticeText: String? = null,
)

class SettingsStore(
    private val context: Context,
) {
    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val KEY_SPEAK_REPLIES = booleanPreferencesKey("speak_replies")
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_CLOUD_HELP = booleanPreferencesKey("cloud_help_enabled")
        val KEY_FIRST_RUN = booleanPreferencesKey("first_run_completed")
        val KEY_NOTICE = stringPreferencesKey("notice_text")
    }

    val settings: Flow<Settings> =
        context.mitraDataStore.data.map { prefs ->
            Settings(
                language = prefs[KEY_LANGUAGE] ?: "en-IN",
                wakeWordEnabled = prefs[KEY_WAKE_WORD] ?: false,
                speakReplies = prefs[KEY_SPEAK_REPLIES] ?: true,
                wifiOnly = prefs[KEY_WIFI_ONLY] ?: true,
                cloudHelpEnabled = prefs[KEY_CLOUD_HELP] ?: false,
                firstRunCompleted = prefs[KEY_FIRST_RUN] ?: false,
                noticeText = prefs[KEY_NOTICE],
            )
        }

    suspend fun setLanguage(lang: String) {
        context.mitraDataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.mitraDataStore.edit { it[KEY_WAKE_WORD] = enabled }
    }

    suspend fun setSpeakReplies(enabled: Boolean) {
        context.mitraDataStore.edit { it[KEY_SPEAK_REPLIES] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.mitraDataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setCloudHelpEnabled(enabled: Boolean) {
        context.mitraDataStore.edit { it[KEY_CLOUD_HELP] = enabled }
    }

    suspend fun setFirstRunCompleted(completed: Boolean) {
        context.mitraDataStore.edit { it[KEY_FIRST_RUN] = completed }
    }

    suspend fun setNotice(notice: String?) {
        context.mitraDataStore.edit {
            if (notice != null) it[KEY_NOTICE] = notice else it.remove(KEY_NOTICE)
        }
    }

    suspend fun clear() {
        context.mitraDataStore.edit { it.clear() }
    }
}
