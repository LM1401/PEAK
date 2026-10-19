package com.example.peak.ui.screens.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.WatchlistRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            watchlistRepository.watchlists.collectLatest { lists ->
                _uiState.update { currentState ->
                    val defaultHeroMovie = currentState.focusedMovie
                        ?: lists.firstOrNull { it.items.isNotEmpty() }?.items?.firstOrNull()
                    currentState.copy(
                        watchlists = lists,
                        focusedMovie = defaultHeroMovie,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onMovieFocused(movie: Movie) {
        _uiState.update { it.copy(focusedMovie = movie) }
    }

    fun showCreateListDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateListDialog = show, error = null) }
    }

    fun showEditListDialog(watchlist: Watchlist?) {
        _uiState.update { it.copy(listToEdit = watchlist, error = null) }
    }

    fun createList(name: String) {
        viewModelScope.launch {
            val result = watchlistRepository.createWatchlist(name)
            result.onSuccess {
                _uiState.update { currentState -> currentState.copy(showCreateListDialog = false, error = null) }
            }.onFailure { ex ->
                _uiState.update { currentState -> currentState.copy(error = ex.message ?: "Failed to create list") }
            }
        }
    }

    fun renameList(listId: String, newName: String) {
        viewModelScope.launch {
            val result = watchlistRepository.renameWatchlist(listId, newName)
            result.onSuccess {
                _uiState.update { currentState -> currentState.copy(listToEdit = null, error = null) }
            }.onFailure { ex ->
                _uiState.update { currentState -> currentState.copy(error = ex.message ?: "Failed to rename list") }
            }
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            val result = watchlistRepository.deleteWatchlist(listId)
            result.onSuccess {
                _uiState.update { currentState -> currentState.copy(listToEdit = null, error = null) }
            }.onFailure { ex ->
                _uiState.update { currentState -> currentState.copy(error = ex.message ?: "Failed to delete list") }
            }
        }
    }

    fun removeTitleFromList(listId: String, movieId: String, mediaType: MediaType) {
        viewModelScope.launch {
            watchlistRepository.removeTitleFromWatchlist(listId, movieId, mediaType)
        }
    }

    fun randomPickAll() {
        val randomMovie = watchlistRepository.getRandomTitleAcrossAllLists()
        _uiState.update { 
            it.copy(
                randomPickResult = randomMovie,
                randomPickListName = "All Lists",
                error = if (randomMovie == null) "Your lists are currently empty. Add titles to use Random Pick." else null
            )
        }
    }

    fun randomPickFromList(listId: String, listName: String) {
        val randomMovie = watchlistRepository.getRandomTitleFromList(listId)
        _uiState.update { 
            it.copy(
                randomPickResult = randomMovie,
                randomPickListName = listName,
                error = if (randomMovie == null) "This list is currently empty. Add titles to use Random Pick." else null
            )
        }
    }

    fun clearRandomPickResult() {
        _uiState.update { it.copy(randomPickResult = null, randomPickListName = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

class WatchlistViewModelFactory(
    private val watchlistRepository: WatchlistRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WatchlistViewModel(watchlistRepository) as T
    }
}
