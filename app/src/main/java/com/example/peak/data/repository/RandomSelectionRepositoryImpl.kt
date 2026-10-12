package com.example.peak.data.repository

import android.util.Log
import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.RandomSelectionRequest
import com.example.peak.domain.repository.RandomSelectionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.min
import kotlin.random.Random

class RandomSelectionRepositoryImpl(
    private val api: TmdbApi,
    private val continueWatchingRepository: ContinueWatchingRepository
) : RandomSelectionRepository {

    private val TAG = "RandomSelectionRepo"
    private val MAX_TMDB_PAGES = 500
    private val PAGES_TO_FETCH_PER_REQUEST = 3

    override suspend fun fetchRandomSelection(request: RandomSelectionRequest): Result<List<Movie>> {
        val moviesResult = mutableListOf<Movie>()
        val tvResult = mutableListOf<Movie>()

        return try {
            coroutineScope {
                val movieDeferred = if (request.moviesRequested > 0) {
                    async { fetchCandidates(request, MediaType.MOVIE, request.moviesRequested) }
                } else null

                val tvDeferred = if (request.tvRequested > 0) {
                    async { fetchCandidates(request, MediaType.TV, request.tvRequested) }
                } else null

                movieDeferred?.await()?.let { moviesResult.addAll(it) }
                tvDeferred?.await()?.let { tvResult.addAll(it) }
            }
            
            // We shuffle the combined results just in case, but they are already separated
            val finalSelection = (moviesResult + tvResult).shuffled()
            Result.success(finalSelection)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching random selection", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchSingleReplacement(
        request: RandomSelectionRequest,
        mediaType: MediaType,
        existingResults: List<Movie>
    ): Result<Movie> {
        return try {
            // Include existing results in exclusions so we don't pick them again
            val combinedExclusions = request.sessionExclusions + existingResults.map { "${it.mediaType.name}_${it.movieId}" }
            val modifiedRequest = request.copy(sessionExclusions = combinedExclusions)
            
            val candidates = fetchCandidates(modifiedRequest, mediaType, 1)
            if (candidates.isNotEmpty()) {
                Result.success(candidates.first())
            } else {
                Result.failure(Exception("No replacement found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replacement", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchCandidates(
        request: RandomSelectionRequest,
        mediaType: MediaType,
        requiredCount: Int
    ): List<Movie> {
        val filters = request.filters.toMutableMap()
        
        // Remove 'page' if it's there
        filters.remove("page")
        
        // Get initial page to determine total_pages
        val initialResponse = SafeApiCall.execute(TAG) {
            if (mediaType == MediaType.MOVIE) {
                api.discoverAdvancedMovies(filters, 1)
            } else {
                api.discoverAdvancedSeries(filters, 1)
            }
        }

        if (initialResponse.data == null) {
            return emptyList()
        }

        val totalPages = initialResponse.data.totalPages ?: 1
        val maxPage = min(totalPages, MAX_TMDB_PAGES)
        
        val cwItems = continueWatchingRepository.continueWatchingItems.value
        val cwIds = cwItems.map { "${it.mediaType.name}_${it.movieId}" }.toSet()

        val pagesToFetch = min(maxPage, PAGES_TO_FETCH_PER_REQUEST)
        val selectedPages = if (maxPage <= pagesToFetch) {
            (1..maxPage).toList()
        } else {
            val pages = mutableSetOf<Int>()
            while (pages.size < pagesToFetch) {
                pages.add(Random.nextInt(1, maxPage + 1))
            }
            pages.toList()
        }

        val allCandidates = mutableListOf<Movie>()

        coroutineScope {
            val deferredPages = selectedPages.map { page ->
                async {
                    val resp = SafeApiCall.execute(TAG) {
                        if (mediaType == MediaType.MOVIE) {
                            api.discoverAdvancedMovies(filters, page)
                        } else {
                            api.discoverAdvancedSeries(filters, page)
                        }
                    }
                    resp.data?.results?.mapNotNull { it.toMovie(mediaType) } ?: emptyList()
                }
            }
            
            val pagesResults = deferredPages.awaitAll()
            pagesResults.forEach { allCandidates.addAll(it) }
        }

        // Apply local filtering
        val validCandidates = allCandidates
            .distinctBy { "${it.mediaType.name}_${it.movieId}" }
            .filter { movie ->
                val identity = "${movie.mediaType.name}_${movie.movieId}"
                !request.sessionExclusions.contains(identity) && !cwIds.contains(identity)
            }
            .shuffled()

        return validCandidates.take(requiredCount)
    }
}
