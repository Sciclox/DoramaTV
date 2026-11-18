package com.tvpro.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.mobile.ui.theme.*

@Composable
fun MobileTop10Card(
    movie: MovieItem,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(10.dp)
    val cleanTitle = remember(movie.title) { movie.title.cleanTitle() }

    // Silver metallic gradient
    val silverFill = remember {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFF94A3B8),
            Color(0xFF64748B),
            Color(0xFF334155)
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .wrapContentWidth()
            .clickable { onClick() }
            .padding(end = 8.dp)
    ) {
        // MONUMENTAL 3D METALLIC SILVER RANK NUMBER
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(if (rank == 10) 65.dp else 48.dp)
                .height(180.dp)
        ) {
            val rankText = rank.toString()

            // 1. Dark Shadow Silhouette
            Text(
                text = rankText,
                fontFamily = BebasNeue,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black.copy(alpha = 0.85f),
                style = TextStyle(
                    drawStyle = Stroke(width = 8f),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.95f),
                        offset = Offset(3f, 5f),
                        blurRadius = 14f
                    )
                ),
                softWrap = false,
                modifier = Modifier.offset(x = 2.dp, y = 3.dp)
            )

            // 2. Chrome Perimetral Outline
            Text(
                text = rankText,
                fontFamily = BebasNeue,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFCBD5E1),
                style = TextStyle(
                    drawStyle = Stroke(width = 4f)
                ),
                softWrap = false
            )

            // 3. Polished Metallic Silver Fill
            Text(
                text = rankText,
                fontFamily = BebasNeue,
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = silverFill,
                        startY = 0f,
                        endY = 220f
                    )
                ),
                softWrap = false
            )
        }

        // Movie Poster beside the number
        Box(
            modifier = Modifier
                .width(125.dp)
                .height(185.dp)
                .offset(x = (-8).dp)
                .clip(cardShape)
                .border(BorderStroke(1.dp, SubtleBorder), shape = cardShape)
                .background(
                    Brush.verticalGradient(
                        listOf(CardGradientStart, CardGradientMid, CardGradientEnd)
                    )
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(movie.image)
                    .crossfade(true)
                    .build(),
                contentDescription = cleanTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badges (TMDB & Quality)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rating = movie.rating?.takeIf { it.isNotBlank() && it != "0" } ?: "8.5"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmIvory,
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = rating,
                            color = WarmIvory,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter
                        )
                    }
                }

                // Right Badges: Audio Language & Quality (Matching TV version)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(PillSurface)
                            .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LAT",
                            color = SoftSilver,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(PillSurface)
                            .border(1.dp, WarmIvory.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 3.5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (movie.quality?.contains("4K", ignoreCase = true) == true) "4K" else "HD",
                            color = WarmIvory,
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter
                        )
                    }
                }
            }
        }
    }
}
