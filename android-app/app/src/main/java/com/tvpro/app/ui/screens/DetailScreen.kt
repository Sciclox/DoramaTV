@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tvpro.app.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.components.TvMovieCard
import com.tvpro.app.ui.components.cleanHtml
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun formatRuntime(runtime: String?): String {
    if (runtime.isNullOrBlank()) return "1h 55m"
    val mins = runtime.toDoubleOrNull()?.toInt() ?: runtime.toIntOrNull() ?: return runtime
    if (mins <= 0) return "1h 55m"
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) {
        if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
    } else {
        "${remMins}m"
    }
}

private fun formatRemainingTime(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return "En progreso"
    val remMins = ((durationMs - positionMs).coerceAtLeast(0L) / 60_000L).toInt()
    if (remMins <= 0) return "Menos de 1 min restante"
    val h = remMins / 60
    val m = remMins % 60
    return if (h > 0) "Quedan ${h}h ${m}m" else "Quedan ${m} min"
}

private enum class DetailTab(val label: String) {
    EPISODES("EPISODIOS"),
    SIMILAR("MÁS COMO ESTO"),
    EXTRAS("ESCENAS Y DETALLES")
}

@Composable
fun DetailScreen(
    movie: MovieItem,
    isPlayerActive: Boolean = false,
    onPlayClick: (MovieItem, String?, List<StreamItem>) -> Unit,
    onBackClick: () -> Unit,
    onMovieSelect: (MovieItem) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = remember(movie.url) { ScrollState(0) }
    val playButtonFocusRequester = remember { FocusRequester() }
    val myListFocusRequester = remember { FocusRequester() }
    val restartButtonFocusRequester = remember { FocusRequester() }
    val episodesTabFocusRequester = remember { FocusRequester() }
    val similarTabFocusRequester = remember { FocusRequester() }
    val extrasTabFocusRequester = remember { FocusRequester() }
    val selectedSeasonFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val episodesListState = rememberLazyListState()
    val seasonsListState = rememberLazyListState()

    // Focus restoration when returning from PlayerScreen
    var lastPlayTarget by remember(movie.url) { mutableStateOf<String?>(null) } // "HERO_PLAY", "HERO_RESTART", "EPISODE"
    var lastClickedEpisodeId by remember(movie.url) { mutableStateOf<Int?>(null) }
    var restoreEpisodeFocusId by remember { mutableStateOf<Int?>(null) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var wasPlayerActive by remember { mutableStateOf(false) }

    val heroBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = Rect.Zero
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Keep scroll at 0 while focus stays within the Hero section
            }
        }
    }

    val initialCached = remember(movie.url) { MovieRepository.getCachedDetail(movie.url, movie.title) }
    var isBackgroundLoading by remember(movie.url) { mutableStateOf(initialCached == null) }
    var detailResponse by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(initialCached) }
    var isFavorite by remember(movie.url) { mutableStateOf(WatchHistoryManager.isInMyList(context, movie.url)) }
    var historyItem by remember(movie.url) { mutableStateOf(WatchHistoryManager.getMovieHistory(context, movie.url)) }

    // Modal state (Lightbox Scene Preview)
    var previewSceneUrl by remember { mutableStateOf<String?>(null) }

    // Series / Episodes states
    val isSeriesInit = remember(movie.url, movie.type) {
        movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
            movie.url.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") || movie.url.contains("/novelas/")
    }
    val isSeries = detailResponse?.isSeries ?: isSeriesInit
    val firstTabFocusRequester = remember(isSeries) {
        if (isSeries) episodesTabFocusRequester else similarTabFocusRequester
    }
    val initialResumeSeason = remember(movie.url) {
        val h = WatchHistoryManager.getMovieHistory(context, movie.url)
        val s = h?.seasonNumber
        if (s != null && s > 0) s
        else (h?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.season }) ?: 1
    }
    var selectedSeason by remember(movie.url) { mutableIntStateOf(initialResumeSeason) }
    var seasonEpisodes by remember(movie.url) { mutableStateOf<List<EpisodeItem>>(initialCached?.episodes ?: emptyList()) }
    var isSeasonLoading by remember(movie.url) { mutableStateOf(false) }
    var isEpisodeResolving by remember { mutableStateOf(false) }

    // Active bottom tab (Episodes first for series, Similar Movies first for films)
    val availableTabs = remember(isSeries) {
        if (isSeries) {
            listOf(DetailTab.EPISODES, DetailTab.SIMILAR, DetailTab.EXTRAS)
        } else {
            listOf(DetailTab.SIMILAR, DetailTab.EXTRAS)
        }
    }
    var selectedTab by remember(movie.url, isSeries) {
        mutableStateOf(if (isSeries) DetailTab.EPISODES else DetailTab.SIMILAR)
    }

    val defaultStreams = remember(movie.url) { MovieRepository.getStreamsForMovie(movie.url) }
    var verifiedStreams by remember(movie.url) { mutableStateOf<List<StreamItem>>(emptyList()) }
    val rawStreams = remember(detailResponse, defaultStreams) {
        if (!detailResponse?.streams.isNullOrEmpty()) detailResponse!!.streams else defaultStreams
    }
    val streams = if (verifiedStreams.isNotEmpty()) verifiedStreams else rawStreams
    val selectedStream = streams.firstOrNull()
    val streamUrl = selectedStream?.url

    // Verify and filter streams in background so dead servers are hidden and only working ones remain
    LaunchedEffect(rawStreams) {
        if (rawStreams.isNotEmpty()) {
            val filtered = withContext(Dispatchers.IO) {
                MovieRepository.filterWorkingStreams(rawStreams)
            }
            if (filtered.isNotEmpty()) {
                verifiedStreams = filtered
            }
        }
    }



    val isContentAvailable = remember(isBackgroundLoading, detailResponse, streams, seasonEpisodes, isSeries, movie.postId) {
        if (isBackgroundLoading && detailResponse == null) {
            true // En proceso de verificación inicial, mantenemos el botón activo y enfocado
        } else if (isSeries) {
            seasonEpisodes.isNotEmpty() || !detailResponse?.episodes.isNullOrEmpty() || streams.isNotEmpty() || (movie.postId ?: 0) > 0
        } else {
            streams.isNotEmpty() || !detailResponse?.streams.isNullOrEmpty() || (movie.postId ?: 0) > 0 || (detailResponse?.postId?.toIntOrNull() ?: 0) > 0
        }
    }
    val primaryActionFocusRequester = if (isContentAvailable) playButtonFocusRequester else myListFocusRequester

    // Auto-focus en el botón primario cuando se cargan los datos o cambia la disponibilidad
    LaunchedEffect(isBackgroundLoading, isContentAvailable) {
        delay(80)
        try {
            primaryActionFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Intercept physical Back button on Android TV remote
    BackHandler {
        when {
            previewSceneUrl != null -> previewSceneUrl = null
            scrollState.value > 80 -> {
                scope.launch {
                    try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                    delay(60)
                    scrollState.animateScrollTo(0)
                }
            }
            else -> onBackClick()
        }
    }

    // Ensure screen starts at top (offset 0) and focuses primary Play button when movie changes
    LaunchedEffect(movie.url) {
        val h = WatchHistoryManager.getMovieHistory(context, movie.url)
        historyItem = h
        val s = h?.seasonNumber ?: (h?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.season }) ?: 1
        if (s > 0) selectedSeason = s
        delay(100)
        try {
            primaryActionFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    // Safety auto-clear for focus restoration
    LaunchedEffect(isRestoringFocus) {
        if (isRestoringFocus) {
            delay(700)
            isRestoringFocus = false
        }
    }

    // Restore focus and refresh watch history when returning from PlayerScreen
    LaunchedEffect(isPlayerActive) {
        if (wasPlayerActive && !isPlayerActive) {
            // Player closed, returned to DetailScreen!
            // 1. Refresh watch history and favorite status
            val updatedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
            historyItem = updatedHistory
            isFavorite = WatchHistoryManager.isInMyList(context, movie.url)
            val s = updatedHistory?.seasonNumber ?: (updatedHistory?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.season }) ?: selectedSeason
            if (s > 0) selectedSeason = s

            // 2. Restore focus to the specific element clicked
            isRestoringFocus = true
            when (lastPlayTarget) {
                "HERO_PLAY" -> {
                    delay(80)
                    try {
                        primaryActionFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                    isRestoringFocus = false
                }
                "HERO_RESTART" -> {
                    delay(80)
                    try {
                        restartButtonFocusRequester.requestFocus()
                    } catch (_: Exception) {
                        try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                    isRestoringFocus = false
                }
                "EPISODE" -> {
                    val historyEpNum = updatedHistory?.episodeNumber
                    val historySeasonNum = updatedHistory?.seasonNumber
                    val epFromHistory = if (historyEpNum != null && (historySeasonNum == null || historySeasonNum == selectedSeason)) {
                        seasonEpisodes.find { it.episodeNumber == historyEpNum }
                    } else null

                    val targetEpId = epFromHistory?.id ?: lastClickedEpisodeId
                    if (targetEpId != null) {
                        val epIndex = seasonEpisodes.indexOfFirst { it.id == targetEpId }
                        if (epIndex >= 0) {
                            try {
                                episodesListState.scrollToItem((epIndex - 1).coerceAtLeast(0))
                            } catch (_: Exception) {}
                        }
                        delay(100)
                        restoreEpisodeFocusId = targetEpId
                    } else {
                        delay(80)
                        try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                        isRestoringFocus = false
                    }
                }
                else -> {
                    delay(80)
                    try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                    isRestoringFocus = false
                }
            }
        }
        wasPlayerActive = isPlayerActive
    }

    // Background fetch of full metadata, episodes, similar movies, and streams
    LaunchedEffect(movie.url) {
        val needsFetch = detailResponse == null || (isSeries && seasonEpisodes.isEmpty())
        if (needsFetch) {
            isBackgroundLoading = true
            try {
                val res = withContext(Dispatchers.IO) {
                    MovieRepository.extractMovie(movie)
                }
                detailResponse = res
                if (res.episodes.isNotEmpty() && selectedSeason == 1) {
                    seasonEpisodes = res.episodes
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isBackgroundLoading = false
            }
        } else {
            isBackgroundLoading = false
        }
    }

    // Load episodes when user switches season pill
    LaunchedEffect(selectedSeason, detailResponse?.postId) {
        val res = detailResponse ?: return@LaunchedEffect
        if (!res.isSeries) return@LaunchedEffect
        if (selectedSeason == 1 && res.episodes.isNotEmpty() && seasonEpisodes.isNotEmpty()) return@LaunchedEffect

        isSeasonLoading = true
        try {
            val postIdInt = res.postId?.toIntOrNull() ?: -1
            val eps = MovieRepository.fetchEpisodesForSeason(
                postId = postIdInt,
                season = selectedSeason,
                fallbackTitle = res.title ?: movie.title,
                fallbackBackdrop = res.backdrop ?: movie.backdrop ?: movie.image,
                fallbackGallery = res.gallery ?: emptyList(),
                serieUrl = movie.url
            )
            seasonEpisodes = eps
        } catch (_: Exception) {
        } finally {
            isSeasonLoading = false
        }
    }

    // Auto-scroll episodes carousel to the resume episode when loaded
    LaunchedEffect(seasonEpisodes, historyItem, selectedSeason) {
        val h = historyItem
        val targetEpNum = h?.episodeNumber ?: (h?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.episode })
        val targetSeasonNum = h?.seasonNumber ?: (h?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.season }) ?: 1
        if (targetEpNum != null && selectedSeason == targetSeasonNum && seasonEpisodes.isNotEmpty()) {
            val epIndex = seasonEpisodes.indexOfFirst { it.episodeNumber == targetEpNum }
            if (epIndex > 0) {
                try {
                    episodesListState.scrollToItem((epIndex - 1).coerceAtLeast(0))
                } catch (_: Exception) {}
            }
        }
    }

    // Auto-scroll season selector so the active season pill is visible
    LaunchedEffect(selectedSeason, detailResponse?.seasonsCount) {
        val totalSeasons = (detailResponse?.seasonsCount ?: 1).coerceAtLeast(1)
        if (totalSeasons > 1 && selectedSeason > 1) {
            try {
                // Item 0 is "TEMPORADA:" label, Item 1 is Season 1, etc.
                seasonsListState.animateScrollToItem((selectedSeason - 1).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }



    // High-resolution 16:9 backdrop & dynamic palette extraction
    val rawBackdropUrl = detailResponse?.gallery?.firstOrNull { it.contains("image.tmdb.org") }
        ?: detailResponse?.backdrop
        ?: movie.backdrop
        ?: movie.image
    val backdropUrl = rawBackdropUrl
        .replace("/t/p/w500/", "/t/p/original/")
        .replace("/t/p/w780/", "/t/p/original/")
        .replace("/t/p/w1280/", "/t/p/original/")
    val logoUrl = detailResponse?.logo ?: movie.logo
    val cleanInitialTitle = remember(movie.title) {
        movie.title.replace(Regex("\\s*-\\s*T\\d+[:E\\d+]*.*", RegexOption.IGNORE_CASE), "").trim().cleanHtml()
    }
    val displayTitle = (detailResponse?.title ?: cleanInitialTitle).cleanHtml()
    val ratingValue = detailResponse?.rating ?: movie.rating ?: "8.5"
    val yearFromTitle = remember(cleanInitialTitle) {
        Regex("\\((\\d{4})\\)").find(cleanInitialTitle)?.groupValues?.get(1)
    }
    val yearValue = detailResponse?.releaseDate?.take(4) ?: movie.year ?: yearFromTitle ?: "2024"
    val runtimeFormatted = formatRuntime(detailResponse?.runtime ?: movie.runtime)
    val seasonsCount = (detailResponse?.seasonsCount ?: 1).coerceAtLeast(1)
    val tagline = detailResponse?.tagline
    val genresList = detailResponse?.genres?.takeIf { it.isNotEmpty() }
        ?: if (isBackgroundLoading) emptyList() else listOf("Acción", "Ciencia Ficción", "Aventura")
    val synopsisText = (detailResponse?.overview?.takeIf { it.isNotBlank() }
        ?: movie.overview?.takeIf { it.isNotBlank() }
        ?: "$displayTitle está disponible en streaming completo en alta definición con múltiples servidores de alta velocidad y audio latino en Mochi.").cleanHtml()

    // Extract dynamic ambient color from backdrop/poster
    var dominantColor by remember(movie.url) { mutableStateOf(MochiPlumCard) }
    val animatedAmbientColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 900),
        label = "detailAmbientColor"
    )

    LaunchedEffect(backdropUrl) {
        if (backdropUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .size(300, 169)
                        .allowHardware(false)
                        .target { drawable ->
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null && !bitmap.isRecycled) {
                                Palette.from(bitmap).generate { palette ->
                                    val rgb = palette?.vibrantSwatch?.rgb
                                        ?: palette?.dominantSwatch?.rgb
                                    rgb?.let {
                                        val hsl = FloatArray(3)
                                        ColorUtils.colorToHSL(it, hsl)
                                        hsl[2] = hsl[2].coerceIn(0.35f, 0.58f)
                                        dominantColor = Color(ColorUtils.HSLToColor(hsl))
                                    }
                                }
                            }
                        }
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }
    }

    // Saved progress calculation
    val hasSavedProgress = remember(historyItem) {
        val h = historyItem
        h != null && h.playbackPositionMs > 15_000L &&
            (h.durationMs <= 0L || h.playbackPositionMs < h.durationMs - 30_000L)
    }
    val progressFraction = remember(historyItem) {
        val h = historyItem
        if (h != null && h.durationMs > 0L) {
            (h.playbackPositionMs.toFloat() / h.durationMs.toFloat()).coerceIn(0.04f, 0.98f)
        } else 0.35f
    }

    val deepBackground = MochiDeepPlum
    val warmIvory = MochiCream
    val softSilver = MochiLavender
    val mutedSlate = MochiMutedLilac
    val glassSurface = Color(0xD9160E1E)
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground)
    ) {
        // 1. Edge-to-Edge 16:9 Cinematic Backdrop (Static, 4K Original)
        Crossfade(
            targetState = backdropUrl,
            animationSpec = tween(durationMillis = 600),
            label = "backdropCrossfade",
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(540.dp)
                .align(Alignment.TopEnd)
        ) { targetBackdrop ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetBackdrop)
                    .size(coil.size.Size.ORIGINAL)
                    .allowRgb565(false)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .crossfade(true)
                    .build(),
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Scrim A: Left-to-Right Cinematic Gradient (Protects left hero text while keeping right side vivid)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to deepBackground,
                        0.34f to deepBackground.copy(alpha = 0.96f),
                        0.55f to deepBackground.copy(alpha = 0.74f),
                        0.76f to deepBackground.copy(alpha = 0.28f),
                        1.0f to Color.Transparent
                    )
                )
        )

        // 3. Scrim B: Subtle Ambient Glow (refined cinema tone, no harsh color tint)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAmbientColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(280f, 360f),
                        radius = 850f
                    )
                )
        )

        // 4. Scrim C: Vertical Vignette matching PlayerScreen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to deepBackground.copy(alpha = 0.84f),
                        0.16f to Color.Transparent,
                        0.52f to deepBackground.copy(alpha = 0.70f),
                        0.78f to deepBackground.copy(alpha = 0.96f),
                        1.0f to deepBackground
                    )
                )
        )

        // 5. Main Foreground Scrollable Content (Two-Plane Layout)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 44.dp)
        ) {
            // =========================================================================
            // PLANO 1: HERO VIEWPORT (Monumental, amplio y cinematográfico)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewResponder(heroBringIntoViewResponder)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // 0. Top Navigation & Sync Status Bar (Inside scroll)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBackFocused) warmIvory else pillSurface,
                            contentColor = if (isBackFocused) Color.Black else warmIvory
                        ),
                        border = if (isBackFocused) BorderStroke(2.dp, warmIvory)
                        else BorderStroke(1.dp, subtleBorder),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .focusRequester(backButtonFocusRequester)
                            .focusProperties {
                                down = primaryActionFocusRequester
                                canFocus = !isRestoringFocus
                            }
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                    try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else false
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ATRÁS",
                            fontSize = 11.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp
                        )
                    }

                    if (isBackgroundLoading || isEpisodeResolving) {
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(50))
                                .background(pillSurface)
                                .border(1.dp, subtleBorder, RoundedCornerShape(50))
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = warmIvory,
                                strokeWidth = 1.8.dp,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEpisodeResolving) "PREPARANDO SECUENCIA..." else "SINCRONIZANDO METADATOS...",
                                color = warmIvory,
                                fontSize = 10.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                // HERO LEFT COLUMN
                Column(
                    modifier = Modifier.fillMaxWidth(0.66f)
                ) {
                    // 1. Pre-title Editorial Kicker
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categoryLabel = when {
                            movie.type == "anime" || movie.url.contains("/animes/") -> "ANIME OFICIAL"
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

                    // 3. Metadata Badges Strip (Editorial Glass & Warm Ivory)
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
                                if (isBackgroundLoading && detailResponse == null) "Serie"
                                else if (seasonsCount == 1) "1 Temporada"
                                else "$seasonsCount Temporadas"
                            } else runtimeFormatted,
                            color = Color(0xFFD1D5DB),
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium
                        )

                        // Quality Badge (4K ULTRA HD / FULL HD)
                        val cleanQuality = (movie.quality?.takeIf { it.isNotBlank() } ?: "4K Ultra HD")
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

                    // 5. Synopsis (3 full lines, comfortable reading)
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

                    // 6. Continue Watching Progress Bar (Warm Ivory style matching PlayerScreen timeline)
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
                                text = formatRemainingTime(historyItem!!.playbackPositionMs, historyItem!!.durationMs).uppercase(),
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

                    // 7. Action Buttons Row (Solo Reanudar/Reproducir, Reiniciar y Mi Lista; SIN Tráiler ni Información)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isContentAvailable) {
                            // Primary CTA: Play / Resume (Comportamiento de foco igual al reproductor)
                            var isPlayFocused by remember { mutableStateOf(false) }
                            val savedSeriesInfo = remember(historyItem, isSeries) {
                                if (isSeries) historyItem?.let { com.tvpro.app.ui.components.extractSeriesInfo(it) } else null
                            }
                            val isCurrentSeasonTheSavedOne = (savedSeriesInfo != null && (savedSeriesInfo.season ?: 1) == selectedSeason)
                            val primaryLabel = when {
                                isCurrentSeasonTheSavedOne && hasSavedProgress -> "REANUDAR ${savedSeriesInfo!!.formattedLabel}"
                                isCurrentSeasonTheSavedOne -> "REPRODUCIR ${savedSeriesInfo!!.formattedLabel}"
                                hasSavedProgress && !isSeries -> "REANUDAR"
                                isSeries -> "REPRODUCIR T$selectedSeason:E1"
                                else -> "REPRODUCIR"
                            }
                            Button(
                                onClick = {
                                    if (isEpisodeResolving) return@Button
                                    lastPlayTarget = "HERO_PLAY"
                                    if (isSeries) {
                                        val targetSeason = if (isCurrentSeasonTheSavedOne) (savedSeriesInfo?.season ?: selectedSeason) else selectedSeason
                                        val targetEpNum = if (isCurrentSeasonTheSavedOne) (savedSeriesInfo?.episode ?: 1) else 1
                                        val targetEp = seasonEpisodes.find { it.seasonNumber == targetSeason && it.episodeNumber == targetEpNum }
                                            ?: seasonEpisodes.find { it.episodeNumber == targetEpNum }
                                        isEpisodeResolving = true
                                        scope.launch {
                                            val epStreams = MovieRepository.resolveEpisodeStreams(
                                                episodeId = targetEp?.id ?: 0,
                                                seriesTitle = displayTitle,
                                                fallbackStreams = streams,
                                                seasonNumber = targetSeason,
                                                episodeNumber = targetEpNum,
                                                seriesPostId = detailResponse?.postId?.toIntOrNull(),
                                                serieUrl = movie.url,
                                                episodeSlug = targetEp?.slug ?: "",
                                                seriesPostIdStr = detailResponse?.postId
                                            )
                                            isEpisodeResolving = false
                                            val epMovie = movie.copy(
                                                title = "$displayTitle - T${targetSeason}:E${targetEpNum}",
                                                postId = detailResponse?.postId?.toIntOrNull() ?: movie.postId,
                                                type = "series"
                                            )
                                            if (epStreams.isNotEmpty()) {
                                                onPlayClick(epMovie, epStreams.firstOrNull()?.url, epStreams)
                                            }
                                        }
                                    } else if (detailResponse == null || streams.isEmpty()) {
                                        isEpisodeResolving = true
                                        scope.launch {
                                            val resolved = try {
                                                withContext(Dispatchers.IO) {
                                                    MovieRepository.extractMovie(movie)
                                                }
                                            } catch (_: Exception) {
                                                null
                                            }
                                            if (resolved != null) {
                                                detailResponse = resolved
                                            }
                                            val finalStreams = resolved?.streams?.takeIf { it.isNotEmpty() } ?: streams
                                            isEpisodeResolving = false
                                            val playMovie = movie.copy(
                                                postId = resolved?.postId?.toIntOrNull() ?: detailResponse?.postId?.toIntOrNull() ?: movie.postId,
                                                type = if (isSeries) "series" else "movie"
                                            )
                                            if (finalStreams.isNotEmpty()) {
                                                onPlayClick(playMovie, finalStreams.firstOrNull()?.url, finalStreams)
                                            }
                                        }
                                    } else {
                                        val playMovie = movie.copy(
                                            postId = detailResponse?.postId?.toIntOrNull() ?: movie.postId,
                                            type = if (isSeries) "series" else "movie"
                                        )
                                        onPlayClick(playMovie, streamUrl ?: streams.firstOrNull()?.url, streams)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayFocused) warmIvory else pillSurface,
                                    contentColor = if (isPlayFocused) Color.Black else warmIvory
                                ),
                                border = if (isPlayFocused) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, subtleBorder),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .height(42.dp)
                                    .focusRequester(playButtonFocusRequester)
                                    .focusProperties {
                                        up = backButtonFocusRequester
                                        down = firstTabFocusRequester
                                        right = if (hasSavedProgress) restartButtonFocusRequester else myListFocusRequester
                                    }
                                    .onFocusChanged { isPlayFocused = it.isFocused }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                            try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                            try { firstTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        } else false
                                    }
                            ) {
                                if (isEpisodeResolving) {
                                    CircularProgressIndicator(
                                        color = if (isPlayFocused) Color.Black else warmIvory,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ABRIENDO...",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 1.6.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = primaryLabel,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 1.6.sp
                                    )
                                }
                            }

                            // Secondary Button: Restart from beginning (visible when saved progress exists)
                            if (hasSavedProgress) {
                                var isRestartFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        if (isEpisodeResolving) return@Button
                                        lastPlayTarget = "HERO_RESTART"
                                        WatchHistoryManager.resetMoviePosition(context, movie.url)
                                        historyItem = WatchHistoryManager.getMovieHistory(context, movie.url)
                                        if (isSeries) {
                                            val targetSeason = if (isCurrentSeasonTheSavedOne) (savedSeriesInfo?.season ?: selectedSeason) else selectedSeason
                                            val targetEpNum = if (isCurrentSeasonTheSavedOne) (savedSeriesInfo?.episode ?: 1) else 1
                                            val targetEp = seasonEpisodes.find { it.seasonNumber == targetSeason && it.episodeNumber == targetEpNum }
                                                ?: seasonEpisodes.find { it.episodeNumber == targetEpNum }
                                            isEpisodeResolving = true
                                            scope.launch {
                                                val epStreams = MovieRepository.resolveEpisodeStreams(
                                                    episodeId = targetEp?.id ?: 0,
                                                    seriesTitle = displayTitle,
                                                    fallbackStreams = streams,
                                                    seasonNumber = targetSeason,
                                                    episodeNumber = targetEpNum,
                                                    seriesPostId = detailResponse?.postId?.toIntOrNull(),
                                                    serieUrl = movie.url,
                                                    episodeSlug = targetEp?.slug ?: "",
                                                    seriesPostIdStr = detailResponse?.postId
                                                )
                                                isEpisodeResolving = false
                                                val epMovie = movie.copy(
                                                    title = "$displayTitle - T${targetSeason}:E${targetEpNum}",
                                                    postId = detailResponse?.postId?.toIntOrNull() ?: movie.postId,
                                                    type = "series"
                                                )
                                                if (epStreams.isNotEmpty()) {
                                                    onPlayClick(epMovie, epStreams.firstOrNull()?.url, epStreams)
                                                }
                                            }
                                        } else if (streams.isEmpty()) {
                                            isEpisodeResolving = true
                                            scope.launch {
                                                val resolved = try {
                                                    withContext(Dispatchers.IO) {
                                                        MovieRepository.extractMovie(movie)
                                                    }
                                                } catch (_: Exception) {
                                                    null
                                                }
                                                if (resolved != null) detailResponse = resolved
                                                val finalStreams = resolved?.streams?.takeIf { it.isNotEmpty() } ?: streams
                                                isEpisodeResolving = false
                                                if (finalStreams.isNotEmpty()) {
                                                    onPlayClick(movie, finalStreams.firstOrNull()?.url, finalStreams)
                                                }
                                            }
                                        } else {
                                            onPlayClick(movie, streamUrl ?: streams.firstOrNull()?.url, streams)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRestartFocused) warmIvory else pillSurface,
                                        contentColor = if (isRestartFocused) Color.Black else warmIvory
                                    ),
                                    border = if (isRestartFocused) BorderStroke(2.dp, warmIvory)
                                    else BorderStroke(1.dp, subtleBorder),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .height(42.dp)
                                        .focusRequester(restartButtonFocusRequester)
                                        .focusProperties {
                                            up = backButtonFocusRequester
                                            down = firstTabFocusRequester
                                            left = playButtonFocusRequester
                                            right = myListFocusRequester
                                        }
                                        .onFocusChanged { isRestartFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                                try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                                try { firstTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = "Reiniciar",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "REINICIAR",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.6.sp
                                    )
                                }
                            }
                        } else {
                            // Badge Informativo: AÚN NO DISPONIBLE
                            Surface(
                                color = MochiPlumCard,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.2.dp, MochiStrawberryPink.copy(alpha = 0.7f)),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MochiStrawberryPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AÚN NO DISPONIBLE",
                                        color = warmIvory,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 1.4.sp
                                    )
                                }
                            }
                        }

                        // My List Button (Sin Tráiler ni Info)
                        var isFavoriteFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                isFavorite = WatchHistoryManager.toggleMyList(context, movie)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isFavoriteFocused -> warmIvory
                                    isFavorite -> Color(0xFF1A202C)
                                    else -> pillSurface
                                },
                                contentColor = if (isFavoriteFocused) Color.Black else warmIvory
                            ),
                            border = when {
                                isFavoriteFocused -> BorderStroke(2.dp, warmIvory)
                                isFavorite -> BorderStroke(1.2.dp, warmIvory.copy(alpha = 0.5f))
                                else -> BorderStroke(1.dp, subtleBorder)
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .focusRequester(myListFocusRequester)
                                .focusProperties {
                                    up = backButtonFocusRequester
                                    down = firstTabFocusRequester
                                    left = if (isContentAvailable) {
                                        if (hasSavedProgress) restartButtonFocusRequester else playButtonFocusRequester
                                    } else FocusRequester.Default
                                }
                                .onFocusChanged { isFavoriteFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                        try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                        try { firstTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    } else false
                                }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isFavorite) "Quitar de Mi Lista" else "Añadir a Mi Lista",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFavorite) "EN MI LISTA" else "MI LISTA",
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.6.sp
                            )
                        }
                    }

                    // Banner explicativo cuando el contenido aún no está disponible
                    if (!isBackgroundLoading && !isContentAvailable) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MochiStrawberryPink.copy(alpha = 0.12f))
                                .border(1.dp, MochiStrawberryPink.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MochiStrawberryPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Aún no disponemos de enlaces para reproducir este contenido en los servidores. ¡Agrégalo a tu lista para estar al tanto de su estreno!",
                                color = warmIvory,
                                fontFamily = Inter,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Generous separation between Action Buttons and Tabs
            Spacer(modifier = Modifier.height(32.dp))

            // =========================================================================
            // PLANO 2: TABS & CONTENIDO COMPLETO (Focus capsule + active indicator)
            // =========================================================================
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableTabs.forEachIndexed { index, tab ->
                        var isTabFocused by remember { mutableStateOf(false) }
                        val isSelected = (selectedTab == tab)
                        val tabTitle = when (tab) {
                            DetailTab.EPISODES -> if (isBackgroundLoading && seasonEpisodes.isEmpty()) "EPISODIOS" else "EPISODIOS (${seasonEpisodes.size})"
                            DetailTab.SIMILAR -> "MÁS COMO ESTO"
                            DetailTab.EXTRAS -> "ESCENAS Y DETALLES"
                        }

                        val tabFocusRequester = when (tab) {
                            DetailTab.EPISODES -> episodesTabFocusRequester
                            DetailTab.SIMILAR -> similarTabFocusRequester
                            DetailTab.EXTRAS -> extrasTabFocusRequester
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .focusRequester(tabFocusRequester)
                                .focusProperties {
                                    up = primaryActionFocusRequester
                                    if (tab == DetailTab.EPISODES) {
                                        down = selectedSeasonFocusRequester
                                    }
                                }
                                .onFocusChanged { focusState ->
                                    isTabFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        selectedTab = tab
                                    }
                                }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                        scope.launch {
                                            try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                                            delay(60)
                                            scrollState.animateScrollTo(0)
                                        }
                                        true
                                    } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown && tab == DetailTab.EPISODES) {
                                        try {
                                            selectedSeasonFocusRequester.requestFocus()
                                            true
                                        } catch (_: Exception) {
                                            false
                                        }
                                    } else false
                                }
                                .clickable { selectedTab = tab }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        when {
                                            isTabFocused -> warmIvory
                                            isSelected -> Color(0xFF1B202C)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isTabFocused) 2.dp else if (isSelected) 1.2.dp else 1.dp,
                                        color = when {
                                            isTabFocused -> warmIvory
                                            isSelected -> warmIvory.copy(alpha = 0.55f)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = tabTitle,
                                    color = when {
                                        isTabFocused -> Color.Black
                                        isSelected -> warmIvory
                                        else -> Color(0xFF8E95A5)
                                    },
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = if (isTabFocused || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    letterSpacing = 1.6.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Active underline bar
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(if (isSelected) 36.dp else 0.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isTabFocused) warmIvory else warmIvory.copy(alpha = 0.70f))
                            )
                        }
                    }
                }

                // Subtle full-width divider line under tabs
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.10f))
                )
            }

                Spacer(modifier = Modifier.height(6.dp))

                // =========================================================================
                // 9. TAB CONTENT AREA
                // =========================================================================
                when (selectedTab) {
                    // -----------------------------------------------------------------
                    // TAB 1: EPISODES & SEASONS SELECTOR (Matching PlayerScreen Modal)
                    // -----------------------------------------------------------------
                    DetailTab.EPISODES -> {
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
                                                        selectedSeason = s
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                        .clickable { selectedSeason = s }
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
                                            restoreEpisodeFocusId = null
                                            isRestoringFocus = false
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
                                            .clickable {
                                                lastPlayTarget = "EPISODE"
                                                lastClickedEpisodeId = episode.id
                                                isEpisodeResolving = true
                                                scope.launch {
                                                    val epStreams = MovieRepository.resolveEpisodeStreams(
                                                        episodeId = episode.id,
                                                        seriesTitle = displayTitle,
                                                        fallbackStreams = streams,
                                                        seasonNumber = episode.seasonNumber,
                                                        episodeNumber = episode.episodeNumber,
                                                        seriesPostId = detailResponse?.postId?.toIntOrNull(),
                                                        serieUrl = movie.url,
                                                        episodeSlug = episode.slug,
                                                        seriesPostIdStr = detailResponse?.postId
                                                    )
                                                    isEpisodeResolving = false
                                                    val epMovie = movie.copy(
                                                        title = "$displayTitle - T${episode.seasonNumber}:E${episode.episodeNumber} ${episode.title.cleanHtml()}",
                                                        postId = detailResponse?.postId?.toIntOrNull() ?: movie.postId,
                                                        type = "series"
                                                    )
                                                    onPlayClick(epMovie, epStreams.firstOrNull()?.url, epStreams)
                                                }
                                            }
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
                                                // Play Circle on Focus (Warm Ivory matching PlayerScreen)
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

                    // -----------------------------------------------------------------
                    // TAB: SIMILAR TITLES ("MÁS COMO ESTO")
                    // -----------------------------------------------------------------
                    DetailTab.SIMILAR -> {
                        val similarMovies = remember(detailResponse, movie.url) {
                            detailResponse?.similarMovies?.takeIf { it.isNotEmpty() }
                                ?: MovieRepository.localCatalog.filter { it.url != movie.url }.take(10)
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 2.dp, end = 24.dp)
                        ) {
                            items(similarMovies, key = { it.url }) { simMovie ->
                                TvMovieCard(
                                    movie = simMovie,
                                    onClick = { onMovieSelect(simMovie) },
                                    modifier = Modifier
                                        .focusProperties {
                                            up = similarTabFocusRequester
                                        }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                                try { similarTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            } else false
                                        }
                                )
                            }
                        }
                    }

                    // -----------------------------------------------------------------
                    // TAB: SCENES GALLERY (FOCUSABLE) & TECHNICAL SPECIFICATIONS
                    // -----------------------------------------------------------------
                    DetailTab.EXTRAS -> {
                        val gallery = remember(detailResponse, backdropUrl, movie.image) {
                            (detailResponse?.gallery?.filter { it.isNotBlank() } ?: emptyList())
                                .ifEmpty { listOfNotNull(backdropUrl, movie.backdrop, movie.image).distinct() }
                        }

                        if (gallery.isNotEmpty()) {
                            Text(
                                text = "GALERÍA DE ESCENAS · PRESIONA OK PARA AMPLIAR",
                                fontSize = 10.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                                color = mutedSlate,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 20.dp, start = 2.dp, end = 24.dp)
                            ) {
                                items(gallery) { imgUrl ->
                                    var isSceneFocused by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .height(135.dp)
                                            .width(240.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSceneFocused) 2.dp else 1.dp,
                                                color = if (isSceneFocused) warmIvory else subtleBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .focusProperties {
                                                up = extrasTabFocusRequester
                                            }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                                    try { extrasTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            .onFocusChanged { focusState ->
                                                isSceneFocused = focusState.isFocused
                                            }
                                            .clickable { previewSceneUrl = imgUrl }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(imgUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Escena de $displayTitle",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Technical Specifications Card (Focusable for smooth D-Pad scroll)
                        var isSpecsFocused by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = glassSurface),
                            border = BorderStroke(
                                width = if (isSpecsFocused) 1.5.dp else 1.dp,
                                color = if (isSpecsFocused) warmIvory.copy(alpha = 0.6f) else subtleBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSpecsFocused = it.isFocused }
                                .focusable()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("CALIDAD DE VIDEO", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("1080p Full HD / 4K HDR10", color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Bitrate adaptativo de alta velocidad", color = mutedSlate, fontSize = 10.5.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AUDIO Y SUBTÍTULOS", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Español Latino / Original", color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Subtítulos CC configurables", color = mutedSlate, fontSize = 10.5.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("GÉNEROS", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(genresList.joinToString(", "), color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Clasificación: 16+ · TMDB ★ $ratingValue", color = mutedSlate, fontSize = 10.5.sp)
                                }
                            }
                        }
                    }
                }

                // Bottom Safe Margin Spacer so elements never touch bottom TV bezel
                Spacer(modifier = Modifier.height(80.dp))
            }


        // =========================================================================
        // 10. FULL-SCREEN SCENE LIGHTBOX OVERLAY
        // =========================================================================
        previewSceneUrl?.let { sceneUrl ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF206080C))
                    .clickable { previewSceneUrl = null }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(sceneUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Vista previa de escena",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .fillMaxHeight(0.78f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.5.dp, warmIvory.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PRESIONA OK O ATRÁS PARA CERRAR LA VISTA PREVIA",
                        color = warmIvory.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.6.sp
                    )
                }
            }
        }
    }
}

