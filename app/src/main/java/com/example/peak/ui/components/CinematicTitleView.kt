package com.example.peak.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
import com.example.peak.domain.model.MediaType
import com.example.peak.ui.image.HeroLogoTrace
import com.example.peak.ui.image.PeakImageLoader
import java.util.concurrent.atomic.AtomicBoolean

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
    mediaType: MediaType? = null,
    mediaId: String? = null,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 70.dp,
    maxWidth: Dp = 520.dp,
    alignment: Alignment = Alignment.CenterStart
) {
    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    var isLogoError by remember(titleLogoUrl, title) { mutableStateOf(false) }
    var isLogoLoaded by remember(titleLogoUrl, title) { mutableStateOf(false) }
    val traceIdentity = rememberUpdatedState(mediaType to mediaId)
    val presentationLogged = remember(titleLogoUrl, mediaType, mediaId) { AtomicBoolean(false) }

    val hasLogoUrl = !titleLogoUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .heightIn(min = 48.dp, max = maxHeight)
            .widthIn(max = maxWidth),
        contentAlignment = alignment
    ) {
        if (hasLogoUrl && !isLogoError) {
            AsyncImage(
                model = remember(titleLogoUrl) {
                    ImageRequest.Builder(context)
                        .data(titleLogoUrl)
                        .memoryCacheKey(titleLogoUrl)
                        .diskCacheKey(titleLogoUrl)
                        .crossfade(150)
                        .allowHardware(true)
                        .listener(
                            onStart = {
                                traceIdentity.value.let { (type, id) ->
                                    if (type != null && id != null) {
                                        HeroLogoTrace.record(
                                            event = HeroLogoTrace.Event.HERO_IMAGE_STARTED,
                                            mediaType = type,
                                            mediaId = id
                                        )
                                    }
                                }
                            },
                            onSuccess = { _, result ->
                                traceIdentity.value.let { (type, id) ->
                                    if (type != null && id != null) {
                                        HeroLogoTrace.record(
                                            event = HeroLogoTrace.Event.HERO_IMAGE_SUCCEEDED,
                                            mediaType = type,
                                            mediaId = id,
                                            imageSource = HeroLogoTrace.ImageSource.fromCoilName(result.dataSource.name)
                                        )
                                    }
                                }
                            },
                            onError = { _, _ ->
                                traceIdentity.value.let { (type, id) ->
                                    if (type != null && id != null) {
                                        HeroLogoTrace.record(
                                            event = HeroLogoTrace.Event.HERO_IMAGE_FAILED,
                                            mediaType = type,
                                            mediaId = id
                                        )
                                    }
                                }
                            },
                            onCancel = {
                                traceIdentity.value.let { (type, id) ->
                                    if (type != null && id != null) {
                                        HeroLogoTrace.record(
                                            event = HeroLogoTrace.Event.HERO_IMAGE_CANCELLED,
                                            mediaType = type,
                                            mediaId = id,
                                            outcome = HeroLogoTrace.Outcome.CANCELLED
                                        )
                                    }
                                }
                            }
                        )
                        .build()
                },
                imageLoader = imageLoader,
                contentDescription = title,
                contentScale = ContentScale.Fit,
                alignment = alignment,
                onSuccess = { isLogoLoaded = true },
                onError = { isLogoError = true },
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .widthIn(max = maxWidth)
                    .drawWithContent {
                        drawContent()
                        val (type, id) = traceIdentity.value
                        if (isLogoLoaded && type != null && id != null &&
                            presentationLogged.compareAndSet(false, true)
                        ) {
                            HeroLogoTrace.record(
                                event = HeroLogoTrace.Event.LOGO_FIRST_DRAWN,
                                mediaType = type,
                                mediaId = id
                            )
                        }
                    }
            )
        } else if (isEnriched) {
            // PEAK CINEMATIC TEXT FALLBACK (Only when metadata enrichment is complete and no valid logo exists)
            CinematicTextFallback(
                title = title,
                maxHeight = maxHeight,
                maxWidth = maxWidth
            )
        } else {
            // Metadata enrichment is pending: Reserve layout bounds without showing basic text prematurely
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
