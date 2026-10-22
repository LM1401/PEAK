package com.example.peak.data.addon

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.peak.domain.model.Addon
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SharedPreferences storage layer for installed add-ons using Gson serialization.
 * Manages installed add-on transport URLs and configurations independently of content libraries.
 */
open class AddonStorage @JvmOverloads constructor(
    context: Context,
    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }
) {
    private val gson = Gson()
    private val addonListType = object : TypeToken<List<Addon>>() {}.type

    companion object {
        private const val PREFS_NAME = "peak_addon_prefs"
        private const val KEY_INSTALLED_ADDONS = "installed_addons"

        /**
         * Normalizes a transport URL for reliable duplicate detection.
         * Converts stremio:// to https:// and ensures manifest.json path termination.
         */
        fun normalizeAddonUrl(rawUrl: String): String {
            var trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return ""

            // Convert custom stremio:// scheme to https://
            if (trimmed.startsWith("stremio://", ignoreCase = true)) {
                trimmed = "https://" + trimmed.substring("stremio://".length)
            } else if (!trimmed.contains("://") && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed = "https://$trimmed"
            }

            // Ensure path ends with /manifest.json if missing (for http/https)
            if ((trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) &&
                !trimmed.endsWith("/manifest.json", ignoreCase = true)) {
                trimmed = if (trimmed.endsWith("/")) {
                    "${trimmed}manifest.json"
                } else {
                    "$trimmed/manifest.json"
                }
            }

            return trimmed
        }
    }

    open fun getAllAddons(): List<Addon> {
        return try {
            val json = prefs?.getString(KEY_INSTALLED_ADDONS, "[]") ?: "[]"
            gson.fromJson(json, addonListType) ?: emptyList()
        } catch (e: Exception) {
            Log.e("ADDON_STORAGE", "getAllAddons failed, returning empty list", e)
            emptyList()
        }
    }

    open suspend fun saveAllAddons(addons: List<Addon>) = withContext(Dispatchers.IO) {
        try {
            prefs?.edit()?.putString(KEY_INSTALLED_ADDONS, gson.toJson(addons))?.apply()
        } catch (e: Exception) {
            Log.e("ADDON_STORAGE", "saveAllAddons failed", e)
        }
    }

    open suspend fun addAddon(addon: Addon): List<Addon> = withContext(Dispatchers.IO) {
        try {
            val current = getAllAddons().toMutableList()
            val normalizedTarget = normalizeAddonUrl(addon.transportUrl)

            // Prevent duplicate installations of identical normalized transport URLs
            val isDuplicate = current.any { existing ->
                normalizeAddonUrl(existing.transportUrl) == normalizedTarget
            }

            if (isDuplicate) {
                return@withContext current
            }

            val normalizedAddon = addon.copy(transportUrl = normalizedTarget)
            current.add(normalizedAddon)
            saveAllAddons(current)
            return@withContext current
        } catch (e: Exception) {
            Log.e("ADDON_STORAGE", "addAddon failed", e)
            return@withContext getAllAddons()
        }
    }

    open suspend fun removeAddon(id: String): List<Addon> = withContext(Dispatchers.IO) {
        try {
            val current = getAllAddons().toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                saveAllAddons(current)
            }
            return@withContext current
        } catch (e: Exception) {
            Log.e("ADDON_STORAGE", "removeAddon failed", e)
            return@withContext getAllAddons()
        }
    }

    open fun isAddonInstalled(transportUrl: String): Boolean {
        val target = normalizeAddonUrl(transportUrl)
        return getAllAddons().any { existing ->
            normalizeAddonUrl(existing.transportUrl) == target
        }
    }
}
