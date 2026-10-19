package com.example.peak.ui.screens.random

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.CasinoDiceIcon
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants

private enum class ResultFilter {
    ALL,
    MOVIES,
    TV_SHOWS
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RandomResultsScreen(
    viewModel: RandomViewModel,
    onSidebarItemSelected: (SidebarItemType) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val navFocusRequester = remember { FocusRequester() }
    val filterChipsFocusRequester = remember { FocusRequester() }
    val carouselFocusRequester = remember { FocusRequester() }
    val bottomActionsFocusRequester = remember { FocusRequester() }
    
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var selectedFilter by remember { mutableStateOf(ResultFilter.ALL) }

    if (uiState.results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040B16)), contentAlignment = Alignment.Center) {
            Text("No results to display. Press Back to return.", color = Color.White)
        }
        return
    }

    val moviesCount = remember(uiState.results) { uiState.results.count { it.mediaType == MediaType.MOVIE } }
    val tvCount = remember(uiState.results) { uiState.results.count { it.mediaType == MediaType.TV } }

    val visibleResults = remember(uiState.results, selectedFilter) {
        when (selectedFilter) {
            ResultFilter.ALL -> uiState.results
            ResultFilter.MOVIES -> uiState.results.filter { it.mediaType == MediaType.MOVIE }
            ResultFilter.TV_SHOWS -> uiState.results.filter { it.mediaType == MediaType.TV }
        }
    }

    LaunchedEffect(visibleResults) {
        if (visibleResults.isNotEmpty()) {
            if (focusedMovie == null || !visibleResults.contains(focusedMovie)) {
                focusedMovie = visibleResults.first()
            }
        } else {
            focusedMovie = null
        }
    }

    LaunchedEffect(Unit) {
        try {
            carouselFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { focusedMovie },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = carouselFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = HomeConstants.HOME_CONTENT_START_PADDING, top = 24.dp, end = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Column {
                Text(
                    text = "R A N D O M  P I C K S",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .focusProperties {
                            down = carouselFocusRequester
                        }
                ) {
                    FilterChipControl(
                        label = "All Picks",
                        count = uiState.results.size.toString(),
                        selected = selectedFilter == ResultFilter.ALL,
                        onClick = { selectedFilter = ResultFilter.ALL },
                        modifier = Modifier.focusRequester(filterChipsFocusRequester)
                    )
                    if (moviesCount > 0) {
                        FilterChipControl(
                            label = "Movies",
                            count = moviesCount.toString(),
                            selected = selectedFilter == ResultFilter.MOVIES,
                            onClick = { selectedFilter = ResultFilter.MOVIES }
                        )
                    }
                    if (tvCount > 0) {
                        FilterChipControl(
                            label = "TV Shows",
                            count = tvCount.toString(),
                            selected = selectedFilter == ResultFilter.TV_SHOWS,
                            onClick = { selectedFilter = ResultFilter.TV_SHOWS }
                        )
                    }
                }

                if (visibleResults.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(end = 48.dp),
                        modifier = Modifier
                            .focusRequester(carouselFocusRequester)
                            .focusProperties {
                                up = filterChipsFocusRequester
                                down = bottomActionsFocusRequester
                            }
                    ) {
                        itemsIndexed(visibleResults, key = { _, movie -> "${movie.mediaType.name}_${movie.movieId}" }) { _, movie ->
                            MovieCard(
                                movie = movie,
                                onFocus = { m -> focusedMovie = m },
                                onClick = { m -> onMovieClick(m) },
                                modifier = Modifier.size(170.dp, 255.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(255.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No titles match the selected filter.",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (focusedMovie != null) {
                val context = LocalContext.current
                val bottomImageRequest = remember(focusedMovie?.movieId, focusedMovie?.imageUrl) {
                    focusedMovie?.imageUrl?.let { url ->
                        ImageRequest.Builder(context)
                            .data(url)
                            .crossfade(true)
                            .build()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(modifier = Modifier.weight(1f).padding(end = 32.dp), verticalAlignment = Alignment.Bottom) {
                        AsyncImage(
                            model = bottomImageRequest,
                            contentDescription = null,
                            modifier = Modifier.width(60.dp).height(90.dp).clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.Bottom) {
                            Text(
                                text = focusedMovie!!.name,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(focusedMovie!!.year, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelMedium)
                                Text("•", color = Color.White.copy(alpha = 0.2f))
                                Text(focusedMovie!!.duration, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelMedium)
                                Text("•", color = Color.White.copy(alpha = 0.2f))
                                Text(focusedMovie!!.genres.split(",").firstOrNull() ?: "", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = focusedMovie!!.description,
                                color = Color.White.copy(alpha = 0.60f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    ResultActions(
                        focusedMovie = focusedMovie,
                        uiState = uiState,
                        viewModel = viewModel,
                        onMovieClick = onMovieClick,
                        carouselFocusRequester = carouselFocusRequester,
                        moreInfoFocusRequester = bottomActionsFocusRequester
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ResultActions(
    focusedMovie: Movie?,
    uiState: RandomUiState,
    viewModel: RandomViewModel,
    onMovieClick: (Movie) -> Unit,
    carouselFocusRequester: FocusRequester,
    moreInfoFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.focusProperties {
            up = carouselFocusRequester
        }
    ) {
        Button(
            onClick = { focusedMovie?.let { onMovieClick(it) } },
            colors = ButtonDefaults.colors(
                containerColor = Color.White,
                focusedContainerColor = Color(0xFF00D2FF),
                contentColor = Color(0xFF040B16),
                focusedContentColor = Color(0xFF040B16)
            ),
            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier.focusRequester(moreInfoFocusRequester)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("More Info", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { 
                if (focusedMovie != null) {
                    val index = uiState.results.indexOf(focusedMovie)
                    if (index != -1) {
                        viewModel.replaceResult(index)
                    }
                }
            },
            colors = ButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White,
                contentColor = Color.White,
                focusedContentColor = Color(0xFF040B16)
            ),
            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = CasinoDiceIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick Another", fontWeight = FontWeight.Bold)
        }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val watchlistRepo = remember { com.example.peak.PeakDependencyProvider.getWatchlistRepository(context) }
        var isAdded by remember(focusedMovie?.movieId) { mutableStateOf(false) }

        Button(
            onClick = {
                focusedMovie?.let { movie ->
                    coroutineScope.launch {
                        watchlistRepo.addTitleToWatchlist(movie = movie)
                        isAdded = true
                    }
                }
            },
            enabled = focusedMovie != null,
            colors = ButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = Color.White,
                contentColor = Color.White,
                focusedContentColor = Color(0xFF040B16)
            ),
            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isAdded) "In Watchlist" else "Add to Watchlist")
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FilterChipControl(
    label: String,
    count: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.Transparent else Color.White.copy(alpha = 0.05f),
            focusedContainerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White,
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF00D2FF) else Color.Transparent), shape = RoundedCornerShape(16.dp)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00D2FF)), shape = RoundedCornerShape(16.dp))
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label, color = Color.White, style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = count, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
