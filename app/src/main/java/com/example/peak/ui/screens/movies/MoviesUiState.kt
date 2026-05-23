package com.example.peak.ui.screens.movies

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row

/**
 * Single state holder for the Movies Screen.
 * Mirror of HomeUiState.
 */
data class MoviesUiState(
    val rows: List<Row> = emptyList(),
    val loading: Boolean = false,
    val selectedMovie: Movie? = null
)
