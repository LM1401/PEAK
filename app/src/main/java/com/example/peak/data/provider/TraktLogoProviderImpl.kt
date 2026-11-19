package com.example.peak.data.provider

import com.example.peak.data.remote.api.TraktApi
import com.example.peak.data.remote.dto.TraktSearchResponseDto
import com.example.peak.domain.model.MediaType
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

            val response = api.searchByTmdbId(tmdbId = tmdbId, type = type)

            if (response.isSuccessful) {
                val logoUrl = validateTraktResponse(response.body(), tmdbId, type)
                if (logoUrl != null) {
                    AlternativeLogoResult.Found(logoUrl)
                } else {
                    AlternativeLogoResult.NotFound
                }
            } else {
                when (response.code()) {
                    404 -> AlternativeLogoResult.NotFound
                    429 -> {
                        val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                        AlternativeLogoResult.RateLimited(retryAfter)
                    }
                    else -> AlternativeLogoResult.Error(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: IOException) {
            AlternativeLogoResult.Error(e)
        } catch (e: Exception) {
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
