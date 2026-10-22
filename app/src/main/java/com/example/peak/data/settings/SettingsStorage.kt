package com.example.peak.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Persistence layer for application settings using SharedPreferences.
 * Manages user configurations independently of content libraries.
 */
open class SettingsStorage(
    context: Context,
    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }
) {
    companion object {
        private const val PREFS_NAME = "peak_settings_prefs"
        private const val KEY_AUTO_PLAY = "pref_auto_play"
        private const val KEY_DEBUG_MODE = "pref_debug_mode"

        public const val DEFAULT_AUTO_PLAY = true
        public const val DEFAULT_DEBUG_MODE = false
    }

    open fun getAutoPlayEnabled(): Boolean {
        return try {
            prefs?.getBoolean(KEY_AUTO_PLAY, DEFAULT_AUTO_PLAY) ?: DEFAULT_AUTO_PLAY
        } catch (e: Exception) {
            Log.e("SETTINGS_STORAGE", "Failed to read auto_play preference", e)
            DEFAULT_AUTO_PLAY
        }
    }

    open fun setAutoPlayEnabled(enabled: Boolean) {
        try {
            prefs?.edit()?.putBoolean(KEY_AUTO_PLAY, enabled)?.apply()
        } catch (e: Exception) {
            Log.e("SETTINGS_STORAGE", "Failed to write auto_play preference", e)
        }
    }

    open fun getDebugModeEnabled(): Boolean {
        return try {
            prefs?.getBoolean(KEY_DEBUG_MODE, DEFAULT_DEBUG_MODE) ?: DEFAULT_DEBUG_MODE
        } catch (e: Exception) {
            Log.e("SETTINGS_STORAGE", "Failed to read debug_mode preference", e)
            DEFAULT_DEBUG_MODE
        }
    }

    open fun setDebugModeEnabled(enabled: Boolean) {
        try {
            prefs?.edit()?.putBoolean(KEY_DEBUG_MODE, enabled)?.apply()
        } catch (e: Exception) {
            Log.e("SETTINGS_STORAGE", "Failed to write debug_mode preference", e)
        }
    }
}
