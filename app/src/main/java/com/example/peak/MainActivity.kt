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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.*
import com.example.peak.domain.model.MediaType
import com.example.peak.ui.components.sidebar.SidebarItemType
import com.example.peak.ui.screens.detail.DetailScreen
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
    val context = androidx.compose.ui.platform.LocalContext.current

    val movieRepository = remember { PeakDependencyProvider.getMovieRepository() }
    val continueWatchingRepository = remember { PeakDependencyProvider.getContinueWatchingRepository(context) }
    val watchlistRepository = remember { PeakDependencyProvider.getWatchlistRepository(context) }

    val onSidebarItemSelected: (SidebarItemType) -> Unit = { item ->
        when (item) {
            SidebarItemType.HOME -> navController.navigate("home") {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
            }
            SidebarItemType.MOVIES -> navController.navigate("movies") {
                launchSingleTop = true
            }
            SidebarItemType.TV -> navController.navigate("series") {
                launchSingleTop = true
            }
            SidebarItemType.RANDOM -> navController.navigate("random_flow") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            SidebarItemType.SEARCH -> navController.navigate("search") {
                launchSingleTop = true
            }
            SidebarItemType.SETTINGS -> navController.navigate("settings") {
                launchSingleTop = true
            }
            SidebarItemType.MY_LIST -> navController.navigate("watchlist") {
                launchSingleTop = true
            }
            SidebarItemType.PROFILE -> {
                // navController.navigate("profile")
            }
        }
    }

    NavHost(
        navController = navController, 
        startDestination = "home"
    ) {
        composable("home") {
            val homeViewModel: com.example.peak.ui.screens.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.home.HomeViewModelFactory(movieRepository, continueWatchingRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("movies") {
            val moviesViewModel: com.example.peak.ui.screens.movies.MoviesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.movies.MoviesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.movies.MoviesScreen(
                viewModel = moviesViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("series") {
            val seriesViewModel: com.example.peak.ui.screens.series.SeriesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.series.SeriesViewModelFactory(movieRepository)
            )
            com.example.peak.ui.screens.series.SeriesScreen(
                viewModel = seriesViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("watchlist") {
            val watchlistViewModel: com.example.peak.ui.screens.watchlist.WatchlistViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.watchlist.WatchlistViewModelFactory(watchlistRepository)
            )
            com.example.peak.ui.screens.watchlist.WatchlistScreen(
                viewModel = watchlistViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onMovieClick = { movie ->
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                },
                onPlayClick = { movie ->
                    navController.navigate("stream_selection/${movie.movieId}/${movie.mediaType.name}")
                }
            )
        }

        composable("settings") {
            val settingsRepository = remember { PeakDependencyProvider.getSettingsRepository(context) }
            val settingsViewModel: com.example.peak.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.settings.SettingsViewModelFactory(settingsRepository)
            )
            com.example.peak.ui.settings.SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToAddons = { navController.navigate("addons") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("addons") {
            val addonRepository = remember { PeakDependencyProvider.getAddonRepository(context) }
            val addonApiClient = remember { PeakDependencyProvider.getAddonApiClient() }
            val addonViewModel: com.example.peak.ui.screens.addons.AddonViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.addons.AddonViewModelFactory(addonRepository, addonApiClient)
            )
            com.example.peak.ui.screens.addons.AddonManagementScreen(
                viewModel = addonViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        navigation(route = "random_flow", startDestination = "random_config") {
            composable(
                route = "random_config",
                enterTransition = { fadeIn(tween(350)) },
                exitTransition = { fadeOut(tween(350)) },
                popEnterTransition = { fadeIn(tween(350)) },
                popExitTransition = { fadeOut(tween(350)) }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("random_flow")
                }
                val randomViewModel: com.example.peak.ui.screens.random.RandomViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    parentEntry,
                    factory = com.example.peak.ui.screens.random.RandomViewModelFactory(
                        PeakDependencyProvider.getRandomSelectionRepository(context),
                        movieRepository
                    )
                )
                com.example.peak.ui.screens.random.RandomConfigScreen(
                    viewModel = randomViewModel,
                    onSidebarItemSelected = onSidebarItemSelected,
                    onNavigateToResults = {
                        navController.navigate("random_results") {
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            composable(
                route = "random_results",
                enterTransition = { fadeIn(tween(350)) },
                exitTransition = { fadeOut(tween(350)) },
                popEnterTransition = { fadeIn(tween(350)) },
                popExitTransition = { fadeOut(tween(350)) }
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("random_flow")
                }
                val randomViewModel: com.example.peak.ui.screens.random.RandomViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    parentEntry,
                    factory = com.example.peak.ui.screens.random.RandomViewModelFactory(
                        PeakDependencyProvider.getRandomSelectionRepository(context),
                        movieRepository
                    )
                )
                com.example.peak.ui.screens.random.RandomResultsScreen(
                    viewModel = randomViewModel,
                    onSidebarItemSelected = onSidebarItemSelected,
                    onMovieClick = { movie ->
                        navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(
            route = "detail/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.MOVIE }
            
            DetailScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                movieRepository = movieRepository,
                continueWatchingRepository = continueWatchingRepository,
                onPlayClick = { movie ->
                    navController.navigate("stream_selection/${movie.movieId}/${movie.mediaType.name}")
                },
                onMovieClick = { movie ->
                    // Replace current detail instead of stacking
                    navController.navigate("detail/${movie.movieId}/${movie.mediaType.name}") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onSidebarItemSelected = onSidebarItemSelected
            )
        }

        composable(
            route = "stream_selection/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.MOVIE }

            val addonRepository = remember { PeakDependencyProvider.getAddonRepository(context) }
            val streamHandoffManager = remember { PeakDependencyProvider.getStreamHandoffManager() }
            val streamViewModel: com.example.peak.ui.screens.stream_selection.StreamSelectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.screens.stream_selection.StreamSelectionViewModelFactory(
                    movieRepository,
                    addonRepository
                )
            )

            com.example.peak.ui.screens.stream_selection.StreamSelectionScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                viewModel = streamViewModel,
                onStreamSelected = { stream ->
                    streamHandoffManager.setStream(mediaId, mediaType, stream)
                    navController.navigate("player/$mediaId/${mediaType.name}")
                },
                onNavigateToAddons = { navController.navigate("addons") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "player/{mediaId}/{mediaType}",
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
            val mediaTypeStr = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
            val mediaType = try { MediaType.valueOf(mediaTypeStr) } catch (e: Exception) { MediaType.MOVIE }
            
            PlayerScreen(
                mediaId = mediaId,
                mediaType = mediaType,
                onPlaybackFinished = { navController.popBackStack() },
                continueWatchingRepository = continueWatchingRepository,
                movieRepository = movieRepository
            )
        }

        composable("search") {
            val searchViewModel: com.example.peak.ui.search.SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.search.SearchViewModelFactory(movieRepository)
            )
            com.example.peak.ui.search.SearchScreen(
                viewModel = searchViewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onItemClick = { item ->
                    navController.navigate("detail/${item.movieId}/${item.mediaType.name}")
                },
                onDiscoveryClick = { title, genreIds ->
                    navController.navigate("discovery_results/$title/$genreIds")
                },
                onLearnMoreClick = {
                    navController.navigate("all_genres")
                }
            )
        }

        composable(
            route = "discovery_results/{title}/{genreIds}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("genreIds") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Results"
            val genreIds = backStackEntry.arguments?.getString("genreIds") ?: ""
            
            val viewModel: com.example.peak.ui.search.DiscoveryResultsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.example.peak.ui.search.DiscoveryResultsViewModelFactory(movieRepository)
            )
            
            // Load the results once when navigating
            LaunchedEffect(title, genreIds) {
                viewModel.loadDiscoveryResults(title, genreIds)
            }
            
            com.example.peak.ui.search.DiscoveryResultsScreen(
                viewModel = viewModel,
                onSidebarItemSelected = onSidebarItemSelected,
                onItemClick = { item ->
                    navController.navigate("detail/${item.movieId}/${item.mediaType.name}")
                }
            )
        }

        composable("all_genres") {
            com.example.peak.ui.search.AllGenresScreen(
                onSidebarItemSelected = onSidebarItemSelected,
                onGenreClick = { title, genreIds ->
                    navController.navigate("discovery_results/$title/$genreIds")
                }
            )
        }
    }
}
