package io.github.calcplus.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.calcplus.domain.SpeedPreset
import io.github.calcplus.domain.SvtDifficulty
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
    val geminiModelId: String = "",
    val highScores: Map<SpeedPreset, Int> = emptyMap(),
    val svtHighScores: Map<SvtDifficulty, Int> = emptyMap()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HAPTIC = stringPreferencesKey("haptic_strength")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val GEMINI_MODEL_ID = stringPreferencesKey("gemini_model_id")

        fun highScoreKey(preset: SpeedPreset) = intPreferencesKey("high_score_${preset.name}")
        fun svtHighScoreKey(difficulty: SvtDifficulty) = intPreferencesKey("high_score_svt_${difficulty.name}")
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

        val scores = SpeedPreset.entries.associateWith { preset ->
            prefs[Keys.highScoreKey(preset)] ?: 0
        }

        val svtScores = SvtDifficulty.entries.associateWith { diff ->
            prefs[Keys.svtHighScoreKey(diff)] ?: 0
        }

        StoredSettings(
            themeMode = theme,
            useDynamicColor = dynamic,
            hapticStrength = haptic,
            decryptedApiKey = apiKey,
            geminiModelId = modelId,
            highScores = scores,
            svtHighScores = svtScores
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

    suspend fun updateHighScoreIfBetter(preset: SpeedPreset, score: Int) {
        context.dataStore.edit { prefs ->
            val key = Keys.highScoreKey(preset)
            val current = prefs[key] ?: 0
            if (score > current) {
                prefs[key] = score
            }
        }
    }

    suspend fun updateSvtHighScoreIfBetter(difficulty: SvtDifficulty, score: Int) {
        context.dataStore.edit { prefs ->
            val key = Keys.svtHighScoreKey(difficulty)
            val current = prefs[key] ?: 0
            if (score > current) {
                prefs[key] = score
            }
        }
    }
}