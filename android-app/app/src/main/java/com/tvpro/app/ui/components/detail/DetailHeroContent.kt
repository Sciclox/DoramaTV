package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.app.ui.theme.*
import com.tvpro.core.data.model.WatchHistoryItem

@Composable
fun DetailHeroContent(
    displayTitle: String,
    ratingValue: String,
    yearValue: String,
    runtimeFormatted: String,
    seasonsCount: Int,
    isSeries: Boolean,
    isAnime: Boolean,
    isBackgroundLoading: Boolean,
    quality: String?,
    genresList: List<String>,
    synopsisText: String,
    hasSavedProgress: Boolean,
    historyItem: WatchHistoryItem?,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    val warmIvory = MochiCream
    val softSilver = MochiLavender
    val mutedSlate = MochiMutedLilac
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder

    Column(modifier = modifier.fillMaxWidth(0.66f)) {
        // 1. Pre-title Editorial Kicker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categoryLabel = when {
                isAnime -> "ANIME OFICIAL"
                isSeries -> "SERIE DESTACADA"
                else -> "PELÍCULA DESTACADA"
            }
            Text(
                text = categoryLabel,
                color = mutedSlate,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Inter,
                letterSpacing = 2.0.sp
            )

            Text(
                text = "·",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp
            )

            val matchScore = remember(ratingValue) {
                val r = ratingValue.toDoubleOrNull() ?: 8.5
                (r * 10 + 7).toInt().coerceIn(84, 99)
            }
            Text(
                text = "$matchScore% RECOMENDADO",
                color = softSilver,
                fontSize = 10.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Monumental Title (BebasNeue in Warm Ivory)
        Text(
            text = displayTitle.uppercase(),
            fontSize = 48.sp,
            fontFamily = BebasNeue,
            fontWeight = FontWeight.Normal,
            color = warmIvory,
            letterSpacing = 2.0.sp,
            lineHeight = 50.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Metadata Badges Strip
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TMDB Rating Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TMDB",
                        color = mutedSlate,
                        fontSize = 9.5.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = warmIvory,
                        modifier = Modifier.size(10.5.dp)
                    )
                    Text(
                        text = ratingValue,
                        color = warmIvory,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        fontSize = 10.5.sp
                    )
                }
            }

            // Release Year
            Text(
                text = yearValue,
                color = warmIvory,
                fontSize = 12.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold
            )

            // Maturity Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "16+",
                    color = warmIvory,
                    fontSize = 10.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Duration or Seasons count
            Text(
                text = if (isSeries) {
                    if (isBackgroundLoading) "Serie"
                    else if (seasonsCount == 1) "1 Temporada"
                    else "$seasonsCount Temporadas"
                } else runtimeFormatted,
                color = Color(0xFFD1D5DB),
                fontSize = 12.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium
            )

            // Quality Badge
            val cleanQuality = (quality?.takeIf { it.isNotBlank() } ?: "4K Ultra HD")
                .let { if (it.contains("4K", ignoreCase = true)) "4K HDR" else "FULL HD" }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(PillSurface)
                    .border(1.dp, warmIvory.copy(alpha = 0.30f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = cleanQuality,
                    color = warmIvory,
                    fontSize = 9.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
            }

            // Audio Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LATINO",
                    color = softSilver,
                    fontSize = 9.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.0.sp
                )
            }

            // 5.1 Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "5.1",
                    color = Color(0xFFD1D5DB),
                    fontSize = 9.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Subtitles CC Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "CC",
                    color = Color(0xFFD1D5DB),
                    fontSize = 9.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 4. Genres & Tagline
        if (genresList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = genresList.joinToString("  ·  ").uppercase(),
                fontSize = 11.sp,
                fontFamily = Inter,
                color = softSilver,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp
            )
        }

        // 5. Synopsis
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = synopsisText,
            fontSize = 13.5.sp,
            fontFamily = Inter,
            color = Color(0xFFD1D5DB),
            lineHeight = 22.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // 6. Continue Watching Progress Bar
        if (hasSavedProgress && historyItem != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(0.80f)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(warmIvory)
                    )
                }
                Text(
                    text = formatRemainingTime(historyItem.playbackPositionMs, historyItem.durationMs).uppercase(),
                    color = mutedSlate,
                    fontSize = 10.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
