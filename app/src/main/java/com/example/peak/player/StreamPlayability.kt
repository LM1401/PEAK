package com.example.peak.player

import com.example.peak.domain.model.AddonStream

/**
 * Sealed classification hierarchy representing stream playability analysis for Media3 ExoPlayer.
 */
sealed class StreamPlayability {
    data class Playable(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val streamName: String? = null,
        val streamTitle: String? = null
    ) : StreamPlayability()

    data class Unsupported(
        val reason: String
    ) : StreamPlayability()
}

/**
 * Analyzes an AddonStream to classify whether it can be played directly by Media3 ExoPlayer
 * or if it is an unsupported format (e.g. raw BitTorrent infohash, external link, non-video).
 */
fun AddonStream.classifyPlayability(): StreamPlayability {
    if (behaviorHints?.notVideo == true) {
        return StreamPlayability.Unsupported("The selected stream is flagged as non-video content.")
    }

    val directUrl = url?.trim()
    if (!directUrl.isNullOrEmpty()) {
        val lowerUrl = directUrl.lowercase()
        if (lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://")) {
            val proxyHeaders = behaviorHints?.proxyHeaders?.request ?: emptyMap()
            return StreamPlayability.Playable(
                url = directUrl,
                headers = proxyHeaders,
                streamName = name,
                streamTitle = title
            )
        } else {
            return StreamPlayability.Unsupported("Unsupported URL scheme '$directUrl'. Only HTTP and HTTPS are supported.")
        }
    }

    if (!infoHash.isNullOrBlank()) {
        return StreamPlayability.Unsupported("Raw BitTorrent infohash streams are not supported directly. A Debrid or proxy add-on is required.")
    }

    if (!externalUrl.isNullOrBlank()) {
        return StreamPlayability.Unsupported("External web links are not supported for in-app TV playback.")
    }

    return StreamPlayability.Unsupported("No valid streaming URL was provided by the add-on.")
}
