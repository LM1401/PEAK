package com.example.peak.data.search

import com.example.peak.ui.search.SearchItem

/**
 * Autocomplete-style suggestion engine.
 * Ranks items by prefix match priority and popularity.
 */
object SearchSuggestionEngine {

    fun generateSuggestions(items: List<SearchItem>, query: String): List<String> {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return items.asSequence()
            .map { item ->
                item to calculateAutocompleteScore(item.title.lowercase(), normalizedQuery, item.popularity)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first.title }
            .distinct()
            .take(6) // Only show top 6 suggestions for TV space
            .toList()
    }

    private fun calculateAutocompleteScore(title: String, query: String, popularity: Double): Double {
        var score = popularity * 0.05 // Base popularity boost

        when {
            title == query -> score += 1000.0
            title.startsWith(query) -> score += 500.0
            title.contains(" $query") -> score += 200.0
            title.contains(query) -> score += 50.0
            else -> return 0.0
        }

        return score
    }
}
