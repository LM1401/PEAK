package com.example.peak.data.provider

import java.net.URI

/**
 * Utility functions for processing Trakt API image URLs.
 */
internal fun List<String>?.firstValidTraktLogoUrl(): String? {
    return orEmpty()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { url ->
            when {
                url.startsWith("https://", ignoreCase = true) -> url
                url.startsWith("http://", ignoreCase = true) -> "https://${url.substringAfter("://")}"
                url.startsWith("//") -> "https:$url"
                else -> url
            }
        }
        .mapNotNull { url ->
            try {
                val uri = URI(url)
                if (uri.scheme?.lowercase() == "https") uri else null
            } catch (e: Exception) {
                null
            }
        }
        .filter { uri ->
            val host = uri.host?.lowercase() ?: ""
            // Allow exact matches for known Trakt CDN subdomains. 
            // Do not allow arbitrary subdomains (e.g. evil.trakt.tv) or assets.fanart.tv unless proven.
            host == "walter.trakt.tv" || host == "walter-r2.trakt.tv"
        }
        .filter { uri ->
            val path = uri.path?.lowercase() ?: ""
            path.endsWith(".png") || path.endsWith(".webp")
        }
        .map { it.toString() }
        .firstOrNull()
}
