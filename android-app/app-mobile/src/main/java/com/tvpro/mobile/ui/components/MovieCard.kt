package com.tvpro.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.mobile.ui.theme.*

@Composable
fun MobileMovieCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(14.dp)
    val cleanTitle = remember(movie.title) { movie.title.cleanTitle() }

    Column(
        modifier = modifier
            .then(
                if (fillWidth) Modifier.fillMaxWidth()
                else Modifier.width(135.dp)
            )
            .clickable { onClick() }
    ) {
        // Poster Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fillWidth) Modifier.aspectRatio(2f / 3f)
                    else Modifier.height(195.dp)
                )
                .clip(cardShape)
                .border(
                    BorderStroke(1.dp, SubtleBorder),
                    shape = cardShape
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardGradientStart,
                            CardGradientMid,
                            CardGradientEnd
                        )
                    )
                )
        ) {
            // Placeholder watermark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = LaMovieMuted.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp)
                )
            }

            // High-res poster
            val imageSource = remember(movie.image, movie.backdrop) {
                movie.image.ifBlank { movie.backdrop ?: "" }
                    .replace("image.tmdb.org", "media.themoviedb.org")
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .build(),
                contentDescription = cleanTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
            )

            // Bottom subtle dark vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x99130D19)),
                            startY = 250f
                        )
                    )
            )

            // Top Badges (TMDB left, LAT & Quality right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TMDB Rating badge (top left)
                val rating = movie.rating?.takeIf { it.isNotBlank() && it != "0" } ?: "8.2"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.5.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "TMDB",
                            color = MutedSlate,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmIvory,
                            modifier = Modifier.size(8.5.dp)
                        )
                        Text(
                            text = rating,
                            color = WarmIvory,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Audio Language & Quality badges (top right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Outlined LAT capsule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(PillSurface)
                            .border(1.dp, SubtleBorder, RoundedCornerShape(3.5.dp))
                            .padding(horizontal = 3.5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "LAT",
                            color = SoftSilver,
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
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(PillSurface)
                            .border(1.dp, WarmIvory.copy(alpha = 0.25f), RoundedCornerShape(3.5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = cleanQuality,
                            color = WarmIvory,
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

        Spacer(modifier = Modifier.height(8.dp))

        // Title and Year
        Text(
            text = cleanTitle,
            color = WarmIvory,
            fontFamily = Inter,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val year = movie.year
        if (!year.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = year,
                color = MutedSlate,
                fontFamily = Inter,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
