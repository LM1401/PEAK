package com.example.peak

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.*
import com.example.peak.ui.screens.detail.NetflixDetailScreen
import com.example.peak.ui.screens.home.HomeScreen
import com.example.peak.ui.screens.player.PlayerScreen
import com.example.peak.ui.theme.PEAKTheme

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
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = "home"
    ) {
        composable("home") {
            val movieRepository = com.example.peak.data.repository.MovieRepositoryImpl(
                com.example.peak.data.remote.retrofit.RetrofitInstance.api
            )
            val homeViewModel: com.example.peak.ui.screens.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.home.HomeViewModelFactory(movieRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onMovieClick = { movie ->
                    navController.navigate("movie_detail/${movie.movieId}")
                }
            )
        }

        composable(
            route = "movie_detail/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
            NetflixDetailScreen(
                movieId = movieId,
                onPlayClick = { movie ->
                    navController.navigate("player/${movie.movieId}")
                },
                onMovieClick = { movie ->
                    // Replace current detail instead of stacking
                    navController.navigate("movie_detail/${movie.movieId}") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        // 5. Proper Player Route
        composable(
            route = "player/{movieId}",
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { _ ->
            // Using a placeholder URL as per existing PlayerScreen requirement
            PlayerScreen(
                videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                onPlaybackFinished = { navController.popBackStack() }
            )
        }

        composable("search") {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Text("Search Screen Placeholder", color = Color.White)
            }
        }
    }
}
