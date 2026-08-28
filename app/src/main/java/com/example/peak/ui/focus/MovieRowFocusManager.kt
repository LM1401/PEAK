package com.example.peak.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.focus.FocusRequester

/**
 * Registry for stable FocusRequesters across multiple MovieRows.
 * Enables deterministic vertical navigation by allowing cards to target 
 * specific FocusRequesters in adjacent rows.
 */
class MovieRowFocusManager {
    // Outer key: rowId, Inner key: cardIndex
    private val cardRequesters = mutableStateMapOf<String, MutableMap<Int, FocusRequester>>()

    /**
     * Returns a stable FocusRequester for a given row and card index.
     * Creates it if it doesn't exist.
     */
    fun getCardRequester(rowId: String, cardIndex: Int): FocusRequester {
        val rowMap = cardRequesters.getOrPut(rowId) { mutableMapOf() }
        return rowMap.getOrPut(cardIndex) { FocusRequester() }
    }
}

@Composable
fun rememberMovieRowFocusManager(): MovieRowFocusManager {
    return remember { MovieRowFocusManager() }
}
