package com.example.peak.data.continuewatching

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.peak.domain.model.ContinueWatchingItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Robust local storage for Continue Watching items.
 * Ensures write operations return the full updated list for reactivity.
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
            val items: List<ContinueWatchingItem> = gson.fromJson(json, itemType) ?: emptyList()
            Log.d("CW_DEBUG", "Storage getAllItems -> size=${items.size}")
            items
        } catch (e: Exception) {
            Log.e("CW_DEBUG", "Storage getAllItems failed", e)
            emptyList()
        }
    }

    suspend fun saveItem(item: ContinueWatchingItem): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        try {
            val items = getAllItems().toMutableList()
            items.removeAll { it.movieId == item.movieId }
            
            // Completion check (95%)
            if (!item.isEffectivelyCompleted() && !item.completed) {
                items.add(0, item)
            }
            
            val limitedItems = items.take(20)
            prefs.edit().putString(KEY_ITEMS, gson.toJson(limitedItems)).apply()
            return@withContext limitedItems
        } catch (e: Exception) {
            getAllItems()
        }
    }

    suspend fun deleteItem(movieId: String): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        try {
            val items = getAllItems().toMutableList()
            if (items.removeAll { it.movieId == movieId }) {
                prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
                return@withContext items
            }
            getAllItems()
        } catch (e: Exception) {
            getAllItems()
        }
    }
    
    suspend fun clearAll(): List<ContinueWatchingItem> = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_ITEMS).apply()
        emptyList()
    }

    suspend fun getItem(movieId: String): ContinueWatchingItem? = withContext(Dispatchers.IO) {
        getAllItems().find { it.movieId == movieId }
    }
}
