package com.example.peak.ui.screens.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.retrofit.RetrofitInstance
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.sampleRows
import com.example.peak.ui.components.MovieCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Netflix-style Movie Detail Screen for Android TV.
 * Structure: Fixed Hero Zone (Top) + Scrollable Info & Rows (Bottom).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixDetailScreen(
    movieId: String,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    var movie by remember { mutableStateOf<Movie?>(null) }
    var similarMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(movieId) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.getTrending()
            }
            val foundMovie = response.results.find { it.id.toString() == movieId }?.toMovie()
            
            movie = foundMovie
            similarMovies = response.results.take(10).map { it.toMovie() }
            isLoading = false
        } catch (e: Exception) {
            movie = sampleRows().firstOrNull()?.movies?.firstOrNull()
            similarMovies = sampleRows().flatMap { it.movies }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading...", color = Color.White)
        }
    } else {
        movie?.let { currentMovie ->
            NetflixDetailContent(
                movie = currentMovie,
                similarMovies = similarMovies,
                onPlayClick = onPlayClick,
                onMovieClick = onMovieClick
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixDetailContent(
    movie: Movie,
    similarMovies: List<Movie>,
    onPlayClick: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val playButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        playButtonFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // FIXED BACKDROP LAYER
        Image(
            painter = rememberAsyncImagePainter(movie.backdropUrl),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop
        )

        // CINEMATIC GRADIENTS (Pinned to background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.95f),
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        endX = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // CONTENT LAYOUT
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 🔵 1. HERO ZONE (FIXED / NON-SCROLLING)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 58.dp)
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Text(
                    text = "A NETFLIX FILM",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )

                Text(
                    text = movie.name.uppercase(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        lineHeight = 72.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "${(movie.rating.toDoubleOrNull()?.times(10))?.toInt() ?: 92}% Match",
                        color = Color(0xFF46D369),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(text = "•", color = Color.Gray)
                    Text(text = movie.year, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "•", color = Color.Gray)
                    MetadataBadge(text = movie.ageRating)
                    Text(text = "•", color = Color.Gray)
                    Text(text = movie.duration, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Text(text = "•", color = Color.Gray)
                    MetadataBadge(text = "HD")
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionButton(
                        icon = Icons.Default.PlayArrow,
                        text = "Play",
                        isPrimary = true,
                        modifier = Modifier
                            .width(160.dp)
                            .focusRequester(playButtonFocusRequester),
                        onClick = { onPlayClick(movie) }
                    )
                    ActionButton(
                        icon = Icons.Default.Add,
                        text = "My List",
                        modifier = Modifier.width(160.dp),
                        onClick = { /* Add to list logic */ }
                    )
                }
            }

            // 🟡 2. INFO (SCROLLABLE) + 🟣 3. ROWS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 58.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 800.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Cast, Director, Genres
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailInfoText(label = "Cast", value = movie.cast)
                    DetailInfoText(label = "Director", value = movie.director)
                    DetailInfoText(label = "Genres", value = movie.genres)
                }

                Spacer(modifier = Modifier.height(48.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.15f)))
                Spacer(modifier = Modifier.height(24.dp))

                // Suggested Rows
                Text(
                    text = "More Like This",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.focusProperties {
                        up = playButtonFocusRequester
                    }
                ) {
                    items(similarMovies) { simMovie ->
                        MovieCard(
                            movie = simMovie,
                            onMovieFocused = {},
                            onMovieClick = { onMovieClick(it) }
                        )
                    }
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
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) Color.White else Color.Transparent,
            contentColor = if (isPrimary) Color.Black else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        shape = ButtonDefaults.shape(RoundedCornerShape(4.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
