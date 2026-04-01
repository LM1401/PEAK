package com.example.peak

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import coil.compose.rememberAsyncImagePainter
import com.example.peak.ui.theme.PEAKTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PEAK", "MainActivity onCreate")

        setContent {
            PEAKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    PEAKApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PEAKApp() {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var focusedMovie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitInstance.api.getTrending()
            }
            val fetchedMovies = response.results.map { tmdbMovie ->
                Movie(
                    name = tmdbMovie.title,
                    imageUrl = tmdbMovie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                        ?: "https://via.placeholder.com/1280x720?text=${tmdbMovie.title}",
                    backdropUrl = tmdbMovie.backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
                        ?: "https://via.placeholder.com/1280x720?text=${tmdbMovie.title}",
                    description = tmdbMovie.overview ?: "Experience the latest trending story. Now streaming on PEAK."
                )
            }
            movies = fetchedMovies
            if (fetchedMovies.isNotEmpty()) {
                focusedMovie = fetchedMovies.first()
            }
            isLoading = false
        } catch (e: Exception) {
            Log.e("PEAK", "Error fetching movies", e)
            movies = sampleRows().flatMap { it.movies }
            if (movies.isNotEmpty()) focusedMovie = movies.first()
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Loading PEAK...", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
    } else {
        val movieRows = remember(movies) {
            if (movies.size >= 10) {
                listOf(
                    Row("Trending This Week", movies.subList(0, 10)),
                    Row("Recommended for You", movies.subList(10, movies.size.coerceAtMost(20)))
                )
            } else {
                listOf(Row("Trending", movies))
            }
        }

        // Single Hero pinned at top and follows scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Hero Section
            BoxWithConstraints {
                val heroHeight = maxHeight * 0.35f
                HeroSection(
                    movie = focusedMovie,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Scrollable movie rows
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                itemsIndexed(movieRows) { _, row ->
                    MovieRow(
                        row = row,
                        onMovieFocused = { movie -> focusedMovie = movie },
                        onMovieClick = { movie ->
                            Toast.makeText(context, "Clicked: ${movie.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HeroSection(
    movie: Movie?,
    modifier: Modifier = Modifier
) {
    movie?.let { currentMovie ->
        Box(
            modifier = modifier
                .focusable(false)
        ) {
            // Hero image with Crossfade
            Crossfade(targetState = currentMovie, animationSpec = tween(800)) { target ->
                Image(
                    painter = rememberAsyncImagePainter(target.backdropUrl),
                    contentDescription = target.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Left gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Movie info (title + description)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 24.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = currentMovie.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentMovie.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Age rating badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PG-13",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieRow(
    row: Row, 
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 58.dp, bottom = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 58.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(row.movies) { _, movie ->
                MovieCard(
                    movie = movie,
                    onMovieFocused = onMovieFocused,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie, 
    onMovieFocused: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Card(
        onClick = { onMovieClick(movie) },
        modifier = Modifier
            .width(140.dp)
            .aspectRatio(2f / 3f)
            .onFocusChanged {
                if (it.isFocused) {
                    onMovieFocused(movie)
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.1f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color.White.copy(alpha = 0.1f),
                elevation = 10.dp
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(movie.imageUrl),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
