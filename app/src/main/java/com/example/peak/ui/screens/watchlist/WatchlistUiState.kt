package com.example.peak.ui.screens.watchlist

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist

/**
 * UI State for the Watchlist Screen.
 */
data class WatchlistUiState(
    val watchlists: List<Watchlist> = emptyList(),
    val focusedMovie: Movie? = null,
    val isLoading: Boolean = false,
    val showCreateListDialog: Boolean = false,
    val listToEdit: Watchlist? = null,
    val randomPickResult: Movie? = null,
    val randomPickListName: String? = null,
    val error: String? = null
)
