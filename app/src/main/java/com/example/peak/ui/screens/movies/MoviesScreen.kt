package com.example.peak.ui.screens.movies

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.focus.rememberMovieRowFocusManager
import com.example.peak.ui.screens.home.HomeConstants
import com.example.peak.ui.screens.movies.components.CollectionsRow
import com.example.peak.ui.screens.movies.components.MovieLandscapeRow
import com.example.peak.ui.screens.movies.components.Top10Row
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Ordered sequence of rendered sections on the Movies Screen.
 */
private val MOVIES_SECTION_ORDER = listOf(
    "movies_hero",
    "movies_collections",
    "movies_trending",
    "movies_top10",
    "movies_now_playing",
    "movies_top_rated",
    "movies_action",
    "movies_comedy",
    "movies_scifi",
    "movies_horror",
    "movies_thriller"
)

/**
 * Movies Screen (Phase 6 Final Target Rebuild & Row-Level Section Framing Architecture).
 * Complete Movies destination utilizing the target PEAK Movies visual system:
 * MoviesHeroSection, CollectionsRow, 16:9 MovieLandscapeRow, and Top10Row,
 * supported by section bounds viewport framing, clamped camera translation,
 * deterministic focus memory, and 1-10 D-pad accessibility.
 */
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusStateFlow = viewModel.focusState
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val density = LocalDensity.current

    // Movies Focus Requesters & Memory Infrastructure
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val moreInfoFocusRequester = remember { FocusRequester() }

    val focusMemoryManager = rememberFocusMemoryManager()
    val focusRegistry = rememberMovieRowFocusManager()

    // Active focused section ID
    var currentFocusSection by remember { mutableStateOf<String?>("movies_hero") }

    // Dynamic Un-transformed Section Measurement & Viewport Geometry State
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }
    var columnHeightPx by remember { mutableFloatStateOf(0f) }
    val sectionYPositions = remember { mutableStateMapOf<String, Float>() }
    val sectionHeights = remember { mutableStateMapOf<String, Float>() }

    // Movies Camera Translation
    val cameraOffsetY = remember { Animatable(0f) }

    // Focus Restoration & Initial Focus Logic
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitialFocusRequested by rememberSaveable { mutableStateOf(false) }
    var hasTransferredFocusToPlay by rememberSaveable { mutableStateOf(false) }
    var hasUserNavigated by rememberSaveable { mutableStateOf(false) }

    // Row item counts map for focus navigation calculations
    val rowsData = remember(uiState, rows) {
        mapOf(
            "movies_collections" to uiState.collectionsState.items.size,
            "movies_trending" to uiState.trendingState.items.size,
            "movies_top10" to uiState.top10State.items.size
        ) + rows.associate { it.id to it.movies.size }
    }

    fun getPrevRowRequester(currentRowId: String, currentCardIndex: Int): FocusRequester? {
        val currentIndex = MOVIES_SECTION_ORDER.indexOf(currentRowId)
        if (currentIndex <= 0) return playButtonFocusRequester

        val prevSectionId = MOVIES_SECTION_ORDER[currentIndex - 1]
        if (prevSectionId == "movies_hero") {
            return playButtonFocusRequester
        }

        val itemCount = rowsData[prevSectionId] ?: 0
        if (itemCount <= 0) return playButtonFocusRequester

        val rememberedIndex = focusMemoryManager.getRememberedId(prevSectionId)?.toIntOrNull()
        val targetIndex = rememberedIndex ?: currentCardIndex.coerceAtMost(itemCount - 1)

        return focusRegistry.getCardRequester(prevSectionId, targetIndex)
    }

    fun getNextRowRequester(currentRowId: String, currentCardIndex: Int): FocusRequester? {
        val currentIndex = MOVIES_SECTION_ORDER.indexOf(currentRowId)
        if (currentIndex == -1 || currentIndex >= MOVIES_SECTION_ORDER.lastIndex) return null

        val nextSectionId = MOVIES_SECTION_ORDER[currentIndex + 1]
        val itemCount = rowsData[nextSectionId] ?: 0
        if (itemCount <= 0) return null

        val rememberedIndex = focusMemoryManager.getRememberedId(nextSectionId)?.toIntOrNull()
        val targetIndex = rememberedIndex ?: currentCardIndex.coerceAtMost(itemCount - 1)

        return focusRegistry.getCardRequester(nextSectionId, targetIndex)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!isInitialFocusRequested) {
                    try {
                        contentFocusRequester.requestFocus()
                    } catch (e: Exception) {
                        // Fallback handling
                    }
                    isInitialFocusRequested = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Current active hero movie provider
    val activeHeroMovie = uiState.heroState.currentMovie

    // Deferred focus transfer to Hero Play button ONCE activeHeroMovie arrives
    LaunchedEffect(activeHeroMovie) {
        if (activeHeroMovie != null && !hasTransferredFocusToPlay && !hasUserNavigated) {
            try {
                playButtonFocusRequester.requestFocus()
                hasTransferredFocusToPlay = true
            } catch (e: Exception) {
                // Retry when layout updates
            }
        }
    }

    // SECTION VIEWPORT FRAMING CAMERA ALGORITHM
    val topMarginPx = with(density) { 40.dp.toPx() }
    val bottomMarginPx = with(density) { 40.dp.toPx() }

    LaunchedEffect(currentFocusSection, sectionYPositions.toMap(), sectionHeights.toMap(), viewportHeightPx, columnHeightPx) {
        val targetSection = currentFocusSection ?: "movies_hero"

        val targetOffsetPx = if (targetSection == "movies_hero") {
            0f
        } else {
            val measuredY = sectionYPositions[targetSection]
            val sectionHeight = sectionHeights[targetSection] ?: 0f

            if (measuredY != null && viewportHeightPx > 0f) {
                val currentCamera = cameraOffsetY.value
                val sectionTopInViewport = measuredY + currentCamera
                val sectionBottomInViewport = measuredY + sectionHeight + currentCamera
                val viewportMaxBottom = viewportHeightPx - bottomMarginPx

                val desiredCameraY = when {
                    sectionTopInViewport < topMarginPx -> {
                        -(measuredY - topMarginPx)
                    }
                    sectionBottomInViewport > viewportMaxBottom -> {
                        -(measuredY + sectionHeight - viewportMaxBottom)
                    }
                    else -> {
                        currentCamera
                    }
                }

                val maxScrollPx = maxOf(0f, columnHeightPx - viewportHeightPx)
                desiredCameraY.coerceIn(-maxScrollPx, 0f)
            } else {
                cameraOffsetY.value
            }
        }

        cameraOffsetY.animateTo(
            targetValue = targetOffsetPx,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    fun Modifier.trackSectionPosition(sectionId: String): Modifier = this.onGloballyPositioned { coordinates ->
        if (coordinates.isAttached) {
            // positionInParent().y measures the un-transformed layout position inside the root Column
            sectionYPositions[sectionId] = coordinates.positionInParent().y
            sectionHeights[sectionId] = coordinates.size.height.toFloat()
        }
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.MOVIES,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { activeHeroMovie ?: focusStateFlow.collectAsState().value?.movie },
        showLoadingOverlay = loading && rows.isEmpty() && uiState.heroState.featuredMovies.isEmpty(),
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester,
        heroHud = null // Suppress static Home Hero HUD overlay
    ) { modifier ->
        // VIEWPORT
        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    viewportHeightPx = coordinates.size.height.toFloat()
                }
                .drawWithContent {
                    clipRect(left = HomeConstants.HOME_SIDEBAR_WIDTH.toPx()) {
                        this@drawWithContent.drawContent()
                    }
                }
                .clipToBounds()
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    left = navFocusRequester
                }
        ) {
            // WORLD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true, align = Alignment.Top)
                    .onGloballyPositioned { coordinates ->
                        columnHeightPx = coordinates.size.height.toFloat()
                    }
                    .graphicsLayer {
                        translationY = cameraOffsetY.value
                    }
            ) {
                // 1. MOVIES HERO SECTION
                val collectionsFirstRequester = remember(rowsData) {
                    val collectionsRemembered = focusMemoryManager.getRememberedId("movies_collections")?.toIntOrNull() ?: 0
                    focusRegistry.getCardRequester("movies_collections", collectionsRemembered)
                }

                MoviesHeroSection(
                    heroState = uiState.heroState,
                    onPlayClick = { movie ->
                        if (movie != null) {
                            onMovieClick(movie)
                        }
                    },
                    onMoreInfoClick = { movie ->
                        if (movie != null) {
                            onMovieClick(movie)
                        }
                    },
                    onSelectHeroIndex = { index ->
                        viewModel.selectHeroIndex(index)
                    },
                    playButtonFocusRequester = playButtonFocusRequester,
                    moreInfoFocusRequester = moreInfoFocusRequester,
                    navFocusRequester = navFocusRequester,
                    onHeroFocused = {
                        currentFocusSection = "movies_hero"
                    },
                    modifier = Modifier
                        .trackSectionPosition("movies_hero")
                        .focusProperties {
                            down = collectionsFirstRequester
                        }
                        .padding(
                            start = HomeConstants.HOME_CONTENT_START_PADDING,
                            top = 40.dp,
                            end = 48.dp,
                            bottom = 16.dp
                        )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. COLLECTIONS ROW (PHASE 3)
                CollectionsRow(
                    collectionsState = uiState.collectionsState,
                    onCollectionClick = { collectionItem ->
                        // Collection brand discovery click handler
                    },
                    onCollectionFocused = { rowId, collectionItem ->
                        hasUserNavigated = true
                        currentFocusSection = rowId
                    },
                    rowId = "movies_collections",
                    navFocusRequester = navFocusRequester,
                    getRequester = { rowId, index -> focusRegistry.getCardRequester(rowId, index) },
                    getPrevRowRequester = { index -> getPrevRowRequester("movies_collections", index) },
                    getNextRowRequester = { index -> getNextRowRequester("movies_collections", index) },
                    focusMemoryManager = focusMemoryManager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trackSectionPosition("movies_collections")
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 3. TRENDING MOVIES LANDSCAPE ROW (PHASE 4)
                val trendingItems = uiState.trendingState.items
                if (trendingItems.isNotEmpty()) {
                    MovieLandscapeRow(
                        rowId = "movies_trending",
                        title = "Trending Movies",
                        movies = trendingItems,
                        onMovieFocused = { rowId, movie ->
                            hasUserNavigated = true
                            currentFocusSection = rowId
                            viewModel.onMovieFocused(rowId, movie)
                        },
                        onMovieSelected = viewModel::onMovieSelected,
                        onMovieClick = onMovieClick,
                        navFocusRequester = navFocusRequester,
                        getRequester = { rowId, index -> focusRegistry.getCardRequester(rowId, index) },
                        getPrevRowRequester = { index -> getPrevRowRequester("movies_trending", index) },
                        getNextRowRequester = { index -> getNextRowRequester("movies_trending", index) },
                        focusMemoryManager = focusMemoryManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackSectionPosition("movies_trending")
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }

                // 4. TOP 10 IN THE UK TODAY ROW (PHASE 5)
                val top10Items = uiState.top10State.items
                if (top10Items.isNotEmpty()) {
                    Top10Row(
                        top10State = uiState.top10State,
                        onMovieFocused = { rowId, movie ->
                            hasUserNavigated = true
                            currentFocusSection = rowId
                            viewModel.onMovieFocused(rowId, movie)
                        },
                        onMovieSelected = viewModel::onMovieSelected,
                        onMovieClick = onMovieClick,
                        rowId = "movies_top10",
                        navFocusRequester = navFocusRequester,
                        getRequester = { rowId, index -> focusRegistry.getCardRequester(rowId, index) },
                        getPrevRowRequester = { index -> getPrevRowRequester("movies_top10", index) },
                        getNextRowRequester = { index -> getNextRowRequester("movies_top10", index) },
                        focusMemoryManager = focusMemoryManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trackSectionPosition("movies_top10")
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }

                // 5. LOWER CATALOGUE LANDSCAPE ROWS (PHASE 6)
                // Filter out movies_trending and movies_popular (which drives Top 10)
                val lowerRows = remember(rows) {
                    rows.filterNot { it.id == "movies_trending" || it.id == "movies_popular" }
                }

                lowerRows.forEachIndexed { index, row ->
                    key(row.id) {
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        MovieLandscapeRow(
                            rowId = row.id,
                            title = row.title,
                            movies = row.movies,
                            onMovieFocused = { rowId, movie ->
                                hasUserNavigated = true
                                currentFocusSection = rowId
                                viewModel.onMovieFocused(rowId, movie)
                            },
                            onMovieSelected = viewModel::onMovieSelected,
                            onMovieClick = onMovieClick,
                            navFocusRequester = navFocusRequester,
                            getRequester = { rowId, idx -> focusRegistry.getCardRequester(rowId, idx) },
                            getPrevRowRequester = { idx -> getPrevRowRequester(row.id, idx) },
                            getNextRowRequester = { idx -> getNextRowRequester(row.id, idx) },
                            focusMemoryManager = focusMemoryManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .trackSectionPosition(row.id)
                        )
                    }
                }

                if (!loading && rows.isEmpty() && uiState.heroState.featuredMovies.isEmpty()) {
                    EmptyMoviesPlaceholder()
                }

                Spacer(modifier = Modifier.height(600.dp))
            }
        }
    }
}

@Composable
fun EmptyMoviesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = HomeConstants.HOME_CONTENT_START_PADDING),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "No movies found.",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try again later or check back for new additions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
