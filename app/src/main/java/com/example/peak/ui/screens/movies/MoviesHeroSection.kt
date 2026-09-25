package com.example.peak.ui.screens.movies

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.example.peak.domain.model.Movie
import com.example.peak.ui.components.CinematicTitleView

private val HERO_CONTENT_WIDTH = 640.dp

/**
 * Movies Hero Section UI (Phase 2 Target Design).
 * Displays large cinematic backdrop metadata, title artwork/fallback, metadata badges,
 * concise synopsis, focusable Play and More Info TV buttons, and interactive carousel indicators.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun MoviesHeroSection(
    heroState: HeroSectionState,
    onPlayClick: (Movie?) -> Unit,
    onMoreInfoClick: (Movie?) -> Unit,
    onSelectHeroIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    playButtonFocusRequester: FocusRequester = remember { FocusRequester() },
    moreInfoFocusRequester: FocusRequester = remember { FocusRequester() },
    navFocusRequester: FocusRequester? = null,
    onHeroFocused: (() -> Unit)? = null,
    technicalFlags: TechnicalQualityFlags = TechnicalQualityFlags()
) {
    val currentMovie = heroState.currentMovie

    key(currentMovie?.movieId ?: "hero_loading") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.TopStart
        ) {
            AnimatedContent(
                targetState = currentMovie,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(250))
                },
                label = "MoviesHeroTransition"
            ) { movie ->
                if (movie != null) {
                    Column(
                        modifier = Modifier.width(HERO_CONTENT_WIDTH),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // 1. FEATURED LABEL
                        Text(
                            text = "FEATURED",
                            color = Color(0xFF51B6FF),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. TITLE / TITLE LOGO
                        CinematicTitleView(
                            title = movie.name,
                            titleLogoUrl = movie.titleLogoUrl,
                            isEnriched = movie.isEnriched,
                            maxHeight = 84.dp,
                            maxWidth = 540.dp,
                            alignment = Alignment.CenterStart
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. METADATA ROW & TECHNICAL BADGES
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Year
                            if (movie.year.isNotBlank()) {
                                Text(
                                    text = movie.year,
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Runtime / Duration
                            if (movie.duration.isNotBlank()) {
                                if (movie.year.isNotBlank()) {
                                    Text(text = "•", color = Color.White.copy(alpha = 0.3f))
                                }
                                Text(
                                    text = movie.duration,
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Rating
                            if (movie.rating.isNotBlank()) {
                                Text(text = "•", color = Color.White.copy(alpha = 0.3f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFC107),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = movie.rating,
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Technical Badges (Only displayed when confirmed)
                            if (technicalFlags.has4k) {
                                TechnicalBadge(text = "4K")
                            }
                            if (technicalFlags.hasHdr) {
                                TechnicalBadge(text = "HDR")
                            }
                            if (technicalFlags.hasDolbyVision) {
                                TechnicalBadge(text = "Dolby Vision")
                            }
                            if (technicalFlags.hasDolbyAtmos) {
                                TechnicalBadge(text = "Dolby Atmos")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. SYNOPSIS
                        if (movie.description.isNotBlank()) {
                            Text(
                                text = movie.description,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 22.sp,
                                modifier = Modifier.fillMaxWidth(0.92f)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // 5. ACTION BUTTONS & CAROUSEL PAGINATION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Buttons Group
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // PLAY BUTTON (Primary)
                                MoviesHeroButton(
                                    text = "Play",
                                    icon = Icons.Default.PlayArrow,
                                    isPrimary = true,
                                    onClick = { onPlayClick(movie) },
                                    focusRequester = playButtonFocusRequester,
                                    onFocus = onHeroFocused,
                                    modifier = Modifier.focusProperties {
                                        if (navFocusRequester != null) {
                                            left = navFocusRequester
                                        }
                                        right = moreInfoFocusRequester
                                    }
                                )

                                // MORE INFO BUTTON (Secondary)
                                MoviesHeroButton(
                                    text = "More Info",
                                    icon = Icons.Outlined.Info,
                                    isPrimary = false,
                                    onClick = { onMoreInfoClick(movie) },
                                    focusRequester = moreInfoFocusRequester,
                                    onFocus = onHeroFocused,
                                    modifier = Modifier.focusProperties {
                                        left = playButtonFocusRequester
                                    }
                                )
                            }

                            // Carousel Pagination Indicators
                            if (heroState.featuredMovies.size > 1) {
                                HeroPaginationDots(
                                    count = heroState.featuredMovies.size,
                                    selectedIndex = heroState.selectedIndex,
                                    onSelectIndex = onSelectHeroIndex,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                        }
                    }
                } else if (heroState.isLoading) {
                    // Loading Placeholder
                    Box(
                        modifier = Modifier
                            .width(HERO_CONTENT_WIDTH)
                            .height(260.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Loading Featured Movies...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV Focusable Action Button for Hero Section.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MoviesHeroButton(
    text: String,
    icon: ImageVector,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocus: (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(42.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    onFocus?.invoke()
                }
            },
        colors = ButtonDefaults.colors(
            containerColor = if (isPrimary) Color(0xFFD6E8FF) else Color.White.copy(alpha = 0.12f),
            contentColor = if (isPrimary) Color(0xFF040B16) else Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color(0xFF040B16)
        ),
        scale = ButtonDefaults.scale(focusedScale = 1.05f),
        shape = ButtonDefaults.shape(RoundedCornerShape(21.dp)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Translucent Technical Quality Badge Pill.
 */
@Composable
private fun TechnicalBadge(text: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Interactive Hero Carousel Pagination Indicators.
 */
@Composable
private fun HeroPaginationDots(
    count: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isSelected = index == selectedIndex
            val width by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 6.dp,
                animationSpec = tween(300),
                label = "DotWidth"
            )
            val height by animateDpAsState(
                targetValue = if (isSelected) 10.dp else 6.dp,
                animationSpec = tween(300),
                label = "DotHeight"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                animationSpec = tween(300),
                label = "DotColor"
            )

            Box(
                modifier = Modifier
                    .size(width = width, height = height)
                    .clip(CircleShape)
                    .background(color)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectIndex(index) }
            )
        }
    }
}
