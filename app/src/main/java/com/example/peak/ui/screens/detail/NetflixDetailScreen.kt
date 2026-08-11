package com.example.peak.ui.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.ui.components.DetailMovieCard
import com.example.peak.data.repository.ContinueWatchingRepository

/**
 * A Netflix-style Movie Detail Screen for Android TV.
 * Refactored to Option A: movieId is the single source of truth.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixDetailScreen(
    mediaId: String,
    mediaType: MediaType,
    movieRepository: MovieRepository,
    continueWatchingRepository: ContinueWatchingRepository,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val viewModel: DetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = DetailViewModelFactory(movieRepository, continueWatchingRepository)
    )
    
    val movie by viewModel.movie.collectAsState()
    val similarMovies by viewModel.similarMovies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val resumePosition by viewModel.resumePosition.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    LaunchedEffect(mediaId, mediaType) {
        viewModel.loadMedia(mediaId, mediaType)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            isLoading && movie == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading Details...", color = Color.White)
                }
            }
            error != null && movie == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "Unknown Error", color = Color.Red)
                }
            }
            movie != null -> {
                NetflixDetailContent(
                    movie = movie!!,
                    similarMovies = similarMovies,
                    resumePosition = resumePosition,
                    totalDuration = totalDuration,
                    onPlayClick = onPlayClick,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixDetailContent(
    movie: Movie,
    similarMovies: List<Movie>,
    resumePosition: Long = 0L,
    totalDuration: Long = 0L,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val context = LocalContext.current
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var backdropMovie by remember(movie) { mutableStateOf(movie) }

    LaunchedEffect(focusedMovie) {
        focusedMovie?.let {
            kotlinx.coroutines.delay(120)
            backdropMovie = it
        }
    }

    val displayMovie = backdropMovie

    val playButtonFocusRequester = remember { FocusRequester() }
    val addToListFocusRequester = remember { FocusRequester() }
    val rowFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. BACKDROP (Fixed behind everything)
        AsyncImage(
            model = remember(displayMovie.movieId, displayMovie.mediaType) {
                val cacheKey = "${displayMovie.mediaType.name}_${displayMovie.movieId}-bg"
                ImageRequest.Builder(context)
                    .data(displayMovie.backdropUrl)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .allowHardware(true)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Cinematic Gradients for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        endX = 1000f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black
                        )
                    )
                )
        )

        // MAIN CONTENT LAYER
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 2. HERO SECTION (LOCKED / NON-SCROLLING - Top 55%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .padding(start = 48.dp, top = 24.dp, end = 48.dp, bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .widthIn(max = 800.dp)
                ) {
                    // Title
                    Text(
                        text = displayMovie.name,
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    // Metadata Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val matchPercent = (displayMovie.rating.toDoubleOrNull()?.times(10))?.toInt() ?: 92
                        Text(text = "$matchPercent% Match", color = Color(0xFF46D369), fontWeight = FontWeight.Bold)
                        Text(text = displayMovie.year, color = Color.White)
                        MetadataBadge(text = displayMovie.ageRating)
                        Text(text = displayMovie.duration, color = Color.White)
                        MetadataBadge(text = "HD 5.1")
                    }

                    Spacer(Modifier.height(16.dp))

                    // Vertical Buttons - PRIORITY (Moved above description)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .width(300.dp)
                            .heightIn(min = 140.dp)
                    ) {
                        val playText = if (resumePosition > 0) {
                            val remainingMs = totalDuration - resumePosition
                            if (remainingMs > 60000) {
                                val remainingMin = remainingMs / 60000
                                "Resume • ${remainingMin}m left"
                            } else {
                                "Resume"
                            }
                        } else {
                            "Play"
                        }

                        ActionButton(
                            icon = Icons.Default.PlayArrow,
                            text = playText,
                            isPrimary = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(playButtonFocusRequester),
                            onClick = { onPlayClick(displayMovie) }
                        )
                        ActionButton(
                            icon = Icons.AutoMirrored.Filled.List,
                            text = "Audio & Subtitles",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* Audio & Subtitles logic */ }
                        )
                        ActionButton(
                            icon = Icons.Default.Add,
                            text = "Add to My List",
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(addToListFocusRequester)
                                .focusProperties {
                                    down = rowFocusRequester
                                },
                            onClick = { /* My List logic */ }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Description (Secondary - Moved below buttons)
                    Text(
                        text = displayMovie.description,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                }
            }

            // 3. SCROLLABLE SECTION (Bottom 45%)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(horizontal = 48.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "More Like This",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))
                }

                item {
                    LazyRow(
                        modifier = Modifier
                            .focusRequester(rowFocusRequester)
                            .focusProperties {
                                up = addToListFocusRequester
                            },
                        contentPadding = PaddingValues(end = 80.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(similarMovies, key = { "${it.mediaType.name}_${it.movieId}" }) { simMovie ->
                            DetailMovieCard(
                                movie = simMovie,
                                onMovieFocused = { movie ->
                                    if (movie == null) {
                                        // Handle focus loss if needed
                                    } else if (focusedMovie?.movieId != movie.movieId || focusedMovie?.mediaType != movie.mediaType) {
                                        focusedMovie = movie
                                    }
                                },
                                onMovieClick = { onMovieClick(it) }
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Additional info
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailInfoText(label = "Cast", value = movie.cast)
                        DetailInfoText(label = "Director", value = movie.director)
                        DetailInfoText(label = "Genres", value = movie.genres)
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun NetflixDetailScreenPreview() {
    val mockMovie = Movie(
        movieId = "1",
        name = "The Gray Man",
        description = "When the CIA's most skilled mercenary—whose true identity is known to no one—accidentally uncovers dark agency secrets, a psychopathic former colleague puts a bounty on his head, setting off a global manhunt by international assassins.",
        imageUrl = "",
        backdropUrl = "",
        rating = "9.2",
        year = "2022",
        ageRating = "13+",
        duration = "2h 9m",
        cast = "Ryan Gosling, Chris Evans, Ana de Armas",
        director = "Anthony Russo, Joe Russo",
        genres = "Action, Thriller"
    )
    MaterialTheme {
        NetflixDetailContent(
            movie = mockMovie,
            similarMovies = listOf(mockMovie, mockMovie, mockMovie),
            onPlayClick = {},
            onMovieClick = {}
        )
    }
}

@Composable
fun MetadataBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DetailInfoText(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) Color.White else Color.Transparent,
            contentColor = if (isPrimary) Color.Black else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
