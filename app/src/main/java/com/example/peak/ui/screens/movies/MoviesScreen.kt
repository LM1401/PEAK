package com.example.peak.ui.screens.movies

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieRow
import com.example.peak.ui.focus.rememberFocusMemoryManager
import com.example.peak.ui.focus.rememberTvCameraState
import com.example.peak.ui.focus.tvCameraWorld
import com.example.peak.ui.focus.onRowPositioned
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Movies Screen. 
 * REFACTORED: Uses the deterministic TV Camera System instead of LazyColumn.
 */
@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel,
    onTabSelected: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val focusState by viewModel.focusState.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    val focusManager = rememberFocusMemoryManager()
    val contentFocusRequester = remember { FocusRequester() }
    val navFocusRequester = remember { FocusRequester() }

    // TV CAMERA SYSTEM INITIALIZATION
    val cameraState = rememberTvCameraState()
    var isInitialised by remember { mutableStateOf(false) }

    // 1. VIEWPORT TRACKING
    val viewportModifier = Modifier.onGloballyPositioned { coords ->
        cameraState.viewportHeight = coords.size.height.toFloat()
    }

    // 2. FOCUS -> CAMERA SYNC
    // Derived state to track the target row ID for camera anchoring
    val focusRowId = remember(focusState?.movieId, rows) {
        val movieId = focusState?.movieId
        rows.find { it.movies.any { m -> m.movieId == movieId } }?.id
    }

    LaunchedEffect(focusRowId) {
        val rowId = focusRowId ?: return@LaunchedEffect
        
        // CRITICAL SYNC: Wait for the layout system to measure the target row.
        // This ensures snapToRow has real coordinates and prevents the first-frame jump.
        snapshotFlow { cameraState.hasPosition(rowId) }
            .filter { it }
            .first()

        if (!isInitialised) {
            cameraState.snapToRow(rowId)
            isInitialised = true
        } else {
            cameraState.scrollToRow(rowId)
        }
    }

    LaunchedEffect(rows) {
        if (rows.isNotEmpty()) {
            val moviesToWarm = rows.take(3).flatMap { it.movies }
            ImageWarmingManager.warm(context, imageLoader, moviesToWarm)
        }
    }

    HomeBaseLayout(
        selectedTab = "Films",
        onTabSelected = onTabSelected,
        onSettingsClick = onSettingsClick,
        onSearchClick = onSearchClick,
        focusedMovie = focusState?.movie,
        showLoadingOverlay = loading && rows.isEmpty(),
        navFocusRequester = navFocusRequester,
        contentFocusRequester = contentFocusRequester
    ) { modifier ->
        // THE VIEWPORT
        Box(
            modifier = modifier
                .then(viewportModifier)
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = navFocusRequester
                }
        ) {
            // THE WORLD (Translates based on Camera State)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvCameraWorld(cameraState)
            ) {
                // 1. GLOBAL ANCHOR: Only this spacer defines the start of content.
                Spacer(modifier = Modifier.height(220.dp))

                // 2. CONTENT RHYTHM: Inter-row spacing applied manually to avoid anchor inflation.
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    MovieRow(
                        row = row,
                        focusedMovieId = focusState?.movieId,
                        onMovieFocused = { id -> 
                            row.movies.find { it.movieId == id }?.let(viewModel::onMovieFocused)
                        },
                        onMovieSelected = viewModel::onMovieSelected,
                        onMovieClick = onMovieClick,
                        focusManager = focusManager,
                        modifier = Modifier.onRowPositioned(row.id, cameraState)
                    )
                }

                if (!loading && rows.isEmpty()) {
                    EmptyMoviesPlaceholder()
                }

                // Bottom spacer for overshoot/safe-area
                Spacer(modifier = Modifier.height(200.dp))
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
            .padding(horizontal = 120.dp),
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
