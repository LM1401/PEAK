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
    LaunchedEffect(focusState?.movieId, rows) {
        val movieId = focusState?.movieId ?: return@LaunchedEffect
        val row = rows.find { it.movies.any { m -> m.movieId == movieId } }
        
        row?.let {
            if (!isInitialised) {
                cameraState.snapToRow(it.id)
                isInitialised = true
            } else {
                cameraState.scrollToRow(it.id)
            }
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
                    .tvCameraWorld(cameraState),
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Spacer(modifier = Modifier.height(100.dp))

                rows.forEach { row ->
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
