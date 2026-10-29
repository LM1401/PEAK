package com.example.peak.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.TvListType
import com.example.peak.ui.image.HeroLogoTrace
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Optimized: Progressive content loading to ensure immediate UI responsiveness.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val continueWatchingProgress = _uiState.map { it.continueWatchingProgress }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Tracks categories that have arrived
    private val _apiRowsMap = MutableStateFlow<Map<String, Row>>(emptyMap())
    private var focusDebounceJob: Job? = null
    private var hasUserInteracted = false

    init {
        setupStatePipeline()
        fetchMovies()
    }

    private fun setupStatePipeline() {
        combine(
            continueWatchingRepository.continueWatchingItems,
            _apiRowsMap
        ) { cwItems, apiRowsMap ->
            val validCwItems = cwItems.filter { item ->
                item.movieId.isNotBlank() &&
                item.title.isNotBlank() &&
                item.progress > 0.01f &&
                item.progress < 0.95f &&
                !item.posterPath.isNullOrBlank() &&
                !item.movieId.startsWith("test_")
            }

            val progressMap = validCwItems.associateBy({ "${it.mediaType.name}_${it.movieId}" }, { it.progress })
            
            // Deterministic Row Ordering
            val combinedRows = buildList {
                if (validCwItems.isNotEmpty()) {
                    add(Row("continue_watching", "Continue Watching", validCwItems.map { it.toMovie() }))
                }
                
                // Add API rows in priority order if they exist
                val priorityOrder = listOf("trending", "popular_movies", "popular_series", "top_rated_movies")
                priorityOrder.forEach { id ->
                    apiRowsMap[id]?.let { add(it) }
                }
            }

            Pair(combinedRows, progressMap)
        }.onEach { (rows, progressMap) ->
            _uiState.update { current ->
                current.copy(
                    rows = rows,
                    continueWatchingProgress = progressMap,
                    loading = current.loading && rows.isEmpty()
                )
            }

            // Establish or update initial focus based on row priority
            val firstRealRow = rows.firstOrNull { !it.isPlaceholder }
            val firstMovie = firstRealRow?.movies?.firstOrNull { it.movieId.isNotBlank() }

            if (firstMovie != null) {
                val potentialFocus = FocusState(firstRealRow.id, firstMovie.movieId, firstMovie.mediaType, firstMovie)
                
                if (_focusState.value == null) {
                    // Initial establishment
                    _focusState.value = potentialFocus
                    if (!firstMovie.isEnriched) {
                        launchInitialHeroEnrichment(firstRealRow.id, firstMovie)
                    }
                } else if (!hasUserInteracted && _focusState.value?.rowId != potentialFocus.rowId) {
                    // Follow Row 0 if it changes (e.g. Continue Watching arrives) before user interaction
                    _focusState.value = potentialFocus
                    if (!firstMovie.isEnriched) {
                        launchInitialHeroEnrichment(firstRealRow.id, firstMovie)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun launchInitialHeroEnrichment(rowId: String, movie: Movie) {
        viewModelScope.launch {
            HeroLogoTrace.record(
                event = HeroLogoTrace.Event.INITIAL_HERO_DETAIL_STARTED,
                mediaType = movie.mediaType,
                mediaId = movie.movieId
            )
            val result = try {
                repository.getMediaById(movie.movieId, movie.mediaType)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.INITIAL_HERO_DETAIL_CANCELLED,
                    mediaType = movie.mediaType,
                    mediaId = movie.movieId,
                    outcome = HeroLogoTrace.Outcome.CANCELLED
                )
                throw cancellation
            } catch (exception: Exception) {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.INITIAL_HERO_DETAIL_FAILED,
                    mediaType = movie.mediaType,
                    mediaId = movie.movieId,
                    outcome = HeroLogoTrace.Outcome.FAILED
                )
                throw exception
            }

            result.onSuccess { enriched ->
                val hasLogo = !enriched.titleLogoUrl.isNullOrBlank()
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.INITIAL_HERO_DETAIL_SUCCEEDED,
                    mediaType = movie.mediaType,
                    mediaId = movie.movieId,
                    logoAvailable = hasLogo
                )
                if (_focusState.value?.movieId == movie.movieId && _focusState.value?.mediaType == movie.mediaType) {
                    _focusState.value = FocusState(rowId, movie.movieId, movie.mediaType, enriched)
                }
                updateRowMovie(rowId, enriched)
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.LOGO_URL_AVAILABLE,
                    mediaType = movie.mediaType,
                    mediaId = movie.movieId,
                    outcome = if (hasLogo) HeroLogoTrace.Outcome.AVAILABLE else HeroLogoTrace.Outcome.MISSING
                )
            }.onFailure {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.INITIAL_HERO_DETAIL_FAILED,
                    mediaType = movie.mediaType,
                    mediaId = movie.movieId,
                    outcome = HeroLogoTrace.Outcome.FAILED
                )
            }
        }
    }

    fun fetchMovies() {
        _uiState.update { it.copy(loading = true) }

        // Keep category requests independent so one unexpected failure cannot cancel the others.
        viewModelScope.launch {
            try {
                kotlinx.coroutines.supervisorScope {
                    val jobs = listOf(
                        launch {
                            loadCategory("trending", "Trending Now") {
                                repository.getTrendingMovies()
                            }
                        },
                        launch {
                            loadCategory("popular_movies", "Popular Movies") {
                                repository.getMovies(MovieListType.POPULAR)
                            }
                        },
                        launch {
                            loadCategory("popular_series", "Popular Series") {
                                repository.getSeries(TvListType.POPULAR)
                            }
                        },
                        launch {
                            loadCategory("top_rated_movies", "Top Rated Movies") {
                                repository.getMovies(MovieListType.TOP_RATED)
                            }
                        }
                    )
                    kotlinx.coroutines.joinAll(*jobs.toTypedArray())
                }
            } finally {
                // Also release the initial loading state if the parent request scope is cancelled.
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun loadCategory(
        id: String,
        title: String,
        request: suspend () -> Result<List<Movie>>
    ) {
        try {
            val result = request()
            val movies = result.getOrNull().orEmpty()
            val firstMovie = movies.firstOrNull()
            HeroLogoTrace.record(
                event = HeroLogoTrace.Event.CATEGORY_RESULT_RECEIVED,
                mediaType = firstMovie?.mediaType ?: if (id == "popular_series") MediaType.TV else MediaType.MOVIE,
                mediaId = firstMovie?.movieId,
                category = id,
                outcome = when {
                    result.isFailure -> HeroLogoTrace.Outcome.FAILED
                    movies.isEmpty() -> HeroLogoTrace.Outcome.EMPTY
                    else -> HeroLogoTrace.Outcome.CONTENT
                }
            )
            result.onSuccess { updateRow(id, title, it) }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            HeroLogoTrace.record(
                event = HeroLogoTrace.Event.CATEGORY_REQUEST_CANCELLED,
                mediaType = if (id == "popular_series") MediaType.TV else MediaType.MOVIE,
                category = id,
                outcome = HeroLogoTrace.Outcome.CANCELLED
            )
            throw cancellation
        } catch (exception: Exception) {
            HeroLogoTrace.record(
                event = HeroLogoTrace.Event.CATEGORY_REQUEST_FAILED,
                mediaType = if (id == "popular_series") MediaType.TV else MediaType.MOVIE,
                category = id,
                outcome = HeroLogoTrace.Outcome.FAILED
            )
            Log.e("HomeViewModel", "Failed to load home category $id", exception)
        }
    }

    private fun updateRow(id: String, title: String, movies: List<Movie>) {
        if (movies.isEmpty()) return
        _apiRowsMap.update { current ->
            current + (id to Row(id, title, movies.take(20)))
        }
    }

    private fun updateRowMovie(rowId: String, movie: Movie) {
        _apiRowsMap.update { currentMap ->
            currentMap.mapValues { (currentId, row) ->
                if (currentId == rowId) {
                    val updatedMovies = row.movies.map { m ->
                        if (m.movieId == movie.movieId && m.mediaType == movie.mediaType) movie else m
                    }
                    row.copy(movies = updatedMovies)
                } else {
                    row
                }
            }
        }
    }

    fun onUserNavigate() {
        hasUserInteracted = true
    }

    fun onMovieFocused(rowId: String, movieId: String, mediaType: MediaType) {
        Log.e("PEAK_DIAGNOSTIC", "onMovieFocused: rowId=$rowId, movieId=$movieId, mediaType=$mediaType")
        val currentFocus = _focusState.value
        if (currentFocus != null && currentFocus.movieId == movieId && currentFocus.mediaType == mediaType && currentFocus.rowId == rowId && currentFocus.movie?.isEnriched == true) {
            Log.e("PEAK_DIAGNOSTIC", "  Already focused and enriched. Ignoring.")
            return
        }

        // Instant cache hit: Bypass 300ms debounce if enriched media is already in repository cache
        val repoCached = repository.getCachedMedia(movieId, mediaType)
        if (repoCached != null) {
            focusDebounceJob?.cancel()
            _focusState.value = FocusState(rowId, movieId, mediaType, repoCached)
            updateRowMovie(rowId, repoCached)
            return
        }

        val cachedMovie = _uiState.value.rows.find { it.id == rowId }?.movies?.find { it.movieId == movieId && it.mediaType == mediaType }
        
        if (cachedMovie != null) {
            _focusState.value = FocusState(rowId, movieId, mediaType, cachedMovie)
            if (cachedMovie.isEnriched) return
        }

        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(300L)
            repository.getMediaById(movieId, mediaType).onSuccess { movie ->
                if (_focusState.value?.movieId == movieId && _focusState.value?.mediaType == mediaType && _focusState.value?.rowId == rowId) {
                    _focusState.value = FocusState(rowId, movieId, mediaType, movie)
                }
                updateRowMovie(rowId, movie)
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
        hasUserInteracted = true
        // Selection logic
    }
}

class HomeViewModelFactory(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, continueWatchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
