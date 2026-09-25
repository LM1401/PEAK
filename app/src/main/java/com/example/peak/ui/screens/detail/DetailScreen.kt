package com.example.peak.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.CastMember
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.formatBrandingText
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.ui.components.DetailMovieCard
import com.example.peak.ui.components.sidebar.Sidebar
import com.example.peak.ui.components.sidebar.SidebarItemType

/**
 * PEAK Detail Screen V2 — Precision Forensic Polish.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    mediaId: String,
    mediaType: MediaType,
    movieRepository: MovieRepository,
    continueWatchingRepository: ContinueWatchingRepository,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSidebarItemSelected: (SidebarItemType) -> Unit = {}
) {
    val viewModel: DetailViewModel = viewModel(
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040B16))) {
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
                DetailContent(
                    movie = movie!!,
                    similarMovies = similarMovies,
                    resumePosition = resumePosition,
                    totalDuration = totalDuration,
                    onPlayClick = onPlayClick,
                    onMovieClick = onMovieClick,
                    onSidebarItemSelected = onSidebarItemSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailContent(
    movie: Movie,
    similarMovies: List<Movie>,
    resumePosition: Long = 0L,
    totalDuration: Long = 0L,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSidebarItemSelected: (SidebarItemType) -> Unit = {}
) {
    val context = LocalContext.current

    val playButtonFocusRequester = remember { FocusRequester() }
    val myListFocusRequester = remember { FocusRequester() }
    val trailerFocusRequester = remember { FocusRequester() }
    val recommendationRowFocusRequester = remember { FocusRequester() }
    val sidebarFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040B16))
    ) {
        // 1. FULL-BLEED BACKDROP (High detail alignment)
        AsyncImage(
            model = remember(movie.movieId, movie.mediaType) {
                val cacheKey = "${movie.mediaType.name}_${movie.movieId}-bg"
                ImageRequest.Builder(context)
                    .data(movie.backdropUrl)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .allowHardware(true)
                    .build()
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // 2. CINEMATIC GRADIENTS (Precise left-side darkening + right-side artwork visibility)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF040B16).copy(alpha = 0.96f),
                            Color(0xFF040B16).copy(alpha = 0.70f),
                            Color(0xFF040B16).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 820f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.30f),
                            Color.Transparent,
                            Color(0xFF040B16).copy(alpha = 0.85f),
                            Color(0xFF040B16)
                        )
                    )
                )
        )

        // 3. MAIN CONTENT LAYER (Anchored at 100dp start, strictly balanced geometry for 540dp canvas)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 100.dp, top = 20.dp, end = 32.dp, bottom = 12.dp)
        ) {
            // PRODUCTION BRANDING
            val rawBrandText = movie.brand?.formatPresentationText()
                ?: if (movie.productionCompany.isNotBlank()) formatBrandingText(movie.productionCompany) else ""
            if (rawBrandText.isNotBlank()) {
                Text(
                    text = rawBrandText.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // LARGE CINEMATIC TITLE OR LOGO ARTWORK
            if (!movie.titleLogoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = remember(movie.movieId, movie.titleLogoUrl) {
                        ImageRequest.Builder(context)
                            .data(movie.titleLogoUrl)
                            .crossfade(false)
                            .allowHardware(true)
                            .build()
                    },
                    contentDescription = movie.name,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier
                        .heightIn(max = 60.dp)
                        .widthIn(max = 440.dp)
                )
            } else {
                Text(
                    text = movie.name.uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 580.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // GENRES
            if (movie.genres.isNotBlank()) {
                val genreDisplay = movie.genres.split(", ").joinToString("  •  ")
                Text(
                    text = genreDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // METADATA ROW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val matchPercent = (movie.rating.toDoubleOrNull()?.times(10))?.toInt()?.coerceIn(50, 99) ?: 92
                Text(
                    text = "$matchPercent% Match",
                    color = Color(0xFF22C55E), // PEAK Green accent
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Text(text = "|", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)

                if (movie.year.isNotBlank()) {
                    Text(
                        text = movie.year,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "|", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
                }

                if (movie.ageRating.isNotBlank()) {
                    MetadataBadge(text = movie.ageRating)
                    Text(text = "|", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
                }

                if (movie.duration.isNotBlank()) {
                    Text(
                        text = movie.duration,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SYNOPSIS
            if (movie.description.isNotBlank()) {
                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 540.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // CAST + DIRECTOR / CREATOR ROW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.widthIn(max = 560.dp)
            ) {
                val displayCast = movie.castMembers.take(3)
                if (displayCast.isNotEmpty()) {
                    displayCast.forEach { actor ->
                        CastAvatarItem(castMember = actor)
                    }
                } else if (movie.cast.isNotBlank()) {
                    movie.cast.split(", ").take(3).forEach { actorName ->
                        CastAvatarItem(castMember = CastMember(name = actorName))
                    }
                }

                if (movie.director.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Column {
                        Text(
                            text = if (movie.mediaType == MediaType.TV) "Creator" else "Director",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = movie.director,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HORIZONTAL ACTION BUTTONS
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
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

                DetailActionButton(
                    icon = Icons.Default.PlayArrow,
                    text = playText,
                    isPrimary = true,
                    focusRequester = playButtonFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = sidebarFocusRequester
                        right = myListFocusRequester
                        down = recommendationRowFocusRequester
                    },
                    onClick = { onPlayClick(movie) }
                )

                DetailActionButton(
                    icon = Icons.Default.Add,
                    text = "My List",
                    isPrimary = false,
                    focusRequester = myListFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = playButtonFocusRequester
                        right = trailerFocusRequester
                        down = recommendationRowFocusRequester
                    },
                    onClick = { /* Add to My List */ }
                )

                DetailActionButton(
                    icon = Icons.AutoMirrored.Filled.List,
                    text = "Trailer",
                    isPrimary = false,
                    focusRequester = trailerFocusRequester,
                    modifier = Modifier.focusProperties {
                        left = myListFocusRequester
                        down = recommendationRowFocusRequester
                    },
                    onClick = { /* Trailer action */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MORE LIKE THIS SECTION HEADER
            Text(
                text = "More Like This",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // RECOMMENDATION CAROUSEL (115dp x 165dp poster cards with horizontal right chevron)
            Box(modifier = Modifier.fillMaxWidth()) {
                LazyRow(
                    modifier = Modifier
                        .focusRequester(recommendationRowFocusRequester)
                        .focusProperties {
                            up = playButtonFocusRequester
                            left = sidebarFocusRequester
                        },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 48.dp)
                ) {
                    items(similarMovies, key = { "${it.mediaType.name}_${it.movieId}" }) { simMovie ->
                        DetailMovieCard(
                            movie = simMovie,
                            onMovieFocused = { /* preserve hero background */ },
                            onMovieClick = { onMovieClick(it) }
                        )
                    }
                }

                // Subtle right scroll indicator chevron
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "More recommendations",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. PEAK SIDEBAR OVERLAY
        Sidebar(
            selectedItem = SidebarItemType.HOME,
            onItemSelected = onSidebarItemSelected,
            onMoveRight = { playButtonFocusRequester.requestFocus() },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .focusRequester(sidebarFocusRequester)
                .focusProperties {
                    right = playButtonFocusRequester
                }
        )
    }
}

@Composable
fun CastAvatarItem(castMember: CastMember) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!castMember.profileUrl.isNullOrBlank()) {
            AsyncImage(
                model = castMember.profileUrl,
                contentDescription = castMember.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = castMember.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = castMember.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MetadataBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(38.dp)
            .focusRequester(focusRequester),
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) Color.White else Color.White.copy(alpha = 0.12f),
            contentColor = if (isPrimary) Color.Black else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        shape = ButtonDefaults.shape(RoundedCornerShape(19.dp)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
fun DetailScreenPreview() {
    val mockMovie = Movie(
        movieId = "1",
        name = "RESIDENT EVIL",
        description = "A medical courier finds himself fighting for survival as a night of chaos engulfs Raccoon City.",
        imageUrl = "",
        backdropUrl = "",
        rating = "7.3",
        year = "2026",
        ageRating = "R",
        duration = "1h 35m",
        cast = "Austin Abrams, Paul Walter Hauser, Kali Reis",
        castMembers = listOf(
            CastMember("Austin Abrams"),
            CastMember("Paul Walter Hauser"),
            CastMember("Kali Reis")
        ),
        director = "Johannes Roberts",
        genres = "Horror, Thriller, Survival",
        productionCompany = "Sony Pictures"
    )
    MaterialTheme {
        DetailContent(
            movie = mockMovie,
            similarMovies = listOf(mockMovie, mockMovie, mockMovie),
            onPlayClick = {},
            onMovieClick = {}
        )
    }
}
