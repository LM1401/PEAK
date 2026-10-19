package com.example.peak.data.repository

import com.example.peak.data.watchlist.WatchlistStorage
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe repository managing user watchlists and title associations.
 */
class WatchlistRepository(
    private val storage: WatchlistStorage
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _watchlists = MutableStateFlow<List<Watchlist>>(emptyList())
    val watchlists: StateFlow<List<Watchlist>> = _watchlists.asStateFlow()

    init {
        scope.launch {
            val initial = storage.getAllWatchlists()
            _watchlists.value = initial
        }
    }

    suspend fun createWatchlist(name: String): Result<Watchlist> = mutex.withLock {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("List name cannot be blank"))
        }

        val current = _watchlists.value
        if (current.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("A list with this name already exists"))
        }

        val newList = Watchlist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            isDefault = false,
            items = emptyList()
        )

        val updated = current + newList
        _watchlists.value = updated
        storage.saveAllWatchlists(updated)
        return Result.success(newList)
    }

    suspend fun renameWatchlist(listId: String, newName: String): Result<Watchlist> = mutex.withLock {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("List name cannot be blank"))
        }

        val current = _watchlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == listId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Watchlist not found"))
        }

        val listToEdit = current[index]
        if (listToEdit.isDefault) {
            return Result.failure(IllegalStateException("Default Watchlist cannot be renamed"))
        }

        if (current.any { it.id != listId && it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("A list with this name already exists"))
        }

        val updatedList = listToEdit.copy(name = trimmed)
        current[index] = updatedList
        _watchlists.value = current
        storage.saveAllWatchlists(current)
        return Result.success(updatedList)
    }

    suspend fun deleteWatchlist(listId: String): Result<Boolean> = mutex.withLock {
        val current = _watchlists.value.toMutableList()
        val listToDelete = current.find { it.id == listId }
            ?: return Result.failure(IllegalArgumentException("Watchlist not found"))

        if (listToDelete.isDefault) {
            return Result.failure(IllegalStateException("The default 'My Watchlist' cannot be deleted."))
        }

        current.removeAll { it.id == listId }
        _watchlists.value = current
        storage.saveAllWatchlists(current)
        return Result.success(true)
    }

    suspend fun addTitleToWatchlist(listId: String = WatchlistStorage.DEFAULT_WATCHLIST_ID, movie: Movie): Result<Boolean> = mutex.withLock {
        val current = _watchlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == listId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Watchlist not found"))
        }

        val targetList = current[index]
        if (targetList.items.any { it.movieId == movie.movieId && it.mediaType == movie.mediaType }) {
            return Result.success(false) // Already present
        }

        val updatedItems = listOf(movie) + targetList.items
        current[index] = targetList.copy(items = updatedItems)
        _watchlists.value = current
        storage.saveAllWatchlists(current)
        return Result.success(true)
    }

    suspend fun removeTitleFromWatchlist(listId: String, movieId: String, mediaType: MediaType): Result<Boolean> = mutex.withLock {
        val current = _watchlists.value.toMutableList()
        val index = current.indexOfFirst { it.id == listId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Watchlist not found"))
        }

        val targetList = current[index]
        val updatedItems = targetList.items.filterNot { it.movieId == movieId && it.mediaType == mediaType }
        if (updatedItems.size == targetList.items.size) {
            return Result.success(false)
        }

        current[index] = targetList.copy(items = updatedItems)
        _watchlists.value = current
        storage.saveAllWatchlists(current)
        return Result.success(true)
    }

    fun isTitleInWatchlist(listId: String = WatchlistStorage.DEFAULT_WATCHLIST_ID, movieId: String, mediaType: MediaType): Boolean {
        val targetList = _watchlists.value.find { it.id == listId } ?: return false
        return targetList.items.any { it.movieId == movieId && it.mediaType == mediaType }
    }

    /**
     * Deduplicated random selection across ALL user watchlists.
     */
    fun getRandomTitleAcrossAllLists(): Movie? {
        val allMovies = _watchlists.value.flatMap { it.items }
            .distinctBy { "${it.mediaType.name}_${it.movieId}" }
        return if (allMovies.isNotEmpty()) allMovies.random() else null
    }

    /**
     * Random selection from an individual specified list.
     */
    fun getRandomTitleFromList(listId: String): Movie? {
        val targetList = _watchlists.value.find { it.id == listId } ?: return null
        return if (targetList.items.isNotEmpty()) targetList.items.random() else null
    }
}
