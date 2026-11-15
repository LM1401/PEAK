package com.example.peak.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MovieDtoTest {

    private fun createLogo(
        filePath: String? = "/logo.png",
        iso6391: String? = "en",
        aspectRatio: Double? = 2.5,
        width: Int? = 1000,
        height: Int? = 400
    ): TmdbLogo = TmdbLogo(
        filePath = filePath,
        iso6391 = iso6391,
        aspectRatio = aspectRatio,
        width = width,
        height = height
    )

    // --- 1. FILE ELIGIBILITY ---

    @Test
    fun selectBestTitleLogo_acceptsPngJpgJpegAndUppercase() {
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.png"))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.PNG"))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.jpg"))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.JPG"))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.jpeg"))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.JPEG"))))
    }

    @Test
    fun selectBestTitleLogo_rejectsSvgUnsupportedAndNullFilePaths() {
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.svg"))))
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.SVG"))))
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.gif"))))
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = "/logo.webp"))))
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = null))))
        assertNull(selectBestTitleLogo(listOf(createLogo(filePath = "   "))))
        assertNull(selectBestTitleLogo(emptyList()))
        assertNull(selectBestTitleLogo(null))
    }

    // --- 2. ASPECT-RATIO BOUNDARIES & EXACT TIER SCORING ---

    @Test
    fun selectBestTitleLogo_aspectRatioDisqualificationsAndNullHandling() {
        // Disqualified outside 0.5..8.0
        assertNull(selectBestTitleLogo(listOf(createLogo(aspectRatio = 0.499))))
        assertNull(selectBestTitleLogo(listOf(createLogo(aspectRatio = 8.001))))

        // Null and zero aspect ratios remain eligible
        assertNotNull(selectBestTitleLogo(listOf(createLogo(aspectRatio = null))))
        assertNotNull(selectBestTitleLogo(listOf(createLogo(aspectRatio = 0.0))))
    }

    @Test
    fun selectBestTitleLogo_exactAspectRatioBoundaryTransitions() {
        // Comparative candidates with identical language ("en") and resolution (1000x500)
        // 0.5 vs 0.499: 0.5 is eligible (+20), 0.499 is disqualified (null)
        val ar05 = createLogo(filePath = "/ar05.png", aspectRatio = 0.5)
        val ar0499 = createLogo(filePath = "/ar0499.png", aspectRatio = 0.499)
        assertEquals("https://image.tmdb.org/t/p/w500/ar05.png", selectBestTitleLogo(listOf(ar0499, ar05)))

        // 0.7 (+30, score 160) vs 0.699 (+20, score 150): 0.7 wins
        val ar07 = createLogo(filePath = "/ar07.png", aspectRatio = 0.7)
        val ar0699 = createLogo(filePath = "/ar0699.png", aspectRatio = 0.699)
        assertEquals("https://image.tmdb.org/t/p/w500/ar07.png", selectBestTitleLogo(listOf(ar0699, ar07)))

        // 1.2 (+40, score 170) vs 1.199 (+30, score 160): 1.2 wins
        val ar12 = createLogo(filePath = "/ar12.png", aspectRatio = 1.2)
        val ar1199 = createLogo(filePath = "/ar1199.png", aspectRatio = 1.199)
        assertEquals("https://image.tmdb.org/t/p/w500/ar12.png", selectBestTitleLogo(listOf(ar1199, ar12)))

        // 1.8 (+50, score 180) vs 1.799 (+40, score 170): 1.8 wins
        val ar18 = createLogo(filePath = "/ar18.png", aspectRatio = 1.8)
        val ar1799 = createLogo(filePath = "/ar1799.png", aspectRatio = 1.799)
        assertEquals("https://image.tmdb.org/t/p/w500/ar18.png", selectBestTitleLogo(listOf(ar1799, ar18)))

        // 4.499 (+50, score 180) vs 4.5 (+40, score 170): 4.499 wins
        val ar4499 = createLogo(filePath = "/ar4499.png", aspectRatio = 4.499)
        val ar45 = createLogo(filePath = "/ar45.png", aspectRatio = 4.5)
        assertEquals("https://image.tmdb.org/t/p/w500/ar4499.png", selectBestTitleLogo(listOf(ar45, ar4499)))

        // 5.999 (+40, score 170) vs 6.0 (+30, score 160): 5.999 wins
        val ar5999 = createLogo(filePath = "/ar5999.png", aspectRatio = 5.999)
        val ar60 = createLogo(filePath = "/ar60.png", aspectRatio = 6.0)
        assertEquals("https://image.tmdb.org/t/p/w500/ar5999.png", selectBestTitleLogo(listOf(ar60, ar5999)))

        // 8.0 (+30, score 160) vs 8.001: 8.0 is eligible, 8.001 is disqualified (null)
        val ar80 = createLogo(filePath = "/ar80.png", aspectRatio = 8.0)
        val ar8001 = createLogo(filePath = "/ar8001.png", aspectRatio = 8.001)
        assertEquals("https://image.tmdb.org/t/p/w500/ar80.png", selectBestTitleLogo(listOf(ar8001, ar80)))
    }

    // --- 3. MISSING AND ZERO ASPECT-RATIO SCORING ---

    @Test
    fun selectBestTitleLogo_missingAndZeroAspectRatioScoring() {
        // Isolated aspect ratio comparison with identical language ("en") and no resolution bonus (missing dimensions)
        val nullAr = createLogo(filePath = "/null_ar.png", aspectRatio = null, width = null, height = null) // Score: 100 + 25 + 0 = 125
        val zeroAr = createLogo(filePath = "/zero_ar.png", aspectRatio = 0.0, width = null, height = null) // Score: 100 + 25 + 0 = 125
        val portraitLogo = createLogo(filePath = "/portrait.png", aspectRatio = 0.6, width = null, height = null) // Score: 100 + 20 + 0 = 120
        val squareLogo = createLogo(filePath = "/square.png", aspectRatio = 1.0, width = null, height = null) // Score: 100 + 30 + 0 = 130

        // Missing AR (125) strictly beats Portrait AR 0.6 (120)
        assertEquals("https://image.tmdb.org/t/p/w500/null_ar.png", selectBestTitleLogo(listOf(portraitLogo, nullAr)))

        // Zero AR (125) strictly beats Portrait AR 0.6 (120)
        assertEquals("https://image.tmdb.org/t/p/w500/zero_ar.png", selectBestTitleLogo(listOf(portraitLogo, zeroAr)))

        // Square AR 1.0 (130) strictly beats Missing AR (125)
        assertEquals("https://image.tmdb.org/t/p/w500/square.png", selectBestTitleLogo(listOf(nullAr, squareLogo)))
    }

    // --- 4. RESOLUTION HANDLING & DIMENSION THRESHOLDS ---

    @Test
    fun selectBestTitleLogo_resolutionBonusRequiresBothDimensionsKnownAndPositive() {
        val fullyKnown = createLogo(filePath = "/known.png", width = 2000, height = 1000) // Score: 100 + 50 + 30 = 180
        val missingWidth = createLogo(filePath = "/no_w.png", width = null, height = 1000) // Score: 100 + 50 + 0 = 150
        val missingHeight = createLogo(filePath = "/no_h.png", width = 2000, height = null) // Score: 100 + 50 + 0 = 150
        val zeroWidth = createLogo(filePath = "/zero_w.png", width = 0, height = 1000) // Score: 100 + 50 + 0 = 150
        val zeroHeight = createLogo(filePath = "/zero_h.png", width = 2000, height = 0) // Score: 100 + 50 + 0 = 150

        // Fully known dimensions (+30 res bonus) strictly beats missing/zero dimensions (+0 res bonus)
        assertEquals("https://image.tmdb.org/t/p/w500/known.png", selectBestTitleLogo(listOf(missingWidth, fullyKnown)))
        assertEquals("https://image.tmdb.org/t/p/w500/known.png", selectBestTitleLogo(listOf(missingHeight, fullyKnown)))
        assertEquals("https://image.tmdb.org/t/p/w500/known.png", selectBestTitleLogo(listOf(zeroWidth, fullyKnown)))
        assertEquals("https://image.tmdb.org/t/p/w500/known.png", selectBestTitleLogo(listOf(zeroHeight, fullyKnown)))
    }

    @Test
    fun selectBestTitleLogo_exactResolutionThresholdTransitions() {
        val res199 = createLogo(filePath = "/res199.png", width = 199, height = 199) // Disqualified (tiny thumbnail)
        val res200 = createLogo(filePath = "/res200.png", width = 200, height = 200) // maxDim 200 >= 200 (+10)
        val res499 = createLogo(filePath = "/res499.png", width = 499, height = 499) // maxDim 499 >= 200 (+10)
        val res500 = createLogo(filePath = "/res500.png", width = 500, height = 500) // maxDim 500 >= 500 (+20)
        val res999 = createLogo(filePath = "/res999.png", width = 999, height = 999) // maxDim 999 >= 500 (+20)
        val res1000 = createLogo(filePath = "/res1000.png", width = 1000, height = 1000) // maxDim 1000 >= 1000 (+30)

        // maxDim 199 is disqualified
        assertNull(selectBestTitleLogo(listOf(res199)))
        assertNotNull(selectBestTitleLogo(listOf(res200)))

        // maxDim 500 (+20, score 150) strictly beats maxDim 499 (+10, score 140)
        assertEquals("https://image.tmdb.org/t/p/w500/res500.png", selectBestTitleLogo(listOf(res499, res500)))

        // maxDim 1000 (+30, score 160) strictly beats maxDim 999 (+20, score 150)
        assertEquals("https://image.tmdb.org/t/p/w500/res1000.png", selectBestTitleLogo(listOf(res999, res1000)))
    }

    // --- 5. LANGUAGE SCORING ---

    @Test
    fun selectBestTitleLogo_languageScoringPreferences() {
        val englishLogo = createLogo(filePath = "/en.png", iso6391 = "en")
        val neutralLogo = createLogo(filePath = "/neutral.png", iso6391 = null)
        val foreignLogo = createLogo(filePath = "/ja.png", iso6391 = "ja")

        // English preferred over neutral
        assertEquals("https://image.tmdb.org/t/p/w500/en.png", selectBestTitleLogo(listOf(neutralLogo, englishLogo)))

        // Neutral preferred over foreign
        assertEquals("https://image.tmdb.org/t/p/w500/neutral.png", selectBestTitleLogo(listOf(foreignLogo, neutralLogo)))

        // Foreign logo eligible if it is the only option
        assertEquals("https://image.tmdb.org/t/p/w500/ja.png", selectBestTitleLogo(listOf(foreignLogo)))
    }

    @Test
    fun selectBestTitleLogo_highQualityNeutralBeatsLowQualityEnglish() {
        // High quality neutral logo (AR 2.5 optimal, 2000x800) -> 70 (neutral) + 50 (AR) + 30 (res) = 150
        val highResNeutral = createLogo(filePath = "/neutral.png", iso6391 = null, aspectRatio = 2.5, width = 2000, height = 800)

        // Low quality English logo (AR 0.8 stacked, 200x200) -> 100 (en) + 30 (AR) + 10 (res) = 140
        val lowResEnglish = createLogo(filePath = "/en_low.png", iso6391 = "en", aspectRatio = 0.8, width = 200, height = 200)

        // High quality neutral logo (150) strictly beats low quality English logo (140)
        assertEquals("https://image.tmdb.org/t/p/w500/neutral.png", selectBestTitleLogo(listOf(lowResEnglish, highResNeutral)))
    }

    // --- 6. SELECTION AND TIE-BREAKING ---

    @Test
    fun selectBestTitleLogo_deterministicTieBreaker_usesPixelAreaThenFilePath() {
        // Identical score, larger surface area wins
        val smallerArea = createLogo(filePath = "/b.png", width = 1000, height = 400) // Area = 400,000
        val largerArea = createLogo(filePath = "/a.png", width = 1200, height = 500)  // Area = 600,000

        assertEquals("https://image.tmdb.org/t/p/w500/a.png", selectBestTitleLogo(listOf(smallerArea, largerArea)))

        // Identical score and identical pixel area -> File path string tie-breaker
        val logoA = createLogo(filePath = "/a_logo.png", width = 1000, height = 400)
        val logoB = createLogo(filePath = "/b_logo.png", width = 1000, height = 400)

        assertEquals("https://image.tmdb.org/t/p/w500/b_logo.png", selectBestTitleLogo(listOf(logoA, logoB)))
    }

    // --- 7. REGRESSION EXAMPLES ---

    @Test
    fun selectBestTitleLogo_beverlyHills90210_selectsEnglishPng() {
        val svgLogo = createLogo(filePath = "/wquBG9jZFhZ7It3Nd1l5xK2cM1M.svg", iso6391 = null, aspectRatio = 1.0, width = 800, height = 800)
        val neutralPng = createLogo(filePath = "/4cpZr6fqMMDaGBZEEjwZHaDBEjU.png", iso6391 = null, aspectRatio = 1.0, width = 6938, height = 6939)
        val englishPng = createLogo(filePath = "/87opWwLd7dHUE0IW6tXHlopWe3z.png", iso6391 = "en", aspectRatio = 1.087, width = 288, height = 265)

        // English PNG (Score 140) beats Neutral 7K PNG (Score 130). SVG excluded.
        assertEquals("https://image.tmdb.org/t/p/w500/87opWwLd7dHUE0IW6tXHlopWe3z.png", selectBestTitleLogo(listOf(svgLogo, neutralPng, englishPng)))
    }

    @Test
    fun selectBestTitleLogo_ringsOfPower_selectsHighResPngLogo() {
        val wideBanner = createLogo(filePath = "/2uDS3VQIDlNnwq3Mp8BsC7igftU.png", iso6391 = "en", aspectRatio = 6.047, width = 6434, height = 1064)
        val stackedBadge = createLogo(filePath = "/d4HKgGW4BJJDGeX41PFid9ay44P.png", iso6391 = "en", aspectRatio = 1.147, width = 2857, height = 2491)

        // Stacked badge wins via larger pixel surface area (7.1M px² vs 6.8M px²)
        assertEquals("https://image.tmdb.org/t/p/w500/d4HKgGW4BJJDGeX41PFid9ay44P.png", selectBestTitleLogo(listOf(wideBanner, stackedBadge)))
    }

    @Test
    fun selectBestTitleLogo_starVsTheForcesOfEvil_selectsStackedLogo() {
        val stackedLogo = createLogo(filePath = "/qKYVejnkc8NFAluPNlxW7kdvFSC.png", iso6391 = "en", aspectRatio = 0.868, width = 2838, height = 3270)

        assertEquals("https://image.tmdb.org/t/p/w500/qKYVejnkc8NFAluPNlxW7kdvFSC.png", selectBestTitleLogo(listOf(stackedLogo)))
    }

    @Test
    fun selectBestTitleLogo_wednesday_selectsOptimalPngLogo() {
        val svgLogo = createLogo(filePath = "/2ns0b8dB0wV6Aebe0wI0LBzkoNN.svg", iso6391 = "en", aspectRatio = 3.893, width = 1900, height = 488)
        val pngLogo = createLogo(filePath = "/m5nR52ErmLa0fkY4ItBuYaQuXJJ.png", iso6391 = "en", aspectRatio = 3.781, width = 847, height = 224)

        // Renderable PNG selected, SVG excluded
        assertEquals("https://image.tmdb.org/t/p/w500/m5nR52ErmLa0fkY4ItBuYaQuXJJ.png", selectBestTitleLogo(listOf(svgLogo, pngLogo)))
    }

    @Test
    fun selectBestTitleLogo_loki_selectsOptimalPngLogo() {
        val pngLogo = createLogo(filePath = "/dCfq8mTf4JRuOzP0BWLq463Iyls.png", iso6391 = "en", aspectRatio = 2.041, width = 1753, height = 859)

        assertEquals("https://image.tmdb.org/t/p/w500/dCfq8mTf4JRuOzP0BWLq463Iyls.png", selectBestTitleLogo(listOf(pngLogo)))
    }

    @Test
    fun selectBestTitleLogo_strangerThings_selectsOptimalPngLogo() {
        val svgLogo = createLogo(filePath = "/e9qFk5zrKZaB2IbbOI9N0W77vlh.svg", iso6391 = "en", aspectRatio = 2.765, width = 1200, height = 434)
        val pngLogo = createLogo(filePath = "/uyVM5qGksUzCgwo6UU0UrHex8Oj.png", iso6391 = "en", aspectRatio = 2.689, width = 3781, height = 1406)

        // Renderable PNG selected, SVG excluded
        assertEquals("https://image.tmdb.org/t/p/w500/uyVM5qGksUzCgwo6UU0UrHex8Oj.png", selectBestTitleLogo(listOf(svgLogo, pngLogo)))
    }

    @Test
    fun selectBestTitleLogo_avatar_preservesOptimalHorizontalLogoOverBanner() {
        val horizontalLogo = createLogo(filePath = "/eYBa9fpSvZEYOl88ulP5fnRoCIi.png", iso6391 = "en", aspectRatio = 4.863, width = 1600, height = 329)
        val bannerLogo = createLogo(filePath = "/bET8bJdM8diTCpUVilWqqu0M3EJ.png", iso6391 = "en", aspectRatio = 6.102, width = 4314, height = 707)

        // Horizontal Tier 2 logo (Score 170) strictly beats Ultra-Wide Banner (Score 160)
        assertEquals("https://image.tmdb.org/t/p/w500/eYBa9fpSvZEYOl88ulP5fnRoCIi.png", selectBestTitleLogo(listOf(bannerLogo, horizontalLogo)))
    }

    @Test
    fun selectBestTitleLogo_fightClub_preservesOptimalHorizontalLogoOverBadge() {
        val horizontalLogo = createLogo(filePath = "/7Uqhv24pGJs4Ns31NoOPWFJGWNG.png", iso6391 = "en", aspectRatio = 4.638, width = 1804, height = 389)
        val stackedBadge = createLogo(filePath = "/4XkF0Rf7gvSfea8fYLFbU5tmuJw.png", iso6391 = "en", aspectRatio = 0.848, width = 1098, height = 1295)

        // Horizontal Tier 2 logo (Score 170) strictly beats Stacked Badge (Score 160)
        assertEquals("https://image.tmdb.org/t/p/w500/7Uqhv24pGJs4Ns31NoOPWFJGWNG.png", selectBestTitleLogo(listOf(stackedBadge, horizontalLogo)))
    }
}
