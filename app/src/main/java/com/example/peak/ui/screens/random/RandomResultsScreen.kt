package com.example.peak.ui.screens.random

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.HeroSection
import com.example.peak.ui.components.HomeBaseLayout
import com.example.peak.ui.components.MovieCard
import com.example.peak.ui.components.sidebar.CasinoDiceIcon
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.home.HomeConstants
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.delay

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
    val topActionsFocusRequester = remember { FocusRequester() }
    val bottomActionsFocusRequester = remember { FocusRequester() }
    val carouselFocusRequester = remember { FocusRequester() }
    
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isBrowseView by remember { mutableStateOf(false) }

    if (uiState.results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040B16)), contentAlignment = Alignment.Center) {
            Text("No results to display. Press Back to return.", color = Color.White)
        }
        return
    }

    LaunchedEffect(uiState.results) {
        if (focusedMovie == null || !uiState.results.contains(focusedMovie)) {
            focusedMovie = uiState.results.firstOrNull()
        }
    }

    LaunchedEffect(isBrowseView) {
        if (!isBrowseView) {
            delay(100)
            try { topActionsFocusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    BackHandler(enabled = isBrowseView) {
        isBrowseView = false
    }

    HomeBaseLayout(
        selectedSidebarItem = SidebarItemType.RANDOM,
        onSidebarItemSelected = onSidebarItemSelected,
        focusedMovieProvider = { focusedMovie },
        navFocusRequester = navFocusRequester,
        contentFocusRequester = topActionsFocusRequester,
        heroHud = null
    ) { modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = HomeConstants.HOME_CONTENT_START_PADDING, top = 24.dp, end = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            AnimatedVisibility(
                visible = !isBrowseView,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = "R A N D O M  P I C K S",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HeroSection(focusedMovieProvider = { focusedMovie })
                    Spacer(modifier = Modifier.height(24.dp))
                    ResultActions(
                        focusedMovie = focusedMovie,
                        uiState = uiState,
                        viewModel = viewModel,
                        onMovieClick = onMovieClick,
                        modifier = Modifier.focusRequester(topActionsFocusRequester)
                    )
                }
            }

            if (uiState.results.size > 1) {
                Column {
                    AnimatedVisibility(
                        visible = !isBrowseView,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = "YOUR ${uiState.results.size} PICKS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isBrowseView,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Text(
                                text = "R A N D O M  P I C K S",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 32.dp)) {
                                val moviesCount = uiState.results.count { it.mediaType == MediaType.MOVIE }
                                val tvCount = uiState.results.count { it.mediaType == MediaType.TV }
                                FilterChipText("All Picks", uiState.results.size.toString(), true)
                                if (moviesCount > 0) FilterChipText("Movies", moviesCount.toString(), false)
                                if (tvCount > 0) FilterChipText("TV Shows", tvCount.toString(), false)
                            }
                        }
                    }

                    val cardWidth by animateDpAsState(if (isBrowseView) 170.dp else 120.dp, label = "cardW")
                    val cardHeight by animateDpAsState(if (isBrowseView) 255.dp else 168.dp, label = "cardH")

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(if (isBrowseView) 24.dp else 16.dp),
                        contentPadding = PaddingValues(end = 48.dp),
                        modifier = Modifier
                            .focusRequester(carouselFocusRequester)
                            .focusProperties {
                                down = bottomActionsFocusRequester
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (isBrowseView && keyEvent.key == Key.DirectionUp && keyEvent.type == KeyEventType.KeyDown) {
                                    isBrowseView = false
                                    return@onPreviewKeyEvent true
                                }
                                false
                            }
                    ) {
                        itemsIndexed(uiState.results, key = { _, movie -> "${movie.mediaType.name}_${movie.movieId}" }) { _, movie ->
                            MovieCard(
                                movie = movie,
                                onFocus = { m ->
                                    focusedMovie = m
                                    if (!isBrowseView) {
                                        isBrowseView = true
                                    }
                                },
                                onClick = { m -> onMovieClick(m) },
                                modifier = Modifier.size(cardWidth, cardHeight)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isBrowseView,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (focusedMovie != null) {
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(modifier = Modifier.weight(1f).padding(end = 32.dp), verticalAlignment = Alignment.Bottom) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(focusedMovie!!.imageUrl)
                                        .crossfade(true)
                                        .build(),
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
                                modifier = Modifier.focusRequester(bottomActionsFocusRequester)
                            )
                        }
                    }
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
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        Button(
            onClick = { focusedMovie?.let { onMovieClick(it) } },
            colors = ButtonDefaults.colors(
                containerColor = Color.White,
                focusedContainerColor = Color(0xFF00D2FF),
                contentColor = Color(0xFF040B16),
                focusedContentColor = Color(0xFF040B16)
            ),
            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("More Info", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { 
                val index = uiState.results.indexOf(focusedMovie)
                if (index != -1) {
                    viewModel.replaceResult(index)
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
        
        Button(
            onClick = { /* Watchlist Stub */ },
            enabled = false,
            colors = ButtonDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.05f),
                focusedContainerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White.copy(alpha = 0.5f),
                focusedContentColor = Color.White
            ),
            shape = ButtonDefaults.shape(shape = MaterialTheme.shapes.medium),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to List")
        }
    }
}

@Composable
private fun FilterChipText(label: String, count: String, selected: Boolean) {
    Row(
        modifier = Modifier
            .background(
                color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (selected) Modifier.border(
                    width = 1.dp,
                    color = Color(0xFF00D2FF),
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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