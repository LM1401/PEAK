package com.example.peak.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.peak.domain.model.Movie

/**
 * UI-only focus manager for the Home screen.
 * Keeps track of the currently focused movie without involving the ViewModel.
 */
class HomeFocusManager {
    var focusedMovie: Movie? by mutableStateOf(null)
        private set

    fun onFocus(movie: Movie) {
        focusedMovie = movie
    }

    fun clear() {
        focusedMovie = null
    }
}
