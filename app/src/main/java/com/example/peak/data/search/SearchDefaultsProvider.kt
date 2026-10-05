package com.example.peak.data.search

import android.util.Log
import com.example.peak.domain.repository.MovieRepository

/**
 * Provides and caches global popular content for the empty search state.
 * Ensures we only fetch this once per app session.
 */
object SearchDefaultsProvider {
    private const val TAG = "SearchDefaults"
    private var cachedPopularTitles: List<String> = emptyList()

    /**
     * Returns a list of globally popular movie/TV titles.
     * Uses cache if available, otherwise triggers a fresh fetch.
     */
    suspend fun getPopularSuggestions(repository: MovieRepository): List<String> {
        if (cachedPopularTitles.isNotEmpty()) {
            Log.d(TAG, "Returning cached popular suggestions")
            return cachedPopularTitles
        }

        return try {
            Log.d(TAG, "Fetching fresh popular suggestions for empty state")
            val trending = repository.getTrendingMovies().getOrNull() ?: emptyList()
            cachedPopularTitles = trending
                .asSequence()
                .map { it.name }
                .distinct()
                .take(10)
                .toList()
            
            cachedPopularTitles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load default suggestions", e)
            emptyList()
        }
    }
}
