package com.tvpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.app.ui.theme.*
import com.tvpro.app.ui.theme.LaMovieCardBg

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvTop10Card(
    movie: MovieItem,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)
    val cleanTitle = remember(movie.title) { movie.title.cleanTitle() }

    // Unified High-End Polished Metallic Silver Finish for ALL Ranks (1 to 10)
    val silverFill = remember {
        listOf(
            Color(0xFFFFFFFF), // Specular pure white highlight
            Color(0xFFF1F5F9), // Brilliant platinum
            Color(0xFFE2E8F0), // Polished chrome sheen
            Color(0xFFCBD5E1), // Light metallic silver
            Color(0xFF94A3B8), // Classic steel silver
            Color(0xFF64748B), // Slate chrome
            Color(0xFF334155)  // Deep metallic graphite base
        )
    }

    val silverStroke = if (isFocused) Color(0xFFFFFFFF) else Color(0xFFE2E8F0).copy(alpha = 0.90f)
    val glowColor = if (isFocused) Color(0xFFFFFFFF) else Color(0xFFCBD5E1)
    val posterBorderColor = if (isFocused) Color(0xFFFFFFFF) else Color.White.copy(alpha = 0.16f)

    Card(
        onClick = onClick,
        modifier = modifier
            .wrapContentWidth()
            .height(252.dp)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .zIndex(if (isFocused) 20f else 1f)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
        shape = CardDefaults.shape(shape = cardShape),
        scale = CardDefaults.scale(scale = 1.0f, focusedScale = 1.03f, pressedScale = 1.0f),
        border = CardDefaults.border(
            focusedBorder = Border.None,
            border = Border.None
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .padding(horizontal = 4.dp)
        ) {
            // 1. MONUMENTAL 3D METALLIC SILVER RANK NUMBER (Exact same spacing for all ranks)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .zIndex(1f)
            ) {
                val fontSize = if (rank == 10) 134.sp else 146.sp
                val letterSpacing = if (rank == 10) (-5).sp else (-2).sp
                val lineHeight = if (rank == 10) 122.sp else 134.sp

                // Layer A: Deep shadow & metallic contour stroke
                Text(
                    text = "$rank",
                    color = silverStroke,
                    style = TextStyle(
                        fontFamily = BebasNeue,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = letterSpacing,
                        lineHeight = lineHeight,
                        drawStyle = Stroke(width = if (isFocused) 8.5f else 5.5f),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.95f),
                            offset = Offset(4f, 6f),
                            blurRadius = 12f
                        )
                    ),
                    softWrap = false,
                    maxLines = 1
                )

                // Layer B: Radiant Metallic Silver Gradient Fill
                Text(
                    text = "$rank",
                    style = TextStyle(
                        fontFamily = BebasNeue,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = letterSpacing,
                        lineHeight = lineHeight,
                        brush = Brush.verticalGradient(silverFill)
                    ),
                    softWrap = false,
                    maxLines = 1
                )
            }

            // Fixed uniform gap between number and poster for ALL ranks (1 to 10)
            Spacer(modifier = Modifier.width(8.dp))

            // 2. ENRICHED POSTER CARD + TITLE
            Column(
                modifier = Modifier
                    .width(142.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .zIndex(2f)
                        .clip(cardShape)
                        .border(
                            border = BorderStroke(
                                width = if (isFocused) 3.8.dp else 1.dp,
                                color = posterBorderColor
                            ),
                            shape = cardShape
                        )
                        .background(LaMovieCardBg)
                ) {
                // Background subtle placeholder gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF242731),
                                    Color(0xFF161922),
                                    Color(0xFF0D0F14)
                                )
                            )
                        )
                )

                // Cinematic fallback watermark
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF787D88).copy(alpha = 0.20f),
                        modifier = Modifier.size(42.dp)
                    )
                }

                // High-resolution Poster Image
                val imageSource = remember(movie.image, movie.backdrop) {
                    movie.image.ifBlank { movie.backdrop ?: "" }
                }
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageSource)
                        .crossfade(true)
                        .size(340, 510)
                        .build(),
                    contentDescription = cleanTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badges (Podium Rank / TMDB on left, Audio & Quality on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Badge: Exclusive TOP 1 Platinum Pill or TMDB Score Pill
                    if (rank == 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xE6141824))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.5.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF1F5F9),
                                    modifier = Modifier.size(8.5.dp)
                                )
                                Text(
                                    text = "TOP 1",
                                    color = Color(0xFFFFFFFF),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = Inter,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        // Standard TMDB Pill (only displayed if rating is available)
                        val formattedScore = remember(movie.rating) { formatRating(movie.rating) }
                        if (formattedScore != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xBF0B0E14))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFF5F2EB),
                                        modifier = Modifier.size(7.5.dp)
                                    )
                                    Text(
                                        text = formattedScore,
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = Inter,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }

                    // Right Badges: Audio Language & Quality
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LAT badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xBF0B0E14))
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LAT",
                                color = Color(0xFFD6D0C4),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Quality pill (HD / 4K)
                        val rawQuality = movie.quality?.takeIf { it.isNotBlank() } ?: "HD"
                        val cleanQuality = if (rawQuality.contains("4K", ignoreCase = true)) "4K" else "HD"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PillSurface)
                                .border(1.dp, Color(0xFFF5F2EB).copy(alpha = 0.30f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 3.5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = cleanQuality,
                                color = Color(0xFFF5F2EB),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = cleanTitle.uppercase(),
                color = if (isFocused) Color(0xFFF5F2EB) else Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = Inter,
                letterSpacing = 1.3.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .then(
                        if (isFocused) {
                            Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 800,
                                initialDelayMillis = 600,
                                velocity = 35.dp
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
}


