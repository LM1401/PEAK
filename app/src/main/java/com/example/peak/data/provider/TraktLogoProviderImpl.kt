package com.example.peak.data.provider

import com.example.peak.data.remote.api.TraktApi
import com.example.peak.data.remote.dto.TraktSearchResponseDto
import com.example.peak.domain.model.MediaType
import com.example.peak.util.PeakLogoLogger
import java.io.IOException

class TraktLogoProviderImpl(
    private val api: TraktApi
) : AlternativeLogoProvider {

    override suspend fun findLogo(tmdbId: String, mediaType: MediaType): AlternativeLogoResult {
        return try {
            val type = when (mediaType) {
                MediaType.MOVIE -> "movie"
                MediaType.TV -> "show"
            }

            PeakLogoLogger.d { "Trakt API search initiated for $mediaType ID: $tmdbId (type parameter: $type)" }
            val response = api.searchByTmdbId(tmdbId = tmdbId, type = type)

            if (response.isSuccessful) {
                PeakLogoLogger.d { "Trakt API search HTTP 200 OK for $mediaType ID: $tmdbId" }
                val logoUrl = validateTraktResponse(response.body(), tmdbId, type)
                if (logoUrl != null) {
                    PeakLogoLogger.d { "Trakt API search returned valid candidate logo for $mediaType ID: $tmdbId -> $logoUrl" }
                    AlternativeLogoResult.Found(logoUrl)
                } else {
                    PeakLogoLogger.d { "Trakt API search returned HTTP 200 but no valid logo found for $mediaType ID: $tmdbId" }
                    AlternativeLogoResult.NotFound
                }
            } else {
                PeakLogoLogger.d { "Trakt API search failed with HTTP ${response.code()} for $mediaType ID: $tmdbId" }
                when (response.code()) {
                    404 -> AlternativeLogoResult.NotFound
                    429 -> {
                        val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                        PeakLogoLogger.d { "Trakt API 429 RateLimited for $mediaType ID: $tmdbId (Retry-After: $retryAfter)" }
                        AlternativeLogoResult.RateLimited(retryAfter)
                    }
                    else -> AlternativeLogoResult.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            PeakLogoLogger.d { "Trakt API search cancelled for $mediaType ID: $tmdbId" }
            throw e
        } catch (e: IOException) {
            PeakLogoLogger.d { "Trakt API search IOException for $mediaType ID: $tmdbId: ${e.message}" }
            AlternativeLogoResult.Error(e)
        } catch (e: Exception) {
            PeakLogoLogger.d { "Trakt API search Exception for $mediaType ID: $tmdbId: ${e.message}" }
            AlternativeLogoResult.Error(e)
        }
    }

    private fun validateTraktResponse(
        responseList: List<TraktSearchResponseDto>?,
        requestedTmdbId: String,
        expectedType: String
    ): String? {
        if (responseList.isNullOrEmpty()) return null

        val tmdbIdInt = requestedTmdbId.toIntOrNull() ?: return null

        val matchedResults = responseList.filter { item ->
            val isCorrectType = item.type == expectedType
            val actualTmdbId = when (expectedType) {
                "movie" -> item.movie?.ids?.tmdb
                "show" -> item.show?.ids?.tmdb
                else -> null
            }
            isCorrectType && actualTmdbId == tmdbIdInt
        }

        for (result in matchedResults) {
            val images = if (expectedType == "movie") {
                result.movie?.images
            } else {
                result.show?.images
            }
            val validLogoUrl = images?.logo.firstValidTraktLogoUrl()
            if (validLogoUrl != null) {
                return validLogoUrl
            }
        }

        return null
    }
}
