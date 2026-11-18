package com.tvpro.app.ui.components.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.app.ui.theme.BebasNeue
import com.tvpro.app.ui.theme.Inter
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.domain.PlayerTimeUtils
import kotlinx.coroutines.delay

@Composable
fun TvPlayerEpisodesModal(
    show: Boolean,
    onDismiss: () -> Unit,
    movie: MovieItem,
    detailResponse: MovieDetailResponse?,
    seasonEpisodes: List<EpisodeItem>,
    currentMovieTitle: String,
    cleanMainTitle: String,
    displayPositionMs: Long,
    durationMs: Long,
    activeSeasonInt: Int,
    activeEpisodeInt: Int,
    selectedModalSeason: Int,
    onSelectSeason: (Int) -> Unit,
    activeEpisodeFocusRequester: FocusRequester,
    onEpisodeClick: (EpisodeItem) -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSetPlaybackPlaying: (Boolean) -> Unit,
    onWakeUpControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val context = LocalContext.current
    val warmIvory = Color(0xFFF5F2EB)

    val isSeriesContent = remember(detailResponse, movie) {
        detailResponse?.isSeries == true ||
            movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
            movie.url.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") ||
            currentMovieTitle.contains(Regex("""- T\d+:E\d+""", RegexOption.IGNORE_CASE))
    }
    val totalSeasons = (detailResponse?.seasonsCount ?: 1).coerceAtLeast(1)
    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image

    val modalEpisodes = remember(seasonEpisodes, isSeriesContent, cleanMainTitle, defaultBackdrop, selectedModalSeason) {
        if (seasonEpisodes.isNotEmpty() && (!isSeriesContent || seasonEpisodes.firstOrNull()?.seasonNumber == selectedModalSeason)) {
            seasonEpisodes
        } else if (isSeriesContent) {
            val epNames = listOf(
                "El Comienzo",
                "Sombras del Pasado",
                "Punto de Quiebre",
                "En la Oscuridad",
                "Territorio Hostil",
                "Revelaciones",
                "Sin Retorno",
                "El Desenlace"
            )
            epNames.mapIndexed { idx, name ->
                val num = idx + 1
                EpisodeItem(
                    id = -(selectedModalSeason * 100 + num),
                    episodeNumber = num,
                    seasonNumber = selectedModalSeason,
                    title = name,
                    overview = "Capítulo $num de la Temporada $selectedModalSeason de $cleanMainTitle disponible en alta definición.",
                    image = defaultBackdrop,
                    runtime = "${44 + (idx % 4) * 3} min"
                )
            }
        } else {
            val sceneTitles = listOf(
                "Prólogo e Introducción",
                "El Primer Giro",
                "Punto de No Retorno",
                "Clímax Principal",
                "Desenlace Final"
            )
            val sceneDescs = listOf(
                "Apertura cinematográfica y presentación de los protagonistas en $cleanMainTitle.",
                "Los acontecimientos se aceleran y el conflicto central toma forma.",
                "La tensión alcanza su punto crítico con decisiones irreversibles.",
                "Secuencia cumbre de acción y confrontación definitiva.",
                "Resolución de la historia y secuencia final de cierre."
            )
            val sceneRuntimes = listOf("24 min", "34 min", "43 min", "33 min", "28 min")
            sceneTitles.mapIndexed { idx, name ->
                EpisodeItem(
                    id = -(idx + 1),
                    episodeNumber = idx + 1,
                    seasonNumber = 1,
                    title = name,
                    overview = sceneDescs[idx],
                    image = defaultBackdrop,
                    runtime = sceneRuntimes[idx]
                )
            }
        }
    }

    val progressFraction = if (durationMs > 0L) {
        (displayPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val completedPercent = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val activeCardIndex = remember(modalEpisodes, isSeriesContent, activeSeasonInt, activeEpisodeInt, progressFraction) {
        if (isSeriesContent) {
            if (selectedModalSeason == activeSeasonInt) {
                modalEpisodes.indexOfFirst { it.episodeNumber == activeEpisodeInt }.coerceAtLeast(0)
            } else 0
        } else {
            ((progressFraction * modalEpisodes.size).toInt()).coerceIn(0, (modalEpisodes.size - 1).coerceAtLeast(0))
        }
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(activeCardIndex, selectedModalSeason) {
        try {
            lazyListState.scrollToItem((activeCardIndex - 1).coerceAtLeast(0))
            delay(80)
            activeEpisodeFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF206080C))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .padding(horizontal = 46.dp, vertical = 38.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header superior del modal: CAPÍTULOS · X EN TOTAL  |  Selector de Temporada  |  ACTIVO: CAPÍTULO 0X
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = null,
                            tint = warmIvory,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isSeriesContent) "EPISODIOS" else "ESCENAS",
                            color = warmIvory,
                            fontFamily = BebasNeue,
                            fontSize = 32.sp,
                            letterSpacing = 2.2.sp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "·   ${modalEpisodes.size} ${if (isSeriesContent) "EPISODIOS EN TOTAL" else "ESCENAS EN TOTAL"}",
                            color = Color(0xFF9CA3AF),
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.0.sp
                        )

                        // Selector de Temporadas cuando la serie tiene más de 1 temporada
                        if (isSeriesContent && totalSeasons > 1) {
                            Spacer(modifier = Modifier.width(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (s in 1..totalSeasons.coerceAtMost(30)) {
                                    var isSeasonFocused by remember { mutableStateOf(false) }
                                    val isSelected = (selectedModalSeason == s)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                when {
                                                    isSeasonFocused -> warmIvory
                                                    isSelected -> Color(0xFF1F2430)
                                                    else -> Color(0xFF0E121A)
                                                }
                                            )
                                            .border(
                                                width = if (isSeasonFocused || isSelected) 1.5.dp else 1.dp,
                                                color = if (isSeasonFocused || isSelected) warmIvory else Color.White.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .onFocusChanged { isSeasonFocused = it.isFocused }
                                            .focusable()
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown &&
                                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                                ) {
                                                    onSelectSeason(s)
                                                    true
                                                } else false
                                            }
                                            .clickable { onSelectSeason(s) }
                                            .padding(horizontal = 14.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "T$s",
                                            color = if (isSeasonFocused) Color.Black else warmIvory,
                                            fontFamily = Inter,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isSeriesContent) {
                            "Selecciona un episodio con el mando a distancia para reproducirlo inmediatamente."
                        } else {
                            "Selecciona una escena con el mando a distancia para saltar inmediatamente a ese momento."
                        },
                        color = Color(0xFF9CA3AF),
                        fontFamily = Inter,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Píldora derecha: ● ACTIVO: EPISODIO 0X / ESCENA 0X
                val activeBadgeNum = if (isSeriesContent) activeEpisodeInt else (activeCardIndex + 1)
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10141D))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD6D0C4))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSeriesContent) "ACTIVO: EPISODIO ${String.format("%02d", activeBadgeNum)}" else "ACTIVO: ESCENA ${String.format("%02d", activeBadgeNum)}",
                        color = warmIvory,
                        fontFamily = Inter,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Carrusel horizontal de tarjetas de capítulos / escenas
            LazyRow(
                state = lazyListState,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(modalEpisodes) { index, ep ->
                    var isCardFocused by remember { mutableStateOf(false) }
                    val isCurrentActive = if (isSeriesContent) {
                        selectedModalSeason == activeSeasonInt && ep.episodeNumber == activeEpisodeInt
                    } else {
                        index == activeCardIndex
                    }
                    val isWatched = if (isSeriesContent) {
                        selectedModalSeason < activeSeasonInt ||
                            (selectedModalSeason == activeSeasonInt && ep.episodeNumber < activeEpisodeInt)
                    } else {
                        index < activeCardIndex
                    }

                    val cleanEpTitle = remember(ep.title, cleanMainTitle, ep.episodeNumber) {
                        val stripped = ep.title
                            .replace(Regex("^\\Q$cleanMainTitle\\E\\s*[:\\-·]\\s*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("^Temporada\\s*\\d+\\s*[:\\-·]?\\s*Episodio\\s*\\d+\\s*[:\\-·]?\\s*", RegexOption.IGNORE_CASE), "")
                            .replace(Regex("^Episodio\\s*\\d+\\s*[:\\-·]\\s*", RegexOption.IGNORE_CASE), "")
                            .trim()
                        stripped.ifBlank { if (isSeriesContent) "Episodio ${ep.episodeNumber}" else "Escena ${index + 1}" }
                    }

                    val activateChapter = {
                        if (isSeriesContent) {
                            if (!isCurrentActive) {
                                onEpisodeClick(ep)
                            } else {
                                onDismiss()
                                onSetPlaybackPlaying(true)
                            }
                        } else {
                            // Escena de película: saltar al porcentaje correspondiente
                            onDismiss()
                            val targetFrac = index.toFloat() / modalEpisodes.size.coerceAtLeast(1).toFloat()
                            val targetMs = if (durationMs > 0L) (durationMs * targetFrac).toLong() else 0L
                            onSeekToMs(targetMs)
                            onSetPlaybackPlaying(true)
                            onWakeUpControls()
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCardFocused) Color(0xFF151922) else Color(0xFF0C0F15)
                        ),
                        border = if (isCardFocused) BorderStroke(2.2.dp, Color(0xFFE5DFD3))
                        else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .width(292.dp)
                            .height(352.dp)
                            .then(
                                if (index == activeCardIndex) Modifier.focusRequester(activeEpisodeFocusRequester)
                                else Modifier
                            )
                            .onFocusChanged { isCardFocused = it.isFocused }
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                ) {
                                    activateChapter()
                                    true
                                } else false
                            }
                            .clickable { activateChapter() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // 1. Miniatura 16:9 con insignias VISTO / REPRODUCIENDO AHORA y barra inferior
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(146.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF080A0F))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(MovieRepository.sanitizeThumbnailUrl(ep.image).ifBlank { defaultBackdrop })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = cleanEpTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Black.copy(alpha = 0.35f),
                                                0.5f to Color.Transparent,
                                                1.0f to Color.Black.copy(alpha = 0.82f)
                                            )
                                        )
                                )

                                // Insignia superior izquierda: REPRODUCIENDO AHORA o VISTO
                                if (isCurrentActive) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(warmIvory)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6B665E))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "REPRODUCIENDO AHORA",
                                            color = Color(0xFF0A0D13),
                                            fontFamily = Inter,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.0.sp
                                        )
                                    }
                                } else if (isWatched) {
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xD90A0E14))
                                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.45f), RoundedCornerShape(50))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "VISTO",
                                            color = Color(0xFFD1FAE5),
                                            fontFamily = Inter,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp
                                        )
                                    }
                                }

                                // Insignia de tiempo / duración
                                Box(
                                    modifier = Modifier
                                        .align(if (isCurrentActive) Alignment.TopEnd else Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xD9080A0F))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isCurrentActive && displayPositionMs > 0L) {
                                            PlayerTimeUtils.formatTimeHms(displayPositionMs / 1000)
                                        } else {
                                            ep.runtime.ifBlank { "45 min" }
                                        },
                                        color = warmIvory,
                                        fontFamily = Inter,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                // Barra de progreso inferior de la miniatura
                                if (isWatched) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(Color(0xFF10B981))
                                    )
                                } else if (isCurrentActive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .height(3.5.dp)
                                            .background(Color.White.copy(alpha = 0.22f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progressFraction.coerceIn(0.18f, 1f))
                                                .fillMaxHeight()
                                                .background(warmIvory)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Fila de metadatos: EPISODIO 0X · EN FOCO | % VISTO o Duración
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chapLabel = if (isSeriesContent) {
                                    if (isCardFocused) "EPISODIO ${String.format("%02d", ep.episodeNumber)} · EN FOCO"
                                    else "EPISODIO ${String.format("%02d", ep.episodeNumber)}"
                                } else {
                                    if (isCardFocused) "ESCENA ${String.format("%02d", index + 1)} · EN FOCO"
                                    else "ESCENA ${String.format("%02d", index + 1)}"
                                }
                                Text(
                                    text = chapLabel,
                                    color = if (isCardFocused) Color(0xFFE5DFD3) else Color(0xFF6B7280),
                                    fontFamily = Inter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )

                                Text(
                                    text = if (isCurrentActive) "$completedPercent% VISTO" else ep.runtime.uppercase(),
                                    color = if (isCardFocused) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                                    fontFamily = Inter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3. Título del capítulo
                            Text(
                                text = cleanEpTitle,
                                color = if (isCardFocused) warmIvory else Color(0xFFD1D5DB),
                                fontFamily = Inter,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // 4. Sinopsis del capítulo
                            Text(
                                text = ep.overview,
                                color = if (isCardFocused) Color(0xFFB0B7C3) else Color(0xFF6B7280),
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // 5. Pie de tarjeta con Duración a la izquierda y botón CONTINUAR / REPRODUCIR OK a la derecha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Duración: ${ep.runtime.ifBlank { "45 min" }}",
                                    color = Color(0xFF9CA3AF),
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                if (isCardFocused || isCurrentActive) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isCardFocused) warmIvory else Color.White.copy(alpha = 0.12f)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isCardFocused) Color(0xFF0A0D13) else warmIvory,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isCurrentActive) "CONTINUAR" else "REPRODUCIR",
                                            color = if (isCardFocused) Color(0xFF0A0D13) else warmIvory,
                                            fontFamily = Inter,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.2.sp
                                        )
                                        if (isCardFocused) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFD6D0C4))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "OK",
                                                    color = Color(0xFF0A0D13),
                                                    fontFamily = Inter,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
