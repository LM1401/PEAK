package com.example.peak.ui.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoTrimTransparentBordersTransformationTest {

    private fun createPixels(
        width: Int,
        height: Int,
        baseAlpha: Int = 0,
        visibleLeft: Int = -1,
        visibleTop: Int = -1,
        visibleRight: Int = -1,
        visibleBottom: Int = -1,
        visibleAlpha: Int = 255
    ): IntArray {
        val pixels = IntArray(width * height) { (baseAlpha shl 24) or 0x00FFFFFF }
        if (visibleLeft >= 0 && visibleTop >= 0 && visibleRight >= visibleLeft && visibleBottom >= visibleTop) {
            for (y in visibleTop..visibleBottom) {
                for (x in visibleLeft..visibleRight) {
                    pixels[y * width + x] = (visibleAlpha shl 24) or 0x00FFFFFF
                }
            }
        }
        return pixels
    }

    // Reference Brute-Force Bounding Box implementation for verification against perimeter scan
    private fun referenceBruteForceCrop(
        pixels: IntArray,
        width: Int,
        height: Int,
        alphaThreshold: Int
    ): IntArray? {
        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1
        var found = false

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xFF
                if (alpha > alphaThreshold) {
                    found = true
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (found) intArrayOf(minX, maxX, minY, maxY) else null
    }

    // -----------------------------------------------------------------------------------------
    // CRITICAL INVESTIGATION A: DETACHED ARTWORK & MULTI-GROUP COMPONENT TESTS
    // -----------------------------------------------------------------------------------------

    @Test
    fun calculateCrop_detachedArtworkAtCorners_preservesAllDetachedElements() {
        val width = 200
        val height = 200
        val pixels = IntArray(width * height)

        // Main Title in center: x:80..120, y:80..120
        for (y in 80..120) for (x in 80..120) pixels[y * width + x] = (255 shl 24)
        // Detached star icon at top-left: x:15..25, y:10..20
        for (y in 10..20) for (x in 15..25) pixels[y * width + x] = (255 shl 24)
        // Detached copyright symbol at bottom-right: x:175..185, y:180..190
        for (y in 180..190) for (x in 175..185) pixels[y * width + x] = (255 shl 24)

        val result = calculateCrop(pixels, width, height, alphaThreshold = 16, paddingPx = 0, minAreaSavedPct = 1.0f)
        val ref = referenceBruteForceCrop(pixels, width, height, alphaThreshold = 16)

        assertNotNull(ref)
        assertEquals(ref!![0], result.minX) // 15
        assertEquals(ref[1], result.maxX) // 185
        assertEquals(ref[2], result.minY) // 10
        assertEquals(ref[3], result.maxY) // 190

        assertTrue(result.shouldTrim)
        // Ensure detached top-left star and bottom-right copyright are inside crop bounds
        assertTrue(result.cropLeft <= 15)
        assertTrue(result.cropRight >= 186)
        assertTrue(result.cropTop <= 10)
        assertTrue(result.cropBottom >= 191)
    }

    @Test
    fun calculateCrop_isolatedPixelParticles_matchesReferenceBruteForceScan() {
        val width = 300
        val height = 150
        val pixels = IntArray(width * height)

        // Floating isolated pixels
        pixels[5 * width + 12] = (200 shl 24)  // Far top-left
        pixels[145 * width + 289] = (220 shl 24) // Far bottom-right
        pixels[75 * width + 150] = (255 shl 24)  // Center

        val result = calculateCrop(pixels, width, height, alphaThreshold = 16, paddingPx = 2, minAreaSavedPct = 1.0f)
        val ref = referenceBruteForceCrop(pixels, width, height, alphaThreshold = 16)

        assertNotNull(ref)
        assertEquals(ref!![0], result.minX) // 12
        assertEquals(ref[1], result.maxX) // 289
        assertEquals(ref[2], result.minY) // 5
        assertEquals(ref[3], result.maxY) // 145
    }

    // -----------------------------------------------------------------------------------------
    // CRITICAL INVESTIGATION B: CACHE KEY PRECISION & INTEGRATION PARITY
    // -----------------------------------------------------------------------------------------

    @Test
    fun getLogoMemoryCacheKey_producesIdenticalKeysForWarmingAndDisplayRequests() {
        val logoUrl = "https://image.tmdb.org/t/p/w500/hlnyMaUxeFJTb3QL1hKpHwhWowC.png"
        val transformation = AutoTrimTransparentBordersTransformation()

        // Key generated in ImageWarmingManager
        val warmingMemoryKey = PeakImageLoader.getLogoMemoryCacheKey(logoUrl, transformation.cacheKey)

        // Key generated in CinematicTitleView
        val displayMemoryKey = PeakImageLoader.getLogoMemoryCacheKey(logoUrl, transformation.cacheKey)

        assertEquals("https://image.tmdb.org/t/p/w500/hlnyMaUxeFJTb3QL1hKpHwhWowC.png_auto_trim_v3_th16_pad4_minArea5.0", warmingMemoryKey)
        assertEquals(warmingMemoryKey, displayMemoryKey)
    }

    @Test
    fun cacheKey_floatPrecision_differentiatesFractionalAreaThresholds() {
        val t1 = AutoTrimTransparentBordersTransformation(minAreaSavedPct = 5.1f)
        val t2 = AutoTrimTransparentBordersTransformation(minAreaSavedPct = 5.9f)
        val t3 = AutoTrimTransparentBordersTransformation(minAreaSavedPct = 5.0f)

        assertEquals("auto_trim_v3_th16_pad4_minArea5.1", t1.cacheKey)
        assertEquals("auto_trim_v3_th16_pad4_minArea5.9", t2.cacheKey)
        assertEquals("auto_trim_v3_th16_pad4_minArea5.0", t3.cacheKey)

        assertTrue(t1.cacheKey != t2.cacheKey)
        assertTrue(t1.cacheKey != t3.cacheKey)
    }

    @Test
    fun constructor_invalidParameterValues_areSafelyCoerced() {
        val tNegative = AutoTrimTransparentBordersTransformation(alphaThreshold = -50, paddingPx = -10, minAreaSavedPct = -20.0f)
        assertEquals(0, tNegative.alphaThreshold)
        assertEquals(0, tNegative.paddingPx)
        assertEquals(0.0f, tNegative.minAreaSavedPct, 0.001f)

        val tExcessive = AutoTrimTransparentBordersTransformation(alphaThreshold = 500, paddingPx = 4, minAreaSavedPct = 150.0f)
        assertEquals(255, tExcessive.alphaThreshold)
        assertEquals(100.0f, tExcessive.minAreaSavedPct, 0.001f)
    }

    // -----------------------------------------------------------------------------------------
    // STAGE 10D TEST SCENARIOS (RE-VERIFIED IN STAGE 10E)
    // -----------------------------------------------------------------------------------------

    @Test
    fun calculateCrop_fullyTransparentBitmap_returnsShouldTrimFalse() {
        val pixels = createPixels(100, 100, baseAlpha = 0)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
        assertEquals(100, result.cropWidth)
        assertEquals(100, result.cropHeight)
    }

    @Test
    fun calculateCrop_fullyOpaqueBitmap_returnsShouldTrimFalse() {
        val pixels = createPixels(100, 100, baseAlpha = 255)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
        assertEquals(0, result.cropLeft)
        assertEquals(0, result.cropTop)
        assertEquals(100, result.cropWidth)
        assertEquals(100, result.cropHeight)
    }

    @Test
    fun calculateCrop_noTransparentBorders_returnsShouldTrimFalse() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 0, visibleTop = 0, visibleRight = 99, visibleBottom = 99)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
        assertEquals(100, result.cropWidth)
        assertEquals(100, result.cropHeight)
    }

    @Test
    fun calculateCrop_transparentBordersOnAllFourSides_trimsCorrectly() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 25, visibleTop = 25, visibleRight = 74, visibleBottom = 74)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(25, result.minX)
        assertEquals(74, result.maxX)
        assertEquals(25, result.minY)
        assertEquals(74, result.maxY)
        assertEquals(21, result.cropLeft)
        assertEquals(21, result.cropTop)
        assertEquals(79, result.cropRight)
        assertEquals(79, result.cropBottom)
        assertEquals(58, result.cropWidth)
        assertEquals(58, result.cropHeight)
    }

    @Test
    fun calculateCrop_horizontalOnlyPadding_trimsHorizontally() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 30, visibleTop = 0, visibleRight = 69, visibleBottom = 99)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(26, result.cropLeft)
        assertEquals(0, result.cropTop)
        assertEquals(74, result.cropRight)
        assertEquals(100, result.cropBottom)
        assertEquals(48, result.cropWidth)
        assertEquals(100, result.cropHeight)
    }

    @Test
    fun calculateCrop_verticalOnlyPadding_trimsVertically() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 0, visibleTop = 30, visibleRight = 99, visibleBottom = 69)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(0, result.cropLeft)
        assertEquals(26, result.cropTop)
        assertEquals(100, result.cropRight)
        assertEquals(74, result.cropBottom)
        assertEquals(100, result.cropWidth)
        assertEquals(48, result.cropHeight)
    }

    @Test
    fun calculateCrop_asymmetricPadding_trimsAsymmetrically() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 5, visibleTop = 10, visibleRight = 49, visibleBottom = 69)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(5, result.minX)
        assertEquals(49, result.maxX)
        assertEquals(10, result.minY)
        assertEquals(69, result.maxY)
        assertEquals(1, result.cropLeft)
        assertEquals(6, result.cropTop)
        assertEquals(54, result.cropRight)
        assertEquals(74, result.cropBottom)
    }

    @Test
    fun calculateCrop_singlePixelVisibleContent_cropsToSinglePixelWithPadding() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 50, visibleTop = 50, visibleRight = 50, visibleBottom = 50)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(50, result.minX)
        assertEquals(50, result.maxX)
        assertEquals(50, result.minY)
        assertEquals(50, result.maxY)
        assertEquals(46, result.cropLeft)
        assertEquals(46, result.cropTop)
        assertEquals(55, result.cropRight)
        assertEquals(55, result.cropBottom)
    }

    @Test
    fun calculateCrop_veryNarrowLogo_trimsCorrectly() {
        val pixels = createPixels(500, 300, baseAlpha = 0, visibleLeft = 245, visibleTop = 50, visibleRight = 254, visibleBottom = 249)
        val result = calculateCrop(pixels, 500, 300, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(241, result.cropLeft)
        assertEquals(46, result.cropTop)
        assertEquals(259, result.cropRight)
        assertEquals(254, result.cropBottom)
    }

    @Test
    fun calculateCrop_veryWideLogo_trimsCorrectly() {
        val pixels = createPixels(500, 300, baseAlpha = 0, visibleLeft = 25, visibleTop = 142, visibleRight = 474, visibleBottom = 156)
        val result = calculateCrop(pixels, 500, 300, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(21, result.cropLeft)
        assertEquals(138, result.cropTop)
        assertEquals(479, result.cropRight)
        assertEquals(161, result.cropBottom)
    }

    @Test
    fun calculateCrop_onePixelCanvas_returnsShouldTrimFalse() {
        val pixelsOpaque = intArrayOf((255 shl 24) or 0x00FFFFFF)
        val resultOpaque = calculateCrop(pixelsOpaque, 1, 1, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)
        assertFalse(resultOpaque.shouldTrim)

        val pixelsTransparent = intArrayOf(0)
        val resultTransparent = calculateCrop(pixelsTransparent, 1, 1, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)
        assertFalse(resultTransparent.shouldTrim)
    }

    @Test
    fun calculateCrop_evaluatesAlphaThresholdsCorrectly() {
        val pixels = IntArray(100 * 100)
        for (y in 10..20) for (x in 10..20) pixels[y * 100 + x] = (10 shl 24)
        for (y in 30..40) for (x in 30..40) pixels[y * 100 + x] = (30 shl 24)

        val res16 = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 0, minAreaSavedPct = 1.0f)
        assertEquals(30, res16.minX)
        assertEquals(40, res16.maxX)

        val res5 = calculateCrop(pixels, 100, 100, alphaThreshold = 5, paddingPx = 0, minAreaSavedPct = 1.0f)
        assertEquals(10, res5.minX)
        assertEquals(40, res5.maxX)
    }

    @Test
    fun calculateCrop_evaluatesSafetyMarginsCorrectly() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 30, visibleTop = 30, visibleRight = 69, visibleBottom = 69)

        val res0 = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 0, minAreaSavedPct = 5.0f)
        assertEquals(30, res0.cropLeft)
        assertEquals(70, res0.cropRight)

        val res8 = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 8, minAreaSavedPct = 5.0f)
        assertEquals(22, res8.cropLeft)
        assertEquals(78, res8.cropRight)
    }

    @Test
    fun calculateCrop_areaSavedBelowThreshold_returnsShouldTrimFalse() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 1, visibleTop = 1, visibleRight = 98, visibleBottom = 98)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 0, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
    }

    @Test
    fun calculateCrop_areaSavedAboveThreshold_returnsShouldTrimTrue() {
        val pixels = createPixels(100, 100, baseAlpha = 0, visibleLeft = 10, visibleTop = 10, visibleRight = 89, visibleBottom = 89)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 0, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
    }

    @Test
    fun calculateCrop_invalidDimensions_returnsShouldTrimFalse() {
        val pixels = IntArray(0)
        val result = calculateCrop(pixels, 0, 0, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
    }

    @Test
    fun calculateCrop_verifiesExactCoordinatesAndDimensions() {
        val pixels = createPixels(200, 100, baseAlpha = 0, visibleLeft = 40, visibleTop = 20, visibleRight = 159, visibleBottom = 79)
        val result = calculateCrop(pixels, 200, 100, alphaThreshold = 16, paddingPx = 5, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        assertEquals(40, result.minX)
        assertEquals(159, result.maxX)
        assertEquals(20, result.minY)
        assertEquals(79, result.maxY)
        assertEquals(35, result.cropLeft)
        assertEquals(15, result.cropTop)
        assertEquals(165, result.cropRight)
        assertEquals(85, result.cropBottom)
    }

    @Test
    fun calculateCrop_preservesAllVisiblePixelsInsideCropBounds() {
        val width = 100
        val height = 100
        val pixels = createPixels(width, height, baseAlpha = 0, visibleLeft = 30, visibleTop = 25, visibleRight = 70, visibleBottom = 75)

        val result = calculateCrop(pixels, width, height, alphaThreshold = 16, paddingPx = 2, minAreaSavedPct = 5.0f)

        assertTrue(result.shouldTrim)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xFF
                if (alpha > 16) {
                    assertTrue(x >= result.cropLeft && x < result.cropRight)
                    assertTrue(y >= result.cropTop && y < result.cropBottom)
                }
            }
        }
    }

    @Test
    fun calculateCrop_noCropNecessary_returnsShouldTrimFalse() {
        val pixels = createPixels(100, 100, baseAlpha = 255)
        val result = calculateCrop(pixels, 100, 100, alphaThreshold = 16, paddingPx = 4, minAreaSavedPct = 5.0f)

        assertFalse(result.shouldTrim)
    }
}
