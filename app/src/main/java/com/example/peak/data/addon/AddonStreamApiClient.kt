package com.example.peak.data.addon

import android.util.Log
import com.example.peak.domain.model.AddonStreamResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Dedicated network client for fetching and parsing Stremio-compatible stream resources off the main thread.
 * Enforces 10-second timeouts, endpoint construction preserving URL configurations, and safe error handling.
 */
class AddonStreamApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val gson = Gson()

    companion object {
        /**
         * Constructs the Stremio stream endpoint URL from a manifest transport URL.
         * Replaces trailing /manifest.json with /stream/{type}/{id}.json while preserving
         * inline configuration paths (e.g. /providers=yts/manifest.json -> /providers=yts/stream/movie/tt15239678.json).
         */
        fun buildStreamUrl(manifestUrl: String, type: String, id: String): String {
            var trimmed = manifestUrl.trim()
            if (trimmed.isEmpty()) return ""

            // Convert stremio:// to https://
            if (trimmed.startsWith("stremio://", ignoreCase = true)) {
                trimmed = "https://" + trimmed.substring("stremio://".length)
            } else if (!trimmed.contains("://") && !trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed = "https://$trimmed"
            }

            val cleanType = type.trim()
            val cleanId = id.trim()

            val streamPath = "stream/$cleanType/$cleanId.json"

            return if (trimmed.endsWith("/manifest.json", ignoreCase = true)) {
                trimmed.substring(0, trimmed.length - "/manifest.json".length) + "/" + streamPath
            } else if (trimmed.endsWith("/")) {
                "$trimmed$streamPath"
            } else {
                "$trimmed/$streamPath"
            }
        }
    }

    /**
     * Executes asynchronous stream retrieval for a given content item from a specific add-on transport URL.
     */
    suspend fun fetchStreams(manifestUrl: String, type: String, id: String): Result<AddonStreamResponse> = withContext(ioDispatcher) {
        val trimmedUrl = manifestUrl.trim()
        val trimmedType = type.trim()
        val trimmedId = id.trim()

        if (trimmedUrl.isEmpty() || trimmedType.isEmpty() || trimmedId.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Manifest URL, media type, and content ID cannot be blank"))
        }

        val endpointUrl = buildStreamUrl(trimmedUrl, trimmedType, trimmedId)
        if (!endpointUrl.startsWith("http://", ignoreCase = true) && !endpointUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Unsupported URL scheme. Only HTTP and HTTPS are allowed."))
        }

        try {
            val request = Request.Builder()
                .url(endpointUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("HTTP error ${response.code}: ${response.message}")
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalStateException("Received empty stream response"))
                }

                val parsedResponse = gson.fromJson(bodyString, AddonStreamResponse::class.java)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to parse stream response JSON"))

                @Suppress("UNCHECKED_CAST")
                val safeResponse = parsedResponse.copy(
                    streams = (parsedResponse.streams as List<com.example.peak.domain.model.AddonStream>?) ?: emptyList()
                )

                return@withContext Result.success(safeResponse)
            }
        } catch (e: Exception) {
            // Sanitize log output to prevent leaking full endpoint parameters
            Log.e("ADDON_STREAM_API", "Failed to fetch streams from ${endpointUrl.take(45)}...", e)
            return@withContext Result.failure(e)
        }
    }
}
