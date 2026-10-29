package com.example.peak.ui.image

import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroLogoTraceTest {

    @Test
    fun formatIncludesMonotonicTimestampAndMediaCorrelationKey() {
        val line = HeroLogoTrace.format(
            event = HeroLogoTrace.Event.HERO_IMAGE_SUCCEEDED,
            elapsedRealtimeNanos = 123456789L,
            mediaType = MediaType.TV,
            mediaId = "456",
            imageSource = HeroLogoTrace.ImageSource.DISK
        )

        assertTrue(line.contains("event=hero_image_succeeded"))
        assertTrue(line.contains("elapsedRealtimeNanos=123456789"))
        assertTrue(line.contains("mediaType=TV"))
        assertTrue(line.contains("mediaId=456"))
        assertTrue(line.contains("dataSource=DISK"))
    }

    @Test
    fun formatRedactsUntrustedIdentityAndCategoryValues() {
        val sensitiveValue = "https://image.example/title?api_key=secret"
        val line = HeroLogoTrace.format(
            event = HeroLogoTrace.Event.CATEGORY_RESULT_RECEIVED,
            elapsedRealtimeNanos = 7L,
            mediaType = MediaType.MOVIE,
            mediaId = sensitiveValue,
            category = sensitiveValue
        )

        assertTrue(line.contains("mediaId=redacted"))
        assertTrue(line.contains("category=other"))
        assertFalse(line.contains("https://"))
        assertFalse(line.contains("api_key"))
        assertFalse(line.contains("secret"))
    }

    @Test
    fun apiKeyLikeMediaIdIsRedacted() {
        val line = HeroLogoTrace.format(
            event = HeroLogoTrace.Event.DETAIL_NETWORK_STARTED,
            elapsedRealtimeNanos = 8L,
            mediaType = MediaType.MOVIE,
            mediaId = "28698e1796764c8280ae3896267caecd"
        )

        assertTrue(line.contains("mediaId=redacted"))
        assertFalse(line.contains("28698e1796764c8280ae3896267caecd"))
    }

    @Test
    fun coilDataSourceIsRestrictedToKnownValues() {
        assertEquals(
            HeroLogoTrace.ImageSource.MEMORY_CACHE,
            HeroLogoTrace.ImageSource.fromCoilName("MEMORY_CACHE")
        )
        assertEquals(
            HeroLogoTrace.ImageSource.OTHER,
            HeroLogoTrace.ImageSource.fromCoilName("https://secret")
        )
    }
}
