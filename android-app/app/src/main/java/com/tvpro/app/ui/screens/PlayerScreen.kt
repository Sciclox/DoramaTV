package com.tvpro.app.ui.screens

import android.app.Activity
import android.net.Uri
import com.tvpro.app.PlayerKeyDispatcher
import androidx.activity.compose.BackHandler
import android.view.ViewGroup
import android.view.WindowManager
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.tvpro.app.ui.components.cleanHtml
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.model.SubtitleTrackData
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.theme.*
import com.tvpro.core.domain.PlayerTimeUtils
import com.tvpro.app.ui.components.player.DisplayLanguageOption
import com.tvpro.app.ui.components.player.ExoAudioTrackItem
import com.tvpro.app.ui.components.player.ExoSubtitleTrackItem
import com.tvpro.app.ui.components.player.TvPlayerAudioSubtitleModal
import com.tvpro.app.ui.components.player.TvPlayerEpisodesModal
import com.tvpro.app.ui.components.player.getCleanServerDisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    movie: MovieItem,
    streamUrl: String? = null,
    streams: List<StreamItem> = emptyList(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentMovieTitle by remember(movie.title) { mutableStateOf(movie.title) }
    var dynamicStreams by remember(movie.url) { mutableStateOf<List<StreamItem>>(emptyList()) }
    var isExtracting by remember(movie.url) { mutableStateOf(streams.isEmpty() && streamUrl.isNullOrBlank()) }
    val failedStreamUrls = remember(movie.url) { mutableStateListOf<String>() }
    var detailResponse by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(null) }
    var seasonEpisodes by remember(movie.url) { mutableStateOf<List<EpisodeItem>>(emptyList()) }
    var isSeasonLoading by remember { mutableStateOf(false) }

    val (cleanMainTitle, activeSequenceLabel, currentSeasonNum, currentEpNum) = remember(currentMovieTitle) {
        val raw = currentMovieTitle.trim()
        val epRegex = Regex("""^(.*?)\s*-\s*T(\d+):E(\d+).*$""", RegexOption.IGNORE_CASE)
        val match = epRegex.find(raw)
        if (match != null) {
            val seriesName = match.groupValues[1].trim().ifBlank { raw }
            val sNum = match.groupValues[2].toIntOrNull() ?: 1
            val eNum = match.groupValues[3].toIntOrNull() ?: 1
            listOf(seriesName.uppercase(), "TEMPORADA $sNum : EPISODIO $eNum", sNum.toString(), eNum.toString())
        } else {
            val tempEpRegex = Regex("""^(.*?)(?:\s*[:\-·]\s*)?Temporada\s*(\d+)\s*[:\-·]?\s*Episodio\s*(\d+).*$""", RegexOption.IGNORE_CASE)
            val match2 = tempEpRegex.find(raw)
            if (match2 != null) {
                val seriesName = match2.groupValues[1].trim().ifBlank { raw }
                val sNum = match2.groupValues[2].toIntOrNull() ?: 1
                val eNum = match2.groupValues[3].toIntOrNull() ?: 1
                listOf(seriesName.uppercase(), "TEMPORADA $sNum : EPISODIO $eNum", sNum.toString(), eNum.toString())
            } else {
                listOf(raw.uppercase(), raw.uppercase(), "1", "1")
            }
        }
    }

    val activeSeasonInt = currentSeasonNum.toIntOrNull() ?: 1
    val activeEpisodeInt = currentEpNum.toIntOrNull() ?: 1
    var selectedModalSeason by remember(activeSeasonInt) { mutableIntStateOf(activeSeasonInt) }

    val isSeriesContent = remember(detailResponse, movie, currentMovieTitle) {
        val initIsSeries = movie.type == "series" || movie.type == "anime" || movie.type == "drama" ||
            movie.url.contains("/tvshows/") || movie.url.contains("/series/") || movie.url.contains("/animes/") || movie.url.contains("/novelas/") ||
            currentMovieTitle.contains(Regex("""- T\d+:E\d+""", RegexOption.IGNORE_CASE))
        detailResponse?.isSeries ?: initIsSeries
    }

    val hasEpisodesButton = remember(isSeriesContent, seasonEpisodes, detailResponse) {
        if (!isSeriesContent) {
            false
        } else {
            val totalSeasons = detailResponse?.seasonsCount ?: 1
            !(detailResponse != null && totalSeasons <= 1 && seasonEpisodes.isNotEmpty() && seasonEpisodes.size <= 1)
        }
    }


    val nextEpisode: EpisodeItem? = remember(isSeriesContent, seasonEpisodes, activeSeasonInt, activeEpisodeInt, detailResponse, cleanMainTitle) {
        if (!isSeriesContent) null
        else {
            val totalSeasons = (detailResponse?.seasonsCount ?: 1).coerceAtLeast(1)
            // 1. Buscar siguiente episodio en la misma temporada
            val nextInSeason = seasonEpisodes.firstOrNull { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt + 1 }
            if (nextInSeason != null) {
                nextInSeason
            } else {
                // 2. Si la lista actual de episodios está secuenciada
                val currentIdx = seasonEpisodes.indexOfFirst { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt }
                if (currentIdx != -1 && currentIdx + 1 < seasonEpisodes.size) {
                    seasonEpisodes[currentIdx + 1]
                } else if (seasonEpisodes.isEmpty()) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -(activeSeasonInt * 100 + activeEpisodeInt + 1),
                        episodeNumber = activeEpisodeInt + 1,
                        seasonNumber = activeSeasonInt,
                        title = "Episodio ${activeEpisodeInt + 1}",
                        overview = "Episodio ${activeEpisodeInt + 1} de la Temporada $activeSeasonInt de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else if (activeSeasonInt < totalSeasons) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -((activeSeasonInt + 1) * 100 + 1),
                        episodeNumber = 1,
                        seasonNumber = activeSeasonInt + 1,
                        title = "Episodio 1",
                        overview = "Episodio 1 de la Temporada ${activeSeasonInt + 1} de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else null
            }
        }
    }


    val previousEpisode: EpisodeItem? = remember(isSeriesContent, seasonEpisodes, activeSeasonInt, activeEpisodeInt, detailResponse, cleanMainTitle) {
        if (!isSeriesContent) null
        else {
            // 1. Buscar episodio anterior en la misma temporada
            val prevInSeason = seasonEpisodes.firstOrNull { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt - 1 }
            if (prevInSeason != null) {
                prevInSeason
            } else {
                // 2. Si la lista actual de episodios está secuenciada
                val currentIdx = seasonEpisodes.indexOfFirst { it.seasonNumber == activeSeasonInt && it.episodeNumber == activeEpisodeInt }
                if (currentIdx > 0 && currentIdx - 1 < seasonEpisodes.size) {
                    seasonEpisodes[currentIdx - 1]
                } else if (activeEpisodeInt > 1) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -(activeSeasonInt * 100 + activeEpisodeInt - 1),
                        episodeNumber = activeEpisodeInt - 1,
                        seasonNumber = activeSeasonInt,
                        title = "Episodio ${activeEpisodeInt - 1}",
                        overview = "Episodio ${activeEpisodeInt - 1} de la Temporada $activeSeasonInt de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else if (activeSeasonInt > 1) {
                    val defaultBackdrop = detailResponse?.backdrop ?: movie.backdrop ?: movie.image
                    EpisodeItem(
                        id = -((activeSeasonInt - 1) * 100 + 1),
                        episodeNumber = 1,
                        seasonNumber = activeSeasonInt - 1,
                        title = "Episodio 1",
                        overview = "Episodio 1 de la Temporada ${activeSeasonInt - 1} de $cleanMainTitle.",
                        image = defaultBackdrop,
                        runtime = "45 min"
                    )
                } else null
            }
        }
    }


    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var isNextEpisodeDismissed by remember(currentMovieTitle) { mutableStateOf(false) }
    var nextEpisodeCountdownSeconds by remember(currentMovieTitle) { mutableIntStateOf(10) }
    var consecutiveAutoPlayCount by remember { mutableIntStateOf(0) }
    var nextEpisodeSelectedButton by remember(currentMovieTitle, showNextEpisodePrompt) { mutableIntStateOf(0) }
    val nextEpisodeFocusRequester = remember { FocusRequester() }
    val nextEpisodeCancelFocusRequester = remember { FocusRequester() }

    // Silently pre-fetch streams for the next episode in background so transitions are instant
    LaunchedEffect(nextEpisode?.id, nextEpisode?.seasonNumber, nextEpisode?.episodeNumber) {
        val nEp = nextEpisode ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(3500L)
                MovieRepository.resolveEpisodeStreams(
                    episodeId = nEp.id,
                    seriesTitle = cleanMainTitle,
                    fallbackStreams = emptyList(),
                    seasonNumber = nEp.seasonNumber,
                    episodeNumber = nEp.episodeNumber,
                    seriesPostId = detailResponse?.postId?.toIntOrNull(),
                    serieUrl = movie.url,
                    episodeSlug = nEp.slug,
                    seriesPostIdStr = detailResponse?.postId ?: movie.postId?.toString()
                )
            } catch (_: Exception) {}
        }
    }


    LaunchedEffect(selectedModalSeason, detailResponse?.postId) {
        val res = detailResponse ?: return@LaunchedEffect
        if (!res.isSeries) return@LaunchedEffect
        if (selectedModalSeason == 1 && res.episodes.isNotEmpty() && seasonEpisodes.isNotEmpty() && seasonEpisodes.firstOrNull()?.seasonNumber == 1) return@LaunchedEffect

        isSeasonLoading = true
        try {
            val postIdInt = res.postId?.toIntOrNull() ?: -1
            val eps = withContext(Dispatchers.IO) {
                MovieRepository.fetchEpisodesForSeason(
                    postId = postIdInt,
                    season = selectedModalSeason,
                    fallbackTitle = cleanMainTitle,
                    fallbackBackdrop = res.backdrop ?: movie.backdrop ?: movie.image,
                    fallbackGallery = res.gallery ?: emptyList(),
                    serieUrl = movie.url
                )
            }
            if (eps.isNotEmpty()) {
                seasonEpisodes = eps
            }
        } catch (_: Exception) {
        } finally {
            isSeasonLoading = false
        }
    }


    // Prepare streams list with verified working servers and automatic hiding of failed servers
    val availableStreams = remember(streams, streamUrl, movie.url, dynamicStreams, failedStreamUrls.size) {
        val baseList = when {
            dynamicStreams.isNotEmpty() -> dynamicStreams
            streams.isNotEmpty() -> streams
            !streamUrl.isNullOrBlank() -> listOf(StreamItem(url = streamUrl, type = "EMBED", source = "Servidor 1: Principal HD"))
            else -> MovieRepository.getStreamsForMovie(movie.url)
        }
        android.util.Log.e("TVPRO_PLAY", "availableStreams remember: dynamicStreams=${dynamicStreams.size}, streams=${streams.size}, baseList=${baseList.size}")
        val aliveList = baseList
            .filter { it.url.isNotBlank() && it.url !in failedStreamUrls }
            .distinctBy { it.url }
            .sortedWith(
                compareBy<StreamItem> { stream ->
                    val u = stream.url.lowercase()
                    val src = (stream.source ?: "").lowercase()
                    when {
                        stream.type == "HLS" || stream.type == "MP4" || u.contains(".m3u8") || u.contains(".mp4") -> 0
                        u.contains("ok.ru") || src.contains("ok.ru") -> 1
                        u.contains("voe") || u.contains("megankid") || u.contains("arianathankspecial") || src.contains("voe") -> 2
                        u.contains("primeload") || src.contains("primeload") -> 3
                        u.contains("dood") || u.contains("do7go") || u.contains("playmogo") || src.contains("dood") -> 4
                        u.contains("flaswish") || src.contains("flashwish") -> 5
                        u.contains("streamtape") || src.contains("streamtape") -> 6
                        u.contains("uqload") || src.contains("uqload") -> 7
                        u.contains("mp4upload") || src.contains("mp4upload") -> 8
                        u.contains("vimeos") -> 9
                        u.contains("filemoon") || u.contains("byse") || src.contains("filemoon") -> 20
                        u.contains("callistanise") || u.contains("vidhide") || src.contains("vidhide") -> 21
                        u.contains("mixdrop") || src.contains("mixdrop") -> 22
                        else -> 25
                    }
                }
            )
            .mapIndexed { idx, item ->
                val rawLabel = item.source ?: "Servidor HD"
                val cleanLabel = rawLabel.replace(Regex("^Servidor\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                item.copy(source = "Servidor ${idx + 1}: $cleanLabel")
            }
            .toMutableList()

        if (aliveList.isEmpty()) {
            val fallbackCandidates = baseList.filter { it.url.isNotBlank() }
            if (fallbackCandidates.isNotEmpty()) {
                aliveList.addAll(fallbackCandidates)
            }
        }
        aliveList
    }

    val initialStreamIndex = remember(availableStreams, streamUrl) {
        val found = if (!streamUrl.isNullOrBlank()) availableStreams.indexOfFirst { it.url == streamUrl } else 0
        if (found >= 0) found else 0
    }
    var currentStreamIndex by remember(availableStreams) { mutableIntStateOf(initialStreamIndex) }
    val currentStream: StreamItem = availableStreams.getOrNull(currentStreamIndex)
        ?: availableStreams.firstOrNull()
        ?: StreamItem(url = "", type = "NONE", source = "Sin señal")
    val activeStreamUrl = currentStream.url

    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var loadedStreamUrl by remember { mutableStateOf<String?>(null) }

    var isPlaying by remember { mutableStateOf(true) }

    // Keep TV Screen On only during active playback to prevent OLED panel burn-in and allow TV screensaver
    DisposableEffect(isPlaying) {
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }


    // Clean up WebView and avoid memory leaks when leaving Player
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    onPause()
                    removeAllViews()
                    destroy()
                }
            } catch (_: Exception) {}
            webViewRef = null
        }
    }

    var isBuffering by remember { mutableStateOf(true) }
    var isVideoReady by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showEpisodesDialog by remember { mutableStateOf(false) }
    var subtitlesEnabled by remember { mutableStateOf(false) }
    var selectedSubtitleOption by remember { mutableIntStateOf(0) } // 0 = Desactivados, 1 = Español, 2 = Inglés (CC)
    var nativeWebAudioTracks by remember(activeStreamUrl) { mutableStateOf<List<String>>(emptyList()) }
    var selectedWebAudioTrackIdx by remember(activeStreamUrl) { mutableIntStateOf(0) }
    var exoAudioTracks by remember(activeStreamUrl) { mutableStateOf<List<ExoAudioTrackItem>>(emptyList()) }
    var exoSubtitleTracks by remember(activeStreamUrl) { mutableStateOf<List<ExoSubtitleTrackItem>>(emptyList()) }
    var nativeWebSubtitleTracks by remember(activeStreamUrl) { mutableStateOf<List<String>>(emptyList()) }
    var selectedWebSubtitleTrackIdx by remember(activeStreamUrl) { mutableIntStateOf(0) }
    var userSelectedAudioTrack by remember(activeStreamUrl) { mutableStateOf(false) }
    var seekToastText by remember { mutableStateOf<String?>(null) }
    var loadingSubtitle by remember { mutableStateOf("Preparando reproducción en alta definición") }
    var isEpisodeChanging by remember { mutableStateOf(false) }
    var isFailoverInFlight by remember { mutableStateOf(false) }
    var lastFailedStreamUrl by remember { mutableStateOf("") }
    var isResolvingFailover by remember { mutableStateOf(false) }

    // Watch History & Auto-Resume State (Netflix-style auto-resume)
    val savedHistory = remember(movie.url) { WatchHistoryManager.getMovieHistory(context, movie.url) }
    val initialResumePositionMs = remember(savedHistory) {
        if (savedHistory != null &&
            savedHistory.playbackPositionMs > 15_000L &&
            (savedHistory.durationMs <= 0L || savedHistory.playbackPositionMs < savedHistory.durationMs - 30_000L)
        ) {
            savedHistory.playbackPositionMs
        } else {
            0L
        }
    }


    var currentPositionMs by remember(movie.url) { mutableLongStateOf(initialResumePositionMs) }
    var bufferedPositionMs by remember(movie.url) { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(savedHistory?.durationMs ?: 0L) }
    var pendingResumePositionMs by remember(movie.url) {
        mutableLongStateOf(if (initialResumePositionMs > 0L) initialResumePositionMs else -1L)
    }
    var lastUserSeekTimeMs by remember { mutableLongStateOf(0L) }
    var lastUserPlayPauseTimeMs by remember { mutableLongStateOf(0L) }

    val playButtonFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val ffFocusRequester = remember { FocusRequester() }
    val prevEpisodeDockFocusRequester = remember { FocusRequester() }
    val nextEpisodeDockFocusRequester = remember { FocusRequester() }
    val timelineFocusRequester = remember { FocusRequester() }
    val serverPillFocusRequester = remember { FocusRequester() }
    val restartButtonFocusRequester = remember { FocusRequester() }
    val episodesButtonFocusRequester = remember { FocusRequester() }
    val backButtonFocusRequester = remember { FocusRequester() }
    val activeServerFocusRequester = remember { FocusRequester() }
    val activeEpisodeFocusRequester = remember { FocusRequester() }
    val activeAudioTrackFocusRequester = remember { FocusRequester() }

    BackHandler {
        if (showEpisodesDialog) {
            showEpisodesDialog = false
        } else if (showServerDialog) {
            showServerDialog = false
        } else {
            onBackClick()
        }
    }


    val trackSelector = remember(context) {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                .setPreferredTextLanguages("es", "es-419", "spa")
                .setSelectUndeterminedTextLanguage(false)
                .build()
        }
    }


    // Build ExoPlayer with robust networking and TV-optimized buffer load control
    val exoPlayer = remember(context) {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.streamingOkHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(okHttpDataSourceFactory)

        // TV-optimized buffer: 15s min / 50s max buffer prevents stutter on typical 2.4GHz Wi-Fi
        val tvLoadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(tvLoadControl)
            .setSeekForwardIncrementMs(10000)
            .setSeekBackIncrementMs(10000)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                    .setPreferredTextLanguages("es", "es-419", "spa")
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setSelectUndeterminedTextLanguage(false)
                    .build()
            }
    }


    val isEmbedStream = remember(activeStreamUrl, currentStream) {
        if (currentStream.url.isBlank()) {
            false
        } else if (currentStream.type == "HLS" || currentStream.type == "MP4" || activeStreamUrl.contains(".m3u8") || activeStreamUrl.contains(".mp4")) {
            false
        } else {
            currentStream.type == "EMBED" ||
            activeStreamUrl.contains("vidsrc") ||
            activeStreamUrl.contains("multiembed") ||
            activeStreamUrl.contains("smashystream") ||
            activeStreamUrl.contains("vimeos") ||
            activeStreamUrl.contains("goodstream") ||
            activeStreamUrl.contains("voe") ||
            activeStreamUrl.contains("dood") ||
            activeStreamUrl.contains("filemoon") ||
            activeStreamUrl.contains("byse") ||
            activeStreamUrl.contains("bysesukior") ||
            activeStreamUrl.contains("primeload") ||
            activeStreamUrl.contains("abyss") ||
            activeStreamUrl.contains("minochinos") ||
            activeStreamUrl.contains("hgcloud") ||
            activeStreamUrl.contains("tudorama") ||
            activeStreamUrl.contains("hlswish") ||
            activeStreamUrl.contains("videoapp") ||
            activeStreamUrl.contains("lamovie") ||
            activeStreamUrl.contains("embed")
        }
    }


    // Function to safely switch server, purging prior errors and ExoPlayer instances cleanly
    fun switchServer(newIndex: Int, resetPosition: Boolean = false, streamList: List<StreamItem>? = null) {
        val streamsToUse = streamList ?: availableStreams
        if (streamsToUse.isEmpty()) return
        val targetIndex = newIndex.coerceIn(0, streamsToUse.size - 1)
        currentStreamIndex = targetIndex
        val targetStream = streamsToUse[targetIndex]
        isBuffering = true
        isVideoReady = false
        if (resetPosition) {
            currentPositionMs = 0L
            durationMs = 0L
            pendingResumePositionMs = -1L
        }
        userSelectedAudioTrack = false
        exoAudioTracks = emptyList()
        exoSubtitleTracks = emptyList()
        nativeWebSubtitleTracks = emptyList()
        nativeWebAudioTracks = emptyList()
        loadingSubtitle = "Preparando reproducción en alta definición"

        val isTargetEmbed = if (targetStream.type == "HLS" || targetStream.type == "MP4" || targetStream.url.contains(".m3u8") || targetStream.url.contains(".mp4")) {
            false
        } else {
            targetStream.type == "EMBED" ||
            targetStream.url.contains("vidsrc") ||
            targetStream.url.contains("multiembed") ||
            targetStream.url.contains("smashystream") ||
            targetStream.url.contains("vimeos") ||
            targetStream.url.contains("goodstream") ||
            targetStream.url.contains("voe") ||
            targetStream.url.contains("dood") ||
            targetStream.url.contains("filemoon") ||
            targetStream.url.contains("byse") ||
            targetStream.url.contains("primeload") ||
            targetStream.url.contains("hlswish") ||
            targetStream.url.contains("videoapp") ||
            targetStream.url.contains("lamovie") ||
            targetStream.url.contains("embed")
        }

        try {
            if (!isTargetEmbed) {
                // Target is ExoPlayer stream (HLS / MP4)
                try {
                    webViewRef?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        onPause()
                    }
                } catch (_: Exception) {}
                loadedStreamUrl = null

                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                if (!userSelectedAudioTrack) {
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                            .setPreferredTextLanguages("es", "es-419", "spa")
                            .setSelectUndeterminedTextLanguage(false)
                            .build()
                    } catch (_: Exception) {}
                }
                if (!subtitlesEnabled) {
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    } catch (_: Exception) {}
                }
                val startPos = if (resetPosition) 0L else currentPositionMs.coerceAtLeast(0L)
                val uri = Uri.parse(targetStream.url)
                val subConfigs = if (subtitlesEnabled) {
                    targetStream.safeSubtitles
                        .filter { !it.url.contains("vimeos", ignoreCase = true) && it.url.startsWith("http") }
                        .mapNotNull { sub ->
                            try {
                                val mime = if (sub.url.contains(".srt", ignoreCase = true)) {
                                    MimeTypes.APPLICATION_SUBRIP
                                } else {
                                    MimeTypes.TEXT_VTT
                                }
                                val flags = if (sub.isDefault) C.SELECTION_FLAG_DEFAULT else 0
                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                                    .setMimeType(mime)
                                    .setLanguage(sub.language)
                                    .setLabel(sub.label)
                                    .setSelectionFlags(flags)
                                    .build()
                            } catch (_: Exception) {
                                null
                            }
                        }
                } else {
                    emptyList()
                }
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .apply {
                        if (targetStream.type == "HLS" || targetStream.url.contains(".m3u8")) {
                            setMimeType(MimeTypes.APPLICATION_M3U8)
                        } else if (targetStream.type == "MP4" || targetStream.url.contains(".mp4")) {
                            setMimeType(MimeTypes.VIDEO_MP4)
                        }
                        if (subConfigs.isNotEmpty()) {
                            setSubtitleConfigurations(subConfigs)
                        }
                    }
                    .build()
                exoPlayer.setMediaItem(mediaItem, startPos)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            } else {
                // Target is Web Embed
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.playWhenReady = false

                if (!resetPosition && currentPositionMs > 5_000L) {
                    pendingResumePositionMs = currentPositionMs
                } else if (resetPosition) {
                    currentPositionMs = 0L
                    durationMs = 0L
                    pendingResumePositionMs = -1L
                }

                loadedStreamUrl = targetStream.url

                val isDoramasflix = targetStream.url.contains("doramasflix", ignoreCase = true) ||
                    targetStream.url.contains("primeload", ignoreCase = true) ||
                    targetStream.url.contains("streamtape", ignoreCase = true) ||
                    targetStream.url.contains("embedshortener", ignoreCase = true) ||
                    targetStream.url.contains("ok.ru", ignoreCase = true) ||
                    targetStream.url.contains("odnoklassniki", ignoreCase = true) ||
                    targetStream.url.contains("vidhide", ignoreCase = true) ||
                    targetStream.url.contains("callistanise", ignoreCase = true) ||
                    targetStream.url.contains("do7go", ignoreCase = true) ||
                    targetStream.url.contains("playmogo", ignoreCase = true) ||
                    targetStream.url.contains("dood", ignoreCase = true) ||
                    targetStream.url.contains("flaswish", ignoreCase = true) ||
                    targetStream.url.contains("mp4upload", ignoreCase = true) ||
                    targetStream.url.contains("voe", ignoreCase = true) ||
                    targetStream.url.contains("byse", ignoreCase = true) ||
                    targetStream.url.contains("mixdrop", ignoreCase = true) ||
                    targetStream.url.contains("m1xdrop", ignoreCase = true) ||
                    targetStream.url.contains("filemoon", ignoreCase = true)

                val refererBase = when {
                    isDoramasflix -> "https://doramasflix.in/"
                    targetStream.url.contains("tudorama") || targetStream.url.contains("minochinos") || targetStream.url.contains("bysesukior") || targetStream.url.contains("abyss") || targetStream.url.contains("banana") -> "https://tudorama.com/"
                    else -> "https://lamovie.org/"
                }
                val originBase = when {
                    isDoramasflix -> "https://doramasflix.in"
                    refererBase.contains("tudorama") -> "https://tudorama.com"
                    else -> "https://lamovie.org"
                }

                val headers = mapOf(
                    "Referer" to refererBase,
                    "Origin" to originBase
                )
                try {
                    var normalizedTargetUrl = targetStream.url
                    if (normalizedTargetUrl.contains("bysefujedu.com/d/")) {
                        normalizedTargetUrl = normalizedTargetUrl.replace("/d/", "/e/")
                    } else if (normalizedTargetUrl.contains("filemoon.") && normalizedTargetUrl.contains("/d/")) {
                        normalizedTargetUrl = normalizedTargetUrl.replace("/d/", "/e/")
                    }

                    webViewRef?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        onResume()
                        loadUrl(normalizedTargetUrl, headers)
                    }
                } catch (_: Exception) {}
                isPlaying = true

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun triggerStreamFailover(failedUrl: String, reason: String) {
        val cleanFailed = failedUrl.trim()
        if (cleanFailed.isNotBlank()) {
            failedStreamUrls.add(cleanFailed)
            failedStreamUrls.add(activeStreamUrl.trim())
            if (cleanFailed.contains("/e/")) failedStreamUrls.add(cleanFailed.replace("/e/", "/d/"))
            if (cleanFailed.contains("/d/")) failedStreamUrls.add(cleanFailed.replace("/d/", "/e/"))
            if (cleanFailed.contains("/player/")) failedStreamUrls.add(cleanFailed.replace("/player/", "/embed/").split("?")[0])
            if (cleanFailed.contains("/embed/")) failedStreamUrls.add(cleanFailed.replace("/embed/", "/player/"))
        }
        if (isFailoverInFlight && cleanFailed.isNotBlank() && cleanFailed == lastFailedStreamUrl) {
            return
        }
        isFailoverInFlight = true
        lastFailedStreamUrl = cleanFailed

        val baseCandidates = if (dynamicStreams.isNotEmpty()) dynamicStreams else availableStreams
        val remaining = baseCandidates.filter { it.url !in failedStreamUrls && it.url.isNotBlank() && it.url != cleanFailed && it.url != activeStreamUrl }
        if (remaining.isNotEmpty()) {
            dynamicStreams = remaining
            switchServer(0, resetPosition = false, streamList = remaining)
            scope.launch {
                delay(600)
                isFailoverInFlight = false
            }
        } else {
            val postIdStr = detailResponse?.postId ?: movie.postId?.toString() ?: ""
            if (!isResolvingFailover && isSeriesContent && (postIdStr.isNotBlank() || movie.url.isNotBlank())) {
                isResolvingFailover = true
                scope.launch {
                    try {
                        val fresh = withContext(Dispatchers.IO) {
                            MovieRepository.resolveEpisodeStreams(
                                episodeId = 0,
                                seriesTitle = cleanMainTitle,
                                fallbackStreams = emptyList(),
                                seasonNumber = activeSeasonInt,
                                episodeNumber = activeEpisodeInt,
                                seriesPostId = postIdStr.toIntOrNull(),
                                serieUrl = movie.url,
                                seriesPostIdStr = postIdStr
                            )
                        }
                        val newCandidates = fresh.filter { it.url !in failedStreamUrls && it.url.isNotBlank() }
                        if (newCandidates.isNotEmpty()) {
                            dynamicStreams = (dynamicStreams + newCandidates).distinctBy { it.url }
                            switchServer(0, resetPosition = false, streamList = newCandidates)
                        } else {
                            seekToastText = "No se pudo reproducir este video. Intenta con otro episodio."
                            showServerDialog = true
                            isBuffering = false
                            isVideoReady = true
                        }
                    } catch (_: Exception) {
                        seekToastText = "Error de reproducción. Inténtalo de nuevo más tarde."
                        showServerDialog = true
                        isBuffering = false
                        isVideoReady = true
                    } finally {
                        isResolvingFailover = false
                        delay(2000)
                        isFailoverInFlight = false
                    }
                }
            } else {
                seekToastText = "No se pudo reproducir este video. Intenta con otro episodio."
                showServerDialog = true
                isBuffering = false
                isVideoReady = true
                scope.launch {
                    delay(2000)
                    isFailoverInFlight = false
                }
            }
        }
    }

    LaunchedEffect(movie.url, streams, streamUrl) {
        try {
            isExtracting = true
            if (streams.isNotEmpty()) {
                val verified = withContext(Dispatchers.IO) {
                    MovieRepository.filterWorkingStreams(streams)
                }
                if (verified.isNotEmpty()) {
                    dynamicStreams = verified
                    switchServer(0, resetPosition = false, streamList = verified)
                }
            }
            if (detailResponse == null) {
                val extracted = withContext(Dispatchers.IO) {
                    MovieRepository.extractMovie(movie)
                }
                detailResponse = extracted
                if (extracted.episodes.isNotEmpty() && seasonEpisodes.isEmpty()) {
                    seasonEpisodes = extracted.episodes
                }
                if (streams.isEmpty()) {
                    if (isSeriesContent && (activeEpisodeInt > 1 || activeSeasonInt > 1)) {
                        val epStreams = withContext(Dispatchers.IO) {
                            MovieRepository.resolveEpisodeStreams(
                                episodeId = 0,
                                seriesTitle = cleanMainTitle,
                                fallbackStreams = extracted.streams,
                                seasonNumber = activeSeasonInt,
                                episodeNumber = activeEpisodeInt,
                                seriesPostId = extracted.postId?.toIntOrNull(),
                                serieUrl = movie.url,
                                seriesPostIdStr = extracted.postId
                            )
                        }
                        android.util.Log.e("TVPRO_PLAY", "epStreams resolved: count=${epStreams.size}, activeEp=$activeEpisodeInt, activeSeason=$activeSeasonInt")
                        if (epStreams.isNotEmpty()) {
                            dynamicStreams = epStreams
                            switchServer(0, resetPosition = false, streamList = epStreams)
                        } else if (extracted.streams.isNotEmpty()) {
                            dynamicStreams = extracted.streams
                            switchServer(0, resetPosition = false, streamList = extracted.streams)
                        }
                    } else if (extracted.streams.isNotEmpty()) {
                        dynamicStreams = extracted.streams
                        switchServer(0, resetPosition = false, streamList = extracted.streams)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            isExtracting = false
        }
    }


    // Unified Play/Pause & Seek Functions for both ExoPlayer and Embedded Native Web Player
    fun setPlaybackPlaying(play: Boolean) {
        isPlaying = play
        lastUserPlayPauseTimeMs = System.currentTimeMillis()
        if (play) {
            if (!isEmbedStream) {
                exoPlayer.play()
            }
        } else {
            exoPlayer.pause()
        }

        val wv = webViewRef
        if (wv != null && (isEmbedStream || !play)) {
            val js = """
                (function() {
                    window._tvproUserPaused = !$play;
                    
                    function pauseJw(jp) {
                        if (!jp) return;
                        try { if (typeof jp.pause === 'function') jp.pause(); } catch(e){}
                        try { if (typeof jp.play === 'function') jp.play(false); } catch(e){}
                        try { if (typeof jp.setControls === 'function') jp.setControls(false); } catch(e){}
                    }

                    function playJw(jp) {
                        if (!jp) return;
                        try { if (typeof jp.play === 'function') jp.play(true); } catch(e){}
                        try { if (typeof jp.setControls === 'function') jp.setControls(false); } catch(e){}
                    }

                    function controlDocAndWin(win, doc) {
                        if (!doc) return;
                        try { if (win) win._tvproUserPaused = !$play; } catch(e){}
                        
                        if (win) {
                            if (win.jwplayer && typeof win.jwplayer === 'function') {
                                try {
                                    if ($play) {
                                        playJw(win.jwplayer());
                                        playJw(win.jwplayer(0));
                                        playJw(win.jwplayer("a"));
                                    } else {
                                        pauseJw(win.jwplayer());
                                        pauseJw(win.jwplayer(0));
                                        pauseJw(win.jwplayer("a"));
                                    }
                                } catch(e){}
                            }
                            if (win.player) {
                                try {
                                    if ($play && typeof win.player.play === 'function') win.player.play();
                                    else if (!$play && typeof win.player.pause === 'function') win.player.pause();
                                } catch(e){}
                            }
                            if (win.art) {
                                try {
                                    if ($play && typeof win.art.play === 'function') win.art.play();
                                    else if (!$play && typeof win.art.pause === 'function') win.art.pause();
                                } catch(e){}
                            }
                            if (win.dp) {
                                try {
                                    if ($play && typeof win.dp.play === 'function') win.dp.play();
                                    else if (!$play && typeof win.dp.pause === 'function') win.dp.pause();
                                } catch(e){}
                            }
                            if (win.dsplayer) {
                                try {
                                    if ($play && typeof win.dsplayer.play === 'function') win.dsplayer.play();
                                    else if (!$play && typeof win.dsplayer.pause === 'function') win.dsplayer.pause();
                                } catch(e){}
                            }
                        }

                        var vids = doc.querySelectorAll('video');
                        for (var i = 0; i < vids.length; i++) {
                            try {
                                vids[i].controls = false;
                                vids[i].removeAttribute('controls');
                                if ($play) {
                                    vids[i].play();
                                } else {
                                    vids[i].pause();
                                }
                            } catch(e){}
                        }

                        if ($play) {
                            var pTargets = doc.querySelectorAll('.vid_play, [tsid="player-component_play"], .vid-card_cnt_w, #embedVideoC, .vid_play-ic, [data-l="t,play"], .jw-display-icon-container, .vjs-big-play-button, button.play, .play-button, .play-btn, #play, #loadIframeButton, .plyr__control--overlaid, button.aegis-launch, .aegis-launch, .aegis-launch-btn, [class*="aegis-launch"], button[aria-label="Play"], button[aria-label*="Play"]');
                            pTargets.forEach(function(el) {
                                try { el.click(); } catch(e){}
                                try {
                                    var evt = new MouseEvent('click', { bubbles: true, cancelable: true, view: win || window });
                                    el.dispatchEvent(evt);
                                } catch(e){}
                            });
                        }
                    }

                    controlDocAndWin(window, document);

                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        try {
                            controlDocAndWin(iframes[i].contentWindow, iframes[i].contentDocument);
                        } catch(e){}
                        try {
                            var cw = iframes[i].contentWindow;
                            if (cw) {
                                var msg = $play ? 'PLAY' : 'PAUSE';
                                var act = $play ? 'play' : 'pause';
                                cw.postMessage({ type: msg, action: act, method: act, command: act }, '*');
                                cw.postMessage('{"command":"' + act + '"}', '*');
                                cw.postMessage('{"method":"' + act + '"}', '*');
                                cw.postMessage('{"action":"' + act + '"}', '*');
                            }
                        } catch(e){}
                    }
                })();
            """.trimIndent()
            wv.evaluateJavascript(js, null)
        }
    }


    fun togglePlayPause() {
        val now = System.currentTimeMillis()
        if (now - lastUserPlayPauseTimeMs < 400L) {
            return
        }
        val nextPlay = !isPlaying
        setPlaybackPlaying(nextPlay)
    }

    fun seekToMs(targetMs: Long) {
        val clampedMs = if (durationMs > 0L) targetMs.coerceIn(0L, durationMs) else targetMs.coerceAtLeast(0L)
        currentPositionMs = clampedMs
        lastUserSeekTimeMs = System.currentTimeMillis()
        if (isEmbedStream) {
            val targetSec = clampedMs / 1000.0
            val js = """
                (function() {
                    window._tvproSeeking = true;
                    setTimeout(function() { window._tvproSeeking = false; }, 1500);
                    var v = document.querySelector('video');
                    if (!v) {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                var iv = iframes[i].contentDocument && iframes[i].contentDocument.querySelector('video');
                                if (iv) { v = iv; break; }
                            } catch(e) {}
                        }
                    }
                    if (v) {
                        v.controls = false;
                        v.removeAttribute('controls');
                        try { v.currentTime = $targetSec; } catch(e) {}
                    }
                    if ($clampedMs == 0L) {
                        try { window.localStorage.removeItem('streamfi.resume.v1'); } catch(e){}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.seek === 'function') p.seek($targetSec);
                            if (p && typeof p.setControls === 'function') p.setControls(false);
                        } catch(e) {}
                    }
                    if (window.player && typeof window.player.currentTime !== 'undefined') {
                        try { window.player.currentTime = $targetSec; } catch(e){}
                    }
                    try {
                        var iframes = document.querySelectorAll('iframe');
                        for (var i = 0; i < iframes.length; i++) {
                            try {
                                iframes[i].contentWindow.postMessage({
                                    type: 'SEEK',
                                    action: 'seek',
                                    method: 'seek',
                                    time: $targetSec,
                                    currentTime: $targetSec,
                                    position: $targetSec
                                }, '*');
                            } catch(e) {}
                        }
                    } catch(e) {}
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        } else {
            exoPlayer.seekTo(clampedMs)
            bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
        }
    }


    val playEpisode: (EpisodeItem) -> Unit = { ep ->
        if (isSeriesContent) {
            showEpisodesDialog = false
            showNextEpisodePrompt = false
            showControls = true
            isNextEpisodeDismissed = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            try {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    onPause()
                }
            } catch (_: Exception) {}
            loadedStreamUrl = null

            if (selectedModalSeason != ep.seasonNumber) {
                selectedModalSeason = ep.seasonNumber
            }
            currentMovieTitle = "$cleanMainTitle - T${ep.seasonNumber}:E${ep.episodeNumber}"
            loadingSubtitle = "Cargando Temporada ${ep.seasonNumber} · Episodio ${ep.episodeNumber}..."
            isEpisodeChanging = true
            WatchHistoryManager.saveToHistory(
                context = context,
                movie = movie.copy(title = cleanMainTitle),
                positionMs = 0L,
                durationMs = 0L,
                seasonNumber = ep.seasonNumber,
                episodeNumber = ep.episodeNumber,
                isSeries = true,
                postId = movie.postId,
                slug = movie.slug
            )
            isBuffering = true
            isVideoReady = false
            currentPositionMs = 0L
            durationMs = 0L
            pendingResumePositionMs = -1L
            userSelectedAudioTrack = false
            exoAudioTracks = emptyList()
            exoSubtitleTracks = emptyList()
            nativeWebAudioTracks = emptyList()
            nativeWebSubtitleTracks = emptyList()
            scope.launch {
                loadingSubtitle = "Obteniendo enlaces del Episodio ${ep.episodeNumber}..."
                val newStreams = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(9000L) {
                            MovieRepository.resolveEpisodeStreams(
                                episodeId = ep.id,
                                seriesTitle = cleanMainTitle,
                                fallbackStreams = emptyList(),
                                seasonNumber = ep.seasonNumber,
                                episodeNumber = ep.episodeNumber,
                                seriesPostId = detailResponse?.postId?.toIntOrNull(),
                                serieUrl = movie.url,
                                episodeSlug = ep.slug,
                                seriesPostIdStr = detailResponse?.postId ?: movie.postId?.toString()
                            )
                        } ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                if (newStreams.isNotEmpty()) {
                    loadingSubtitle = "Preparando reproducción en alta definición"
                    failedStreamUrls.clear()
                    dynamicStreams = newStreams
                    switchServer(0, resetPosition = true, streamList = newStreams)
                } else {
                    seekToastText = "Episodio ${ep.episodeNumber} no disponible temporalmente"
                    seekToMs(0L)
                    setPlaybackPlaying(true)
                    isBuffering = false
                    isVideoReady = true
                    isEpisodeChanging = false
                    loadingSubtitle = "Preparando reproducción en alta definición"
                }
            }
        }
    }


    fun selectExoSubtitle(subTrack: ExoSubtitleTrackItem?) {
        if (subTrack == null) {
            subtitlesEnabled = false
            selectedSubtitleOption = 0
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, true)
                    .build()
            } catch (_: Exception) {}
        } else {
            subtitlesEnabled = true
            selectedSubtitleOption = if (subTrack.isEnglish) 2 else 1
            try {
                val override = TrackSelectionOverride(
                    subTrack.group.mediaTrackGroup,
                    listOf(subTrack.trackIndex)
                )
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .addOverride(override)
                    .setPreferredTextLanguage(subTrack.language)
                    .build()
            } catch (_: Exception) {}
        }
    }


    fun selectSubtitleTrackData(subData: SubtitleTrackData?) {
        if (subData == null) {
            selectExoSubtitle(null)
            return
        }
        subtitlesEnabled = true
        selectedSubtitleOption = if (subData.language.startsWith("en", ignoreCase = true)) 2 else 1
        val matched = exoSubtitleTracks.firstOrNull {
            it.language.equals(subData.language, ignoreCase = true) ||
            it.name.contains(subData.label, ignoreCase = true) ||
            (subData.language.startsWith("es", ignoreCase = true) && it.isSpanish) ||
            (subData.language.startsWith("en", ignoreCase = true) && it.isEnglish)
        }
        if (matched != null) {
            selectExoSubtitle(matched)
        } else {
            try {
                val targetLang = subData.language
                val altLang = if (targetLang == "es") "spa" else "eng"
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                    .setPreferredTextLanguages(targetLang, altLang)
                    .build()
            } catch (_: Exception) {}
        }
    }


    fun applyWebSubtitle(enabled: Boolean, trackIdx: Int = -1) {
        subtitlesEnabled = enabled
        selectedWebSubtitleTrackIdx = trackIdx
        if (isEmbedStream) {
            val subJs = """
                (function() {
                    var enableSubs = $enabled;
                    var trackIdx = $trackIdx;
                    function updateDocSubs(doc) {
                        if (!doc) return;
                        var st = doc.getElementById('tvpro-subtitles-toggle');
                        if (!st) {
                            st = doc.createElement('style');
                            st.id = 'tvpro-subtitles-toggle';
                            (doc.head || doc.documentElement).appendChild(st);
                        }
                        if (!enableSubs) {
                            st.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                        } else {
                            st.innerHTML = '';
                        }
                        var v = doc.querySelector('video');
                        if (v && v.textTracks) {
                            for (var i = 0; i < v.textTracks.length; i++) {
                                var tr = v.textTracks[i];
                                if (!enableSubs) {
                                    tr.mode = 'disabled';
                                } else {
                                    tr.mode = (trackIdx < 0 || i === trackIdx) ? 'showing' : 'disabled';
                                }
                            }
                        }
                    }
                    updateDocSubs(document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < iframes.length; j++) {
                        try { updateDocSubs(iframes[j].contentDocument); } catch(e) {}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.setCurrentCaptions === 'function') {
                                if (!enableSubs) {
                                    p.setCurrentCaptions(0);
                                } else {
                                    p.setCurrentCaptions(trackIdx >= 0 ? (trackIdx + 1) : 1);
                                }
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(subJs, null)
        }
    }


    fun applySubtitlePreference(enabled: Boolean, optionIdx: Int) {
        subtitlesEnabled = enabled
        selectedSubtitleOption = optionIdx
        val langCode = if (optionIdx == 2) "en" else "es"
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !enabled)
                .setPreferredTextLanguage(langCode)
                .build()
        } catch (_: Exception) {}

        if (isEmbedStream) {
            val subJs = """
                (function() {
                    var enableSubs = $enabled;
                    var prefLang = '$langCode';
                    function updateDocSubs(doc) {
                        if (!doc) return;
                        var st = doc.getElementById('tvpro-subtitles-toggle');
                        if (!st) {
                            st = doc.createElement('style');
                            st.id = 'tvpro-subtitles-toggle';
                            (doc.head || doc.documentElement).appendChild(st);
                        }
                        if (!enableSubs) {
                            st.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                        } else {
                            st.innerHTML = '';
                        }
                        var v = doc.querySelector('video');
                        if (v && v.textTracks) {
                            for (var i = 0; i < v.textTracks.length; i++) {
                                var tr = v.textTracks[i];
                                if (!enableSubs) {
                                    tr.mode = 'disabled';
                                } else {
                                    var lbl = ((tr.label || '') + ' ' + (tr.language || '')).toLowerCase();
                                    if (prefLang === 'en' && (lbl.indexOf('en') !== -1 || lbl.indexOf('ing') !== -1)) {
                                        tr.mode = 'showing';
                                    } else if (prefLang === 'es' && (lbl.indexOf('es') !== -1 || lbl.indexOf('spa') !== -1 || lbl.indexOf('lat') !== -1 || i === 0)) {
                                        tr.mode = 'showing';
                                    } else {
                                        tr.mode = (v.textTracks.length === 1) ? 'showing' : 'disabled';
                                    }
                                }
                            }
                        }
                    }
                    updateDocSubs(document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var j = 0; j < iframes.length; j++) {
                        try { updateDocSubs(iframes[j].contentDocument); } catch(e) {}
                    }
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try {
                            var p = window.jwplayer();
                            if (p && typeof p.setCurrentCaptions === 'function') {
                                if (!enableSubs) {
                                    p.setCurrentCaptions(0);
                                } else {
                                    var list = (typeof p.getCaptionsList === 'function') ? p.getCaptionsList() : [];
                                    var chosen = 1;
                                    for (var k = 1; k < list.length; k++) {
                                        var cLbl = (list[k].label || '').toLowerCase();
                                        if (prefLang === 'en' && (cLbl.indexOf('en') !== -1 || cLbl.indexOf('ing') !== -1)) { chosen = k; break; }
                                        if (prefLang === 'es' && (cLbl.indexOf('es') !== -1 || cLbl.indexOf('spa') !== -1 || cLbl.indexOf('lat') !== -1)) { chosen = k; break; }
                                    }
                                    p.setCurrentCaptions(chosen);
                                }
                            }
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(subJs, null)
        }
    }


    fun applyWebAudioTrack(trackIdx: Int) {
        userSelectedAudioTrack = true
        selectedWebAudioTrackIdx = trackIdx
        if (isEmbedStream) {
            val audioJs = """
                (function() {
                    function applyTrack(win, doc) {
                        if (!win) return;
                        if (win.jwplayer && typeof win.jwplayer === 'function') {
                            try {
                                var p = win.jwplayer();
                                if (p && typeof p.setCurrentAudioTrack === 'function') {
                                    p.setCurrentAudioTrack($trackIdx);
                                }
                            } catch(e) {}
                        }
                        if (doc) {
                            var v = doc.querySelector('video');
                            if (v && v.audioTracks) {
                                for (var i = 0; i < v.audioTracks.length; i++) {
                                    v.audioTracks[i].enabled = (i === $trackIdx);
                                }
                            }
                        }
                    }
                    applyTrack(window, document);
                    var iframes = document.querySelectorAll('iframe');
                    for (var i = 0; i < iframes.length; i++) {
                        try {
                            applyTrack(iframes[i].contentWindow, iframes[i].contentDocument);
                        } catch(e) {}
                    }
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(audioJs, null)
        }
    }


    // Initialize or re-switch source cleanly and save movie to recent history
    LaunchedEffect(movie.url) {
        val sNum = if (isSeriesContent) currentSeasonNum.toIntOrNull() else null
        val eNum = if (isSeriesContent) currentEpNum.toIntOrNull() else null
        WatchHistoryManager.saveToHistory(
            context,
            movie.copy(title = cleanMainTitle),
            positionMs = initialResumePositionMs,
            durationMs = savedHistory?.durationMs ?: 0L,
            seasonNumber = sNum,
            episodeNumber = eNum,
            isSeries = isSeriesContent,
            postId = movie.postId,
            slug = movie.slug
        )
        if (availableStreams.isNotEmpty()) {
            switchServer(initialStreamIndex, resetPosition = (initialResumePositionMs <= 0L))
            if (initialResumePositionMs > 0L) {
                val resumeTimeStr = formatTimeSec(initialResumePositionMs / 1000)
                seekToastText = "Reanudando en $resumeTimeStr"
            }
        }
    }


    // Automatically promote high-fidelity direct streams (Ok.ru MP4/HLS) when dynamicStreams arrive
    LaunchedEffect(dynamicStreams) {
        if (dynamicStreams.isNotEmpty() && (!isVideoReady || activeStreamUrl.isBlank() || isEmbedStream)) {
            val bestCandidate = dynamicStreams.firstOrNull()
            if (bestCandidate != null && (bestCandidate.type == "MP4" || bestCandidate.type == "HLS" || isEmbedStream || activeStreamUrl != bestCandidate.url)) {
                switchServer(0, resetPosition = false, streamList = dynamicStreams)
            }
        }
    }


    // Real-time synchronization with Embedded Native Web Player (<video> / JWPlayer) + Hide Web Chrome & Grey Poster
    LaunchedEffect(isEmbedStream, webViewRef, activeStreamUrl, subtitlesEnabled, isPlaying) {
        var lastObservedPos = -1L
        var webStallTicks = 0
        var noDurationTicks = 0
        while (isActive && isEmbedStream) {
            delay(350)
            val wv = webViewRef
            if (wv != null) {
                val mustPause = !isPlaying
                val syncJs = """
                    (function() {
                        var mustPause = $mustPause;
                        var subsOn = $subtitlesEnabled;
                        window._tvproUserPaused = mustPause;
                        function hideNativeChrome(doc) {
                            if (!doc) return;
                            if (!doc.getElementById('tvpro-hide-native-ui')) {
                                var st = doc.createElement('style');
                                st.id = 'tvpro-hide-native-ui';
                                st.innerHTML = `
                                    html, body {
                                        background: #000 !important;
                                        background-color: #000 !important;
                                        overflow: hidden !important;
                                        margin: 0 !important;
                                        padding: 0 !important;
                                    }
                                    video {
                                        position: fixed !important;
                                        top: 0 !important;
                                        left: 0 !important;
                                        width: 100vw !important;
                                        height: 100vh !important;
                                        max-width: 100vw !important;
                                        max-height: 100vh !important;
                                        z-index: 2147483640 !important;
                                        object-fit: contain !important;
                                        background: #000 !important;
                                    }
                                    .jw-preview, .vjs-poster, .plyr__poster,
                                    .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container,
                                    .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu,
                                    .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick,
                                    .jw-button-container, .jw-slider-time,
                                    .vjs-control-bar, .vjs-title-bar, .vjs-modal-dialog,
                                    .plyr__controls, .plyr__control--overlaid, .fluid_controls_container,
                                    div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd, #logo-vimeos,
                                    .sf-resume-modal, [class*="sf-resume-modal"], #sf-resume-title, #sf-resume-desc,
                                    .resume-modal, [class*="resume-dialog"], [id*="resume-modal"], [id*="resume-dialog"],
                                    div[role="dialog"][aria-labelledby*="resume"] {
                                        opacity: 0 !important;
                                        visibility: hidden !important;
                                        pointer-events: none !important;
                                        position: fixed !important;
                                        top: -9999px !important;
                                        left: -9999px !important;
                                        width: 0 !important;
                                        height: 0 !important;
                                        z-index: -9999 !important;
                                        background-image: none !important;
                                    }
                                    video::-webkit-media-controls,
                                    video::-webkit-media-controls-enclosure,
                                    video::-webkit-media-controls-panel,
                                    video::-webkit-media-controls-overlay-play-button {
                                        display: none !important;
                                        opacity: 0 !important;
                                        -webkit-appearance: none !important;
                                    }
                                `;
                                (doc.head || doc.documentElement).appendChild(st);
                            }
                            var subSt = doc.getElementById('tvpro-subtitles-toggle');
                            if (!subSt) {
                                subSt = doc.createElement('style');
                                subSt.id = 'tvpro-subtitles-toggle';
                                (doc.head || doc.documentElement).appendChild(subSt);
                            }
                            if (!subsOn) {
                                subSt.innerHTML = '.jw-captions, .jw-text-track-display, .jw-text-track-container, .jw-text-track-cue, .vjs-text-track-display, .plyr__captions, video::cue { display: none !important; opacity: 0 !important; visibility: hidden !important; }';
                            } else {
                                subSt.innerHTML = '';
                            }
                        }
                        function handleResumeModals(d) {
                            if (!d) return;
                            try {
                                var modal = d.querySelector('.sf-resume-modal, [class*="resume-modal"], [id*="resume-modal"], div[role="dialog"][aria-labelledby*="resume"]');
                                if (modal) {
                                    var btn = modal.querySelector('.sf-resume-modal__btn--primary, button.sf-resume-modal__btn--primary, button.primary, button:first-of-type');
                                    if (!btn) {
                                        var btns = modal.querySelectorAll('button');
                                        for (var b = 0; b < btns.length; b++) {
                                            var t = (btns[b].innerText || btns[b].textContent || '').toLowerCase();
                                            if (t.includes('yes') || t.includes('resume') || t.includes('reanudar') || t.includes('continuar')) {
                                                btn = btns[b];
                                                break;
                                            }
                                        }
                                    }
                                    if (btn) {
                                        try { btn.click(); } catch(e){}
                                        try {
                                            var me = new MouseEvent('click', { bubbles: true, cancelable: true, view: d.defaultView || window });
                                            btn.dispatchEvent(me);
                                        } catch(e){}
                                    }
                                }
                            } catch(e){}
                        }
                        hideNativeChrome(document);
                        handleResumeModals(document);
                        document.querySelectorAll('div.video_ad, div.video_ad_fadein, #over_player_msg, #adbd, a[target="_blank"]').forEach(function(el) {
                            try { el.remove(); } catch(e) {}
                        });
                        var ifrs = document.querySelectorAll('iframe');
                        for (var fi = 0; fi < ifrs.length; fi++) {
                            var ifr = ifrs[fi];
                            if (ifr.id === 'sf-player-frame') continue;
                            var src = (ifr.src || '').toLowerCase();
                            if (src.includes('player') || src.includes('embed') || src.includes('video')) continue;
                            if (src.includes('captcha') || src.includes('phruk') || src.includes('turnstile') || src.includes('recaptcha') || src.includes('hcaptcha') || src.includes('doubleclick') || src.includes('ads') || src.includes('syfpd') || src.includes('toast') || src.includes('notix')) {
                                try { ifr.remove(); } catch(e) {}
                                continue;
                            }
                            if (ifr.offsetWidth > 0 && ifr.offsetWidth < 300 && ifr.offsetHeight > 0 && ifr.offsetHeight < 300) {
                                try { ifr.remove(); } catch(e) {}
                            }
                        }
                        document.querySelectorAll('img').forEach(function(m) { try { m.remove(); } catch(e){} });
                        var scamWords = [
                            'congratulations', 'available $', 'withdraw money', 'transfer money',
                            'bonus', 'claim now', 'allow notification', 'permitir notificaciones',
                            'ganaste', 'premio', 'has ganado', 'iphone', 'descargar ahora', 'install',
                            'click allow', 'not a robot', 'e-captcha', 'are not a robot', 'robot', 'captcha',
                            'verify you are human', 'verify that you are human', 'press allow to continue',
                            'going viral', 'why thousands of people', 'simple trick', 'allow to continue',
                            'privacy', 'terms', 'check your browser'
                        ];
                        document.querySelectorAll('div, dialog, section, aside, form, p, span, a, button, h1, h2, h3, h4').forEach(function(el) {
                            var txt = (el.innerText || el.textContent || '').toLowerCase();
                            if (scamWords.some(function(w) { return txt.includes(w); })) {
                                var curr = el;
                                while (curr && curr.parentElement && curr.parentElement !== document.body && curr.parentElement !== document.documentElement && !curr.parentElement.querySelector('video')) {
                                    curr = curr.parentElement;
                                }
                                try { curr.remove(); } catch(e) {}
                            }
                        });
                        var v = document.querySelector('video');
                        if (!v) {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var idoc = iframes[i].contentDocument;
                                    if (idoc) {
                                        hideNativeChrome(idoc);
                                        handleResumeModals(idoc);
                                        var iv = idoc.querySelector('video');
                                        if (iv) { v = iv; break; }
                                    }
                                } catch(e) {}
                            }
                        }
                        if (mustPause) {
                            function pauseDocAndWin(d, w) {
                                if (!d) return;
                                try { if (w) w._tvproUserPaused = true; } catch(e){}
                                var vs = d.querySelectorAll('video');
                                for (var k = 0; k < vs.length; k++) {
                                    try { if (!vs[k].paused) vs[k].pause(); } catch(e){}
                                }
                                if (w) {
                                    if (w.jwplayer && typeof w.jwplayer === 'function') {
                                        try {
                                            var p = w.jwplayer();
                                            if (p) {
                                                if (typeof p.pause === 'function') p.pause();
                                                if (typeof p.play === 'function') p.play(false);
                                            }
                                            var pA = w.jwplayer("a");
                                            if (pA && pA !== p) {
                                                if (typeof pA.pause === 'function') pA.pause();
                                                if (typeof pA.play === 'function') pA.play(false);
                                            }
                                        } catch(e){}
                                    }
                                    if (w.player && typeof w.player.pause === 'function') {
                                        try { w.player.pause(); } catch(e){}
                                    }
                                    if (w.art && typeof w.art.pause === 'function') {
                                        try { w.art.pause(); } catch(e){}
                                    }
                                    if (w.dp && typeof w.dp.pause === 'function') {
                                        try { w.dp.pause(); } catch(e){}
                                    }
                                    if (w.dsplayer && typeof w.dsplayer.pause === 'function') {
                                        try { w.dsplayer.pause(); } catch(e){}
                                    }
                                }
                            }
                            pauseDocAndWin(document, window);
                            var allIfrs = document.querySelectorAll('iframe');
                            for (var fi = 0; fi < allIfrs.length; fi++) {
                                try {
                                    pauseDocAndWin(allIfrs[fi].contentDocument, allIfrs[fi].contentWindow);
                                    var cw = allIfrs[fi].contentWindow;
                                    if (cw) {
                                        cw.postMessage({ type: 'PAUSE', action: 'pause', method: 'pause', command: 'pause' }, '*');
                                        cw.postMessage('{"command":"pause"}', '*');
                                        cw.postMessage('{"method":"pause"}', '*');
                                        cw.postMessage('{"action":"pause"}', '*');
                                    }
                                } catch(e){}
                            }
                        } else {
                            if (v) {
                                v.controls = false;
                                v.removeAttribute('controls');
                                v.removeAttribute('poster');
                                v.style.backgroundColor = '#000';
                                if (!subsOn && v.textTracks) {
                                    for (var t = 0; t < v.textTracks.length; t++) {
                                        v.textTracks[t].mode = 'disabled';
                                    }
                                }
                                if (!window._tvproSeeking && v.paused) {
                                    try { v.play(); } catch(e){}
                                }
                            }
                            try {
                                var sf = document.getElementById('sf-overlay');
                                if (sf) { sf.remove(); }
                                var ad = document.getElementById('adbd');
                                if (ad) { ad.remove(); }
                                if (!window._tvproSeeking && window.dsplayer && typeof window.dsplayer.play === 'function') {
                                    if (window.dsplayer.paused && window.dsplayer.paused()) {
                                        window.dsplayer.play();
                                    }
                                }
                            } catch(e){}
                        }
                        var audioLabels = [];
                        var p = (window.jwplayer && typeof window.jwplayer === 'function') ? window.jwplayer() : null;
                        if (!p || typeof p.getAudioTracks !== 'function') {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var win = iframes[i].contentWindow;
                                    if (win && win.jwplayer && typeof win.jwplayer === 'function') {
                                        var ip = win.jwplayer();
                                        if (ip && typeof ip.getAudioTracks === 'function') { p = ip; break; }
                                    }
                                } catch(e) {}
                            }
                        }
                        if (p) {
                            try {
                                var st = (typeof p.getState === 'function') ? p.getState() : '';
                                if (!mustPause && !window._tvproSeeking && st === 'idle' && typeof p.play === 'function') p.play(true);
                                if (mustPause) {
                                    if (typeof p.pause === 'function') p.pause();
                                    if (typeof p.play === 'function') p.play(false);
                                }
                                if (typeof p.setControls === 'function') p.setControls(false);
                                if (!subsOn && typeof p.setCurrentCaptions === 'function') p.setCurrentCaptions(0);
                                if (typeof p.getAudioTracks === 'function') {
                                    var ats = p.getAudioTracks() || [];
                                    for (var a = 0; a < ats.length; a++) {
                                        audioLabels.push(ats[a].name || ats[a].label || ats[a].language || ('Pista ' + (a + 1)));
                                    }
                                    if (!window.__tvpro_audio_inited && ats.length > 1) {
                                        for (var a = 0; a < ats.length; a++) {
                                            var n = (ats[a].name || ats[a].label || ats[a].language || '').toLowerCase();
                                            if (n.indexOf('espan') !== -1 || n.indexOf('españ') !== -1 || n.indexOf('lat') !== -1 || n.indexOf('spa') !== -1 || n.indexOf('castell') !== -1) {
                                                p.setCurrentAudioTrack(a);
                                                window.__tvpro_audio_inited = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch(e) {}
                        }
                        if (audioLabels.length === 0 && v && v.audioTracks && v.audioTracks.length > 0) {
                            for (var a = 0; a < v.audioTracks.length; a++) {
                                var tr = v.audioTracks[a];
                                audioLabels.push(tr.label || tr.language || ('Pista ' + (a + 1)));
                            }
                            if (!window.__tvpro_audio_inited && v.audioTracks.length > 1) {
                                for (var a = 0; a < v.audioTracks.length; a++) {
                                    var tr = v.audioTracks[a];
                                    var n = ((tr.label || '') + ' ' + (tr.language || '')).toLowerCase();
                                    if (n.indexOf('espan') !== -1 || n.indexOf('españ') !== -1 || n.indexOf('lat') !== -1 || n.indexOf('spa') !== -1 || n.indexOf('castell') !== -1) {
                                        for (var j = 0; j < v.audioTracks.length; j++) {
                                            v.audioTracks[j].enabled = (j === a);
                                        }
                                        window.__tvpro_audio_inited = true;
                                        break;
                                    }
                                }
                            }
                        }
                        var subLabels = [];
                        if (p && typeof p.getCaptionsList === 'function') {
                            var caps = p.getCaptionsList() || [];
                            for (var c = 0; c < caps.length; c++) {
                                var cl = caps[c].label || caps[c].name || caps[c].language || '';
                                if (cl && cl.toLowerCase() !== 'off' && cl.toLowerCase() !== 'desactivado' && cl.toLowerCase() !== 'none') {
                                    subLabels.push(cl);
                                }
                            }
                        }
                        if (subLabels.length === 0 && v && v.textTracks && v.textTracks.length > 0) {
                            for (var c = 0; c < v.textTracks.length; c++) {
                                var tr = v.textTracks[c];
                                var cl = tr.label || tr.language || '';
                                if (cl && cl.toLowerCase() !== 'off' && cl.toLowerCase() !== 'disabled') {
                                    subLabels.push(cl);
                                }
                            }
                        }
                        var bodyText = (document.body ? (document.body.innerText || document.body.textContent || '') : '').toLowerCase();
                        var errorPhrases = [
                            '232001',
                            'not found',
                            'video you are looking for is not found',
                            'file not found',
                            'file was deleted',
                            'video deleted',
                            'video has been removed',
                            'no se puede reproducir',
                            'archivo de video no se puede reproducir',
                            'video file cannot be played',
                            'error loading media',
                            'the media playback was aborted',
                            'expired or has been deleted',
                            'no longer available',
                            'contenido no disponible',
                            'error 404',
                            '404 not found',
                            'access denied',
                            'blocked',
                            'forbidden',
                            'media could not be loaded'
                        ];
                        var isDead = false;
                        var deadReason = '';
                        for (var ep = 0; ep < errorPhrases.length; ep++) {
                            if (bodyText.includes(errorPhrases[ep])) {
                                isDead = true;
                                deadReason = errorPhrases[ep];
                                break;
                            }
                        }

                        if (!isDead) {
                            var iframes = document.querySelectorAll('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                try {
                                    var idoc = iframes[i].contentDocument;
                                    if (idoc && idoc.body) {
                                        var itxt = (idoc.body.innerText || idoc.body.textContent || '').toLowerCase();
                                        for (var ep = 0; ep < errorPhrases.length; ep++) {
                                            if (itxt.includes(errorPhrases[ep])) {
                                                isDead = true;
                                                deadReason = 'iframe: ' + errorPhrases[ep];
                                                break;
                                            }
                                        }
                                        if (isDead) break;
                                    }
                                } catch(e) {}
                            }
                        }

                        if (!isDead && p) {
                            try {
                                if (typeof p.getState === 'function' && p.getState() === 'error') {
                                    isDead = true;
                                    deadReason = 'jwplayer error';
                                }
                            } catch(e) {}
                        }
                        if (!isDead && v && v.error && v.error.code === 4 && v.src && !v.src.startsWith('blob:') && document.readyState === 'complete') {
                            isDead = true;
                            deadReason = 'video error 4';
                        }

                        if (isDead) {
                            return JSON.stringify({ dead: true, reason: deadReason });
                        }

                        if (v) {
                            if (!v.paused && !v.ended) {
                                var okPlaying = document.querySelectorAll('.vid_play, [tsid="player-component_play"], .vid-card_cnt_w, #embedVideoC');
                                okPlaying.forEach(function(el) {
                                    try {
                                        el.style.display = 'none';
                                        el.style.opacity = '0';
                                        el.style.pointerEvents = 'none';
                                    } catch(e){}
                                });
                            }
                            var curD = (!isNaN(v.duration) && v.duration > 0) ? Math.floor(v.duration * 1000) : 0;
                            var curP = Math.floor((v.currentTime || 0) * 1000);
                            var isActivelyPlaying = (!v.paused && !v.ended && curP > 0);
                            var ready = (curP > 100 || isActivelyPlaying);
                            return JSON.stringify({
                                pos: curP,
                                dur: curD,
                                playing: !v.paused && !v.ended,
                                ready: ready,
                                audioTracks: audioLabels,
                                subtitleTracks: subLabels
                            });
                        }
                        if (p && typeof p.getPosition === 'function' && typeof p.getDuration === 'function') {
                            try {
                                var d = p.getDuration();
                                if (d > 0) {
                                    var jwState = (typeof p.getState === 'function') ? p.getState() : '';
                                    var jwReady = (jwState === 'playing' || p.getPosition() > 0.1);
                                    return JSON.stringify({
                                        pos: Math.floor(p.getPosition() * 1000),
                                        dur: Math.floor(d * 1000),
                                        playing: jwState === 'playing',
                                        ready: jwReady,
                                        audioTracks: audioLabels,
                                        subtitleTracks: subLabels
                                    });
                                }
                            } catch(e) {}
                        }
                        return '';
                    })();
                """.trimIndent()
                wv.evaluateJavascript(syncJs) { rawResult ->
                    try {
                        val cleaned = rawResult
                            ?.trim()
                            ?.removeSurrounding("\"")
                            ?.replace("\\\"", "\"")
                            ?.replace("\\\\", "\\")
                        if (!cleaned.isNullOrBlank() && cleaned.startsWith("{")) {
                            val obj = JSONObject(cleaned)
                            if (obj.optBoolean("dead", false)) {
                                val deadReason = obj.optString("reason", "Error 232001")
                                triggerStreamFailover(activeStreamUrl, deadReason)
                                return@evaluateJavascript
                            }
                            val pos = obj.optLong("pos", 0L)
                            val dur = obj.optLong("dur", 0L)
                            val playingState = obj.optBoolean("playing", isPlaying)
                            val readyState = obj.optBoolean("ready", false)
                            val tracksArr = obj.optJSONArray("audioTracks")
                            if (tracksArr != null && tracksArr.length() > 1) {
                                val parsed = mutableListOf<String>()
                                for (idx in 0 until tracksArr.length()) {
                                    parsed.add(tracksArr.optString(idx, "Audio ${idx + 1}"))
                                }
                                if (parsed != nativeWebAudioTracks) {
                                    nativeWebAudioTracks = parsed
                                    if (!userSelectedAudioTrack) {
                                        val spanishIdx = parsed.indexOfFirst { trackStr ->
                                            val s = trackStr.lowercase()
                                            s.contains("espan") || s.contains("españ") || s.contains("lat") || s.contains("spanish") || s.contains("spa") || s.contains("castell")
                                        }
                                        if (spanishIdx >= 0 && spanishIdx != selectedWebAudioTrackIdx) {
                                            applyWebAudioTrack(spanishIdx)
                                        }
                                    }
                                }
                            }
                            val subTracksArr = obj.optJSONArray("subtitleTracks")
                            if (subTracksArr != null) {
                                val parsedSubs = mutableListOf<String>()
                                for (idx in 0 until subTracksArr.length()) {
                                    val s = subTracksArr.optString(idx, "")
                                    if (s.isNotBlank() && !parsedSubs.contains(s)) {
                                        parsedSubs.add(s)
                                    }
                                }
                                if (parsedSubs != nativeWebSubtitleTracks) {
                                    nativeWebSubtitleTracks = parsedSubs
                                }
                            }
                            if (dur > 0L) {
                                durationMs = dur
                                val isRecentlySeeking = (System.currentTimeMillis() - lastUserSeekTimeMs) < 1500L
                                if (!isRecentlySeeking) {
                                    currentPositionMs = pos.coerceIn(0L, dur)
                                }
                                val isRecentlyToggled = (System.currentTimeMillis() - lastUserPlayPauseTimeMs) < 1200L
                                if (!isRecentlyToggled) {
                                    isPlaying = playingState
                                }
                                if (pendingResumePositionMs > 0L) {
                                    val target = pendingResumePositionMs
                                    pendingResumePositionMs = -1L
                                    seekToMs(target)
                                }
                            }
                            if (dur > 0L) {
                                noDurationTicks = 0
                                if (pos != lastObservedPos) {
                                    lastObservedPos = pos
                                    webStallTicks = 0
                                    isVideoReady = true
                                    isBuffering = false
                                    isEpisodeChanging = false
                                    loadingSubtitle = "Preparando reproducción en alta definición"
                                } else if (!isEpisodeChanging && !showServerDialog && isPlaying) {
                                    val isRecentlySeeking = (System.currentTimeMillis() - lastUserSeekTimeMs) < 2000L
                                    val isRecentlyToggled = (System.currentTimeMillis() - lastUserPlayPauseTimeMs) < 2000L
                                    if (!isRecentlySeeking && !isRecentlyToggled) {
                                        webStallTicks++
                                        if (webStallTicks >= 20) { // ~7 seconds stuck without time advancement
                                            webStallTicks = 0
                                            triggerStreamFailover(activeStreamUrl, "Reproductor web congelado en ${formatTimeSec(pos / 1000)}")
                                            return@evaluateJavascript
                                        }
                                    }
                                }
                            } else if (!isEpisodeChanging && !showServerDialog && isPlaying) {
                                noDurationTicks++
                                if (noDurationTicks >= 22) { // ~7.7 seconds without video signal
                                    noDurationTicks = 0
                                    triggerStreamFailover(activeStreamUrl, "Reproductor web sin señal de video o no disponible")
                                    return@evaluateJavascript
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }


    // Player Listener for state, buffering, and automatic failover
    DisposableEffect(exoPlayer, isEmbedStream) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isEmbedStream) {
                    isBuffering = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY) {
                        isVideoReady = true
                        isEpisodeChanging = false
                        loadingSubtitle = "Preparando reproducción en alta definición"
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    } else if (playbackState == Player.STATE_ENDED) {
                        if (isSeriesContent && nextEpisode != null && !isNextEpisodeDismissed && !isEpisodeChanging) {
                            showControls = false
                            showNextEpisodePrompt = true
                        }
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if (isEmbedStream) return
                val audioList = mutableListOf<ExoAudioTrackItem>()
                val subtitleList = mutableListOf<ExoSubtitleTrackItem>()
                var spanishCandidate: ExoAudioTrackItem? = null
                var isAnySpanishSelected = false

                val isCurrentStreamEnglish = ((currentStream.source ?: "") + " " + currentStream.url).lowercase().let { raw ->
                    val isExplicitSpanishOnly = (raw.contains("latino") || raw.contains("castellano")) &&
                        !raw.contains("dual") && !raw.contains("multi") && !raw.contains("ingl") && !raw.contains("sub")
                    if (isExplicitSpanishOnly) false
                    else raw.contains("sub") || raw.contains("inglés") || raw.contains("ingles") || raw.contains("english") || raw.contains("orig")
                }

                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            val lang = (format.language ?: "").lowercase()
                            val label = (format.label ?: "").lowercase()
                            val rawFormatLabel = format.label?.trim() ?: ""

                            var isSpanish = lang.startsWith("es") || lang.startsWith("spa") ||
                                lang.contains("419") ||
                                label.contains("espan") || label.contains("españ") ||
                                label.contains("lat") || label.contains("spanish") ||
                                label.contains("castellano")
                            var isEnglish = lang.startsWith("en") || lang.startsWith("eng") ||
                                label.contains("ingles") || label.contains("inglés") ||
                                label.contains("english") ||
                                (label.contains("orig") && !label.contains("port") && !label.contains("esp"))
                            val isPortuguese = lang.startsWith("pt") || lang.startsWith("por") ||
                                label.contains("portug") || label.contains("brasil") || label.contains("brazil")
                            val isJapanese = lang.startsWith("ja") || lang.startsWith("jpn") ||
                                label.contains("jap") || label.contains("anime")
                            val isFrench = lang.startsWith("fr") || lang.startsWith("fra") || lang.startsWith("fre") ||
                                label.contains("franc") || label.contains("french")
                            val isItalian = lang.startsWith("it") || lang.startsWith("ita") ||
                                label.contains("ital")
                            val isGerman = lang.startsWith("de") || lang.startsWith("deu") || lang.startsWith("ger") ||
                                label.contains("alem") || label.contains("german") || label.contains("deutsch")
                            val isRussian = lang.startsWith("ru") || lang.startsWith("rus") ||
                                label.contains("rus")
                            val isKorean = lang.startsWith("ko") || lang.startsWith("kor") ||
                                label.contains("corean") || label.contains("korean")
                            val isChinese = lang.startsWith("zh") || lang.startsWith("chi") || lang.startsWith("zho") ||
                                label.contains("chin")

                            // When format language and label are undetermined (e.g. "und", "", raw streams):
                            val hasRecognizedLanguage = isSpanish || isEnglish || isPortuguese || isJapanese ||
                                isFrench || isItalian || isGerman || isRussian || isKorean || isChinese
                            if (!hasRecognizedLanguage) {
                                val isUndeterminedLang = lang.isBlank() || lang == "und" || lang == "mis" || lang == "mul" || lang == "zxx"
                                val isGenericAudioLabel = rawFormatLabel.isBlank() || rawFormatLabel.equals("und", true) || rawFormatLabel.startsWith("audio", true)
                                if (isUndeterminedLang && isGenericAudioLabel) {
                                    if (isCurrentStreamEnglish) {
                                        isEnglish = true
                                    } else {
                                        isSpanish = true
                                    }
                                }
                            }

                            val isGenericSpanishLabel = rawFormatLabel.equals("es", true) ||
                                rawFormatLabel.equals("spa", true) ||
                                rawFormatLabel.equals("spanish", true) ||
                                rawFormatLabel.equals("español", true) ||
                                rawFormatLabel.equals("espanol", true) ||
                                rawFormatLabel.startsWith("audio", true) ||
                                rawFormatLabel.equals("und", true)
                            val isGenericEnglishLabel = rawFormatLabel.equals("en", true) ||
                                rawFormatLabel.equals("eng", true) ||
                                rawFormatLabel.equals("english", true) ||
                                rawFormatLabel.equals("ingles", true) ||
                                rawFormatLabel.equals("inglés", true) ||
                                rawFormatLabel.startsWith("audio", true) ||
                                rawFormatLabel.equals("und", true)

                            val displayName = when {
                                isPortuguese -> {
                                    if (label.contains("brasil") || label.contains("brazil")) "Portugués (Brasil)"
                                    else "Portugués"
                                }
                                isSpanish -> {
                                    if (label.contains("castellano") || label.contains("españa") || label.contains("spain")) "Español (Castellano)"
                                    else if (rawFormatLabel.isNotBlank() && !isGenericSpanishLabel) "Español ($rawFormatLabel)"
                                    else "Español Latino"
                                }
                                isEnglish -> {
                                    if (rawFormatLabel.isNotBlank() && !isGenericEnglishLabel && !label.contains("orig")) "Inglés ($rawFormatLabel)"
                                    else "Inglés"
                                }
                                isJapanese -> "Japonés"
                                isFrench -> "Francés"
                                isItalian -> "Italiano"
                                isGerman -> "Alemán"
                                isRussian -> "Ruso"
                                isKorean -> "Coreano"
                                isChinese -> "Chino"
                                !format.label.isNullOrBlank() && !format.label!!.startsWith("audio", true) -> format.label!!
                                !format.language.isNullOrBlank() && !format.language!!.equals("und", true) -> format.language!!.uppercase()
                                else -> if (isCurrentStreamEnglish) "Inglés" else "Español Latino"
                            }

                            val item = ExoAudioTrackItem(
                                group = group,
                                trackIndex = i,
                                name = displayName,
                                language = format.language ?: "",
                                isSelected = isSelected,
                                isSpanish = isSpanish,
                                isEnglish = isEnglish
                            )
                            if (isSelected && isSpanish) {
                                isAnySpanishSelected = true
                            }
                            if (isSpanish && spanishCandidate == null) {
                                spanishCandidate = item
                            }
                            audioList.add(item)
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (i in 0 until group.length) {
                            if (group.isTrackSupported(i)) {
                                val format = group.getTrackFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val lang = (format.language ?: "").lowercase()
                                val label = (format.label ?: "").lowercase()
                                val isSpanish = lang.startsWith("es") || lang.startsWith("spa") ||
                                    lang.contains("419") ||
                                    label.contains("espan") || label.contains("españ") ||
                                    label.contains("lat") || label.contains("spanish") ||
                                    label.contains("castellano")
                                val isEnglish = lang.startsWith("en") || lang.startsWith("eng") ||
                                    label.contains("ingles") || label.contains("inglés") ||
                                    label.contains("english") ||
                                    (label.contains("orig") && !label.contains("port") && !label.contains("esp"))
                                val isPortuguese = lang.startsWith("pt") || lang.startsWith("por") ||
                                    label.contains("portug") || label.contains("brasil") || label.contains("brazil")

                                val rawFormatLabel = format.label?.trim() ?: ""
                                val isGenericSpanishLabel = rawFormatLabel.equals("es", true) ||
                                    rawFormatLabel.equals("spa", true) ||
                                    rawFormatLabel.equals("spanish", true) ||
                                    rawFormatLabel.equals("español", true) ||
                                    rawFormatLabel.equals("espanol", true)
                                val isGenericEnglishLabel = rawFormatLabel.equals("en", true) ||
                                    rawFormatLabel.equals("eng", true) ||
                                    rawFormatLabel.equals("english", true) ||
                                    rawFormatLabel.equals("ingles", true) ||
                                    rawFormatLabel.equals("inglés", true)

                                val displayName = when {
                                    isPortuguese -> {
                                        if (label.contains("brasil") || label.contains("brazil")) "Portugués (Brasil)"
                                        else "Portugués"
                                    }
                                    isSpanish -> if (rawFormatLabel.isNotBlank() && !isGenericSpanishLabel) "Español ($rawFormatLabel)" else "Español"
                                    isEnglish -> if (rawFormatLabel.isNotBlank() && !isGenericEnglishLabel) "Inglés ($rawFormatLabel)" else "Inglés"
                                    rawFormatLabel.contains("portug", true) -> "Portugués"
                                    !format.label.isNullOrBlank() -> format.label!!
                                    !format.language.isNullOrBlank() -> format.language!!.uppercase()
                                    else -> "Subtítulo ${subtitleList.size + 1}"
                                }

                                subtitleList.add(
                                    ExoSubtitleTrackItem(
                                        group = group,
                                        trackIndex = i,
                                        name = displayName,
                                        language = format.language ?: "",
                                        isSelected = isSelected,
                                        isSpanish = isSpanish,
                                        isEnglish = isEnglish
                                    )
                                )
                            }
                        }
                    }
                }

                exoAudioTracks = audioList
                exoSubtitleTracks = subtitleList

                // Automatically switch to Spanish if multiple audio tracks exist, no Spanish track is currently selected,
                // and the user has not explicitly chosen an audio track manually.
                if (!userSelectedAudioTrack && audioList.size > 1 && !isAnySpanishSelected && spanishCandidate != null) {
                    try {
                        val override = TrackSelectionOverride(
                            spanishCandidate.group.mediaTrackGroup,
                            listOf(spanishCandidate.trackIndex)
                        )
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .addOverride(override)
                            .setPreferredAudioLanguages("es", "es-419", "spa", "es-MX", "es-US", "es-ES", "es-CO", "es-AR")
                            .build()
                        android.util.Log.i("TVProAudio", "Auto-switched to Spanish track: ${spanishCandidate.name}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if (!isEmbedStream) {
                    isPlaying = playing
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isEmbedStream) return
                error.printStackTrace()
                triggerStreamFailover(activeStreamUrl, "Error de reproducción: ${error.errorCodeName}")
            }
            override fun onRenderedFirstFrame() {
                if (!isEmbedStream) {
                    isVideoReady = true
                    isBuffering = false
                    isEpisodeChanging = false
                    loadingSubtitle = "Preparando reproducción en alta definición"
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        if (!isEmbedStream && exoPlayer.playbackState == Player.STATE_READY) {
            isVideoReady = true
            isBuffering = false
            isEpisodeChanging = false
            loadingSubtitle = "Preparando reproducción en alta definición"
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
        }

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }


    // Proactive watchdog for ExoPlayer: ensures loading splash clears ONLY when player is actually ready
    LaunchedEffect(activeStreamUrl, isEmbedStream) {
        while (isActive && !isEmbedStream && !isVideoReady) {
            delay(300)
            if (exoPlayer.playbackState == Player.STATE_READY) {
                isVideoReady = true
                isBuffering = false
                isEpisodeChanging = false
                loadingSubtitle = "Preparando reproducción en alta definición"
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
    }


    // Universal Playback Watchdog (ExoPlayer & Web Embed):
    // If the stream fails to reach STATE_READY or start video within timeout, failover!
    LaunchedEffect(activeStreamUrl, isEmbedStream) {
        if (activeStreamUrl.isNotBlank()) {
            val targetUrl = activeStreamUrl
            val startTime = System.currentTimeMillis()
            val maxWaitMs = if (isEmbedStream) 4_500L else 9_000L
            while (isActive && activeStreamUrl == targetUrl) {
                delay(300)
                if (!isEmbedStream && isVideoReady) {
                    // Video playback or readiness confirmed by ExoPlayer
                    break
                }
                if (isEmbedStream && (isVideoReady || currentPositionMs > 1_000L) && durationMs > 0L) {
                    // Video playback actually confirmed by embed web player
                    break
                }
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= maxWaitMs) {
                    triggerStreamFailover(targetUrl, "Tiempo de espera agotado")
                    break
                }
            }
        }
    }


    // Save playback position periodically and on exit (for both ExoPlayer and WebView)
    LaunchedEffect(movie.url, isEmbedStream, currentSeasonNum, currentEpNum) {
        while (isActive) {
            delay(5000)
            val sNum = if (isSeriesContent) currentSeasonNum.toIntOrNull() else null
            val eNum = if (isSeriesContent) currentEpNum.toIntOrNull() else null
            if (isEmbedStream) {
                if (currentPositionMs > 0L && durationMs > 0L) {
                    WatchHistoryManager.updatePosition(
                        context,
                        movie.url,
                        currentPositionMs,
                        durationMs,
                        seasonNumber = sNum,
                        episodeNumber = eNum,
                        isSeries = isSeriesContent
                    )
                }
            } else if (exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                currentPositionMs = pos
                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                if (dur > 0) durationMs = dur
                WatchHistoryManager.updatePosition(
                    context,
                    movie.url,
                    pos,
                    dur,
                    seasonNumber = sNum,
                    episodeNumber = eNum,
                    isSeries = isSeriesContent
                )
            }
        }
    }


    // Sincronización continua de posición y búfer de descarga para la barra de progreso mientras los controles están visibles
    LaunchedEffect(showControls, isPlaying, isEmbedStream) {
        if (showControls) {
            while (isActive) {
                if (!isEmbedStream) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration
                    currentPositionMs = pos
                    bufferedPositionMs = buf
                    if (dur > 0L && dur != durationMs) {
                        durationMs = dur
                    }
                } else {
                    bufferedPositionMs = currentPositionMs
                }
                delay(250L)
            }
        }
    }


    // Trigger para mostrar tarjeta de siguiente episodio cuando restan 30 segundos
    LaunchedEffect(isSeriesContent, nextEpisode, isNextEpisodeDismissed, isEpisodeChanging, showNextEpisodePrompt) {
        while (isActive && isSeriesContent && nextEpisode != null && !isNextEpisodeDismissed && !isEpisodeChanging && !showNextEpisodePrompt) {
            delay(1000L)
            val pos = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition
            val dur = if (isEmbedStream) durationMs else exoPlayer.duration
            if (dur > 60_000L && pos > 0L) {
                val remaining = dur - pos
                if (remaining in 1L..30_000L) {
                    showControls = false
                    showNextEpisodePrompt = true
                    break
                }
            }
        }
    }


    // Cuenta regresiva automática de 10 segundos para el siguiente episodio (estilo Netflix)
    LaunchedEffect(showNextEpisodePrompt, nextEpisode) {
        if (showNextEpisodePrompt && nextEpisode != null) {
            try {
                delay(120)
                nextEpisodeFocusRequester.requestFocus()
            } catch (_: Exception) {}

            if (consecutiveAutoPlayCount < 3) {
                nextEpisodeCountdownSeconds = 10
                while (nextEpisodeCountdownSeconds > 0 && showNextEpisodePrompt) {
                    delay(1000L)
                    if (showNextEpisodePrompt) {
                        nextEpisodeCountdownSeconds--
                    }
                }
                if (showNextEpisodePrompt) {
                    showNextEpisodePrompt = false
                    consecutiveAutoPlayCount++
                    playEpisode(nextEpisode)
                }
            }
        }
    }


    val currentSeasonNumState by rememberUpdatedState(currentSeasonNum)
    val currentEpNumState by rememberUpdatedState(currentEpNum)
    val isSeriesContentState by rememberUpdatedState(isSeriesContent)
    val isEmbedStreamState by rememberUpdatedState(isEmbedStream)
    val currentPositionMsState by rememberUpdatedState(currentPositionMs)
    val durationMsState by rememberUpdatedState(durationMs)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                setPlaybackPlaying(false)
                val pos = if (isEmbedStreamState) currentPositionMsState else exoPlayer.currentPosition
                val dur = if (isEmbedStreamState) durationMsState else exoPlayer.duration
                if (pos > 0L) {
                    val sNum = currentSeasonNumState.toIntOrNull()
                    val eNum = currentEpNumState.toIntOrNull()
                    WatchHistoryManager.updatePosition(
                        context,
                        movie.url,
                        pos,
                        dur,
                        seasonNumber = sNum,
                        episodeNumber = eNum,
                        isSeries = isSeriesContentState
                    )
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val pos = if (isEmbedStreamState) currentPositionMsState else exoPlayer.currentPosition
            val dur = if (isEmbedStreamState) durationMsState else exoPlayer.duration
            if (pos > 0L) {
                val sNum = currentSeasonNumState.toIntOrNull()
                val eNum = currentEpNumState.toIntOrNull()
                WatchHistoryManager.updatePosition(
                    context,
                    movie.url,
                    pos,
                    dur,
                    seasonNumber = sNum,
                    episodeNumber = eNum,
                    isSeries = isSeriesContentState
                )
            }
        }
    }


    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }


    var prevShowControls by remember { mutableStateOf(false) }
    var prevShowServerDialog by remember { mutableStateOf(false) }
    var prevShowEpisodesDialog by remember { mutableStateOf(false) }

    // Auto Focus appropriate button when controls, video readiness, or dialogs open/close
    LaunchedEffect(showServerDialog, showEpisodesDialog, showControls, isVideoReady) {
        delay(80)
        try {
            when {
                showEpisodesDialog -> activeEpisodeFocusRequester.requestFocus()
                showServerDialog -> activeAudioTrackFocusRequester.requestFocus()
                prevShowServerDialog && !showServerDialog && showControls -> {
                    serverPillFocusRequester.requestFocus()
                }
                prevShowEpisodesDialog && !showEpisodesDialog && showControls -> {
                    episodesButtonFocusRequester.requestFocus()
                }
                !prevShowControls && showControls && isVideoReady -> playButtonFocusRequester.requestFocus()
                !prevShowControls && showControls -> backButtonFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
        prevShowControls = showControls
        prevShowServerDialog = showServerDialog
        prevShowEpisodesDialog = showEpisodesDialog
    }

    var controlsWakeTick by remember { mutableIntStateOf(0) }
    var controlsHideFastTick by remember { mutableIntStateOf(0) }

    // Controls Auto-Hide Timer (Rápido y dinámico: 3.5s normal, 1.5s tras pulsar Play)
    LaunchedEffect(showControls, isPlaying, showServerDialog, showEpisodesDialog, controlsWakeTick, controlsHideFastTick) {
        if (showControls && isPlaying && !showServerDialog && !showEpisodesDialog) {
            val timeout = if (controlsHideFastTick > 0) 1500L else 3500L
            delay(timeout)
            showControls = false
            controlsHideFastTick = 0
        }
    }


    // Seek Toast Auto-Dismiss
    LaunchedEffect(seekToastText) {
        if (seekToastText != null) {
            delay(1500)
            seekToastText = null
        }
    }


    fun wakeUpControls() {
        if (showNextEpisodePrompt) return
        showControls = true
        controlsHideFastTick = 0
        controlsWakeTick++
    }

    fun performSeek(forward: Boolean, stepMs: Long = 10_000L) {
        if (showControls) {
            controlsHideFastTick = 0
            controlsWakeTick++
        }
        val basePos = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L)
        val maxDur = if (durationMs > 0L) durationMs else Long.MAX_VALUE
        val newPos = if (forward) {
            minOf(maxDur, basePos + stepMs)
        } else {
            maxOf(0L, basePos - stepMs)
        }
        seekToMs(newPos)
        val timeLabel = formatTimeSec(newPos / 1000)
        seekToastText = if (forward) "+${stepMs / 1000}s  |  $timeLabel" else "-${stepMs / 1000}s  |  $timeLabel"
    }

    DisposableEffect(showControls, showServerDialog, showEpisodesDialog, showNextEpisodePrompt, nextEpisodeSelectedButton, isPlaying, durationMs, isEmbedStream, currentPositionMs, nextEpisode, previousEpisode) {
        var lastHandledDownCode = -1
        PlayerKeyDispatcher.onPlayerKeyEvent = { event ->
            if (event.action == android.view.KeyEvent.ACTION_UP) {
                if (lastHandledDownCode != -1 && lastHandledDownCode == event.keyCode) {
                    lastHandledDownCode = -1
                    true // Consume ACTION_UP for any key whose ACTION_DOWN we handled to prevent phantom clicks
                } else {
                    false
                }
            } else if (showNextEpisodePrompt && nextEpisode != null) {
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    lastHandledDownCode = event.keyCode
                    when (event.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                            nextEpisodeSelectedButton = 0
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            nextEpisodeSelectedButton = 1
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_SPACE -> {
                            if (nextEpisodeSelectedButton == 0) {
                                consecutiveAutoPlayCount = 0
                                showNextEpisodePrompt = false
                                playEpisode(nextEpisode)
                            } else {
                                showNextEpisodePrompt = false
                                isNextEpisodeDismissed = true
                                consecutiveAutoPlayCount = 0
                                wakeUpControls()
                            }
                            true
                        }
                        android.view.KeyEvent.KEYCODE_BACK,
                        android.view.KeyEvent.KEYCODE_ESCAPE -> {
                            showNextEpisodePrompt = false
                            isNextEpisodeDismissed = true
                            consecutiveAutoPlayCount = 0
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            true
                        }
                        else -> true
                    }
                } else {
                    true
                }
            } else if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                val keyCode = event.keyCode
                val handled = if (keyCode == android.view.KeyEvent.KEYCODE_BACK || keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                    if (showEpisodesDialog) {
                        showEpisodesDialog = false
                        wakeUpControls()
                        true
                    } else if (showServerDialog) {
                        showServerDialog = false
                        wakeUpControls()
                        true
                    } else if (showControls) {
                        showControls = false
                        true
                    } else {
                        onBackClick()
                        true
                    }
                } else if (!showControls && !showServerDialog && !showEpisodesDialog) {
                    when (keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            performSeek(false)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT, android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            performSeek(true)
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT, android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                            if (isSeriesContent && nextEpisode != null) {
                                playEpisode(nextEpisode)
                                true
                            } else {
                                wakeUpControls()
                                true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS, android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                            if (isSeriesContent && previousEpisode != null) {
                                playEpisode(previousEpisode)
                                true
                            } else {
                                wakeUpControls()
                                true
                            }
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            togglePlayPause()
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            setPlaybackPlaying(true)
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            setPlaybackPlaying(false)
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_SPACE -> {
                            togglePlayPause()
                            wakeUpControls()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_MENU,
                        android.view.KeyEvent.KEYCODE_INFO -> {
                            wakeUpControls()
                            true
                        }
                        else -> {
                            wakeUpControls()
                            true
                        }
                    }
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    togglePlayPause()
                    wakeUpControls()
                    true
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY) {
                    setPlaybackPlaying(true)
                    wakeUpControls()
                    true
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    setPlaybackPlaying(false)
                    wakeUpControls()
                    true
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_REWIND) {
                    performSeek(false)
                    wakeUpControls()
                    true
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                    performSeek(true)
                    wakeUpControls()
                    true
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_NEXT) {
                    if (isSeriesContent && nextEpisode != null) {
                        playEpisode(nextEpisode)
                        true
                    } else false
                } else if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                    if (isSeriesContent && previousEpisode != null) {
                        playEpisode(previousEpisode)
                        true
                    } else false
                } else {
                    false
                }
                if (handled) {
                    lastHandledDownCode = keyCode
                }
                handled
            } else {
                false
            }
        }
        onDispose {
            PlayerKeyDispatcher.onPlayerKeyEvent = null
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    if (event.key == Key.Back || event.key == Key.Escape) {
                        if (showNextEpisodePrompt) {
                            showNextEpisodePrompt = false
                            isNextEpisodeDismissed = true
                            consecutiveAutoPlayCount = 0
                            wakeUpControls()
                            return@onPreviewKeyEvent true
                        } else if (showEpisodesDialog) {
                            showEpisodesDialog = false
                            showControls = true
                            return@onPreviewKeyEvent true
                        } else if (showServerDialog) {
                            showServerDialog = false
                            showControls = true
                            return@onPreviewKeyEvent true
                        } else if (showControls) {
                            showControls = false
                            return@onPreviewKeyEvent true
                        } else {
                            onBackClick()
                            return@onPreviewKeyEvent true
                        }
                    }

                    // Floating Next Episode Overlay D-Pad Controls
                    if (showNextEpisodePrompt && nextEpisode != null) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                nextEpisodeSelectedButton = 0
                                try { nextEpisodeFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionRight -> {
                                nextEpisodeSelectedButton = 1
                                try { nextEpisodeCancelFocusRequester.requestFocus() } catch (_: Exception) {}
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar -> {
                                if (nextEpisodeSelectedButton == 0) {
                                    consecutiveAutoPlayCount = 0
                                    showNextEpisodePrompt = false
                                    playEpisode(nextEpisode)
                                } else {
                                    showNextEpisodePrompt = false
                                    isNextEpisodeDismissed = true
                                    consecutiveAutoPlayCount = 0
                                    wakeUpControls()
                                }
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionUp, Key.DirectionDown -> {
                                return@onPreviewKeyEvent true
                            }
                        }
                    }

                    // When controls are hidden, allow immediate pause/play or seek with a single remote press!
                    if (!showControls && !showServerDialog && !showEpisodesDialog && !showNextEpisodePrompt) {
                        when (event.key) {
                            Key.DirectionLeft, Key.MediaRewind -> {
                                performSeek(false)
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionRight, Key.MediaFastForward -> {
                                performSeek(true)
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.MediaPlayPause -> {
                                togglePlayPause()
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                            Key.MediaPlay -> {
                                setPlaybackPlaying(true)
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                            Key.MediaPause -> {
                                setPlaybackPlaying(false)
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                            else -> {
                                wakeUpControls()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    wakeUpControls()
                    when (event.key) {
                        Key.MediaRewind -> {
                            performSeek(false)
                            true
                        }
                        Key.MediaFastForward -> {
                            performSeek(true)
                            true
                        }
                        Key.MediaPlayPause, Key.Spacebar -> {
                            togglePlayPause()
                            true
                        }
                        Key.MediaPlay -> {
                            setPlaybackPlaying(true)
                            true
                        }
                        Key.MediaPause -> {
                            setPlaybackPlaying(false)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        if (availableStreams.isEmpty()) {
            if (isExtracting || isEpisodeChanging) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0D10))
                        .zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else {
                val noContentFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(120)
                    try { noContentFocusRequester.requestFocus() } catch (_: Exception) {}
                }
                BackHandler { onBackClick() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0D10))
                        .zIndex(100f),
                    contentAlignment = Alignment.Center
                ) {
                if (!movie.backdrop.isNullOrBlank() || movie.image.isNotBlank()) {
                    AsyncImage(
                        model = movie.backdrop ?: movie.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.2f)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161922).copy(alpha = 0.96f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Contenido aún no disponible",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aún no tenemos este contenido disponible para reproducir en nuestros servidores.",
                        fontFamily = Inter,
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    var isBtnFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBtnFocused) Color(0xFFF5F2EB) else Color(0xFF1E232A),
                            contentColor = if (isBtnFocused) Color.Black else Color(0xFFF5F2EB)
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        modifier = Modifier
                            .focusRequester(noContentFocusRequester)
                            .onFocusChanged { isBtnFocused = it.isFocused }
                    ) {
                        Text(
                            text = "VOLVER AL DETALLE",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.4.sp
                        )
                    }
                }
            }
        }
    }


        // Video View: Hybrid Native Media3 ExoPlayer & TV Fullscreen Web Player
        if (isEmbedStream) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        try {
                            val cookieMgr = android.webkit.CookieManager.getInstance()
                            cookieMgr.setAcceptCookie(true)
                            cookieMgr.setAcceptThirdPartyCookies(this, true)
                        } catch (_: Exception) {}
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(true)

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun onPlayerDead(reason: String?) {
                                scope.launch(Dispatchers.Main) {
                                    triggerStreamFailover(activeStreamUrl, reason ?: "Error de Reproducción")
                                }
                            }
                        }, "TVProBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                // Silently drop all popup window creations. Never allow new windows or rogue tab hijacking
                                return false
                            }
                            override fun getDefaultVideoPoster(): android.graphics.Bitmap? {
                                return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                            }
                            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                                // Block all web permissions (Push Notifications, Geolocation, Camera, Mic, etc.)
                                request?.deny()
                            }
                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                                result?.cancel()
                                return true
                            }
                            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                                result?.confirm()
                                return true
                            }
                            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?): Boolean {
                                result?.cancel()
                                return true
                            }
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val msg = consoleMessage?.message() ?: ""
                                if (msg.contains("Error 232001", ignoreCase = true) ||
                                    msg.contains("The media could not be loaded", ignoreCase = true) ||
                                    msg.contains("No compatible source", ignoreCase = true) ||
                                    msg.contains("File not found", ignoreCase = true) ||
                                    msg.contains("Video not found", ignoreCase = true) ||
                                    msg.contains("404 Not Found", ignoreCase = true)) {
                                    triggerStreamFailover(activeStreamUrl, "Embed Console: $msg")
                                    return true
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress in 15..95) {
                                    val earlyBootstrap = """
                                        (function() {
                                            try {
                                                var today = new Date().getFullYear() + '-' + (new Date().getMonth() + 1) + '-' + new Date().getDate();
                                                localStorage.setItem('sf_pop_' + today, '99');
                                            } catch(e){}
                                            try {
                                                window.addEventListener('unhandledrejection', function(event) {
                                                    try {
                                                        var r = (event && event.reason) ? (event.reason.message || String(event.reason)) : '';
                                                        if (r.includes('dynamically imported module') || r.includes('Failed to fetch') || r.includes('hls') || r.includes('player')) {
                                                            if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                                window.TVProBridge.onPlayerDead('JS Unhandled: ' + r);
                                                            }
                                                        }
                                                    } catch(e){}
                                                });
                                            } catch(e){}
                                            try {
                                                if (!document.getElementById('tvpro-adblock-style')) {
                                                    var es = document.createElement('style');
                                                    es.id = 'tvpro-adblock-style';
                                                    es.innerHTML = 'html, body { background: #000 !important; background-color: #000 !important; overflow: hidden !important; margin: 0 !important; padding: 0 !important; } video { position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 2147483640 !important; object-fit: contain !important; background: #000 !important; } #sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, #logo-vimeos, .sf-resume-modal, [class*="sf-resume-modal"], #sf-resume-title, #sf-resume-desc, .resume-modal, [class*="resume-dialog"], [id*="resume-modal"], [id*="resume-dialog"], div[role="dialog"][aria-labelledby*="resume"], div[style*="2147483647"], div[style*="opacity: 0.01"], div[style*="opacity: 0.001"], .jw-preview, .vjs-poster, .plyr__poster, .jw-ad-container, .jw-ad-controls, .ima-ad-container, .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container, .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu, .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick, .jw-button-container, .jw-slider-time, .vjs-control-bar, .vjs-title-bar, .vjs-modal-dialog, .plyr__controls, .fluid_controls_container, .captcha-gate__hint, [id*="ad-"], [class*="ad-"], [id*="banner"], [class*="banner"], [id*="toast"], [class*="toast"], [id*="notification"], [class*="notification"], [id*="popup"], [class*="popup"], dialog, a[href*="ggpick"], a[href*="adsanalytics"], iframe[src*="ad"], iframe[src*="phruk"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; width: 0 !important; height: 0 !important; z-index: -9999 !important; background-image: none !important; } video::-webkit-media-controls, video::-webkit-media-controls-enclosure, video::-webkit-media-controls-panel, video::-webkit-media-controls-overlay-play-button { display: none !important; opacity: 0 !important; -webkit-appearance: none !important; }';
                                                    (document.head || document.documentElement).appendChild(es);
                                                }
                                            } catch(e) {}
                                            try {
                                                var dummyWin = {
                                                    closed: false,
                                                    focus: function(){}, blur: function(){}, close: function(){},
                                                    location: { href: '', replace: function(){}, assign: function(){}, reload: function(){} },
                                                    document: { write: function(){}, close: function(){}, location: { href: '' } },
                                                    postMessage: function(){}, addEventListener: function(){}, removeEventListener: function(){}
                                                };
                                                window.open = function() { return dummyWin; };
                                                window.alert = function() {};
                                                window.confirm = function() { return true; };
                                                window.prompt = function() { return null; };
                                                window.f98a024e = { add: function(){}, config: function(){ return this; } };
                                                window.c5022bca5c8bac1451cf = '';
                                                window.u7 = function() { return ''; };
                                                window.pickDirect = function() {};
                                                if (window.Notification) {
                                                    window.Notification.requestPermission = function() { return Promise.resolve('denied'); };
                                                    window.Notification.permission = 'denied';
                                                }
                                                if (navigator.serviceWorker) {
                                                    navigator.serviceWorker.register = function() { return Promise.reject(new Error('Blocked')); };
                                                }
                                            } catch(e) {}

                                            try {
                                                if (!window._tvproCaptureAttached) {
                                                    window._tvproCaptureAttached = true;
                                                    ['click', 'mousedown', 'mouseup', 'touchend', 'pointerdown'].forEach(function(evt) {
                                                        document.addEventListener(evt, function(e) {
                                                            var t = e.target;
                                                            while (t && t !== document && t !== document.documentElement) {
                                                                if (t.tagName === 'A') {
                                                                    var h = (t.getAttribute('href') || '').toLowerCase().trim();
                                                                    if (h.startsWith('http://') || h.startsWith('https://') || h.startsWith('//')) {
                                                                        var curHost = window.location.hostname.toLowerCase();
                                                                        if (!h.includes(curHost) && !h.includes('.m3u8') && !h.includes('.mp4')) {
                                                                            e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
                                                                            try { t.remove(); } catch(err){}
                                                                            return false;
                                                                        }
                                                                    }
                                                                }
                                                                if (t.id === 'sf-overlay' || t.id === 'adbd') {
                                                                    e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
                                                                    try { t.remove(); } catch(err){}
                                                                    return false;
                                                                }
                                                                t = t.parentElement;
                                                            }
                                                        }, true);
                                                    });
                                                }
                                            } catch(e) {}

                                            try {
                                                var scamWords = [
                                                    'congratulations', 'available $', 'withdraw money', 'transfer money',
                                                    'bonus', 'claim now', 'allow notification', 'permitir notificaciones',
                                                    'ganaste', 'premio', 'has ganado', 'iphone', 'descargar ahora', 'install',
                                                    'click allow', 'not a robot', 'e-captcha', 'are not a robot', 'robot', 'captcha',
                                                    'verify you are human', 'verify that you are human', 'press allow to continue'
                                                ];
                                                document.querySelectorAll('div, dialog, section, aside, form, p, span, a, button, h1, h2, h3, h4').forEach(function(el) {
                                                    if (el.tagName === 'VIDEO' || (el.querySelector && el.querySelector('video'))) return;
                                                    var txt = (el.innerText || el.textContent || '').toLowerCase();
                                                    if (scamWords.some(function(w) { return txt.includes(w); })) {
                                                        var targetNode = el;
                                                        var curr = el;
                                                        while (curr && curr.parentElement && curr.parentElement !== document.body && curr.parentElement !== document.documentElement) {
                                                            curr = curr.parentElement;
                                                            if (curr.tagName !== 'BODY' && curr.tagName !== 'HTML' && !curr.querySelector('video')) {
                                                                targetNode = curr;
                                                            }
                                                        }
                                                        try { targetNode.remove(); } catch(e) {}
                                                    }
                                                });
                                                document.querySelectorAll('#sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, iframe[src*="phruk"], iframe[src*="captcha"]').forEach(function(el) {
                                                    try { el.remove(); } catch(e){}
                                                });
                                                if (!document.getElementById('tvpro-early-resume-hide')) {
                                                    var es = document.createElement('style');
                                                    es.id = 'tvpro-early-resume-hide';
                                                    es.innerHTML = '.sf-resume-modal, [class*="sf-resume-modal"], #sf-resume-title, #sf-resume-desc, .resume-modal, [class*="resume-dialog"], div[role="dialog"][aria-labelledby*="resume"] { opacity: 0 !important; visibility: hidden !important; pointer-events: none !important; position: fixed !important; top: -9999px !important; left: -9999px !important; width: 0 !important; height: 0 !important; z-index: -9999 !important; }';
                                                    (document.head || document.documentElement).appendChild(es);
                                                }
                                                var rBtn = document.querySelector('.sf-resume-modal__btn--primary, button.sf-resume-modal__btn--primary, .sf-resume-modal button');
                                                if (rBtn) {
                                                    try { rBtn.click(); } catch(e){}
                                                    try {
                                                        var rEvt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                        rBtn.dispatchEvent(rEvt);
                                                    } catch(e){}
                                                }
                                            } catch(e) {}

                                            if (!window._tvproUserPaused && !window._tvproSeeking) {
                                                var btn = document.querySelector('.jw-display-icon-container, .vjs-big-play-button, button.play, .play-button, #play, #loadIframeButton');
                                                if (btn) { try { btn.click(); } catch(e){} }
                                                var v = document.querySelector('video');
                                                if (v && v.paused && !v.seeking) { try { v.play(); } catch(e){} }
                                                if (window.jwplayer && typeof window.jwplayer === 'function') {
                                                    try {
                                                        var p = window.jwplayer();
                                                        if (p && typeof p.play === 'function') p.play(true);
                                                    } catch(e) {}
                                                }
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(earlyBootstrap, null)
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    val failedUrl = request.url?.toString() ?: activeStreamUrl
                                    triggerStreamFailover(failedUrl, "Error de Red")
                                }
                            }
                            override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                                val status = errorResponse?.statusCode ?: 200
                                val isMainFrame = request?.isForMainFrame == true
                                if (isMainFrame && status in listOf(404, 410, 500, 502, 503)) {
                                    val failedUrl = request?.url?.toString() ?: activeStreamUrl
                                    triggerStreamFailover(failedUrl, "Error HTTP $status")
                                }
                            }
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): android.webkit.WebResourceResponse? {
                                val u = request?.url?.toString()?.lowercase() ?: return null
                                val adKeywords = listOf(
                                    "adangle", "imasdk", "ima3.js", "ima-ad-player", "adsanalytics",
                                    "ggpick", "doubleclick", "googlesyndication", "popads", "propeller",
                                    "adsterra", "monetag", "exoclick", "juicyads", "trafficjunky",
                                    "adnxs", "bidswitch", "rubiconproject", "criteo", "taboola",
                                    "outbrain", "yandex.ru/ads", "adsystem", "adserver", "/vast",
                                    "vpaid", "delivery/ajs.php", "a.magsrv.com", "tsyndicate",
                                    "google-analytics", "googletagmanager", "histats", "statcounter",
                                    "scorecardresearch", "hotjar", "clarity.ms", "disqus", "crypto",
                                    // VOE / Vidhide / Primeload scam & push notification networks & fake captchas
                                    "phruk.org", "phrenological", "fulahunslack", "quge5", "chaliceguzzler", "periodpen",
                                    "ads-twitter", "megankid", "onesignal", "pushengage", "cleverpush", "pushalert", "subscribers.com",
                                    "clickadu", "hilltopads", "richpush", "evadav", "rollerads",
                                    "popcash", "popunder", "adcash", "ad-delivery", "adsco",
                                    "bonus", "withdraw", "congratulations", "fcm.googleapis.com",
                                    "captcha", "e-captcha", "not-a-robot", "verify-robot", "check-robot",
                                    "click-allow", "allow-to-continue", "novemberfrost", "adblockghost",
                                    "savantwaxwing", "auqot", "ekhay", "b3mny", "quieterangle", "6opo", "vespinemudroom", "rtmark.net",
                                    "gillyspencie", "blockadsnot", "ragckcvjwcgos", "ffiahobjyrbzoc",
                                    "americanacademyofthermology", "syfpd", "dnhfi5nn2dt67",
                                    "notix", "pushground", "adkeeper", "mgid", "propellerads",
                                    "recaptcha", "turnstile", "hcaptcha", "challenge-platform",
                                    "cloudflareinsights", "beacon.min.js", "topmostlayer", "popunders"
                                )
                                if (adKeywords.any { u.contains(it) }) {
                                    val corsHeaders = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Access-Control-Allow-Methods" to "GET, POST, OPTIONS, HEAD",
                                        "Access-Control-Allow-Headers" to "*"
                                    )
                                    return android.webkit.WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        200,
                                        "OK",
                                        corsHeaders,
                                        java.io.ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val target = request?.url?.toString() ?: return false
                                val scheme = request.url?.scheme?.lowercase() ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    return true // block non-web schemes (intent:, market:, etc.)
                                }
                                val targetLower = target.lowercase()
                                val targetHost = request.url?.host?.lowercase() ?: ""
                                val activeHost = try { java.net.URI(activeStreamUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }

                                val blockedKeywords = listOf(
                                    "captcha", "e-captcha", "robot", "click-allow", "allow-to-continue",
                                    "phruk", "phrenological", "fulahunslack", "quge5", "chaliceguzzler", "periodpen",
                                    "adangle", "imasdk", "popads", "propeller", "adsterra", "monetag", "exoclick",
                                    "juicyads", "trafficjunky", "adnxs", "bidswitch", "criteo", "taboola", "outbrain",
                                    "bet365", "1xbet", "casino", "yandex.ru/ads", "delivery/ajs", "tsyndicate",
                                    "ads-twitter", "megankid", "clickadu", "adcash",
                                    "bonus", "withdraw", "congratulations", "pushengage", "onesignal", "cleverpush",
                                    "novemberfrost", "adblockghost", "savantwaxwing",
                                    "gillyspencie", "blockadsnot", "ragckcvjwcgos", "ffiahobjyrbzoc", "americanacademyofthermology", "syfpd", "dnhfi5nn2dt67",
                                    "notix", "pushground", "adkeeper", "mgid", "propellerads",
                                    "recaptcha", "turnstile", "hcaptcha", "challenge-platform",
                                    "cloudflareinsights", "beacon.min.js", "topmostlayer", "popunders"
                                )
                                if (blockedKeywords.any { targetLower.contains(it) }) {
                                    return true // BLOCK scam/captcha/ad URLs
                                }

                                // Main frame navigation guard: Never allow the main window to be redirected away to a non-video domain
                                if (request.isForMainFrame) {
                                    val isAllowedHost = (activeHost.isNotBlank() && (targetHost == activeHost || targetHost.endsWith(".$activeHost") || activeHost.endsWith(".$targetHost"))) ||
                                        targetHost.contains("ok.ru") || targetHost.contains("odnoklassniki") ||
                                        targetHost.contains("streamtape") || targetHost.contains("filemoon") ||
                                        targetHost.contains("byse") || targetHost.contains("bysefujedu") || targetHost.contains("uqload") ||
                                        targetHost.contains("primeload") || targetHost.contains("mixdrop") || targetHost.contains("m1xdrop") ||
                                        targetHost.contains("voe.sx") || targetHost.contains("arianathankspecial") ||
                                        targetHost.contains("callistanise") || targetHost.contains("vidhide") ||
                                        targetHost.contains("dramiyos-cdn") || targetHost.contains("delivery") ||
                                        targetHost.contains("megankid") || targetHost.contains("vimeos") ||
                                        targetHost.contains("watchsb") || targetHost.contains("streamsb") ||
                                        targetHost.contains("do7go") || targetHost.contains("playmogo") ||
                                        targetHost.contains("dood") || targetHost.contains("mp4upload") ||
                                        targetHost.contains("flaswish") || targetHost.contains("primecdn")

                                    if (!isAllowedHost) {
                                        return true // Block rogue external redirect away from player
                                    }
                                }
                                return false
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isBuffering = true
                                isVideoReady = false
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        try {
                                            if (!document.getElementById('tvpro-adblock-style')) {
                                                var es = document.createElement('style');
                                                es.id = 'tvpro-adblock-style';
                                                es.innerHTML = 'html, body { background: #000 !important; background-color: #000 !important; overflow: hidden !important; margin: 0 !important; padding: 0 !important; } video { position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 2147483640 !important; object-fit: contain !important; background: #000 !important; } #sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, #logo-vimeos, .sf-resume-modal, [class*="sf-resume-modal"], #sf-resume-title, #sf-resume-desc, .resume-modal, [class*="resume-dialog"], [id*="resume-modal"], [id*="resume-dialog"], div[role="dialog"][aria-labelledby*="resume"], div[style*="2147483647"], div[style*="opacity: 0.01"], div[style*="opacity: 0.001"], .jw-preview, .vjs-poster, .plyr__poster, .jw-ad-container, .jw-ad-controls, .ima-ad-container, .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container, .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu, .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick, .jw-button-container, .jw-slider-time, .vjs-control-bar, .vjs-big-play-button, .vjs-title-bar, .vjs-modal-dialog, .plyr__controls, .plyr__control--overlaid, .fluid_controls_container, .media-control, .captcha-gate__hint, [id*="ad-"], [class*="ad-"], [id*="banner"], [class*="banner"], [id*="toast"], [class*="toast"], [id*="notification"], [class*="notification"], [id*="popup"], [class*="popup"], dialog, a[href*="ggpick"], a[href*="adsanalytics"], iframe[src*="ad"], iframe[src*="phruk"] { display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important; width: 0 !important; height: 0 !important; z-index: -9999 !important; background-image: none !important; } video::-webkit-media-controls, video::-webkit-media-controls-enclosure, video::-webkit-media-controls-panel, video::-webkit-media-controls-overlay-play-button { display: none !important; opacity: 0 !important; -webkit-appearance: none !important; }';
                                                (document.head || document.documentElement).appendChild(es);
                                            }
                                        } catch(e) {}
                                        try {
                                            var dummyWin = {
                                                closed: false,
                                                focus: function(){}, blur: function(){}, close: function(){},
                                                location: { href: '', replace: function(){}, assign: function(){}, reload: function(){} },
                                                document: { write: function(){}, close: function(){}, location: { href: '' } },
                                                postMessage: function(){}, addEventListener: function(){}, removeEventListener: function(){}
                                            };
                                            window.open = function() { return dummyWin; };
                                            window.alert = function() {};
                                            window.confirm = function() { return true; };
                                            window.prompt = function() { return null; };
                                            if (window.Notification) {
                                                window.Notification.requestPermission = function() { return Promise.resolve('denied'); };
                                                window.Notification.permission = 'denied';
                                            }
                                            if (navigator.serviceWorker) {
                                                navigator.serviceWorker.register = function() { return Promise.reject(new Error('Blocked')); };
                                            }
                                        } catch(e) {}
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val adBlockAndDeadCheckJs = """
                                (function() {
                                    // 1. Check dead stream or hijacked fake-captcha page
                                    var t = document.body ? (document.body.innerText || document.body.textContent || '').toLowerCase() : '';
                                    if (t.includes('232001') || t.includes('no se puede reproducir') || t.includes('archivo de video no se puede reproducir') || t.includes('video file cannot be played') || t.includes('error loading media') || t.includes('the media playback was aborted') || t.includes('no longer available') || t.includes('expired or has been deleted') || t.includes('file was deleted') || t.includes('file has been deleted') || t.includes('video deleted') || t.includes('video not found') || t.includes('deleted by the creator') || t.includes('contenido no disponible') || t.includes('404 not found') || t.includes('page not found') || t.includes('file not found') || t.includes('video unavailable') || t.includes('video is unavailable') || t.includes('not found') || t.includes('does not exist') || t.includes('this video cannot be played') || t.includes('no video with supported format')) {
                                        return 'DEAD';
                                    }
                                    
                                    // 2. Safe dummy window.open & suppress popups, alerts, and navigation hijacking
                                    var dummyWin = {
                                        closed: false,
                                        focus: function(){}, blur: function(){}, close: function(){},
                                        location: { href: '', replace: function(){}, assign: function(){}, reload: function(){} },
                                        document: { write: function(){}, close: function(){}, location: { href: '' } },
                                        postMessage: function(){}, addEventListener: function(){}, removeEventListener: function(){}
                                    };
                                    window.open = function() { return dummyWin; };
                                    window.alert = function() {};
                                    window.confirm = function() { return true; };
                                    window.prompt = function() { return null; };
                                    if (window.Notification) {
                                        window.Notification.requestPermission = function() { return Promise.resolve('denied'); };
                                        window.Notification.permission = 'denied';
                                    }
                                    if (navigator.serviceWorker) {
                                        navigator.serviceWorker.register = function() { return Promise.reject(new Error('Blocked')); };
                                    }
                                    
                                    // 3. Inject CSS removing ad overlays, banners, clickjackers, grey posters, scam modals, push dialogs AND native web player chrome
                                    var style = document.createElement('style');
                                    style.id = 'tvpro-adblock-style';
                                    style.innerHTML = `
                                        html, body {
                                            background: #000 !important;
                                            background-color: #000 !important;
                                            overflow: hidden !important;
                                            margin: 0 !important;
                                            padding: 0 !important;
                                            width: 100vw !important;
                                            height: 100vh !important;
                                        }
                                        video {
                                            position: fixed !important;
                                            top: 0 !important;
                                            left: 0 !important;
                                            width: 100vw !important;
                                            height: 100vh !important;
                                            max-width: 100vw !important;
                                            max-height: 100vh !important;
                                            z-index: 2147483640 !important;
                                            object-fit: contain !important;
                                            background: #000 !important;
                                        }
                                        #sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, #logo-vimeos,
                                        .sf-resume-modal, [class*="sf-resume-modal"], #sf-resume-title, #sf-resume-desc,
                                        .resume-modal, [class*="resume-dialog"], [id*="resume-modal"], [id*="resume-dialog"],
                                        div[role="dialog"][aria-labelledby*="resume"],
                                        div[style*="2147483647"], div[style*="opacity: 0.01"], div[style*="opacity: 0.001"],
                                        .jw-preview, .vjs-poster, .plyr__poster,
                                        .jw-ad-container, .jw-ad-controls, .ima-ad-container,
                                        .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container,
                                        .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu,
                                        .jw-controls-backdrop, .jw-nextup-container, .jw-float-icon, .jw-rightclick,
                                        .jw-button-container, .jw-slider-time,
                                        .vjs-control-bar, .vjs-big-play-button, .vjs-title-bar, .vjs-modal-dialog,
                                        .plyr__controls, .plyr__control--overlaid, .fluid_controls_container, .media-control,
                                        .captcha-gate__hint,
                                        [id*="ad-"], [class*="ad-"], [id*="banner"], [class*="banner"],
                                        [id*="toast"], [class*="toast"],
                                        [id*="notification"], [class*="notification"],
                                        [id*="popup"], [class*="popup"],
                                        dialog,
                                        a[href*="ggpick"], a[href*="adsanalytics"], iframe[src*="ad"], iframe[src*="phruk"] {
                                            display: none !important;
                                            visibility: hidden !important;
                                            opacity: 0 !important;
                                            pointer-events: none !important;
                                            width: 0 !important;
                                            height: 0 !important;
                                            z-index: -9999 !important;
                                            background-image: none !important;
                                        }
                                        video::-webkit-media-controls,
                                        video::-webkit-media-controls-enclosure,
                                        video::-webkit-media-controls-panel,
                                        video::-webkit-media-controls-overlay-play-button {
                                            display: none !important;
                                            opacity: 0 !important;
                                            -webkit-appearance: none !important;
                                        }
                                    `;
                                    if (!document.getElementById('tvpro-adblock-style')) {
                                        (document.head || document.documentElement).appendChild(style);
                                    }
                                    
                                    function handleResumeModals(d) {
                                        if (!d) return;
                                        try {
                                            var modal = d.querySelector('.sf-resume-modal, [class*="resume-modal"], [id*="resume-modal"], div[role="dialog"][aria-labelledby*="resume"]');
                                            if (modal) {
                                                var btn = modal.querySelector('.sf-resume-modal__btn--primary, button.sf-resume-modal__btn--primary, button.primary, button:first-of-type');
                                                if (!btn) {
                                                    var btns = modal.querySelectorAll('button');
                                                    for (var b = 0; b < btns.length; b++) {
                                                        var t = (btns[b].innerText || btns[b].textContent || '').toLowerCase();
                                                        if (t.includes('yes') || t.includes('resume') || t.includes('reanudar') || t.includes('continuar')) {
                                                            btn = btns[b];
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (btn) {
                                                    try { btn.click(); } catch(e){}
                                                    try {
                                                        var me = new MouseEvent('click', { bubbles: true, cancelable: true, view: d.defaultView || window });
                                                        btn.dispatchEvent(me);
                                                    } catch(e){}
                                                }
                                            }
                                        } catch(e){}
                                    }

                                    // 4. Aggressively remove physical ad overlay & scam dialog nodes
                                    function purgeScamAndAds() {
                                        handleResumeModals(document);
                                        // Auto-solve Filemoon / Byse proof-of-work play button immediately
                                        var gateBtn = document.querySelector('.captcha-gate__play, button.captcha-gate__play, [class*="captcha-gate__play"], [class*="captcha-gate"] button');
                                        if (gateBtn) {
                                            try { gateBtn.click(); } catch(e){}
                                            try {
                                                var ge = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                gateBtn.dispatchEvent(ge);
                                            } catch(e){}
                                        }

                                        var v = document.querySelector('video');
                                        if (!v) {
                                            var ifrs = document.querySelectorAll('iframe');
                                            for (var f = 0; f < ifrs.length; f++) {
                                                try {
                                                    var idoc = ifrs[f].contentDocument;
                                                    if (idoc) {
                                                        handleResumeModals(idoc);
                                                        var iv = idoc.querySelector('video');
                                                        if (iv) { v = iv; break; }
                                                    }
                                                } catch(e) {}
                                            }
                                        }
                                        
                                        // 1. Force video element to fill screen above all background ad elements
                                        if (v) {
                                            try {
                                                v.style.setProperty('position', 'fixed', 'important');
                                                v.style.setProperty('top', '0', 'important');
                                                v.style.setProperty('left', '0', 'important');
                                                v.style.setProperty('width', '100vw', 'important');
                                                v.style.setProperty('height', '100vh', 'important');
                                                v.style.setProperty('max-width', '100vw', 'important');
                                                v.style.setProperty('max-height', '100vh', 'important');
                                                v.style.setProperty('z-index', '2147483640', 'important');
                                                v.style.setProperty('object-fit', 'contain', 'important');
                                                v.style.setProperty('background', '#000', 'important');
                                                v.style.setProperty('margin', '0', 'important');
                                                v.style.setProperty('padding', '0', 'important');

                                                var anc = v.parentElement;
                                                while (anc && anc !== document.body && anc !== document.documentElement) {
                                                    anc.style.setProperty('transform', 'none', 'important');
                                                    anc.style.setProperty('filter', 'none', 'important');
                                                    anc.style.setProperty('perspective', 'none', 'important');
                                                    anc.style.setProperty('contain', 'none', 'important');
                                                    anc = anc.parentElement;
                                                }
                                            } catch(e) {}
                                        }
                                        
                                        // 2. Kill all ad/scam/captcha iframes, but preserve player frames
                                        var iframes = document.querySelectorAll('iframe');
                                        for (var i = 0; i < iframes.length; i++) {
                                            var ifr = iframes[i];
                                            if (ifr.id === 'sf-player-frame') continue;
                                            var src = (ifr.src || '').toLowerCase();
                                            if (src.includes('player') || src.includes('embed') || src.includes('video')) continue;
                                            if (src.includes('captcha') || src.includes('phruk') || src.includes('turnstile') || src.includes('recaptcha') || src.includes('hcaptcha') || src.includes('doubleclick') || src.includes('ads') || src.includes('syfpd') || src.includes('toast') || src.includes('notix')) {
                                                try { ifr.remove(); } catch(e) {}
                                                continue;
                                            }
                                            if (ifr.offsetWidth > 0 && ifr.offsetWidth < 300 && ifr.offsetHeight > 0 && ifr.offsetHeight < 300) {
                                                try { ifr.remove(); } catch(e) {}
                                            }
                                        }
                                        
                                        // 3. Remove all images (ads, icons, captcha illustrations, portraits)
                                        var imgs = document.querySelectorAll('img');
                                        for (var m = 0; m < imgs.length; m++) {
                                            try { imgs[m].remove(); } catch(e){}
                                        }
                                        
                                        // 4. Remove all scam / notification / captcha / ad nodes by text
                                        var scamWords = [
                                            'congratulations', 'available $', 'withdraw money', 'transfer money',
                                            'bonus', 'claim now', 'allow notification', 'permitir notificaciones',
                                            'ganaste', 'premio', 'has ganado', 'iphone', 'descargar ahora', 'install',
                                            'click allow', 'not a robot', 'e-captcha', 'are not a robot', 'robot', 'captcha',
                                            'verify you are human', 'verify that you are human', 'press allow to continue',
                                            'going viral', 'why thousands of people', 'simple trick', 'allow to continue',
                                            'privacy', 'terms', 'check your browser'
                                        ];
                                        var candidates = document.querySelectorAll('div, dialog, section, aside, form, p, span, a, button, h1, h2, h3, h4');
                                        candidates.forEach(function(el) {
                                            if (v && (el === v || el.contains(v))) return;
                                            if (el.classList && (el.classList.contains('captcha-gate__play') || el.querySelector('.captcha-gate__play'))) {
                                                var b = el.classList.contains('captcha-gate__play') ? el : el.querySelector('.captcha-gate__play');
                                                try { b.click(); } catch(e){}
                                                return;
                                            }
                                            var txt = (el.innerText || el.textContent || '').toLowerCase();
                                            if (scamWords.some(function(w) { return txt.includes(w); })) {
                                                var targetNode = el;
                                                var curr = el;
                                                while (curr && curr.parentElement && curr.parentElement !== document.body && curr.parentElement !== document.documentElement && (!v || !curr.parentElement.contains(v))) {
                                                    curr = curr.parentElement;
                                                }
                                                try { curr.remove(); } catch(e) {}
                                                return;
                                            }
                                            // Remove floating/overlay elements covering screen
                                            var st = window.getComputedStyle ? window.getComputedStyle(el) : null;
                                            if (st) {
                                                var pos = st.position;
                                                var z = parseInt(st.zIndex, 10);
                                                if ((pos === 'fixed' || pos === 'absolute') && (z > 5 || z >= 2147483640)) {
                                                    try { el.remove(); } catch(e) {}
                                                }
                                            }
                                        });
                                        document.querySelectorAll('#sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, iframe[src*="phruk"], iframe[src*="captcha"]').forEach(function(el) {
                                            try { el.remove(); } catch(e){}
                                        });

                                        // Clickjacking transparent overlays
                                        var allDivs = document.querySelectorAll('div');
                                        allDivs.forEach(function(d) {
                                            if (d.tagName === 'VIDEO' || (d.querySelector && d.querySelector('video'))) return;
                                            var st = window.getComputedStyle ? window.getComputedStyle(d) : null;
                                            if (!st) return;
                                            var z = parseInt(st.zIndex, 10);
                                            var op = parseFloat(st.opacity);
                                            var pos = st.position;
                                            if ((pos === 'fixed' || pos === 'absolute') && z > 999 && (op < 0.1 || st.visibility === 'hidden') && (st.width === '100%' || d.offsetWidth > 300)) {
                                                try { d.remove(); } catch(e) {}
                                            }
                                        });
                                    }
                                    purgeScamAndAds();

                                    // Continuous MutationObserver for real-time scam/popup destruction
                                    if (window.MutationObserver && !window._tvproObserver) {
                                        window._tvproObserver = new MutationObserver(function() {
                                            purgeScamAndAds();
                                        });
                                        window._tvproObserver.observe(document.documentElement, {
                                            childList: true,
                                            subtree: true
                                        });
                                    }

                                    // Capture-phase click & touch blocker: neutralizes clickjacking before ad listeners run
                                    if (!window._tvproCaptureAttached) {
                                        window._tvproCaptureAttached = true;
                                        ['click', 'mousedown', 'mouseup', 'touchend', 'pointerdown'].forEach(function(evt) {
                                            document.addEventListener(evt, function(e) {
                                                var t = e.target;
                                                while (t && t !== document && t !== document.documentElement) {
                                                    if (t.tagName === 'A') {
                                                        var h = (t.getAttribute('href') || '').toLowerCase().trim();
                                                        if (h.startsWith('http://') || h.startsWith('https://') || h.startsWith('//')) {
                                                            var curHost = window.location.hostname.toLowerCase();
                                                            if (!h.includes(curHost) && !h.includes('.m3u8') && !h.includes('.mp4')) {
                                                                e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
                                                                try { t.remove(); } catch(err){}
                                                                return false;
                                                            }
                                                        }
                                                    }
                                                    if (t.id === 'sf-overlay' || t.id === 'adbd') {
                                                        e.preventDefault(); e.stopPropagation(); e.stopImmediatePropagation();
                                                        try { t.remove(); } catch(err){}
                                                        return false;
                                                    }
                                                    t = t.parentElement;
                                                }
                                            }, true);
                                        });
                                    }

                                    // 5. JWPlayer: Auto skip ads, auto play, and disable native controls
                                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                                        try {
                                            var p = window.jwplayer();
                                            if (p && typeof p.on === 'function') {
                                                p.on('adPlay', function() {
                                                    if (typeof p.skipAd === 'function') p.skipAd();
                                                    else if (typeof p.stopAd === 'function') p.stopAd();
                                                });
                                                p.on('adError', function() {
                                                    if (typeof p.play === 'function') p.play();
                                                });
                                                p.on('adBlock', function() {
                                                    if (typeof p.play === 'function') p.play();
                                                });
                                                p.on('error', function(err) {
                                                    if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                        window.TVProBridge.onPlayerDead('JW Error 232001');
                                                    }
                                                });
                                                p.on('setupError', function(err) {
                                                    if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                        window.TVProBridge.onPlayerDead('JW Setup Error');
                                                    }
                                                });
                                            }
                                            if (!window._tvproUserPaused && !window._tvproSeeking && p && typeof p.play === 'function') p.play(true);
                                            if (p && typeof p.setControls === 'function') p.setControls(false);
                                        } catch(e) {}
                                    }
                                    
                                    // 6. Auto-start video & remove native HTML5 controls and poster
                                    var playSelectors = '.vid_play, [tsid="player-component_play"], .vid-card_cnt_w, #embedVideoC, .vid_play-ic, [data-l="t,play"], .jw-display-icon-container, .vjs-big-play-button, button.play, .play-button, .play-btn, #play, #loadIframeButton, .plyr__control--overlaid, button.aegis-launch, .aegis-launch, .aegis-launch-btn, [class*="aegis-launch"], button[aria-label="Play"], button[aria-label*="Play"], .vp-play, .vp_video_stub, .vp_play_btn, .html5-player-controls, .html5-vdata, [data-module="Player"], .v_play, .widget_play, div[class*="play-btn"], button[class*="play"], [aria-label*="play" i], [aria-label*="reproducir" i], .captcha-gate__play, button.captcha-gate__play, [class*="captcha-gate__play"], [class*="captcha-gate"] button, button[aria-label*="Play video" i], .sf-resume-modal__btn--primary, button.sf-resume-modal__btn--primary, .sf-resume-modal button, [class*="resume-modal"] button, [id*="resume"] button, .media-button--play, [class*="media-button--play"], .media-icon--play, media-player, media-play-button, [data-media-player], .sf-player, [class*="sf-player"], .sf-loading';
                                    if (!window._tvproUserPaused && !window._tvproSeeking) {
                                        var clickTargets = document.querySelectorAll(playSelectors);
                                        clickTargets.forEach(function(el) {
                                            try { el.click(); } catch(e){}
                                            try {
                                                var evt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                el.dispatchEvent(evt);
                                            } catch(e){}
                                        });
                                    }
                                    var sfOver = document.getElementById('sf-overlay');
                                    if (sfOver) { try { sfOver.remove(); } catch(e){} }
                                    var adbd = document.getElementById('adbd');
                                    if (adbd) { try { adbd.remove(); } catch(e){} }
                                    var v = document.querySelector('video');
                                    if (v) {
                                        v.controls = false;
                                        v.removeAttribute('controls');
                                        v.removeAttribute('poster');
                                        v.style.backgroundColor = '#000';
                                        v.muted = false;
                                        if (!window._tvproUserPaused && !window._tvproSeeking && v.paused) { try { v.play(); } catch(e){} }
                                    }
                                    
                                    // Continuous periodic sweeper that NEVER STOPS
                                    if (!window._tvproContinuousPurge) {
                                        window._tvproContinuousPurge = setInterval(function() {
                                            purgeScamAndAds();
                                            var deadTxt = (document.body ? (document.body.innerText || document.body.textContent || '') : '').toLowerCase();
                                            if (deadTxt.includes('404 not found') || deadTxt.includes('file not found') || deadTxt.includes('video not found') || deadTxt.includes('video deleted') || deadTxt.includes('file was deleted') || deadTxt.includes('deleted by the creator') || deadTxt.includes('no longer available') || deadTxt.includes('expired or has been deleted') || deadTxt.includes('video unavailable') || deadTxt.includes('video is unavailable') || deadTxt.includes('does not exist') || deadTxt.includes('contenido no disponible') || deadTxt.includes('232001') || deadTxt.includes('no se puede reproducir') || deadTxt.includes('archivo de video no se puede reproducir')) {
                                                if (window.TVProBridge && typeof window.TVProBridge.onPlayerDead === 'function') {
                                                    window.TVProBridge.onPlayerDead('Client 404 detected');
                                                }
                                            }
                                            var sf = document.getElementById('sf-overlay');
                                            if (sf) { try { sf.remove(); } catch(e){} }
                                            var ad = document.getElementById('adbd');
                                            if (ad) { try { ad.remove(); } catch(e){} }
                                            var vid = document.querySelector('video');
                                            if (!window._tvproUserPaused && !window._tvproSeeking) {
                                                if (vid && vid.paused && !vid.seeking) { try { vid.play(); } catch(e){} }
                                                var pBtn = document.querySelectorAll(playSelectors);
                                                pBtn.forEach(function(btn) {
                                                    if (!vid || (vid.paused && !vid.seeking)) {
                                                        try { btn.click(); } catch(e){}
                                                        try {
                                                            var me = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                            btn.dispatchEvent(me);
                                                        } catch(e){}
                                                    }
                                                });
                                            } else if (window._tvproUserPaused) {
                                                if (vid && !vid.paused) { try { vid.pause(); } catch(e){} }
                                                if (window.jwplayer && typeof window.jwplayer === 'function') {
                                                    try {
                                                        var jp = window.jwplayer();
                                                        if (jp) {
                                                            if (typeof jp.pause === 'function') jp.pause();
                                                            if (typeof jp.play === 'function') jp.play(false);
                                                        }
                                                        var jpA = window.jwplayer("a");
                                                        if (jpA && jpA !== jp) {
                                                            if (typeof jpA.pause === 'function') jpA.pause();
                                                            if (typeof jpA.play === 'function') jpA.play(false);
                                                        }
                                                    } catch(e){}
                                                }
                                            }
                                            var iframes = document.querySelectorAll('iframe');
                                            for (var i = 0; i < iframes.length; i++) {
                                                try {
                                                    var idoc = iframes[i].contentDocument;
                                                    if (idoc) {
                                                        handleResumeModals(idoc);
                                                        var isf = idoc.getElementById('sf-overlay');
                                                        if (isf) { try { isf.remove(); } catch(e){} }
                                                        if (!window._tvproUserPaused && !window._tvproSeeking) {
                                                            var iv = idoc.querySelector('video');
                                                            if (iv && iv.paused && !iv.seeking) { try { iv.play(); } catch(e){} }
                                                            var ipBtn = idoc.querySelectorAll(playSelectors);
                                                            ipBtn.forEach(function(b) {
                                                                if (!iv || (iv.paused && !iv.seeking)) {
                                                                    try { b.click(); } catch(e){}
                                                                    try {
                                                                        var me2 = new MouseEvent('click', { bubbles: true, cancelable: true, view: iframes[i].contentWindow || window });
                                                                        b.dispatchEvent(me2);
                                                                    } catch(e){}
                                                                }
                                                            });
                                                        } else if (window._tvproUserPaused) {
                                                            var iv = idoc.querySelector('video');
                                                            if (iv && !iv.paused) { try { iv.pause(); } catch(e){} }
                                                            try {
                                                                var iwin = iframes[i].contentWindow;
                                                                if (iwin && iwin.jwplayer && typeof iwin.jwplayer === 'function') {
                                                                    var ijp = iwin.jwplayer();
                                                                    if (ijp) {
                                                                        if (typeof ijp.pause === 'function') ijp.pause();
                                                                        if (typeof ijp.play === 'function') ijp.play(false);
                                                                    }
                                                                }
                                                            } catch(e){}
                                                        }
                                                    }
                                                } catch(e) {}
                                            }
                                        }, 400);
                                    }
                                    
                                    return 'OK';
                                })();
                                """.trimIndent()

                                view?.evaluateJavascript(adBlockAndDeadCheckJs) { result ->
                                    if (result != null && result.contains("DEAD")) {
                                        val failedUrl = url ?: activeStreamUrl
                                        triggerStreamFailover(failedUrl, "Servidor caído")
                                    }
                                }
                            }
                        }
                        fun loadTargetUrl(targetUrl: String) {
                            var normalizedUrl = targetUrl
                            if (normalizedUrl.contains("bysefujedu.com/d/")) {
                                normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                            } else if (normalizedUrl.contains("filemoon.") && normalizedUrl.contains("/d/")) {
                                normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                            }

                            val isDoramasflix = normalizedUrl.contains("doramasflix", ignoreCase = true) ||
                                normalizedUrl.contains("primeload", ignoreCase = true) ||
                                normalizedUrl.contains("vidhide", ignoreCase = true) ||
                                normalizedUrl.contains("callistanise", ignoreCase = true) ||
                                normalizedUrl.contains("do7go", ignoreCase = true) ||
                                normalizedUrl.contains("playmogo", ignoreCase = true) ||
                                normalizedUrl.contains("dood", ignoreCase = true) ||
                                normalizedUrl.contains("flaswish", ignoreCase = true) ||
                                normalizedUrl.contains("mp4upload", ignoreCase = true) ||
                                normalizedUrl.contains("voe", ignoreCase = true) ||
                                normalizedUrl.contains("byse", ignoreCase = true) ||
                                normalizedUrl.contains("mixdrop", ignoreCase = true) ||
                                normalizedUrl.contains("m1xdrop", ignoreCase = true) ||
                                normalizedUrl.contains("filemoon", ignoreCase = true)

                            val refererBase = when {
                                isDoramasflix -> "https://doramasflix.in/"
                                targetUrl.contains("tudorama") || targetUrl.contains("minochinos") || targetUrl.contains("bysesukior") || targetUrl.contains("abyss") || targetUrl.contains("banana") -> "https://tudorama.com/"
                                else -> "https://lamovie.org/"
                            }
                            val originBase = when {
                                isDoramasflix -> "https://doramasflix.in"
                                refererBase.contains("tudorama") -> "https://tudorama.com"
                                else -> "https://lamovie.org"
                            }

                            if (normalizedUrl.contains("vimeos", ignoreCase = true) || normalizedUrl.contains("lamovie", ignoreCase = true)) {
                                val iframeHtml = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <style>
                                            body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                            iframe { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe src="$normalizedUrl" allowfullscreen="true" allow="autoplay; fullscreen; encrypted-media"></iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL(refererBase, iframeHtml, "text/html", "UTF-8", null)
                            } else {
                                val headers = mapOf(
                                    "Referer" to refererBase,
                                    "Origin" to originBase
                                )
                                loadUrl(normalizedUrl, headers)
                            }
                        }
                        loadTargetUrl(activeStreamUrl)
                        scope.launch {
                            delay(800)
                            val w = width.toFloat().takeIf { it > 100f } ?: 960f
                            val h = height.toFloat().takeIf { it > 100f } ?: 540f
                            val now = android.os.SystemClock.uptimeMillis()
                            val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, w / 2f, h / 2f, 0)
                            val up = android.view.MotionEvent.obtain(now, now + 40, android.view.MotionEvent.ACTION_UP, w / 2f, h / 2f, 0)
                            dispatchTouchEvent(down)
                            dispatchTouchEvent(up)
                            down.recycle()
                            up.recycle()

                            delay(1200)
                            val now2 = android.os.SystemClock.uptimeMillis()
                            val down2 = android.view.MotionEvent.obtain(now2, now2, android.view.MotionEvent.ACTION_DOWN, w / 2f, h / 2f, 0)
                            val up2 = android.view.MotionEvent.obtain(now2, now2 + 40, android.view.MotionEvent.ACTION_UP, w / 2f, h / 2f, 0)
                            dispatchTouchEvent(down2)
                            dispatchTouchEvent(up2)
                            down2.recycle()
                            up2.recycle()
                        }
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    if (loadedStreamUrl != activeStreamUrl) {
                        loadedStreamUrl = activeStreamUrl
                        isVideoReady = false
                        isBuffering = true
                        var normalizedActiveUrl = activeStreamUrl
                        if (normalizedActiveUrl.contains("bysefujedu.com/d/")) {
                            normalizedActiveUrl = normalizedActiveUrl.replace("/d/", "/e/")
                        } else if (normalizedActiveUrl.contains("filemoon.") && normalizedActiveUrl.contains("/d/")) {
                            normalizedActiveUrl = normalizedActiveUrl.replace("/d/", "/e/")
                        } else if (normalizedActiveUrl.contains("primeload.co/embed/")) {
                            normalizedActiveUrl = normalizedActiveUrl.replace("/embed/", "/player/") + "?ref=https://doramasflix.in/"
                        }

                        val isDoramasflix = normalizedActiveUrl.contains("doramasflix", ignoreCase = true) ||
                            normalizedActiveUrl.contains("primeload", ignoreCase = true) ||
                            normalizedActiveUrl.contains("streamtape", ignoreCase = true) ||
                            normalizedActiveUrl.contains("embedshortener", ignoreCase = true) ||
                            normalizedActiveUrl.contains("ok.ru", ignoreCase = true) ||
                            normalizedActiveUrl.contains("odnoklassniki", ignoreCase = true) ||
                            normalizedActiveUrl.contains("vidhide", ignoreCase = true) ||
                            normalizedActiveUrl.contains("callistanise", ignoreCase = true) ||
                            normalizedActiveUrl.contains("do7go", ignoreCase = true) ||
                            normalizedActiveUrl.contains("playmogo", ignoreCase = true) ||
                            normalizedActiveUrl.contains("dood", ignoreCase = true) ||
                            normalizedActiveUrl.contains("flaswish", ignoreCase = true) ||
                            normalizedActiveUrl.contains("mp4upload", ignoreCase = true) ||
                            normalizedActiveUrl.contains("voe", ignoreCase = true) ||
                            normalizedActiveUrl.contains("byse", ignoreCase = true) ||
                            normalizedActiveUrl.contains("mixdrop", ignoreCase = true) ||
                            normalizedActiveUrl.contains("m1xdrop", ignoreCase = true) ||
                            normalizedActiveUrl.contains("filemoon", ignoreCase = true)

                        val refererBase = when {
                            isDoramasflix -> "https://doramasflix.in/"
                            normalizedActiveUrl.contains("tudorama") || normalizedActiveUrl.contains("minochinos") || normalizedActiveUrl.contains("bysesukior") || normalizedActiveUrl.contains("abyss") || normalizedActiveUrl.contains("banana") -> "https://tudorama.com/"
                            else -> "https://lamovie.org/"
                        }
                        val originBase = when {
                            isDoramasflix -> "https://doramasflix.in"
                            refererBase.contains("tudorama") -> "https://tudorama.com"
                            else -> "https://lamovie.org"
                        }

                        if (normalizedActiveUrl.contains("vimeos", ignoreCase = true) || normalizedActiveUrl.contains("lamovie", ignoreCase = true)) {
                            val iframeHtml = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #000; }
                                        iframe { width: 100%; height: 100%; border: none; }
                                    </style>
                                </head>
                                <body>
                                    <iframe src="$normalizedActiveUrl" allowfullscreen="true" allow="autoplay; fullscreen; encrypted-media"></iframe>
                                </body>
                                </html>
                            """.trimIndent()
                            webView.loadDataWithBaseURL(refererBase, iframeHtml, "text/html", "UTF-8", null)
                        } else {
                            val headers = mapOf(
                                "Referer" to refererBase,
                                "Origin" to originBase
                            )
                            webView.loadUrl(normalizedActiveUrl, headers)
                        }
                        scope.launch {
                            delay(800)
                            val w = webView.width.toFloat().takeIf { it > 100f } ?: 960f
                            val h = webView.height.toFloat().takeIf { it > 100f } ?: 540f
                            val now = android.os.SystemClock.uptimeMillis()
                            val down = android.view.MotionEvent.obtain(now, now, android.view.MotionEvent.ACTION_DOWN, w / 2f, h / 2f, 0)
                            val up = android.view.MotionEvent.obtain(now, now + 40, android.view.MotionEvent.ACTION_UP, w / 2f, h / 2f, 0)
                            webView.dispatchTouchEvent(down)
                            webView.dispatchTouchEvent(up)
                            down.recycle()
                            up.recycle()

                            delay(1200)
                            val now2 = android.os.SystemClock.uptimeMillis()
                            val down2 = android.view.MotionEvent.obtain(now2, now2, android.view.MotionEvent.ACTION_DOWN, w / 2f, h / 2f, 0)
                            val up2 = android.view.MotionEvent.obtain(now2, now2 + 40, android.view.MotionEvent.ACTION_UP, w / 2f, h / 2f, 0)
                            webView.dispatchTouchEvent(down2)
                            webView.dispatchTouchEvent(up2)
                            down2.recycle()
                            up2.recycle()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isVideoReady) 1f else 0f }
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        keepScreenOn = true
                        isFocusable = false
                        isFocusableInTouchMode = false
                        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    }
                },
                update = { view ->
                    if (view.player != exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isVideoReady) 1f else 0f }
            )
        }

        // Transparent tap shield when controls are hidden: wakes up TVPro controls instead of touching WebView
        if (!showControls && !showServerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { wakeUpControls() }
                        )
                    }
            )
        }

        // Full-Screen Initial Loading Splash until the video has actually loaded its first frame
        if (!isVideoReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07090E)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = NetflixRed,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(58.dp)
                )
            }
        }

        // Mid-playback Buffering Indicator (Clean floating spinner over the video without blocking the screen or controls)
        if (isVideoReady && isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Animated Seek Toast Bubble OSD
        seekToastText?.let { toastMsg ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xE610141D))
                    .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMsg,
                    color = Color.White,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }



        // Audio & Subtitles Selection Modal (Estilo Cinema de Alta Gama)
        TvPlayerAudioSubtitleModal(
            show = showServerDialog,
            onDismiss = { showServerDialog = false },
            isEmbedStream = isEmbedStream,
            currentStream = currentStream,
            availableStreams = availableStreams,
            currentStreamIndex = currentStreamIndex,
            exoPlayer = exoPlayer,
            exoAudioTracks = exoAudioTracks,
            exoSubtitleTracks = exoSubtitleTracks,
            nativeWebAudioTracks = nativeWebAudioTracks,
            nativeWebSubtitleTracks = nativeWebSubtitleTracks,
            selectedWebAudioTrackIdx = selectedWebAudioTrackIdx,
            selectedWebSubtitleTrackIdx = selectedWebSubtitleTrackIdx,
            subtitlesEnabled = subtitlesEnabled,
            selectedSubtitleOption = selectedSubtitleOption,
            activeAudioTrackFocusRequester = activeAudioTrackFocusRequester,
            onSwitchServer = { targetIdx, resume ->
                switchServer(targetIdx, resume)
            },
            onApplyWebAudioTrack = { idx ->
                userSelectedAudioTrack = true
                applyWebAudioTrack(idx)
            },
            onApplyWebSubtitle = { enabled, idx ->
                applyWebSubtitle(enabled, idx)
            },
            onSelectExoSubtitle = { subItem ->
                selectExoSubtitle(subItem)
            },
            onSelectSubtitleTrackData = { subData ->
                selectSubtitleTrackData(subData)
            },
            onApplySubtitlePreference = { enabled, opt ->
                applySubtitlePreference(enabled, opt)
            }
        )

        // Chapters / Episodes Selection Modal (Estilo Cinema de Alta Gama como en la imagen de referencia)
        TvPlayerEpisodesModal(
            show = showEpisodesDialog,
            onDismiss = { showEpisodesDialog = false },
            movie = movie,
            detailResponse = detailResponse,
            seasonEpisodes = seasonEpisodes,
            currentMovieTitle = currentMovieTitle,
            cleanMainTitle = cleanMainTitle,
            displayPositionMs = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = durationMs,
            activeSeasonInt = activeSeasonInt,
            activeEpisodeInt = activeEpisodeInt,
            selectedModalSeason = selectedModalSeason,
            onSelectSeason = { s -> selectedModalSeason = s },
            activeEpisodeFocusRequester = activeEpisodeFocusRequester,
            onEpisodeClick = { ep -> playEpisode(ep) },
            onSeekToMs = { ms -> seekToMs(ms) },
            onSetPlaybackPlaying = { playing -> setPlaybackPlaying(playing) },
            onWakeUpControls = { wakeUpControls() }
        )

        // TV Controls Overlay — HUD Cinematográfico de Alta Gama (Inspirado en el diseño de referencia del usuario)
        AnimatedVisibility(
            visible = showControls && !showServerDialog && !showEpisodesDialog && !showNextEpisodePrompt,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(280))
        ) {
            val warmIvory = Color(0xFFF5F2EB)
            val glassSurface = Color(0xD90A0D13)
            val pillSurface = Color(0xCC0B0F17)
            val subtleBorder = Color.White.copy(alpha = 0.14f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.80f),
                                0.22f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = false }
                        )
                    }
                    .padding(start = 44.dp, end = 44.dp, top = 24.dp, bottom = 8.dp)
            ) {
                // 1. TOP BAR: Botón "← VOLVER" + Título con Temporada y Episodio debajo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
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
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.key) {
                                        Key.DirectionDown -> {
                                            try {
                                                if (availableStreams.size > 1) {
                                                    activeServerFocusRequester.requestFocus()
                                                } else {
                                                    timelineFocusRequester.requestFocus()
                                                }
                                            } catch (_: Exception) {
                                                try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOLVER",
                            fontFamily = Inter,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cleanMainTitle,
                            color = warmIvory,
                            fontFamily = BebasNeue,
                            fontSize = 26.sp,
                            letterSpacing = 1.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSeriesContent && activeSequenceLabel.isNotBlank() && activeSequenceLabel != cleanMainTitle) {
                            Text(
                                text = activeSequenceLabel,
                                color = Color(0xFFD1D5DB),
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. BOTTOM FLOATING CINEMA HUD (Siempre disponible para control total)
                var isTimelineFocused by remember { mutableStateOf(false) }
                val displayPosMs = if (isEmbedStream) currentPositionMs else exoPlayer.currentPosition.coerceAtLeast(0L)
                val bufferedPosMs = if (isEmbedStream) currentPositionMs else maxOf(bufferedPositionMs, exoPlayer.bufferedPosition.coerceAtLeast(0L))
                val progressFraction = if (durationMs > 0L) {
                    (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val bufferedFraction = if (durationMs > 0L) {
                    (bufferedPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val remainingSec = if (durationMs > displayPosMs) (durationMs - displayPosMs) / 1000 else 0L

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 0. CHIPS DE SERVIDORES INTERACTIVOS (Inspirado en la Web de Doramasflix: [★ MEJOR Primeload] [Dood] [Ok.ru] ...)
                    if (availableStreams.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SERVIDORES",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                letterSpacing = 1.2.sp,
                                color = warmIvory.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                availableStreams.take(6).forEachIndexed { sIndex, streamItem ->
                                    val isCurrent = (sIndex == currentStreamIndex)
                                    var isChipFocused by remember { mutableStateOf(false) }
                                    val cleanName = remember(streamItem) {
                                        getCleanServerDisplayName(streamItem)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isChipFocused -> warmIvory
                                                    isCurrent -> Color(0xFF2B2015)
                                                    else -> Color(0x66181E2B)
                                                }
                                            )
                                            .border(
                                                width = if (isChipFocused) 2.dp else if (isCurrent) 1.5.dp else 1.dp,
                                                color = when {
                                                    isChipFocused -> warmIvory
                                                    isCurrent -> Color(0xFFFFB800)
                                                    else -> Color.White.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .then(
                                                if (isCurrent) Modifier.focusRequester(activeServerFocusRequester) else Modifier
                                            )
                                            .focusable()
                                            .onFocusChanged { isChipFocused = it.isFocused }
                                            .onKeyEvent { keyEvent ->
                                                if (keyEvent.type == KeyEventType.KeyDown) {
                                                    when (keyEvent.key) {
                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                            if (!isCurrent) {
                                                                switchServer(sIndex, resetPosition = false)
                                                            }
                                                            true
                                                        }
                                                        Key.DirectionDown -> {
                                                            try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        Key.DirectionUp -> {
                                                            try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            true
                                                        }
                                                        else -> false
                                                    }
                                                } else false
                                            }
                                            .clickable {
                                                if (!isCurrent) {
                                                    switchServer(sIndex, resetPosition = false)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isChipFocused) Color(0xFF0A0D13) else Color(0xFFFFB800))
                                                )
                                            }
                                            Text(
                                                text = cleanName,
                                                fontFamily = Inter,
                                                fontWeight = if (isCurrent || isChipFocused) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp,
                                                color = when {
                                                    isChipFocused -> Color(0xFF0A0D13)
                                                    isCurrent -> Color(0xFFFFD54F)
                                                    else -> warmIvory.copy(alpha = 0.85f)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // A. BARRA DE PROGRESO EN UNA SOLA LÍNEA CON EL TIEMPO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = formatTimeHms(displayPosMs / 1000),
                            color = if (isTimelineFocused) Color.White else warmIvory,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.1.sp
                        )

                        // Barra de progreso interactiva con marcas de cuarto y scrubber
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .focusRequester(timelineFocusRequester)
                                .onFocusChanged { isTimelineFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionLeft -> {
                                                performSeek(false, 10_000L)
                                                true
                                            }
                                            Key.DirectionRight -> {
                                                performSeek(true, 10_000L)
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                try {
                                                    if (availableStreams.size > 1) {
                                                        activeServerFocusRequester.requestFocus()
                                                    } else {
                                                        backButtonFocusRequester.requestFocus()
                                                    }
                                                } catch (_: Exception) {
                                                    try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                                togglePlayPause()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .pointerInput(durationMs) {
                                    detectTapGestures { offset ->
                                        if (durationMs > 0L && size.width > 0) {
                                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            val target = (frac * durationMs).toLong()
                                            seekToMs(target)
                                            seekToastText = formatTimeHms(target / 1000)
                                            wakeUpControls()
                                        }
                                    }
                                }
                                .pointerInput(durationMs) {
                                    detectHorizontalDragGestures { change, _ ->
                                        change.consume()
                                        if (durationMs > 0L && size.width > 0) {
                                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            val target = (frac * durationMs).toLong()
                                            seekToMs(target)
                                            seekToastText = formatTimeHms(target / 1000)
                                            wakeUpControls()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val trackHeight = if (isTimelineFocused) 4.dp else 2.5.dp
                            val haloSize = if (isTimelineFocused) 20.dp else 14.dp
                            val ringSize = if (isTimelineFocused) 13.dp else 9.dp
                            val thumbOffset = ((maxWidth - haloSize) * progressFraction).coerceAtLeast(0.dp)

                            // Riel base
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.20f))
                            )

                            // Riel de búfer (precarga descargada en memoria/caché)
                            if (bufferedFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = bufferedFraction)
                                        .height(trackHeight)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.45f))
                                )
                            }

                            // Marcas sutiles de capítulos/cuartos (25%, 50%, 75%)
                            listOf(0.25f, 0.50f, 0.75f).forEach { tickFrac ->
                                val tickX = maxWidth * tickFrac
                                Box(
                                    modifier = Modifier
                                        .offset(x = tickX)
                                        .width(1.dp)
                                        .height(trackHeight + 4.dp)
                                        .background(Color.White.copy(alpha = 0.30f))
                                )
                            }

                            // Riel de progreso completado en blanco marfil cálido o acento Mochi si enfocado
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .height(trackHeight)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isTimelineFocused) MochiStrawberryPink else warmIvory)
                            )

                            // Scrubber Thumb: Anillo hueco luminoso con núcleo oscuro y punto central
                            Box(
                                modifier = Modifier
                                    .offset(x = thumbOffset)
                                    .size(haloSize)
                                    .clip(CircleShape)
                                    .background((if (isTimelineFocused) MochiStrawberryPink else warmIvory).copy(alpha = if (isTimelineFocused) 0.35f else 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(ringSize)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0A0D13))
                                        .border(2.dp, if (isTimelineFocused) MochiStrawberryPink else warmIvory, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isTimelineFocused) MochiStrawberryPink else warmIvory)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (durationMs > 0L) "-${formatTimeHms(remainingSec)}" else "--:--:--",
                            color = if (isTimelineFocused) Color.White.copy(alpha = 0.9f) else Color(0xFF9CA3AF),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.1.sp
                        )
                    }

                    // B. DOCK INFERIOR DE CONTROLES (Izquierda: Audio y Subtítulos | Centro: Píldora Oval | Derecha: Desde el inicio / Episodios)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Botón Izquierdo: AUDIO Y SUBTÍTULOS
                        var isServerPillFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showServerDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServerPillFocused) warmIvory else pillSurface,
                                contentColor = if (isServerPillFocused) Color.Black else warmIvory
                            ),
                            border = if (isServerPillFocused) BorderStroke(2.dp, warmIvory)
                            else BorderStroke(1.dp, subtleBorder),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .height(36.dp)
                                .focusRequester(serverPillFocusRequester)
                                .onFocusChanged { isServerPillFocused = it.isFocused }
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.DirectionRight -> {
                                                try {
                                                    if (isSeriesContent) {
                                                        prevEpisodeDockFocusRequester.requestFocus()
                                                    } else {
                                                        rewindFocusRequester.requestFocus()
                                                    }
                                                } catch (_: Exception) {}
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            Key.DirectionDown -> {
                                                showControls = false
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Audio y Subtítulos",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUDIO Y SUBTÍTULOS",
                                fontFamily = Inter,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp
                            )
                        }

                        // Dock Central Ovalado: [⏮ Ep. Anterior] [-10s]  [ Play / Pausa ]  [+10s] [⏭ Ep. Siguiente]
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .height(50.dp)
                                .clip(RoundedCornerShape(50))
                                .background(glassSurface)
                                .border(1.dp, subtleBorder, RoundedCornerShape(50))
                                .padding(horizontal = if (isSeriesContent) 12.dp else 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(if (isSeriesContent) 8.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón: EPISODIO ANTERIOR (Solo en series)
                            if (isSeriesContent) {
                                var isPrevEpFocused by remember { mutableStateOf(false) }
                                val hasPrev = previousEpisode != null
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isPrevEpFocused) warmIvory else Color.Transparent)
                                        .focusable()
                                        .focusRequester(prevEpisodeDockFocusRequester)
                                        .onFocusChanged {
                                            isPrevEpFocused = it.isFocused
                                            if (it.isFocused) {
                                                wakeUpControls()
                                            }
                                        }
                                        .clickable {
                                            if (previousEpisode != null) {
                                                playEpisode(previousEpisode)
                                            } else {
                                                seekToMs(0L)
                                                seekToastText = "Ya estás en el primer episodio"
                                                wakeUpControls()
                                            }
                                        }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionLeft -> {
                                                        try { serverPillFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        try { rewindFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = if (hasPrev) "Episodio anterior: ${previousEpisode?.title ?: ""}" else "Primer episodio",
                                        tint = if (isPrevEpFocused) Color.Black else if (hasPrev) warmIvory else warmIvory.copy(alpha = 0.38f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            var isRewindFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isRewindFocused) warmIvory else Color.Transparent)
                                    .focusable()
                                    .focusRequester(rewindFocusRequester)
                                    .onFocusChanged { isRewindFocused = it.isFocused }
                                    .clickable { performSeek(false) }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    try {
                                                        if (isSeriesContent) {
                                                            prevEpisodeDockFocusRequester.requestFocus()
                                                        } else {
                                                            serverPillFocusRequester.requestFocus()
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    showControls = false
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Retroceder 10 segundos",
                                    tint = if (isRewindFocused) Color.Black else warmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            var isPlayFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlayFocused) warmIvory else Color.Transparent)
                                    .focusable()
                                    .focusRequester(playButtonFocusRequester)
                                    .onFocusChanged { isPlayFocused = it.isFocused }
                                    .clickable {
                                        val willPlay = !isPlaying
                                        togglePlayPause()
                                        if (willPlay) {
                                            controlsHideFastTick++
                                        } else {
                                            wakeUpControls()
                                        }
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    try { rewindFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    try { ffFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    showControls = false
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir o Pausar",
                                    tint = if (isPlayFocused) Color.Black else warmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            var isFfFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isFfFocused) warmIvory else Color.Transparent)
                                    .focusable()
                                    .focusRequester(ffFocusRequester)
                                    .onFocusChanged { isFfFocused = it.isFocused }
                                    .clickable { performSeek(true) }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    try {
                                                        if (isSeriesContent) {
                                                            nextEpisodeDockFocusRequester.requestFocus()
                                                        } else {
                                                            restartButtonFocusRequester.requestFocus()
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionUp -> {
                                                    try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    showControls = false
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Adelantar 10 segundos",
                                    tint = if (isFfFocused) Color.Black else warmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Botón: SIGUIENTE EPISODIO (Solo en series)
                            if (isSeriesContent) {
                                var isNextEpFocused by remember { mutableStateOf(false) }
                                val hasNext = nextEpisode != null
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isNextEpFocused) warmIvory else Color.Transparent)
                                        .focusable()
                                        .focusRequester(nextEpisodeDockFocusRequester)
                                        .onFocusChanged {
                                            isNextEpFocused = it.isFocused
                                            if (it.isFocused) {
                                                wakeUpControls()
                                            }
                                        }
                                        .clickable {
                                            if (nextEpisode != null) {
                                                playEpisode(nextEpisode)
                                            } else {
                                                seekToastText = "No hay más episodios disponibles"
                                                wakeUpControls()
                                            }
                                        }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionLeft -> {
                                                        try { ffFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionRight -> {
                                                        try { restartButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = if (hasNext) "Siguiente episodio: ${nextEpisode?.title ?: ""}" else "Último episodio",
                                        tint = if (isNextEpFocused) Color.Black else if (hasNext) warmIvory else warmIvory.copy(alpha = 0.38f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Botones Derechos: [DESDE EL INICIO (Solo icono)] y opcionalmente [CAPÍTULOS]
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón: DESDE EL INICIO (Solo icono, circular)
                            var isRestartFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isRestartFocused) warmIvory else pillSurface)
                                    .border(
                                        width = if (isRestartFocused) 2.dp else 1.dp,
                                        color = if (isRestartFocused) warmIvory else subtleBorder,
                                        shape = CircleShape
                                    )
                                    .focusable()
                                    .focusRequester(restartButtonFocusRequester)
                                    .onFocusChanged { isRestartFocused = it.isFocused }
                                    .clickable {
                                        seekToMs(0L)
                                        WatchHistoryManager.resetMoviePosition(context, movie.url)
                                        seekToastText = "Reproduciendo desde el inicio"
                                        wakeUpControls()
                                    }
                                    .onKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown) {
                                            when (keyEvent.key) {
                                                Key.DirectionLeft -> {
                                                    try {
                                                        if (isSeriesContent) {
                                                            nextEpisodeDockFocusRequester.requestFocus()
                                                        } else {
                                                            ffFocusRequester.requestFocus()
                                                        }
                                                    } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    if (hasEpisodesButton) {
                                                        try { episodesButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    } else false
                                                }
                                                Key.DirectionUp -> {
                                                    try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                }
                                                Key.DirectionDown -> {
                                                    showControls = false
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Desde el inicio",
                                    tint = if (isRestartFocused) Color.Black else warmIvory,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // Botón: CAPÍTULOS (Solo visible en series con más de 1 episodio)
                            if (hasEpisodesButton) {
                                var isEpisodesFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = {
                                        selectedModalSeason = activeSeasonInt
                                        showEpisodesDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEpisodesFocused) warmIvory else pillSurface,
                                        contentColor = if (isEpisodesFocused) Color.Black else warmIvory
                                    ),
                                    border = if (isEpisodesFocused) BorderStroke(2.dp, warmIvory)
                                    else BorderStroke(1.dp, subtleBorder),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .focusRequester(episodesButtonFocusRequester)
                                        .onFocusChanged { isEpisodesFocused = it.isFocused }
                                        .onKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.key) {
                                                    Key.DirectionLeft -> {
                                                        try { restartButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        try { timelineFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    }
                                                    Key.DirectionDown -> {
                                                        showControls = false
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatListBulleted,
                                        contentDescription = if (isSeriesContent) "Episodios" else "Escenas",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isSeriesContent) "EPISODIOS" else "ESCENAS",
                                        fontFamily = Inter,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.4.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FLOATING NEXT EPISODE COUNTDOWN OVERLAY (Estilo Netflix / Disney+) ---
        if (isSeriesContent && nextEpisode != null) {
            val warmIvory = Color(0xFFF5F2EB)
            AnimatedVisibility(
                visible = showNextEpisodePrompt,
                enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(350)) { it / 2 },
                exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(250)) { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 40.dp, bottom = 36.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF20B0E14)
                    ),
                    border = BorderStroke(1.5.dp, Color(0xFFE5DFD3).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .width(440.dp)
                        .shadow(28.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color.Black, spotColor = Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header superior: SIGUIENTE EPISODIO  |  EN 5s o ¿SIGUES AHÍ?
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = warmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SIGUIENTE EPISODIO",
                                    color = warmIvory,
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.6.sp
                                )
                            }

                            if (consecutiveAutoPlayCount < 3) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFE5DFD3).copy(alpha = 0.18f))
                                        .border(1.dp, Color(0xFFE5DFD3).copy(alpha = 0.35f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "EN ${nextEpisodeCountdownSeconds}s",
                                        color = warmIvory,
                                        fontSize = 11.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFE5DFD3).copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "¿SIGUES AHÍ?",
                                        color = warmIvory,
                                        fontSize = 11.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Thumbnail + Info del capítulo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(116.dp)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF151922))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(com.tvpro.core.data.repository.MovieRepository.sanitizeThumbnailUrl(nextEpisode.image).ifBlank { detailResponse?.backdrop ?: movie.backdrop ?: movie.image })
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = nextEpisode.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TEMPORADA ${nextEpisode.seasonNumber} : EPISODIO ${nextEpisode.episodeNumber}",
                                    color = warmIvory.copy(alpha = 0.70f),
                                    fontSize = 10.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                val cleanNextEpTitle = remember(nextEpisode.title, cleanMainTitle, movie.title, nextEpisode.episodeNumber) {
                                    var s = nextEpisode.title.cleanHtml().trim()
                                    // Limpiar prefijo con el nombre de la serie (incluyendo variantes sin año)
                                    val nameCandidates = listOf(
                                        cleanMainTitle,
                                        cleanMainTitle.replace(Regex("""\s*\(\d{4}\).*$"""), "").trim(),
                                        movie.title,
                                        movie.title.replace(Regex("""\s*\(\d{4}\).*$"""), "").trim()
                                    ).filter { it.isNotBlank() }.distinct()

                                    nameCandidates.forEach { name ->
                                        s = s.replace(Regex("^\\Q$name\\E\\s*[:\\-·/]?\\s*", RegexOption.IGNORE_CASE), "")
                                        // También por si el nombre de la serie aparece en cualquier parte
                                        s = s.replace(Regex("\\Q$name\\E", RegexOption.IGNORE_CASE), "")
                                    }
                                    // Limpiar prefijos o residuos de Temporada/Capítulo (ej. "Temporada 1 Episodio 5", "T1:E5", "Temporada 1 : Episodio 2")
                                    s = s.replace(Regex("""(?i)\b(?:Temporada|Temp|T)\s*\d+\b\s*[:\-·/]?\s*"""), "")
                                    s = s.replace(Regex("""(?i)\b(?:Episodio|Capítulo|Cap|E)\s*\d+\b\s*[:\-·/]?\s*"""), "")
                                    s = s.trim().trim(':', '-', '·', ' ', '/', '.')
                                    if (s.isBlank() ||
                                        s.equals("Capítulo ${nextEpisode.episodeNumber}", ignoreCase = true) ||
                                        s.equals("Episodio ${nextEpisode.episodeNumber}", ignoreCase = true) ||
                                        s.equals("Episodio", ignoreCase = true) ||
                                        s.equals("Capítulo", ignoreCase = true) ||
                                        nameCandidates.any { candidate -> s.contains(candidate, ignoreCase = true) }
                                    ) {
                                        ""
                                    } else {
                                        s
                                    }
                                }

                                if (cleanNextEpTitle.isNotBlank()) {
                                    Text(
                                        text = cleanNextEpTitle,
                                        color = Color.White,
                                        fontSize = 13.5.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (nextEpisode.overview.isNotBlank()) {
                                    Text(
                                        text = nextEpisode.overview.cleanHtml(),
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 10.5.sp,
                                        fontFamily = Inter,
                                        maxLines = if (cleanNextEpTitle.isNotBlank()) 2 else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Botones de acción (Control Remoto)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isPlayActive = nextEpisodeSelectedButton == 0
                            val isCancelActive = nextEpisodeSelectedButton == 1

                            Button(
                                onClick = {
                                    nextEpisodeSelectedButton = 0
                                    consecutiveAutoPlayCount = 0
                                    showNextEpisodePrompt = false
                                    playEpisode(nextEpisode)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayActive) warmIvory else Color(0xFF1E2430),
                                    contentColor = if (isPlayActive) Color(0xFF0B0E14) else warmIvory
                                ),
                                border = if (isPlayActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(40.dp)
                                    .focusRequester(nextEpisodeFocusRequester)
                                    .onFocusChanged { if (it.isFocused) nextEpisodeSelectedButton = 0 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "REPRODUCIR AHORA",
                                    fontSize = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    nextEpisodeSelectedButton = 1
                                    showNextEpisodePrompt = false
                                    isNextEpisodeDismissed = true
                                    consecutiveAutoPlayCount = 0
                                    wakeUpControls()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCancelActive) warmIvory else Color.Transparent,
                                    contentColor = if (isCancelActive) Color(0xFF0B0E14) else Color(0xFF9CA3AF)
                                ),
                                border = if (isCancelActive) BorderStroke(2.dp, warmIvory)
                                else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(40.dp)
                                    .focusRequester(nextEpisodeCancelFocusRequester)
                                    .onFocusChanged { if (it.isFocused) nextEpisodeSelectedButton = 1 }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CANCELAR",
                                    fontSize = 10.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

internal fun formatTimeHms(seconds: Long): String {
    return PlayerTimeUtils.formatTimeHms(seconds)
}

internal fun formatTimeSec(seconds: Long): String {
    return PlayerTimeUtils.formatTimeSec(seconds)
}


