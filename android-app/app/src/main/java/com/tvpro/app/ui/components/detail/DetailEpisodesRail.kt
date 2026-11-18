package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.app.ui.components.cleanHtml
import com.tvpro.app.ui.theme.*
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.delay

@Composable
fun DetailEpisodesRail(
    seasonsCount: Int,
    selectedSeason: Int,
    onSeasonSelect: (Int) -> Unit,
    seasonEpisodes: List<EpisodeItem>,
    isSeasonLoading: Boolean,
    restoreEpisodeFocusId: Int?,
    onEpisodeFocusRestored: () -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit,
    backdropUrl: String,
    seasonsListState: LazyListState,
    episodesListState: LazyListState,
    episodesTabFocusRequester: FocusRequester,
    selectedSeasonFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warmIvory = MochiCream
    val mutedSlate = MochiMutedLilac
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder

    Column(modifier = modifier.fillMaxWidth()) {
        // Season Selector Pills (Horizontal Scroll for any number of seasons)
        LazyRow(
            state = seasonsListState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp)
        ) {
            item(key = "season_label") {
                Text(
                    text = "TEMPORADA:",
                    color = mutedSlate,
                    fontSize = 10.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            items(seasonsCount.coerceAtMost(30), key = { index -> "season_${index + 1}" }) { index ->
                val s = index + 1
                var isSeasonFocused by remember(s) { mutableStateOf(false) }
                val isSeasonSelected = (selectedSeason == s)
                Box(
                    modifier = Modifier
                        .then(if (isSeasonSelected) Modifier.focusRequester(selectedSeasonFocusRequester) else Modifier)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isSeasonFocused -> warmIvory
                                isSeasonSelected -> Color(0xFF1B202C)
                                else -> pillSurface
                            }
                        )
                        .border(
                            width = if (isSeasonFocused || isSeasonSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSeasonFocused -> warmIvory
                                isSeasonSelected -> warmIvory.copy(alpha = 0.65f)
                                else -> subtleBorder
                            },
                            shape = RoundedCornerShape(50)
                        )
                        .focusProperties {
                            up = episodesTabFocusRequester
                        }
                        .onFocusChanged { isSeasonFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionUp -> {
                                        try { episodesTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        onSeasonSelect(s)
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .clickable { onSeasonSelect(s) }
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "TEMPORADA $s",
                        color = when {
                            isSeasonFocused -> Color.Black
                            isSeasonSelected -> warmIvory
                            else -> mutedSlate
                        },
                        fontSize = 10.sp,
                        fontFamily = Inter,
                        fontWeight = if (isSeasonFocused || isSeasonSelected) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 1.4.sp
                    )
                }
            }
        }

        if (isSeasonLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                CircularProgressIndicator(
                    color = warmIvory,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Cargando episodios de la Temporada $selectedSeason...",
                    color = mutedSlate,
                    fontSize = 12.sp,
                    fontFamily = Inter
                )
            }
        } else {
            LazyRow(
                state = episodesListState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp, start = 2.dp, end = 24.dp)
            ) {
                items(seasonEpisodes, key = { it.id }) { episode ->
                    val epFocusRequester = remember(episode.id) { FocusRequester() }
                    var isEpFocused by remember { mutableStateOf(false) }

                    LaunchedEffect(restoreEpisodeFocusId) {
                        if (restoreEpisodeFocusId == episode.id) {
                            delay(60)
                            try {
                                epFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            onEpisodeFocusRestored()
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEpFocused) Color(0xFF131822) else Color(0xFF0B0E15)
                        ),
                        border = if (isEpFocused) BorderStroke(2.dp, warmIvory)
                        else BorderStroke(1.dp, subtleBorder),
                        modifier = Modifier
                            .width(260.dp)
                            .focusRequester(epFocusRequester)
                            .focusProperties {
                                up = selectedSeasonFocusRequester
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                    try { selectedSeasonFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else false
                            }
                            .onFocusChanged { focusState ->
                                isEpFocused = focusState.isFocused
                            }
                            .clickable { onEpisodeClick(episode) }
                    ) {
                        Column {
                            // 16:9 Episode Thumbnail with Play Badge Overlay
                            var thumbFailed by remember(episode.image) { mutableStateOf(false) }
                            val cleanThumb = MovieRepository.sanitizeThumbnailUrl(episode.image)
                            val displayEpImage = if (thumbFailed || cleanThumb.isBlank()) backdropUrl else cleanThumb
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(displayEpImage)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = episode.title,
                                    contentScale = ContentScale.Crop,
                                    onError = { thumbFailed = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                1.0f to Color(0xFF0B0E15).copy(alpha = 0.85f)
                                            )
                                        )
                                )
                                // Episode Number Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xD9080A0F))
                                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = "T${episode.seasonNumber} · EP ${String.format("%02d", episode.episodeNumber)}",
                                        color = warmIvory,
                                        fontSize = 9.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.0.sp
                                    )
                                }
                                // Runtime Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xD9080A0F))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = episode.runtime,
                                        color = Color(0xFFD1D5DB),
                                        fontSize = 9.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                // Play Circle on Focus
                                if (isEpFocused) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(warmIvory)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = episode.title.cleanHtml(),
                                    color = warmIvory,
                                    fontSize = 12.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = episode.overview.cleanHtml(),
                                    color = mutedSlate,
                                    fontSize = 10.5.sp,
                                    fontFamily = Inter,
                                    lineHeight = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
