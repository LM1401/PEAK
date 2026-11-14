package com.example.peak.ui.components

import android.content.ContextWrapper
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.ui.image.ImageWarmingManager
import java.lang.reflect.Proxy
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test

class FocusedLogoWarmTargetTest {

    @Test
    fun initiallyFocusedMovieBecomesEligibleWhenEnrichmentProvidesLogo() {
        val basicMovie = createMovie(id = "101")
        assertNull(basicMovie.toFocusedLogoWarmTarget())

        val enrichedMovie = basicMovie.copy(
            isEnriched = true,
            titleLogoUrl = "https://images.example/logo-101.png"
        )

        assertEquals(
            FocusedLogoWarmTarget(
                mediaType = MediaType.MOVIE,
                mediaId = "101",
                logoUrl = "https://images.example/logo-101.png"
            ),
            enrichedMovie.toFocusedLogoWarmTarget()
        )
    }

    @Test
    fun enrichedFocusStateSuppliesLogoBeforeUpdatedRowArrives() {
        val basicRowMovie = createMovie(id = "101", enriched = false)
        val enrichedFocusedMovie = createMovie(
            id = "101",
            url = "https://images.example/logo-101.png"
        )

        val focusedMovie = resolveFocusedMovie(
            rowMovies = listOf(basicRowMovie),
            focusedMovieId = "101",
            focusedMediaType = MediaType.MOVIE,
            focusedStateMovie = enrichedFocusedMovie
        )

        assertEquals(enrichedFocusedMovie, focusedMovie)
        assertEquals(
            "https://images.example/logo-101.png",
            focusedMovie.toFocusedLogoWarmTarget()?.logoUrl
        )
    }

    @Test
    fun sameMovieWithUnchangedLogoProducesEqualEffectKey() {
        val first = createMovie(id = "101", url = "https://images.example/logo-101.png")
            .toFocusedLogoWarmTarget()
        val recomposed = createMovie(
            id = "101",
            name = "Updated metadata",
            url = "https://images.example/logo-101.png"
        ).toFocusedLogoWarmTarget()

        assertEquals(first, recomposed)
    }

    @Test
    fun changedLogoUrlProducesNewEffectKey() {
        val first = createMovie(id = "101", url = "https://images.example/logo-old.png")
            .toFocusedLogoWarmTarget()
        val updated = createMovie(id = "101", url = "https://images.example/logo-new.png")
            .toFocusedLogoWarmTarget()

        assertNotEquals(first, updated)
    }

    @Test
    fun missingAndBlankLogoUrlsAreNotWarmTargets() {
        assertNull(createMovie(id = "101", url = null).toFocusedLogoWarmTarget())
        assertNull(createMovie(id = "101", url = "  ").toFocusedLogoWarmTarget())
        assertNull(createMovie(id = "101", url = "https://images.example/logo.png", enriched = false)
            .toFocusedLogoWarmTarget())
    }

    @Test
    fun focusMovingBetweenEnrichedMovieAndTvProducesDifferentTargets() {
        val movieTarget = createMovie(
            id = "101",
            type = MediaType.MOVIE,
            url = "https://images.example/movie-logo.png"
        ).toFocusedLogoWarmTarget()
        val tvTarget = createMovie(
            id = "101",
            type = MediaType.TV,
            url = "https://images.example/tv-logo.png"
        ).toFocusedLogoWarmTarget()

        assertNotEquals(movieTarget, tvTarget)
    }

    @Test
    fun duplicateInFlightLogoWarmIsSkippedAndCancellationAllowsRetry() {
        val enqueued = mutableListOf<ImageRequest>()
        val imageLoader = fakeImageLoader(enqueued)
        val context = ContextWrapper(null)
        val logoUrl = "https://images.example/logo-inflight-test.png"

        ImageWarmingManager.warmLogo(context, imageLoader, MediaType.MOVIE, "201", logoUrl)
        ImageWarmingManager.warmLogo(context, imageLoader, MediaType.TV, "202", logoUrl)

        assertEquals(1, enqueued.size)
        assertEquals(logoUrl, enqueued.single().memoryCacheKey?.key)
        assertEquals(logoUrl, enqueued.single().diskCacheKey)

        enqueued.single().listener?.onCancel(enqueued.single())
        ImageWarmingManager.warmLogo(context, imageLoader, MediaType.MOVIE, "201", logoUrl)

        assertEquals(2, enqueued.size)
        enqueued.last().listener?.onCancel(enqueued.last())
    }

    @Test
    fun imageWarmingManager_warm_onlyWarmsLogoForEnrichedMoviesWithLogoUrl() {
        val enqueued = mutableListOf<ImageRequest>()
        val imageLoader = fakeImageLoader(enqueued)
        val context = ContextWrapper(null)

        val enrichedWithLogo = createMovie(id = "1", url = "https://images.example/logo-1.png", enriched = true)
        val enrichedNoLogo = createMovie(id = "2", url = null, enriched = true)
        val unenrichedWithLogo = createMovie(id = "3", url = "https://images.example/logo-3.png", enriched = false)

        ImageWarmingManager.warm(context, imageLoader, listOf(enrichedWithLogo, enrichedNoLogo, unenrichedWithLogo), warmBackdrops = false)

        val logoRequests = enqueued.filter { it.memoryCacheKey?.key?.contains("logo") == true }
        assertEquals(1, logoRequests.size)
        assertEquals("https://images.example/logo-1.png", logoRequests.single().memoryCacheKey?.key)
    }

    @Test
    fun imageWarmingManager_warm_preservesMediaTypeAndIdForLogos() {
        val enqueued = mutableListOf<ImageRequest>()
        val imageLoader = fakeImageLoader(enqueued)
        val context = ContextWrapper(null)

        val movie = createMovie(id = "100", type = MediaType.MOVIE, url = "https://images.example/movie-logo.png", enriched = true)
        val tvShow = createMovie(id = "100", type = MediaType.TV, url = "https://images.example/tv-logo.png", enriched = true)

        ImageWarmingManager.warm(context, imageLoader, listOf(movie, tvShow), warmBackdrops = false)

        val logoRequests = enqueued.filter { it.memoryCacheKey?.key?.contains("logo") == true }
        assertEquals(2, logoRequests.size)
        assertEquals("https://images.example/movie-logo.png", logoRequests[0].memoryCacheKey?.key)
        assertEquals("https://images.example/tv-logo.png", logoRequests[1].memoryCacheKey?.key)
    }



    private fun createMovie(
        id: String,
        name: String = "Title $id",
        type: MediaType = MediaType.MOVIE,
        url: String? = null,
        enriched: Boolean = true
    ): Movie = Movie(
        movieId = id,
        mediaType = type,
        name = name,
        imageUrl = "https://images.example/poster-$id.png",
        backdropUrl = "https://images.example/backdrop-$id.png",
        titleLogoUrl = url,
        isEnriched = enriched
    )

    private fun fakeImageLoader(enqueued: MutableList<ImageRequest>): ImageLoader {
        val disposable = Proxy.newProxyInstance(
            Disposable::class.java.classLoader,
            arrayOf(Disposable::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "isDisposed" -> false
                "dispose" -> null
                "toString" -> "TestDisposable"
                else -> null
            }
        } as Disposable

        return Proxy.newProxyInstance(
            ImageLoader::class.java.classLoader,
            arrayOf(ImageLoader::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "getMemoryCache", "getDiskCache" -> null
                "enqueue" -> {
                    enqueued += args!![0] as ImageRequest
                    disposable
                }
                "toString" -> "TestImageLoader"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.get(0)
                else -> null
            }
        } as ImageLoader
    }
}
