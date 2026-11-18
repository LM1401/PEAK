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
import com.example.peak.ui.image.AutoTrimTransparentBordersTransformation
import com.example.peak.ui.image.HeroLogoTrace
import com.example.peak.ui.image.PeakImageLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Stage 8B: Calculates adaptive logo presentation height based on aspect ratio geometry.
 */
fun calculateEffectiveMaxHeight(
    aspectRatio: Float?,
    maxHeight: Dp = 70.dp
): Dp = when {
    aspectRatio != null && aspectRatio.isNaN() -> maxHeight
    aspectRatio != null && aspectRatio >= 2.5f -> maxHeight
    aspectRatio != null && aspectRatio >= 1.8f -> maxOf(maxHeight, 80.dp)
    aspectRatio != null && aspectRatio >= 1.4f -> maxOf(maxHeight, 88.dp)
    aspectRatio != null && aspectRatio > 0.0f -> maxOf(maxHeight, 96.dp)
    else -> maxHeight
}

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
    @Suppress("UNUSED_PARAMETER") isEnriched: Boolean,
    mediaType: MediaType? = null,
    mediaId: String? = null,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 70.dp,
    maxWidth: Dp = 520.dp,
    alignment: Alignment = Alignment.CenterStart,
    aspectRatio: Float? = null
) {
    val context = LocalContext.current
    val imageLoader = remember { PeakImageLoader.getInstance(context) }
    var isLogoError by remember(titleLogoUrl, title) { mutableStateOf(false) }
    var isLogoLoaded by remember(titleLogoUrl, title) { mutableStateOf(false) }
    var retryTrigger by remember(titleLogoUrl, title) { mutableIntStateOf(0) }
    val traceIdentity = rememberUpdatedState(mediaType to mediaId)
    val presentationLogged = remember(titleLogoUrl, mediaType, mediaId) { AtomicBoolean(false) }

    val hasLogoUrl = !titleLogoUrl.isNullOrBlank()

    // Stage 8B: Calculate adaptive height based on aspect ratio geometry
    val effectiveMaxHeight = calculateEffectiveMaxHeight(aspectRatio, maxHeight)

    // Bounded auto-retry for transient cold-boot image failures (max 2 retries, 1.5s delay)
    LaunchedEffect(titleLogoUrl, title, isLogoError, retryTrigger) {
        if (hasLogoUrl && isLogoError && retryTrigger < 2) {
            kotlinx.coroutines.delay(1500L)
            isLogoError = false
            retryTrigger++
        }
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp, max = effectiveMaxHeight)
            .widthIn(max = maxWidth),
        contentAlignment = alignment
    ) {
        if (hasLogoUrl && (!isLogoError || retryTrigger < 2)) {
            val transformation = remember { AutoTrimTransparentBordersTransformation() }
            val logoMemoryKey = remember(titleLogoUrl) {
                if (titleLogoUrl.isNullOrBlank()) null
                else PeakImageLoader.getLogoMemoryCacheKey(titleLogoUrl, transformation.cacheKey)
            }

            AsyncImage(
                model = remember(titleLogoUrl, retryTrigger) {
                    ImageRequest.Builder(context)
                        .data(titleLogoUrl)
                        .memoryCacheKey(logoMemoryKey)
                        .diskCacheKey(titleLogoUrl)
                        .transformations(transformation)
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
                onSuccess = {
                    isLogoLoaded = true
                    isLogoError = false
                },
                onError = { isLogoError = true },
                modifier = Modifier
                    .heightIn(max = effectiveMaxHeight)
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
        }
    }
}
