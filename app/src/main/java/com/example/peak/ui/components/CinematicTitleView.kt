package com.example.peak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Unified PEAK Cinematic Title Presentation View.
 * Renders official TMDB title logos when available and suitable, or falls back to
 * a dynamic PEAK Cinematic Text Fallback treatment.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinematicTitleView(
    title: String,
    titleLogoUrl: String?,
    isEnriched: Boolean,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 70.dp,
    maxWidth: Dp = 520.dp,
    alignment: Alignment = Alignment.CenterStart
) {
    val context = LocalContext.current
    var isLogoError by remember(title, titleLogoUrl) { mutableStateOf(false) }

    val hasLogoUrl = !titleLogoUrl.isNullOrBlank()
    val isPendingEnrichment = !isEnriched && !hasLogoUrl

    Box(
        modifier = modifier
            .heightIn(min = 52.dp, max = maxHeight)
            .widthIn(max = maxWidth),
        contentAlignment = alignment
    ) {
        when {
            hasLogoUrl && !isLogoError -> {
                AsyncImage(
                    model = remember(title, titleLogoUrl) {
                        ImageRequest.Builder(context)
                            .data(titleLogoUrl)
                            .crossfade(false)
                            .allowHardware(true)
                            .build()
                    },
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    alignment = alignment,
                    onError = { isLogoError = true },
                    modifier = Modifier
                        .heightIn(max = maxHeight)
                        .widthIn(max = maxWidth)
                )
            }
            isPendingEnrichment && !isLogoError -> {
                // Stable placeholder space while lazy enrichment completes in the background.
                // Prevents flashing plain-text titles prior to logo availability.
            }
            else -> {
                // PEAK CINEMATIC TEXT FALLBACK
                CinematicTextFallback(
                    title = title,
                    maxHeight = maxHeight,
                    maxWidth = maxWidth
                )
            }
        }
    }
}

/**
 * PEAK Cinematic Text Fallback.
 * Generates an authoritative title presentation with dynamic typography scaling,
 * tracking, and depth shadow matching PEAK's visual language.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinematicTextFallback(
    title: String,
    maxHeight: Dp,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    val cleanTitle = title.uppercase().trim()
    val charCount = cleanTitle.length

    // Dynamic typography scaling based on title length
    val (fontSize, lineHeight, letterSpacing, maxLines) = when {
        charCount <= 12 -> Quadruple(44.sp, 48.sp, 2.5.sp, 1)
        charCount <= 25 -> Quadruple(36.sp, 40.sp, 2.0.sp, 1)
        else -> Quadruple(28.sp, 32.sp, 1.5.sp, 2)
    }

    val cinematicShadow = Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        offset = Offset(2f, 3f),
        blurRadius = 8f
    )

    Text(
        text = cleanTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = letterSpacing,
            shadow = cinematicShadow
        ),
        color = Color.White,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .heightIn(max = maxHeight)
            .widthIn(max = maxWidth)
    )
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
