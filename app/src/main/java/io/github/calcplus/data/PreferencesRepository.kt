package io.github.calcplus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.calcplus.model.HapticStrength
import io.github.calcplus.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calcplus_prefs")

data class StoredSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val decryptedApiKey: String = "",
    val geminiModelId: String = ""
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAPTIC = stringPreferencesKey("haptic_strength")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val GEMINI_MODEL_ID = stringPreferencesKey("gemini_model_id")
    }

    val settingsFlow: Flow<StoredSettings> = context.dataStore.data.map { prefs ->
        val theme = prefs[Keys.THEME_MODE]?.let {
            runCatching { ThemeMode.valueOf(it) }.getOrNull()
        } ?: ThemeMode.SYSTEM

        val dynamic = prefs[Keys.DYNAMIC_COLOR] ?: true

        val haptic = prefs[Keys.HAPTIC]?.let {
            runCatching { HapticStrength.valueOf(it) }.getOrNull()
        } ?: HapticStrength.MEDIUM

        val encryptedKey = prefs[Keys.ENCRYPTED_API_KEY].orEmpty()
        val apiKey = CryptoManager.decrypt(encryptedKey)
        val modelId = prefs[Keys.GEMINI_MODEL_ID].orEmpty()

        StoredSettings(
            themeMode = theme,
            useDynamicColor = dynamic,
            hapticStrength = haptic,
            decryptedApiKey = apiKey,
            geminiModelId = modelId
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setHapticStrength(strength: HapticStrength) {
        context.dataStore.edit { it[Keys.HAPTIC] = strength.name }
    }

    suspend fun saveAiCredentials(plainApiKey: String, modelId: String) {
        val encrypted = CryptoManager.encrypt(plainApiKey.trim())
        context.dataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_API_KEY] = encrypted
            prefs[Keys.GEMINI_MODEL_ID] = modelId.trim()
        }
    }
}
