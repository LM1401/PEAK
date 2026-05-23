package com.example.peak.ui.screens.series

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row

/**
 * Single state holder for the Series Screen.
 * Mirror of HomeUiState.
 */
data class SeriesUiState(
    val rows: List<Row> = emptyList(),
    val loading: Boolean = false,
    val selectedMovie: Movie? = null
)
