package com.example.peak.data.continuewatching

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.ContinueWatchingItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Robust local storage for Continue Watching items.
 */
class ContinueWatchingStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val itemType = object : TypeToken<List<ContinueWatchingItem>>() {}.type

    companion object {
        private const val PREFS_NAME = "peak_continue_watching_prefs"
        private const val KEY_ITEMS = "continue_watching_items"
    }

    fun getAllItems(): List<ContinueWatchingItem> {
        return try {
            val json = prefs.getString(KEY_ITEMS, "[]") ?: "[]"
            gson.fromJson(json, itemType) ?: emptyList()
        } catch (e: Exception) {
            Log.e("CW_STORAGE", "getAllItems failed", e)
            emptyList()
        }
    }

    /**
     * Persists the entire list at once.
     * Use this after in-memory list mutation in the repository.
     */
    suspend fun saveAllItems(items: List<ContinueWatchingItem>) = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
        } catch (e: Exception) {
            Log.e("CW_STORAGE", "saveAllItems failed", e)
        }
    }

    suspend fun saveItem(item: ContinueWatchingItem): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        try {
            val items = getAllItems().toMutableList()
            items.removeAll { it.movieId == item.movieId && it.mediaType == item.mediaType }
            
            if (!item.isEffectivelyCompleted() && !item.completed) {
                items.add(0, item)
            }
            
            val limitedItems = items.take(20)
            saveAllItems(limitedItems)
            return@withContext limitedItems
        } catch (e: Exception) {
            getAllItems()
        }
    }

    suspend fun deleteItem(movieId: String, mediaType: MediaType): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        try {
            val items = getAllItems().toMutableList()
            if (items.removeAll { it.movieId == movieId && it.mediaType == mediaType }) {
                saveAllItems(items)
                return@withContext items
            }
            getAllItems()
        } catch (e: Exception) {
            getAllItems()
        }
    }
    
    suspend fun getItem(movieId: String, mediaType: MediaType): ContinueWatchingItem? = withContext(Dispatchers.IO) {
        getAllItems().find { it.movieId == movieId && it.mediaType == mediaType }
    }
}
