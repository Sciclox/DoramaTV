package com.tvpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.app.ui.theme.*

fun String.cleanHtml(): String {
    return this
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

fun String.cleanTitle(): String {
    return this
        .cleanHtml()
        .replace(Regex("""\s*\(\s*\d{4}\s*\)\s*"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

fun formatRating(rating: String?): String? {
    if (rating.isNullOrBlank()) return null
    val clean = rating.trim()
    if (clean == "0" || clean == "0.0" || clean.equals("N/A", ignoreCase = true)) return null
    val num = clean.toDoubleOrNull() ?: return clean
    return if (num > 0.0) String.format(java.util.Locale.US, "%.1f", num) else null
}

data class SeriesEpisodeInfo(
    val season: Int?,
    val episode: Int?,
    val formattedLabel: String
)

fun extractSeriesInfo(historyItem: WatchHistoryItem): SeriesEpisodeInfo? {
    if (historyItem.seasonNumber != null && historyItem.episodeNumber != null) {
        return SeriesEpisodeInfo(
            season = historyItem.seasonNumber,
            episode = historyItem.episodeNumber,
            formattedLabel = "T${historyItem.seasonNumber} : E${historyItem.episodeNumber}"
        )
    }
    if (historyItem.episodeNumber != null) {
        val s = historyItem.seasonNumber ?: 1
        return SeriesEpisodeInfo(
            season = s,
            episode = historyItem.episodeNumber,
            formattedLabel = if (historyItem.seasonNumber != null) "T$s : E${historyItem.episodeNumber}" else "E${historyItem.episodeNumber}"
        )
    }

    val title = historyItem.title
    val url = historyItem.movieUrl

    val tPattern = Regex("""T(\d+)\s*[:\-·x]?\s*E(\d+)""", RegexOption.IGNORE_CASE)
    val tMatch = tPattern.find(title)
    if (tMatch != null) {
        val s = tMatch.groupValues[1].toIntOrNull() ?: 1
        val e = tMatch.groupValues[2].toIntOrNull() ?: 1
        return SeriesEpisodeInfo(s, e, "T$s : E$e")
    }

    val tempEpPattern = Regex("""Temporada\s*(\d+).*?(?:Episodio|Cap[ií]tulo|Cap\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    val tempEpMatch = tempEpPattern.find(title)
    if (tempEpMatch != null) {
        val s = tempEpMatch.groupValues[1].toIntOrNull() ?: 1
        val e = tempEpMatch.groupValues[2].toIntOrNull() ?: 1
        return SeriesEpisodeInfo(s, e, "T$s : E$e")
    }

    val epOnlyPattern = Regex("""(?:Episodio|Cap[ií]tulo|Cap\.?|Ep\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    val epOnlyMatch = epOnlyPattern.find(title)
    if (epOnlyMatch != null) {
        val e = epOnlyMatch.groupValues[1].toIntOrNull() ?: 1
        return SeriesEpisodeInfo(1, e, "E$e")
    }

    val isSeriesUrl = url.contains("/series/", ignoreCase = true) ||
            url.contains("/animes/", ignoreCase = true) ||
            url.contains("/novelas/", ignoreCase = true) ||
            historyItem.isSeries
    if (isSeriesUrl) {
        return SeriesEpisodeInfo(1, 1, "T1 : E1")
    }

    return null
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvMovieCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false,
    fillContainer: Boolean = false,
    onMovieFocused: ((MovieItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)

    val cleanTitle = remember(movie.title) { movie.title.cleanTitle() }

    val columnModifier = when {
        fillContainer -> Modifier.fillMaxSize()
        fillWidth -> Modifier.fillMaxWidth()
        else -> Modifier.width(170.dp)
    }

    Column(
        modifier = columnModifier,
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            onClick = onClick,
            modifier = modifier
                .then(
                    when {
                        fillContainer -> Modifier.weight(1f).fillMaxWidth()
                        fillWidth -> Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                        else -> Modifier.fillMaxWidth().height(238.dp)
                    }
                )
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .zIndex(if (isFocused) 20f else 1f)
                .onFocusChanged { 
                    isFocused = it.isFocused || it.hasFocus
                    if (it.isFocused) {
                        onMovieFocused?.invoke(movie)
                    }
                },
            shape = CardDefaults.shape(shape = cardShape),
            scale = CardDefaults.scale(scale = 1.0f, focusedScale = 1.0f, pressedScale = 1.0f),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.8.dp, Color(0xFFF5F2EB)),
                    shape = cardShape
                ),
                border = Border(
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    shape = cardShape
                )
            ),
            colors = CardDefaults.colors(containerColor = com.tvpro.app.ui.theme.LaMovieCardBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                .clip(cardShape)
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
            // Branded cinematic watermark placeholder (visible while loading or as graceful fallback)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF787D88).copy(alpha = 0.25f),
                    modifier = Modifier.size(46.dp)
                )
            }

            // High-res poster loaded smoothly over placeholder with TV memory-optimized downsampling
            val imageSource = remember(movie.image, movie.backdrop) {
                movie.image.ifBlank { movie.backdrop ?: "" }
                    .replace("image.tmdb.org", "media.themoviedb.org")
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .size(340, 510)
                    .build(),
                contentDescription = cleanTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
            )

            val displayRating = remember(movie.rating) { formatRating(movie.rating) }

            // Top Badges (TMDB left, LAT & Quality right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TMDB Rating badge (top left) - only shown when a verified rating exists
                if (displayRating != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xBF0B0E14))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                        ) {
                            Text(
                                text = "TMDB",
                                color = Color(0xFF9CA3AF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF5F2EB),
                                modifier = Modifier.size(8.5.dp)
                            )
                            Text(
                                text = displayRating,
                                color = Color(0xFFF5F2EB),
                                fontSize = 8.5.sp,
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

                // Audio Language & Quality badges (top right)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Outlined LAT capsule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.5.dp))
                            .background(Color(0xBF0B0E14))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(3.5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "LAT",
                            color = Color(0xFFD6D0C4),
                            fontSize = 7.5.sp,
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
                            .border(1.dp, Color(0xFFF5F2EB).copy(alpha = 0.30f), RoundedCornerShape(3.5.dp))
                            .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = cleanQuality,
                            color = Color(0xFFF5F2EB),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
        
    Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cleanTitle.uppercase(),
            color = if (isFocused) Color(0xFFF5F2EB) else Color(0xFF9CA3AF),
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = Inter,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
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

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TvHistoryCard(
    historyItem: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(10.dp)

    val cleanTitle = remember(historyItem.title) { historyItem.title.cleanTitle() }
    val seriesInfo = remember(historyItem) { extractSeriesInfo(historyItem) }

    Column(
        modifier = Modifier.width(170.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(238.dp)
                .padding(vertical = 4.dp, horizontal = 2.dp)
            .zIndex(if (isFocused) 20f else 1f)
            .onFocusChanged { isFocused = it.isFocused || it.hasFocus },
        shape = CardDefaults.shape(shape = cardShape),
        scale = CardDefaults.scale(scale = 1.0f, focusedScale = 1.0f, pressedScale = 1.0f),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.8.dp, Color(0xFFF5F2EB)),
                shape = cardShape
            ),
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shape = cardShape
            )
        ),
        colors = CardDefaults.colors(containerColor = com.tvpro.app.ui.theme.LaMovieCardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF787D88).copy(alpha = 0.25f),
                    modifier = Modifier.size(46.dp)
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(historyItem.image)
                    .crossfade(true)
                    .size(340, 510)
                    .build(),
                contentDescription = cleanTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
            )

            // Series / Anime badge: T1 : E3 (Solo si es serie o anime; las películas quedan 100% limpias)
            if (seriesInfo != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xD90B0E14))
                        .border(1.dp, Color(0xFFF5F2EB).copy(alpha = 0.40f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.5.dp, vertical = 2.5.dp)
                ) {
                    Text(
                        text = seriesInfo.formattedLabel,
                        color = Color(0xFFF5F2EB),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Playback Progress Track (Warm Ivory timeline matching DetailScreen)
            if (historyItem.durationMs > 0 && historyItem.playbackPositionMs > 0) {
                val progress = (historyItem.playbackPositionMs.toFloat() / historyItem.durationMs.toFloat()).coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.20f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(Color(0xFFF5F2EB))
                    )
                }
            }
        }
    }
        
    Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = cleanTitle.uppercase(),
            color = if (isFocused) Color(0xFFF5F2EB) else Color(0xFF9CA3AF),
            fontSize = 11.5.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = Inter,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
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

