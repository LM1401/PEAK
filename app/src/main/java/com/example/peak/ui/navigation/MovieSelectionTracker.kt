package com.example.peak.ui.navigation

import com.example.peak.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A simple singleton to hold the selected movie during navigation.
 * This satisfies the requirement of "Passing the full Movie object"
 * without complex JSON serialization in the NavHost.
 */
object MovieSelectionTracker {
    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    fun setSelectedMovie(movie: Movie) {
        _selectedMovie.value = movie
    }
}
