@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.tvpro.app.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.app.ui.components.cleanHtml
import com.tvpro.app.ui.components.detail.*
import com.tvpro.app.ui.theme.*
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var lastPlayTarget by remember(movie.url) { mutableStateOf<String?>(null) }
    var lastClickedEpisodeId by remember(movie.url) { mutableStateOf<Int?>(null) }
    var restoreEpisodeFocusId by remember { mutableStateOf<Int?>(null) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var wasPlayerActive by remember { mutableStateOf(false) }

    val heroBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect = Rect.Zero
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
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
    val isAnime = remember(movie.type, movie.url) {
        movie.type == "anime" || movie.url.contains("/animes/")
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

    // Active bottom tab
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

    // Verify and filter streams in background
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
            true
        } else if (isSeries) {
            seasonEpisodes.isNotEmpty() || !detailResponse?.episodes.isNullOrEmpty() || streams.isNotEmpty() || (movie.postId ?: 0) > 0
        } else {
            streams.isNotEmpty() || !detailResponse?.streams.isNullOrEmpty() || (movie.postId ?: 0) > 0 || (detailResponse?.postId?.toIntOrNull() ?: 0) > 0
        }
    }
    val primaryActionFocusRequester = if (isContentAvailable) playButtonFocusRequester else myListFocusRequester

    LaunchedEffect(isBackgroundLoading, isContentAvailable) {
        delay(80)
        try {
            primaryActionFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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

    LaunchedEffect(isRestoringFocus) {
        if (isRestoringFocus) {
            delay(700)
            isRestoringFocus = false
        }
    }

    LaunchedEffect(isPlayerActive) {
        if (wasPlayerActive && !isPlayerActive) {
            val updatedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
            historyItem = updatedHistory
            isFavorite = WatchHistoryManager.isInMyList(context, movie.url)
            val s = updatedHistory?.seasonNumber ?: (updatedHistory?.let { com.tvpro.app.ui.components.extractSeriesInfo(it)?.season }) ?: selectedSeason
            if (s > 0) selectedSeason = s

            isRestoringFocus = true
            when (lastPlayTarget) {
                "HERO_PLAY" -> {
                    delay(80)
                    try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
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

    LaunchedEffect(selectedSeason, detailResponse?.seasonsCount) {
        val totalSeasons = (detailResponse?.seasonsCount ?: 1).coerceAtLeast(1)
        if (totalSeasons > 1 && selectedSeason > 1) {
            try {
                seasonsListState.animateScrollToItem((selectedSeason - 1).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    // High-resolution backdrop & dynamic palette extraction
    val rawBackdropUrl = detailResponse?.gallery?.firstOrNull { it.contains("image.tmdb.org") }
        ?: detailResponse?.backdrop
        ?: movie.backdrop
        ?: movie.image
    val backdropUrl = rawBackdropUrl
        .replace("/t/p/w500/", "/t/p/original/")
        .replace("/t/p/w780/", "/t/p/original/")
        .replace("/t/p/w1280/", "/t/p/original/")
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
    val genresList = detailResponse?.genres?.takeIf { it.isNotEmpty() }
        ?: if (isBackgroundLoading) emptyList() else listOf("Acción", "Ciencia Ficción", "Aventura")
    val synopsisText = (detailResponse?.overview?.takeIf { it.isNotBlank() }
        ?: movie.overview?.takeIf { it.isNotBlank() }
        ?: "$displayTitle está disponible en streaming completo en alta definición con múltiples servidores de alta velocidad y audio latino en Mochi.").cleanHtml()

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

    val deepBackground = MochiDeepPlum

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground)
    ) {
        // 1. Edge-to-Edge 16:9 Cinematic Backdrop
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

        // 2. Scrims
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

        // 3. Main Foreground Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 44.dp)
        ) {
            // PLANO 1: HERO VIEWPORT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewResponder(heroBringIntoViewResponder)
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                DetailTopBar(
                    onBackClick = onBackClick,
                    backButtonFocusRequester = backButtonFocusRequester,
                    downFocusRequester = primaryActionFocusRequester,
                    isRestoringFocus = isRestoringFocus,
                    isBackgroundLoading = isBackgroundLoading,
                    isEpisodeResolving = isEpisodeResolving
                )

                Spacer(modifier = Modifier.height(18.dp))

                DetailHeroContent(
                    displayTitle = displayTitle,
                    ratingValue = ratingValue,
                    yearValue = yearValue,
                    runtimeFormatted = runtimeFormatted,
                    seasonsCount = seasonsCount,
                    isSeries = isSeries,
                    isAnime = isAnime,
                    isBackgroundLoading = isBackgroundLoading,
                    quality = movie.quality,
                    genresList = genresList,
                    synopsisText = synopsisText,
                    hasSavedProgress = hasSavedProgress,
                    historyItem = historyItem,
                    progressFraction = progressFraction
                )

                DetailActionButtons(
                    isContentAvailable = isContentAvailable,
                    isEpisodeResolving = isEpisodeResolving,
                    hasSavedProgress = hasSavedProgress,
                    isFavorite = isFavorite,
                    primaryLabel = primaryLabel,
                    isBackgroundLoading = isBackgroundLoading,
                    playButtonFocusRequester = playButtonFocusRequester,
                    restartButtonFocusRequester = restartButtonFocusRequester,
                    myListFocusRequester = myListFocusRequester,
                    backButtonFocusRequester = backButtonFocusRequester,
                    downFocusRequester = firstTabFocusRequester,
                    onPlayClick = {
                        if (isEpisodeResolving) return@DetailActionButtons
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
                    onRestartClick = {
                        if (isEpisodeResolving) return@DetailActionButtons
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
                    onFavoriteClick = {
                        isFavorite = WatchHistoryManager.toggleMyList(context, movie)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // PLANO 2: TABS & CONTENIDO COMPLETO
            DetailTabsHeader(
                availableTabs = availableTabs,
                selectedTab = selectedTab,
                episodesCount = seasonEpisodes.size,
                isBackgroundLoading = isBackgroundLoading,
                primaryActionFocusRequester = primaryActionFocusRequester,
                episodesTabFocusRequester = episodesTabFocusRequester,
                similarTabFocusRequester = similarTabFocusRequester,
                extrasTabFocusRequester = extrasTabFocusRequester,
                selectedSeasonFocusRequester = selectedSeasonFocusRequester,
                onTabSelected = { selectedTab = it },
                onScrollToTop = {
                    scope.launch {
                        try { primaryActionFocusRequester.requestFocus() } catch (_: Exception) {}
                        delay(60)
                        scrollState.animateScrollTo(0)
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            when (selectedTab) {
                DetailTab.EPISODES -> {
                    DetailEpisodesRail(
                        seasonsCount = seasonsCount,
                        selectedSeason = selectedSeason,
                        onSeasonSelect = { selectedSeason = it },
                        seasonEpisodes = seasonEpisodes,
                        isSeasonLoading = isSeasonLoading,
                        restoreEpisodeFocusId = restoreEpisodeFocusId,
                        onEpisodeFocusRestored = {
                            restoreEpisodeFocusId = null
                            isRestoringFocus = false
                        },
                        onEpisodeClick = { episode ->
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
                        },
                        backdropUrl = backdropUrl,
                        seasonsListState = seasonsListState,
                        episodesListState = episodesListState,
                        episodesTabFocusRequester = episodesTabFocusRequester,
                        selectedSeasonFocusRequester = selectedSeasonFocusRequester
                    )
                }
                DetailTab.SIMILAR -> {
                    val similarMovies = remember(detailResponse, movie.url) {
                        detailResponse?.similarMovies?.takeIf { it.isNotEmpty() }
                            ?: MovieRepository.localCatalog.filter { it.url != movie.url }.take(10)
                    }
                    DetailSimilarRail(
                        similarMovies = similarMovies,
                        similarTabFocusRequester = similarTabFocusRequester,
                        onMovieSelect = onMovieSelect
                    )
                }
                DetailTab.EXTRAS -> {
                    val gallery = remember(detailResponse, backdropUrl, movie.image) {
                        (detailResponse?.gallery?.filter { it.isNotBlank() } ?: emptyList())
                            .ifEmpty { listOfNotNull(backdropUrl, movie.backdrop, movie.image).distinct() }
                    }
                    DetailExtrasContent(
                        gallery = gallery,
                        displayTitle = displayTitle,
                        genresList = genresList,
                        ratingValue = ratingValue,
                        extrasTabFocusRequester = extrasTabFocusRequester,
                        onSceneClick = { previewSceneUrl = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Full-Screen Scene Lightbox
        previewSceneUrl?.let { sceneUrl ->
            DetailLightboxOverlay(
                sceneUrl = sceneUrl,
                onClose = { previewSceneUrl = null }
            )
        }
    }
}
