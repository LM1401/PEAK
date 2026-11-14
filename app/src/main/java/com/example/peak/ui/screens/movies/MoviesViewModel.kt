package com.example.peak.ui.screens.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.remote.recognition.BrandCatalog
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Movies screen (V2 Data & State Infrastructure).
 * Manages Hero Carousel, Collections, Trending Movies, Top 10, and Category Rows.
 */
class MoviesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    // Tracks API category rows as they arrive asynchronously
    private val _apiRowsMap = MutableStateFlow<Map<String, Row>>(emptyMap())

    val rows = _apiRowsMap.map { map ->
        val priorityOrder = listOf(
            "movies_trending", "movies_popular", "movies_now_playing", 
            "movies_top_rated", "movies_action", "movies_comedy", 
            "movies_scifi", "movies_horror", "movies_thriller"
        )
        priorityOrder.mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var focusDebounceJob: Job? = null
    private var heroEnrichJob: Job? = null

    init {
        repository.enrichedMediaFlow
            .onEach { enrichedMovie ->
                updateRowMovie(enrichedMovie)
            }
            .launchIn(viewModelScope)

        initializeCollections()
        fetchMovies()
        
        // SYNC INITIAL FOCUS: Set once when first rows arrive
        rows.filter { it.isNotEmpty() }.take(1).onEach { movieRows ->
            val initialRow = movieRows.firstOrNull()
            val initialMovie = initialRow?.movies?.firstOrNull()
            if (_focusState.value == null && initialMovie != null) {
                _focusState.value = FocusState(initialRow.id, initialMovie.movieId, initialMovie.mediaType, initialMovie)
            }
            _uiState.update { it.copy(loading = false) }
        }.launchIn(viewModelScope)
    }

    /**
     * Initializes the brand collection set specified by the target design using BrandCatalog.
     */
    private fun initializeCollections() {
        val disneyDef = BrandCatalog.brands.find { it.key == "disney" }
        val marvelDef = BrandCatalog.brands.find { it.key == "marvel" }
        val starWarsDef = BrandCatalog.brands.find { it.key == "lucasfilm" }
        val natGeoDef = BrandCatalog.brands.find { it.key == "nat_geo" }
        val universalDef = BrandCatalog.brands.find { it.key == "universal" }

        val targetCollections = listOf(
            CollectionItem(id = "disney", name = "Disney", brandKey = "disney", brandDefinition = disneyDef),
            CollectionItem(id = "pixar", name = "Pixar", brandKey = "pixar", brandDefinition = disneyDef),
            CollectionItem(id = "marvel", name = "Marvel", brandKey = "marvel", brandDefinition = marvelDef),
            CollectionItem(id = "star_wars", name = "Star Wars", brandKey = "lucasfilm", brandDefinition = starWarsDef),
            CollectionItem(id = "nat_geo", name = "National Geographic", brandKey = "nat_geo", brandDefinition = natGeoDef),
            CollectionItem(id = "universal", name = "Universal", brandKey = "universal", brandDefinition = universalDef)
        )

        _uiState.update { current ->
            current.copy(
                collectionsState = CollectionsSectionState(items = targetCollections, isLoading = false)
            )
        }
    }

    fun fetchMovies() {
        _uiState.update { current ->
            current.copy(
                loading = true,
                heroState = current.heroState.copy(isLoading = true),
                trendingState = current.trendingState.copy(isLoading = true),
                top10State = current.top10State.copy(isLoading = true)
            )
        }
        
        // 1. PRIMARY: Trending Movies (drives Hero, Trending Row, and initial focus)
        viewModelScope.launch {
            repository.getTrendingMovies().onSuccess { movies ->
                updateRow("movies_trending", "Trending Movies", movies)
                
                val featuredList = movies.take(7)
                _uiState.update { current ->
                    current.copy(
                        trendingState = SectionState(items = movies, isLoading = false),
                        heroState = HeroSectionState(
                            featuredMovies = featuredList,
                            selectedIndex = 0,
                            isLoading = false
                        )
                    )
                }

                if (featuredList.isNotEmpty()) {
                    val first = featuredList[0]
                    val cached = repository.getCachedMedia(first.movieId, first.mediaType)
                    if (cached != null && cached.isFullyEnriched) {
                        // Already enriched
                    } else {
                        enrichFeaturedMovie(first)
                    }
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        trendingState = SectionState(isLoading = false, error = error.message),
                        heroState = current.heroState.copy(isLoading = false, error = error.message)
                    )
                }
            }
        }

        // 2. SECONDARY: Popular Movies (drives Top 10 list with 1-based ranks)
        viewModelScope.launch {
            repository.getMovies(MovieListType.POPULAR).onSuccess { movies ->
                updateRow("movies_popular", "Popular Movies", movies)
                
                val top10Items = movies.take(10).mapIndexed { index, movie ->
                    RankedMovie(rank = index + 1, movie = movie)
                }

                _uiState.update { current ->
                    current.copy(
                        top10State = Top10SectionState(
                            title = "Top 10 in the UK Today",
                            items = top10Items,
                            isLoading = false
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        top10State = current.top10State.copy(isLoading = false, error = error.message)
                    )
                }
            }
        }

        // 3. TERTIARY: Status Categories (Now Playing, Top Rated)
        viewModelScope.launch {
            repository.getMovies(MovieListType.NOW_PLAYING).onSuccess { updateRow("movies_now_playing", "Now Playing", it) }
            repository.getMovies(MovieListType.TOP_RATED).onSuccess { updateRow("movies_top_rated", "Top Rated", it) }
        }

        // 4. QUATERNARY: Genre Rows (Action, Comedy, Sci-Fi, Horror, Thriller)
        viewModelScope.launch {
            repository.getMovies(MovieListType.GENRE, "28").onSuccess { updateRow("movies_action", "Action", it) }
            repository.getMovies(MovieListType.GENRE, "35").onSuccess { updateRow("movies_comedy", "Comedy", it) }
            repository.getMovies(MovieListType.GENRE, "878").onSuccess { updateRow("movies_scifi", "Sci-Fi & Fantasy", it) }
            repository.getMovies(MovieListType.GENRE, "27").onSuccess { updateRow("movies_horror", "Horror", it) }
            repository.getMovies(MovieListType.GENRE, "53").onSuccess { updateRow("movies_thriller", "Thriller", it) }
        }
    }

    /**
     * Hero Carousel Control: Select specific hero item by index.
     */
    fun selectHeroIndex(index: Int) {
        val featured = _uiState.value.heroState.featuredMovies
        if (featured.isEmpty() || index !in featured.indices) return

        _uiState.update { current ->
            current.copy(
                heroState = current.heroState.copy(selectedIndex = index)
            )
        }

        val targetMovie = featured[index]
        if (!targetMovie.isFullyEnriched) {
            enrichFeaturedMovie(targetMovie)
        }
    }

    /**
     * Hero Carousel Control: Advance to next hero item.
     */
    fun nextHero() {
        val size = _uiState.value.heroState.featuredMovies.size
        if (size > 1) {
            val nextIndex = (_uiState.value.heroState.selectedIndex + 1) % size
            selectHeroIndex(nextIndex)
        }
    }

    /**
     * Hero Carousel Control: Retreat to previous hero item.
     */
    fun previousHero() {
        val size = _uiState.value.heroState.featuredMovies.size
        if (size > 1) {
            val prevIndex = (_uiState.value.heroState.selectedIndex - 1 + size) % size
            selectHeroIndex(prevIndex)
        }
    }

    private fun enrichFeaturedMovie(movie: Movie) {
        heroEnrichJob?.cancel()
        heroEnrichJob = viewModelScope.launch {
            repository.getMediaById(movie.movieId, movie.mediaType, requireFullDetails = true)
        }
    }

    private fun updateRow(id: String, title: String, movies: List<Movie>) {
        if (movies.isEmpty()) return
        val cachedMovies = movies.take(20).map { m -> repository.getCachedMedia(m.movieId, m.mediaType) ?: m }
        val newRow = Row(id, title, cachedMovies)
        _apiRowsMap.update { current ->
            current + (id to newRow)
        }
        _uiState.update { current ->
            val updatedList = current.categoryRows.filterNot { it.id == id } + newRow
            current.copy(
                categoryRows = updatedList,
                rows = updatedList
            )
        }
    }

    private fun updateRowMovie(movie: Movie) {
        _apiRowsMap.update { currentMap ->
            currentMap.mapValues { (_, row) ->
                val updatedMovies = row.movies.map { m ->
                    if (m.movieId == movie.movieId && m.mediaType == movie.mediaType) {
                        repository.getCachedMedia(m.movieId, m.mediaType) ?: movie
                    } else m
                }
                row.copy(movies = updatedMovies)
            }
        }

        _uiState.update { current ->
            val currentFeatured = current.heroState.featuredMovies.toMutableList()
            var changed = false
            for (i in currentFeatured.indices) {
                if (currentFeatured[i].movieId == movie.movieId && currentFeatured[i].mediaType == movie.mediaType) {
                    currentFeatured[i] = repository.getCachedMedia(movie.movieId, movie.mediaType) ?: movie
                    changed = true
                }
            }

            val updatedCategoryRows = current.categoryRows.map { row ->
                val updatedMovies = row.movies.map { m ->
                    if (m.movieId == movie.movieId && m.mediaType == movie.mediaType) {
                        repository.getCachedMedia(m.movieId, m.mediaType) ?: movie
                    } else m
                }
                row.copy(movies = updatedMovies)
            }

            current.copy(
                heroState = if (changed) current.heroState.copy(featuredMovies = currentFeatured) else current.heroState,
                categoryRows = updatedCategoryRows,
                rows = updatedCategoryRows
            )
        }
    }

    fun onMovieFocused(rowId: String, movie: Movie) {
        val currentFocus = _focusState.value
        if (currentFocus != null && currentFocus.movieId == movie.movieId && currentFocus.mediaType == movie.mediaType && currentFocus.rowId == rowId && currentFocus.movie?.isFullyEnriched == true) return

        val repoCached = repository.getCachedMedia(movie.movieId, movie.mediaType)
        val activeMovie = repoCached ?: movie
        
        _focusState.value = FocusState(rowId, movie.movieId, movie.mediaType, activeMovie)

        if (!activeMovie.isFullyEnriched) {
            focusDebounceJob?.cancel()
            focusDebounceJob = viewModelScope.launch {
                if (!activeMovie.isEnriched) {
                    delay(300L)
                }
                repository.getMediaById(movie.movieId, movie.mediaType, requireFullDetails = true).onSuccess { fullMovie ->
                    if (_focusState.value?.movieId == movie.movieId && _focusState.value?.mediaType == movie.mediaType && _focusState.value?.rowId == rowId) {
                        _focusState.value = FocusState(rowId, movie.movieId, movie.mediaType, fullMovie)
                    }
                }
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
        _uiState.update { it.copy(selectedMovie = movie) }
    }
}

class MoviesViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoviesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoviesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
