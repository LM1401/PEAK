package com.example.peak.ui.screens.home

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row

/**
 * Single state holder for the Home Screen.
 * Consolidates rows, loading status, and selection.
 */
data class HomeUiState(
    val rows: List<Row> = emptyList(),
    val loading: Boolean = false,
    val selectedMovie: Movie? = null,
    val continueWatchingProgress: Map<String, Float> = emptyMap()
)
