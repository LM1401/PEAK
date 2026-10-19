package com.example.peak.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.data.repository.WatchlistRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieRepository: MovieRepository,
    private val continueWatchingRepository: ContinueWatchingRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<Movie>>(emptyList())
    val similarMovies: StateFlow<List<Movie>> = _similarMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _resumePosition = MutableStateFlow<Long>(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _totalDuration = MutableStateFlow<Long>(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private val _isInWatchlist = MutableStateFlow<Boolean>(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist.asStateFlow()

    private var currentMediaId: String? = null
    private var currentMediaType: MediaType? = null

    init {
        observeContinueWatching()
        observeWatchlist()
    }

    private fun observeContinueWatching() {
        continueWatchingRepository.continueWatchingItems.onEach { items ->
            val id = currentMediaId ?: return@onEach
            val type = currentMediaType ?: return@onEach
            val item = items.find { it.movieId == id && it.mediaType == type }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L
        }.launchIn(viewModelScope)
    }

    private fun observeWatchlist() {
        watchlistRepository.watchlists.onEach { lists ->
            val id = currentMediaId ?: return@onEach
            val type = currentMediaType ?: return@onEach
            val defaultList = lists.find { it.isDefault }
            _isInWatchlist.value = defaultList?.items?.any { it.movieId == id && it.mediaType == type } == true
        }.launchIn(viewModelScope)
    }

    fun toggleWatchlist() {
        val movieObj = _movie.value ?: return
        viewModelScope.launch {
            val defaultList = watchlistRepository.watchlists.value.find { it.isDefault } ?: return@launch
            if (_isInWatchlist.value) {
                watchlistRepository.removeTitleFromWatchlist(defaultList.id, movieObj.movieId, movieObj.mediaType)
            } else {
                watchlistRepository.addTitleToWatchlist(defaultList.id, movieObj)
            }
        }
    }

    fun loadMedia(id: String, type: MediaType) {
        if (id.isBlank() || (currentMediaId == id && currentMediaType == type)) return
        
        this.currentMediaId = id
        this.currentMediaType = type
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            // Initial checks
            val item = continueWatchingRepository.continueWatchingItems.value.find { 
                it.movieId == id && it.mediaType == type 
            }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L

            val defaultList = watchlistRepository.watchlists.value.find { it.isDefault }
            _isInWatchlist.value = defaultList?.items?.any { it.movieId == id && it.mediaType == type } == true

            // Fetch full media details from repository
            movieRepository.getMediaById(id, type)
                .onSuccess { mediaDetails ->
                    _movie.value = mediaDetails
                    _isLoading.value = false
                    
                    // Fetch "similar" content
                    if (type == MediaType.MOVIE) {
                        movieRepository.getTrendingMovies().onSuccess { trending ->
                            _similarMovies.value = trending.filter { 
                                it.movieId != id || it.mediaType != type 
                            }.shuffled().take(12)
                        }
                    } else {
                        movieRepository.getTrendingSeries().onSuccess { trending ->
                            _similarMovies.value = trending.filter { 
                                it.movieId != id || it.mediaType != type 
                            }.shuffled().take(12)
                        }
                    }
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = "Failed to load media details"
                }
        }
    }
}

class DetailViewModelFactory(
    private val movieRepository: MovieRepository,
    private val continueWatchingRepository: ContinueWatchingRepository,
    private val watchlistRepository: WatchlistRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(movieRepository, continueWatchingRepository, watchlistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
