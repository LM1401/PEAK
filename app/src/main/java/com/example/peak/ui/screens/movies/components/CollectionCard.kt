package com.example.peak.ui.screens.movies.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.peak.ui.screens.movies.CollectionItem

/**
 * Cinematic Brand Collection Discovery Tile (Phase 3 Target Design).
 * Renders landscape 2:1 brand discovery cards for Disney, Pixar, Marvel, Star Wars,
 * National Geographic, and Universal with high-contrast brand styling and TV focus glow.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectionCard(
    item: CollectionItem,
    onClick: (CollectionItem) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocus: ((CollectionItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        label = "CollectionCardScale"
    )

    val shape = RoundedCornerShape(12.dp)
    val backgroundBrush = remember(item.id, item.brandKey) {
        getBrandGradient(item.id, item.brandKey)
    }

    Card(
        onClick = { onClick(item) },
        modifier = modifier
            .width(220.dp)
            .height(110.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocus?.invoke(item)
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
                clip = false
            },
        scale = CardDefaults.scale(focusedScale = 1.0f),
        shape = CardDefaults.shape(shape = shape),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color(0xFF00D2FF)),
                inset = 0.dp
            ),
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                inset = 0.dp
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = Color(0xFF00D2FF).copy(alpha = 0.45f),
                elevation = 16.dp
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            // Optional image/backdrop overlay if provided
            if (!item.backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.backdropUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Darkening overlay for logo readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            // Brand Logo / Presentation
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxSize()
                )
            } else {
                // High-fidelity fallback brand presentation matching target reference
                BrandLogoPresentation(item = item)
            }
        }
    }
}

/**
 * Brand-specific custom gradient backgrounds matching the target reference artwork.
 */
private fun getBrandGradient(id: String, brandKey: String): Brush {
    val normalized = (id + "_" + brandKey).lowercase()
    return when {
        normalized.contains("disney") -> Brush.linearGradient(
            colors = listOf(Color(0xFF0A235C), Color(0xFF1B4E9B), Color(0xFF09193D))
        )
        normalized.contains("pixar") -> Brush.linearGradient(
            colors = listOf(Color(0xFF08182B), Color(0xFF0F3A66), Color(0xFF040E1B))
        )
        normalized.contains("marvel") -> Brush.linearGradient(
            colors = listOf(Color(0xFF70080E), Color(0xFFAC181E), Color(0xFF330004))
        )
        normalized.contains("star_wars") || normalized.contains("lucasfilm") -> Brush.linearGradient(
            colors = listOf(Color(0xFF040810), Color(0xFF0E1E30), Color(0xFF020408))
        )
        normalized.contains("nat_geo") || normalized.contains("national") -> Brush.linearGradient(
            colors = listOf(Color(0xFF1C1A14), Color(0xFF332D08), Color(0xFF0F0E0B))
        )
        normalized.contains("universal") -> Brush.linearGradient(
            colors = listOf(Color(0xFF081830), Color(0xFF113259), Color(0xFF030C1A))
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFF0E1C30), Color(0xFF081220))
        )
    }
}

/**
 * High-fidelity brand logo typography presentation matching the target reference.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BrandLogoPresentation(item: CollectionItem) {
    val key = (item.id + "_" + item.brandKey).lowercase()
    when {
        key.contains("disney") -> {
            Text(
                text = "Disney",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp
            )
        }
        key.contains("pixar") -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "P I X A R",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp
                )
            }
        }
        key.contains("marvel") -> {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE50914), RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "MARVEL",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
        key.contains("star_wars") || key.contains("lucasfilm") -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(1.5.dp)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "STAR WARS",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(1.5.dp)
                        .background(Color.White)
                )
            }
        }
        key.contains("nat_geo") || key.contains("national") -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(26.dp)
                        .border(3.dp, Color(0xFFFFCC00), RoundedCornerShape(1.dp))
                )
                Column {
                    Text(
                        text = "NATIONAL",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "GEOGRAPHIC",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
        key.contains("universal") -> {
            Text(
                text = "UNIVERSAL",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp
            )
        }
        else -> {
            Text(
                text = item.name.uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
