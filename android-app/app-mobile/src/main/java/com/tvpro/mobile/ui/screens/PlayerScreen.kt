@file:OptIn(
    androidx.media3.common.util.UnstableApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.tvpro.mobile.ui.screens

import com.tvpro.core.domain.PlayerTimeUtils
import com.tvpro.mobile.ui.components.player.AudioWaveIndicator
import com.tvpro.mobile.ui.components.player.DoubleTapSeekOverlay
import com.tvpro.mobile.ui.components.player.InteractiveVerticalBar

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.URI
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.cleanHtml
import com.tvpro.mobile.ui.components.cleanTitle
import com.tvpro.mobile.ui.components.cleanEpisodeTitle
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun Context.findPlayerActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MobilePlayerScreen(
    movie: MovieItem,
    episode: EpisodeItem? = null,
    directStreamUrl: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findPlayerActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val scope = rememberCoroutineScope()

    val initialCached = remember(movie.url) { MovieRepository.getCachedDetail(movie.url, movie.title) }
    var seriesDetailCache by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(initialCached) }
    var currentEpisode by remember { mutableStateOf(episode) }

    // Ensure series details are fetched for series content
    LaunchedEffect(movie.url) {
        if (seriesDetailCache == null) {
            try {
                val detail = withContext(Dispatchers.IO) {
                    MovieRepository.extractMovie(movie)
                }
                seriesDetailCache = detail
            } catch (_: Exception) {}
        }
    }

    val isSeriesContent = remember(movie, seriesDetailCache, currentEpisode) {
        currentEpisode != null ||
        movie.type == "series" ||
        movie.type == "anime" ||
        seriesDetailCache?.isSeries == true ||
        (seriesDetailCache?.episodes?.isNotEmpty() == true)
    }

    val nextEpisode = remember(currentEpisode, seriesDetailCache) {
        val ep = currentEpisode ?: return@remember null
        val eps = seriesDetailCache?.episodes ?: return@remember null
        val idx = eps.indexOfFirst { it.id == ep.id || (it.seasonNumber == ep.seasonNumber && it.episodeNumber == ep.episodeNumber) }
        if (idx >= 0 && idx + 1 < eps.size) eps[idx + 1] else null
    }

    val prevEpisode = remember(currentEpisode, seriesDetailCache) {
        val ep = currentEpisode ?: return@remember null
        val eps = seriesDetailCache?.episodes ?: return@remember null
        val idx = eps.indexOfFirst { it.id == ep.id || (it.seasonNumber == ep.seasonNumber && it.episodeNumber == ep.episodeNumber) }
        if (idx > 0) eps[idx - 1] else null
    }

    var showNextEpisodePrompt by remember { mutableStateOf(false) }
    var isNextEpisodeDismissed by remember(currentEpisode) { mutableStateOf(false) }
    var nextEpisodeCountdownSeconds by remember(currentEpisode) { mutableIntStateOf(10) }
    var showEpisodesModal by remember { mutableStateOf(false) }
    var resolvedStreamUrl by remember { mutableStateOf<String?>(directStreamUrl) }
    var streamList by remember { mutableStateOf<List<StreamItem>>(emptyList()) }
    var selectedStreamIndex by remember { mutableIntStateOf(0) }

    val currentStreamItem = remember(streamList, selectedStreamIndex) {
        streamList.getOrNull(selectedStreamIndex)
    }

    val isEmbedStream = remember(resolvedStreamUrl, currentStreamItem) {
        val url = resolvedStreamUrl ?: ""
        if (url.isBlank()) {
            false
        } else if (currentStreamItem?.type == "HLS" || currentStreamItem?.type == "MP4" || url.contains(".m3u8") || url.contains(".mp4")) {
            false
        } else {
            currentStreamItem?.type == "EMBED" ||
            url.contains("/embed/") ||
            url.contains("/e/") ||
            url.contains("primeload") ||
            url.contains("filemoon") ||
            url.contains("byse") ||
            url.contains("vidhide") ||
            url.contains("callistanise") ||
            url.contains("dood") ||
            url.contains("do7go") ||
            url.contains("playmogo") ||
            url.contains("streamtape") ||
            url.contains("uqload") ||
            url.contains("mixdrop") ||
            url.contains("m1xdrop") ||
            url.contains("voe") ||
            url.contains("vidsrc") ||
            url.contains("multiembed")
        }
    }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadedEmbedUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingStream by remember { mutableStateOf(directStreamUrl == null) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resolveRetryCount by remember { mutableIntStateOf(0) }
    var resolveRetryTrigger by remember { mutableIntStateOf(0) }
    var resumeTargetPositionMs by remember { mutableLongStateOf(0L) }

    // Screen Brightness & Volume state (Interactive & Persistent)
    val initialBrightness = remember {
        try {
            val lp = activity?.window?.attributes
            if (lp != null && lp.screenBrightness in 0.01f..1f) {
                lp.screenBrightness
            } else {
                val sysB = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                (sysB / 255f).coerceIn(0.01f, 1f)
            }
        } catch (e: Exception) {
            0.5f
        }
    }
    var currentBrightness by remember { mutableFloatStateOf(initialBrightness) }

    val initialVolume = remember {
        if (audioManager != null) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
        } else 0.5f
    }
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }

    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var isAdjustingVolume by remember { mutableStateOf(false) }
    var activeDragType by remember { mutableStateOf<String?>(null) } // "BRIGHTNESS", "VOLUME" or null
    var accumulatedVolumeDelta by remember { mutableFloatStateOf(0f) }
    var controlsInteractionCounter by remember { mutableIntStateOf(0) }

    fun updateBrightness(frac: Float) {
        val clamped = frac.coerceIn(0.01f, 1f)
        currentBrightness = clamped
        val lp = activity?.window?.attributes
        if (lp != null) {
            lp.screenBrightness = clamped
            activity.window.attributes = lp
        }
    }

    fun updateVolume(frac: Float) {
        val clamped = frac.coerceIn(0f, 1f)
        currentVolume = clamped
        if (audioManager != null) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (maxVol > 0) {
                val targetVolInt = kotlin.math.round(clamped * maxVol).toInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolInt, 0)
                accumulatedVolumeDelta = clamped * maxVol
            }
        }
    }

    var doubleTapSide by remember { mutableStateOf<String?>(null) } // "LEFT" or "RIGHT"
    var doubleTapSeconds by remember { mutableIntStateOf(0) }
    var doubleTapTrigger by remember { mutableIntStateOf(0) }
    var displaySide by remember { mutableStateOf("RIGHT") }
    var displaySeconds by remember { mutableIntStateOf(10) }
    val currentDoubleTapSide by rememberUpdatedState(doubleTapSide)
    val currentDoubleTapSeconds by rememberUpdatedState(doubleTapSeconds)

    // Resize Mode (FIT, ZOOM, FILL)
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Dialog state for server switcher
    var showServerModal by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showSpeedModal by remember { mutableStateOf(false) }
    var showTrackModal by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentTracks by remember { mutableStateOf<Tracks?>(null) }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    // Auto-lock landscape orientation & Fullscreen Immersive Mode (Hide status bar & navigation bar)
    DisposableEffect(activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Lock to landscape sensor
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Modern display cutout (render behind camera notch / punch hole)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Hide Status Bar (seÃ±al, baterÃ­a, hora) and Navigation Bar (botones de navegaciÃ³n)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity.requestedOrientation = originalOrientation
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Resolve streaming URL
    LaunchedEffect(movie.url, currentEpisode, resolveRetryTrigger) {
        if (directStreamUrl != null && currentEpisode == null && resolveRetryTrigger == 0) {
            resolvedStreamUrl = directStreamUrl
            isLoadingStream = false
            return@LaunchedEffect
        }
        isLoadingStream = true
        errorMessage = null
        try {
            val force = resolveRetryTrigger > 0
            val ep = currentEpisode
            if (ep != null) {
                val seriesPostId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
                val streams = MovieRepository.resolveEpisodeStreams(
                    episodeId = ep.id,
                    seriesTitle = movie.title,
                    fallbackStreams = emptyList(),
                    seasonNumber = ep.seasonNumber,
                    episodeNumber = ep.episodeNumber,
                    seriesPostId = seriesPostId,
                    serieUrl = movie.url,
                    episodeSlug = ep.slug,
                    forceRefresh = force
                )
                streamList = streams
                val working = streams.firstOrNull { it.url.isNotBlank() }
                if (working != null) {
                    selectedStreamIndex = streams.indexOf(working)
                    resolvedStreamUrl = working.url
                } else {
                    errorMessage = "No se encontraron enlaces de reproducciÃ³n disponibles para este episodio."
                }
            } else {
                val detail = seriesDetailCache ?: MovieRepository.extractMovie(movie, forceRefresh = force)
                seriesDetailCache = detail
                if (detail.isSeries || detail.episodes.isNotEmpty() || movie.type == "series" || movie.type == "anime") {
                    val savedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
                    val targetEpisode = if (savedHistory != null && savedHistory.seasonNumber != null && savedHistory.episodeNumber != null) {
                        detail.episodes.find {
                            it.seasonNumber == savedHistory.seasonNumber && it.episodeNumber == savedHistory.episodeNumber
                        } ?: detail.episodes.firstOrNull()
                    } else {
                        detail.episodes.firstOrNull()
                    }

                    if (targetEpisode != null) {
                        // Switch currentEpisode: this naturally re-triggers LaunchedEffect with the resolved episode
                        currentEpisode = targetEpisode
                        return@LaunchedEffect
                    } else {
                        errorMessage = "No se encontraron episodios disponibles para esta serie."
                    }
                } else {
                    streamList = detail.streams
                    val working = detail.streams.firstOrNull { it.url.isNotBlank() }
                    if (working != null) {
                        selectedStreamIndex = detail.streams.indexOf(working)
                        resolvedStreamUrl = working.url
                    } else {
                        errorMessage = "Contenido no disponible en este momento."
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error de reproducciÃ³n. Toca para reintentar."
        } finally {
            isLoadingStream = false
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(areControlsVisible, isPlaying, isAdjustingBrightness, isAdjustingVolume, controlsInteractionCounter) {
        if (areControlsVisible && isPlaying && !isAdjustingBrightness && !isAdjustingVolume) {
            delay(4000)
            areControlsVisible = false
        }
    }

    // Double tap feedback auto-hide
    LaunchedEffect(doubleTapTrigger) {
        if (doubleTapTrigger > 0 && doubleTapSide != null) {
            delay(850)
            doubleTapSide = null
            doubleTapSeconds = 0
        }
    }

    // Sync volume and brightness when controls open
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                currentVolume = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
            }
            val lp = activity?.window?.attributes
            if (lp != null && lp.screenBrightness in 0.01f..1f) {
                currentBrightness = lp.screenBrightness
            }
        }
    }

    // Setup ExoPlayer
    val exoPlayer = remember(context) {
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.streamingOkHttpClient)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(okHttpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        currentTracks = tracks
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) {
                            isBuffering = false
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = (state == Player.STATE_BUFFERING)
                        if (state == Player.STATE_READY) {
                            totalDurationMs = duration.coerceAtLeast(0L)
                            if (resolveRetryCount > 0) {
                                resolveRetryCount = 0
                            }
                        } else if (state == Player.STATE_ENDED) {
                            isBuffering = false
                            if (nextEpisode != null && !isNextEpisodeDismissed) {
                                showNextEpisodePrompt = true
                            }
                        }
                    }

                    override fun onPlayWhenReadyChanged(pwr: Boolean, reason: Int) {
                        if (!pwr) {
                            isBuffering = false
                        } else if (playbackState == Player.STATE_BUFFERING) {
                            isBuffering = true
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        isBuffering = false
                        val currentPos = currentPosition.coerceAtLeast(currentPositionMs)
                        if (currentPos > 0L) {
                            resumeTargetPositionMs = currentPos
                        }
                        if (resolveRetryCount < 3) {
                            resolveRetryCount++
                            scope.launch {
                                delay(1200)
                                resolveRetryTrigger++
                            }
                        } else {
                            errorMessage = "Error de reproducciÃ³n. Toca para reintentar."
                        }
                    }
                })
            }
    }

    fun playEpisode(targetEp: EpisodeItem) {
        currentEpisode = targetEp
        resolvedStreamUrl = null
        loadedEmbedUrl = null
        isLoadingStream = true
        errorMessage = null
        resolveRetryCount = 0
        resumeTargetPositionMs = 0L
        currentPositionMs = 0L
        totalDurationMs = 0L
        isBuffering = true
        showNextEpisodePrompt = false
        isNextEpisodeDismissed = false
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun seekRelative(deltaSeconds: Long) {
        if (isEmbedStream) {
            val js = """
            (function() {
                var v = document.querySelector('video');
                if (v) v.currentTime += $deltaSeconds;
                if (window.jwplayer) { try { var p = window.jwplayer(); p.seek(p.getPosition() + $deltaSeconds); } catch(e){} }
            })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
        } else {
            val target = (exoPlayer.currentPosition + deltaSeconds * 1000L).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
            exoPlayer.seekTo(target)
            currentPositionMs = target
            isBuffering = true
        }
    }

    fun togglePlayPauseAction() {
        if (isEmbedStream) {
            val targetPlay = !isPlaying
            isPlaying = targetPlay
            val js = if (targetPlay) {
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) v.play();
                    if (window.jwplayer) { try { window.jwplayer().play(true); } catch(e){} }
                })();
                """.trimIndent()
            } else {
                """
                (function() {
                    var v = document.querySelector('video');
                    if (v) v.pause();
                    if (window.jwplayer) { try { window.jwplayer().pause(); } catch(e){} }
                })();
                """.trimIndent()
            }
            webViewRef?.evaluateJavascript(js, null)
        } else {
            if (isBuffering) {
                exoPlayer.pause()
            } else if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekToTargetMs(targetMs: Long) {
        if (isEmbedStream) {
            val sec = targetMs / 1000.0
            val js = """
            (function() {
                var v = document.querySelector('video');
                if (v) v.currentTime = $sec;
                if (window.jwplayer) { try { window.jwplayer().seek($sec); } catch(e){} }
            })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(js, null)
            currentPositionMs = targetMs
        } else {
            exoPlayer.seekTo(targetMs)
            currentPositionMs = targetMs
            isBuffering = true
        }
    }

    // Prefetch streams for next episode in background when approaching end of current episode
    LaunchedEffect(currentPositionMs, totalDurationMs, nextEpisode) {
        val ep = nextEpisode ?: return@LaunchedEffect
        if (totalDurationMs > 60_000L && currentPositionMs >= (totalDurationMs - 60_000L)) {
            try {
                val seriesPostId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
                withContext(Dispatchers.IO) {
                    MovieRepository.resolveEpisodeStreams(
                        episodeId = ep.id,
                        seriesTitle = movie.title,
                        fallbackStreams = emptyList(),
                        seasonNumber = ep.seasonNumber,
                        episodeNumber = ep.episodeNumber,
                        seriesPostId = seriesPostId,
                        serieUrl = movie.url,
                        episodeSlug = ep.slug
                    )
                }
            } catch (_: Exception) {}
        }
    }

    // Trigger countdown overlay near end of video
    LaunchedEffect(currentPositionMs, totalDurationMs, isNextEpisodeDismissed, nextEpisode) {
        if (nextEpisode != null && !isNextEpisodeDismissed && !showNextEpisodePrompt) {
            val isNearEnd = totalDurationMs > 40_000L && currentPositionMs >= (totalDurationMs - 30_000L)
            if (isNearEnd) {
                showNextEpisodePrompt = true
            }
        }
    }

    // Auto-countdown ticker for next episode
    LaunchedEffect(showNextEpisodePrompt, nextEpisode) {
        if (showNextEpisodePrompt && nextEpisode != null) {
            nextEpisodeCountdownSeconds = 10
            while (nextEpisodeCountdownSeconds > 0 && showNextEpisodePrompt && !isNextEpisodeDismissed) {
                delay(1000L)
                if (showNextEpisodePrompt && !isNextEpisodeDismissed) {
                    nextEpisodeCountdownSeconds--
                }
            }
            if (showNextEpisodePrompt && !isNextEpisodeDismissed) {
                showNextEpisodePrompt = false
                playEpisode(nextEpisode)
            }
        }
    }

    // Track playback position & buffering state sync
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            if (exoPlayer.playbackState == Player.STATE_BUFFERING && exoPlayer.playWhenReady) {
                if (!isBuffering) isBuffering = true
            } else if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.isPlaying) {
                if (isBuffering) isBuffering = false
            }
            delay(400)
        }
    }

    // Load stream into ExoPlayer or Web Player
    LaunchedEffect(resolvedStreamUrl, isEmbedStream) {
        if (isEmbedStream) {
            exoPlayer.pause()
            exoPlayer.clearMediaItems()
            return@LaunchedEffect
        }
        val url = resolvedStreamUrl ?: return@LaunchedEffect
        isBuffering = true
        val uri = Uri.parse(url)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (url.contains(".m3u8") || url.contains("hls")) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                } else {
                    setMimeType(MimeTypes.VIDEO_MP4)
                }
            }
            .build()

        val savedHistory = WatchHistoryManager.getMovieHistory(context, movie.url)
        val historyPos = if (savedHistory != null && savedHistory.playbackPositionMs > 10_000L && savedHistory.playbackPositionMs < (savedHistory.durationMs - 60_000L)) {
            savedHistory.playbackPositionMs
        } else 0L

        val startPos = if (resumeTargetPositionMs > 0L) {
            val p = resumeTargetPositionMs
            resumeTargetPositionMs = 0L
            p
        } else {
            historyPos
        }

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPos > 0L) {
            exoPlayer.seekTo(startPos)
        }
        exoPlayer.play()
    }

    // Embed Video position and state sync ticker with dead-host detector & auto-failover
    LaunchedEffect(isEmbedStream, webViewRef, selectedStreamIndex) {
        if (!isEmbedStream) return@LaunchedEffect
        var lastMobilePos = -1.0
        var mobileStallTicks = 0
        var mobileNoDurTicks = 0
        while (isActive) {
            val syncJs = """
                (function() {
                    var rc = document.getElementById('realcontent');
                    var bodyText = ((document.body ? (document.body.textContent || document.body.innerText || '') : '') + ' ' + (rc ? (rc.textContent || '') : '')).toLowerCase();
                    var errorPhrases = [
                        '232001',
                        'not found',
                        'video not found',
                        'video you are looking for is not found',
                        'deleted by the creator',
                        'file not found',
                        'file was deleted',
                        'video deleted',
                        'video has been removed',
                        'video unavailable',
                        'this video is unavailable',
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
                        'forbidden'
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
                    if (isDead) {
                        return JSON.stringify({ dead: true, reason: deadReason });
                    }
                    var v = document.querySelector('video');
                    if (v && v.duration && !isNaN(v.duration)) {
                        return JSON.stringify({ cur: v.currentTime, dur: v.duration, paused: v.paused });
                    }
                    if (window.jwplayer) {
                        try {
                            var p = window.jwplayer();
                            if (p && p.getDuration && p.getDuration() > 0) {
                                return JSON.stringify({ cur: p.getPosition(), dur: p.getDuration(), paused: p.getState() === 'paused' });
                            }
                        } catch(e) {}
                    }
                    return null;
                })();
            """.trimIndent()
            webViewRef?.evaluateJavascript(syncJs) { res ->
                try {
                    val cleaned = res?.trim('"', '\\', ' ')
                    if (!cleaned.isNullOrBlank() && cleaned != "null" && cleaned.startsWith("{")) {
                        val unescaped = cleaned.replace("\\\"", "\"")
                        val json = org.json.JSONObject(unescaped)
                        if (json.optBoolean("dead", false)) {
                            if (streamList.size > 1 && selectedStreamIndex < streamList.size - 1) {
                                selectedStreamIndex++
                                resolvedStreamUrl = streamList[selectedStreamIndex].url
                            } else if (resolveRetryCount < 3) {
                                resolveRetryCount++
                                scope.launch {
                                    try {
                                        android.webkit.CookieManager.getInstance().removeSessionCookies(null)
                                    } catch (_: Exception) {}
                                    delay(1200)
                                    resolveRetryTrigger++
                                }
                            }
                            return@evaluateJavascript
                        }
                        val curSec = json.optDouble("cur", 0.0)
                        val durSec = json.optDouble("dur", 0.0)
                        val isP = !json.optBoolean("paused", false)
                        if (durSec > 0) {
                            mobileNoDurTicks = 0
                            totalDurationMs = (durSec * 1000).toLong()
                            currentPositionMs = (curSec * 1000).toLong()
                            isPlaying = isP
                            isBuffering = false
                            if (curSec != lastMobilePos) {
                                lastMobilePos = curSec
                                mobileStallTicks = 0
                            } else if (isPlaying) {
                                mobileStallTicks++
                                if (mobileStallTicks >= 10) {
                                    mobileStallTicks = 0
                                    if (streamList.size > 1 && selectedStreamIndex < streamList.size - 1) {
                                        selectedStreamIndex++
                                        resolvedStreamUrl = streamList[selectedStreamIndex].url
                                    } else if (resolveRetryCount < 3) {
                                        resolveRetryCount++
                                        scope.launch {
                                            try {
                                                android.webkit.CookieManager.getInstance().removeSessionCookies(null)
                                            } catch (_: Exception) {}
                                            delay(1200)
                                            resolveRetryTrigger++
                                        }
                                    }
                                }
                            }
                        } else {
                            mobileNoDurTicks++
                            if (mobileNoDurTicks >= 10) {
                                mobileNoDurTicks = 0
                                if (streamList.size > 1 && selectedStreamIndex < streamList.size - 1) {
                                    selectedStreamIndex++
                                    resolvedStreamUrl = streamList[selectedStreamIndex].url
                                } else if (resolveRetryCount < 3) {
                                    resolveRetryCount++
                                    scope.launch {
                                        try {
                                            android.webkit.CookieManager.getInstance().removeSessionCookies(null)
                                        } catch (_: Exception) {}
                                        delay(1200)
                                        resolveRetryTrigger++
                                    }
                                }
                            }
                        }
                    } else {
                        mobileNoDurTicks++
                        if (mobileNoDurTicks >= 10) {
                            mobileNoDurTicks = 0
                            if (streamList.size > 1 && selectedStreamIndex < streamList.size - 1) {
                                selectedStreamIndex++
                                resolvedStreamUrl = streamList[selectedStreamIndex].url
                            } else if (resolveRetryCount < 3) {
                                resolveRetryCount++
                                scope.launch {
                                    try {
                                        android.webkit.CookieManager.getInstance().removeSessionCookies(null)
                                    } catch (_: Exception) {}
                                    delay(1200)
                                    resolveRetryTrigger++
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(800L)
        }
    }

    // Save progress on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            val currentPos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (currentPos > 0L) {
                WatchHistoryManager.saveToHistory(
                    context = context,
                    movie = movie,
                    positionMs = currentPos,
                    durationMs = dur,
                    seasonNumber = currentEpisode?.seasonNumber,
                    episodeNumber = currentEpisode?.episodeNumber,
                    isSeries = currentEpisode != null,
                    postId = movie.postId ?: seriesDetailCache?.postId?.toIntOrNull()
                )
            }
            exoPlayer.release()
        }
    }

    BackHandler {
        onBack()
    }

    fun formatDuration(ms: Long): String {
        return PlayerTimeUtils.formatTimeMs(ms)
    }

    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProPureBlack)
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onTap = { offset ->
                        if (isScreenLocked) {
                            areControlsVisible = !areControlsVisible
                            return@detectTapGestures
                        }
                        val activeSide = currentDoubleTapSide
                        if (activeSide != null) {
                            val screenWidth = size.width
                            val isRight = offset.x >= screenWidth / 2
                            val tapSide = if (isRight) "RIGHT" else "LEFT"
                            if (tapSide == activeSide) {
                                val newSeconds = currentDoubleTapSeconds + 10
                                doubleTapSeconds = newSeconds
                                displaySeconds = newSeconds
                                if (isRight) {
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration))
                                } else {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0L))
                                }
                                currentPositionMs = exoPlayer.currentPosition
                                doubleTapTrigger++
                            } else {
                                doubleTapSide = null
                                doubleTapSeconds = 0
                                areControlsVisible = !areControlsVisible
                            }
                        } else {
                            areControlsVisible = !areControlsVisible
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isScreenLocked) return@detectTapGestures
                        val screenWidth = size.width
                        val isRight = offset.x >= screenWidth / 2
                        val side = if (isRight) "RIGHT" else "LEFT"

                        val newSeconds = if (currentDoubleTapSide == side) currentDoubleTapSeconds + 10 else 10
                        doubleTapSide = side
                        doubleTapSeconds = newSeconds
                        displaySide = side
                        displaySeconds = newSeconds

                        seekRelative(if (isRight) 10L else -10L)
                        doubleTapTrigger++
                    }
                )
            }
            .pointerInput(isScreenLocked) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (isScreenLocked) {
                            activeDragType = null
                            return@detectDragGestures
                        }
                        val screenWidth = size.width
                        val leftZoneThreshold = screenWidth * 0.25f
                        val rightZoneThreshold = screenWidth * 0.75f

                        if (offset.x <= leftZoneThreshold) {
                            activeDragType = "BRIGHTNESS"
                            isAdjustingBrightness = true
                            isAdjustingVolume = false
                            controlsInteractionCounter++
                        } else if (offset.x >= rightZoneThreshold) {
                            activeDragType = "VOLUME"
                            isAdjustingVolume = true
                            isAdjustingBrightness = false
                            controlsInteractionCounter++
                            if (audioManager != null) {
                                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                accumulatedVolumeDelta = curVol.toFloat()
                                currentVolume = (curVol.toFloat() / maxVol.toFloat()).coerceIn(0f, 1f)
                            }
                        } else {
                            activeDragType = null
                        }
                    },
                    onDrag = { change, dragAmount ->
                        if (isScreenLocked) return@detectDragGestures
                        val deltaY = -dragAmount.y // Drag up is positive
                        val screenHeight = size.height.toFloat().coerceAtLeast(1f)

                        when (activeDragType) {
                            "BRIGHTNESS" -> {
                                change.consume()
                                val step = deltaY / screenHeight
                                updateBrightness((currentBrightness + step).coerceIn(0.01f, 1f))
                                controlsInteractionCounter++
                            }
                            "VOLUME" -> {
                                change.consume()
                                if (audioManager != null) {
                                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    if (maxVol > 0) {
                                        val step = (deltaY / screenHeight) * maxVol
                                        accumulatedVolumeDelta = (accumulatedVolumeDelta + step).coerceIn(0f, maxVol.toFloat())
                                        val frac = accumulatedVolumeDelta / maxVol.toFloat()
                                        updateVolume(frac.coerceIn(0f, 1f))
                                        controlsInteractionCounter++
                                    }
                                }
                            }
                            else -> {
                                // Ignore drags starting outside the vertical side zones
                            }
                        }
                    },
                    onDragEnd = {
                        activeDragType = null
                        scope.launch {
                            delay(1200)
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                        }
                    },
                    onDragCancel = {
                        activeDragType = null
                        scope.launch {
                            delay(1200)
                            isAdjustingBrightness = false
                            isAdjustingVolume = false
                        }
                    }
                )
            }
    ) {
        // Player Surface: Hybrid WebView for EMBED streams & PlayerView for native streams
        if (isEmbedStream && resolvedStreamUrl != null) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = false
                            allowContentAccess = false
                            setSupportMultipleWindows(false)
                            javaScriptCanOpenWindowsAutomatically = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (newProgress > 60) {
                                    isBuffering = false
                                }
                            }

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val msg = consoleMessage?.message() ?: ""
                                if (msg.contains("Error 232001", ignoreCase = true) ||
                                    msg.contains("The media could not be loaded", ignoreCase = true) ||
                                    msg.contains("No compatible source", ignoreCase = true) ||
                                    msg.contains("File not found", ignoreCase = true) ||
                                    msg.contains("Video not found", ignoreCase = true) ||
                                    msg.contains("not found", ignoreCase = true) ||
                                    msg.contains("404 Not Found", ignoreCase = true)) {
                                    scope.launch {
                                        resolveRetryCount++
                                        resolveRetryTrigger++
                                    }
                                    return true
                                }
                                return super.onConsoleMessage(consoleMessage)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed()
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                val status = errorResponse?.statusCode ?: 200
                                if (request?.isForMainFrame == true && status in listOf(404, 410, 500, 502, 503)) {
                                    scope.launch {
                                        resolveRetryCount++
                                        resolveRetryTrigger++
                                    }
                                }
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val u = request?.url?.toString()?.lowercase() ?: return null
                                val adKeywords = listOf(
                                    "googleads", "doubleclick", "adservice", "popads", "propeller",
                                    "adsterra", "monetag", "exoclick", "juicyads", "trafficjunky",
                                    "adnxs", "bidswitch", "rubiconproject", "criteo", "taboola",
                                    "outbrain", "yandex.ru/ads", "adsystem", "adserver", "/vast",
                                    "vpaid", "delivery/ajs.php", "a.magsrv.com", "tsyndicate",
                                    "google-analytics", "googletagmanager", "histats", "statcounter",
                                    "scorecardresearch", "hotjar", "clarity.ms", "disqus", "crypto",
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
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        200,
                                        "OK",
                                        corsHeaders,
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url?.toString() ?: return false
                                val scheme = request.url?.scheme?.lowercase() ?: ""
                                if (scheme != "http" && scheme != "https") {
                                    return true
                                }
                                val targetLower = target.lowercase()
                                val targetHost = request.url?.host?.lowercase() ?: ""
                                val activeUrl = resolvedStreamUrl ?: ""
                                val activeHost = try { URI(activeUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }

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
                                if (blockedKeywords.any { targetLower.contains(it) }) return true

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

                                    if (!isAllowedHost) return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isBuffering = true
                                val injectCss = """
                                    (function() {
                                        try {
                                            if (!document.getElementById('tvpro-adblock-style')) {
                                                var es = document.createElement('style');
                                                es.id = 'tvpro-adblock-style';
                                                es.innerHTML = 'html, body { background: #000 !important; overflow: hidden !important; margin: 0 !important; padding: 0 !important; width: 100vw !important; height: 100vh !important; } video { position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 2147483640 !important; object-fit: contain !important; background: #000 !important; } #sf-overlay, #adbd, div.video_ad, div.video_ad_fadein, #over_player_msg, .sf-resume-modal, [class*="sf-resume-modal"], .jw-preview, .vjs-poster, .plyr__poster, .jw-ad-container, .jw-ad-controls, .ima-ad-container, .jw-controls, .jw-controlbar, .jw-display, .jw-display-icon-container, .jw-display-controls, .jw-title, .jw-logo, .jw-overlays, .jw-settings-menu, .vjs-control-bar, .vjs-big-play-button, .plyr__controls, [id*="ad-"], [class*="ad-"], [id*="banner"], [class*="banner"], [id*="popup"], [class*="popup"], dialog { display: none !important; opacity: 0 !important; pointer-events: none !important; } video::-webkit-media-controls { display: none !important; }';
                                                (document.head || document.documentElement).appendChild(es);
                                            }
                                        } catch(e) {}
                                        try {
                                            window.open = function() { return { closed: false, focus: function(){}, close: function(){} }; };
                                            window.alert = function() {};
                                            window.confirm = function() { return true; };
                                            window.prompt = function() { return null; };
                                        } catch(e) {}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(injectCss, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val adBlockAndPlayJs = """
                                    (function() {
                                        try {
                                            var playSelectors = '.vid_play, [tsid="player-component_play"], .vid-card_cnt_w, #embedVideoC, .vid_play-ic, [data-l="t,play"], .jw-display-icon-container, .vjs-big-play-button, button.play, .play-button, .play-btn, #play, #loadIframeButton, .plyr__control--overlaid, button.aegis-launch, .aegis-launch, .aegis-launch-btn, [class*="aegis-launch"], button[aria-label="Play"], button[aria-label*="Play"], .vp-play, .vp_video_stub, .vp_play_btn, .html5-player-controls, [data-module="Player"], .v_play, .widget_play, div[class*="play-btn"], button[class*="play"], [aria-label*="play" i], [aria-label*="reproducir" i], .captcha-gate__play, button.captcha-gate__play, [class*="captcha-gate__play"], [class*="captcha-gate"] button, button[aria-label*="Play video" i]';
                                            var clickTargets = document.querySelectorAll(playSelectors);
                                            clickTargets.forEach(function(el) {
                                                try { el.click(); } catch(e){}
                                                try {
                                                    var evt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                                                    el.dispatchEvent(evt);
                                                } catch(e){}
                                            });

                                            var v = document.querySelector('video');
                                            if (v) {
                                                v.muted = false;
                                                v.controls = false;
                                                if (v.paused) { try { v.play(); } catch(e){} }
                                            }
                                            if (window.jwplayer && typeof window.jwplayer === 'function') {
                                                try {
                                                    var p = window.jwplayer();
                                                    if (p && typeof p.play === 'function') p.play(true);
                                                    if (p && typeof p.setControls === 'function') p.setControls(false);
                                                } catch(e){}
                                            }
                                        } catch(e){}
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(adBlockAndPlayJs, null)
                            }
                        }

                        fun loadTargetUrl(targetUrl: String) {
                            var normalizedUrl = targetUrl
                            if (normalizedUrl.contains("bysefujedu.com/d/")) {
                                normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                            } else if (normalizedUrl.contains("filemoon.") && normalizedUrl.contains("/d/")) {
                                normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                            } else if (normalizedUrl.contains("primeload.co/embed/")) {
                                normalizedUrl = normalizedUrl.replace("/embed/", "/player/") + "?ref=https://doramasflix.in/"
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

                            val headers = mapOf(
                                "Referer" to refererBase,
                                "Origin" to originBase
                            )
                            loadUrl(normalizedUrl, headers)
                        }

                        loadTargetUrl(resolvedStreamUrl ?: "")
                    }
                },
                update = { webView ->
                    webViewRef = webView
                    val targetUrl = resolvedStreamUrl ?: ""
                    if (targetUrl.isNotBlank() && loadedEmbedUrl != targetUrl) {
                        loadedEmbedUrl = targetUrl
                        isBuffering = true

                        var normalizedUrl = targetUrl
                        if (normalizedUrl.contains("bysefujedu.com/d/")) {
                            normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                        } else if (normalizedUrl.contains("filemoon.") && normalizedUrl.contains("/d/")) {
                            normalizedUrl = normalizedUrl.replace("/d/", "/e/")
                        } else if (normalizedUrl.contains("primeload.co/embed/")) {
                            normalizedUrl = normalizedUrl.replace("/embed/", "/player/") + "?ref=https://doramasflix.in/"
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

                        val headers = mapOf(
                            "Referer" to refererBase,
                            "Origin" to originBase
                        )
                        webView.loadUrl(normalizedUrl, headers)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                        this.resizeMode = resizeMode
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Modern Double-tap visual indicator overlay (YouTube/Netflix style)
        AnimatedVisibility(
            visible = doubleTapSide != null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(220)) + scaleOut(animationSpec = tween(220), targetScale = 0.92f),
            modifier = Modifier.align(if (displaySide == "RIGHT") Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            DoubleTapSeekOverlay(
                side = displaySide,
                seconds = displaySeconds,
                trigger = doubleTapTrigger
            )
        }

        // Loading state
        if (isLoadingStream) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarmIvory)
            }
        }

        // Mid-playback Buffering Spinner (Floating indicator when video is buffering and controls are hidden)
        AnimatedVisibility(
            visible = isBuffering && !isLoadingStream && errorMessage == null && !areControlsVisible,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200), initialScale = 0.85f),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200), targetScale = 0.85f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC06080C))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WarmIvory,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Error message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE06080C)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = SoftSilver,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.White,
                        fontFamily = Inter,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                errorMessage = null
                                resolveRetryCount = 0
                                resolveRetryTrigger++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmIvory,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder)
                        ) {
                            Text("Regresar", color = Color.White)
                        }
                    }
                }
            }
        }

        // Interactive Vertical Bars (Brightness Left, Volume Right) - Only visible when actively adjusting
        val isBrightnessBarVisible = isAdjustingBrightness && !isAdjustingVolume
        val isVolumeBarVisible = isAdjustingVolume && !isAdjustingBrightness

        // Left Side Vertical Brightness Bar
        AnimatedVisibility(
            visible = isBrightnessBarVisible && errorMessage == null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.90f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250), targetScale = 0.90f),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            InteractiveVerticalBar(
                level = currentBrightness,
                icon = when {
                    currentBrightness < 0.35f -> Icons.Default.BrightnessLow
                    currentBrightness > 0.70f -> Icons.Default.BrightnessHigh
                    else -> Icons.Default.BrightnessMedium
                },
                accentColor = TvProYellow,
                isLeft = true,
                onLevelChange = { newLevel ->
                    updateBrightness(newLevel)
                    controlsInteractionCounter++
                },
                onInteractionStart = {
                    isAdjustingBrightness = true
                    controlsInteractionCounter++
                },
                onInteractionEnd = {
                    scope.launch {
                        delay(1200)
                        isAdjustingBrightness = false
                    }
                },
                onIconClick = {
                    val toggled = if (currentBrightness > 0.7f) 0.3f else 1.0f
                    updateBrightness(toggled)
                    controlsInteractionCounter++
                }
            )
        }

        // Right Side Vertical Volume Bar
        AnimatedVisibility(
            visible = isVolumeBarVisible && errorMessage == null,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(animationSpec = tween(150), initialScale = 0.90f),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(animationSpec = tween(250), targetScale = 0.90f),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            InteractiveVerticalBar(
                level = currentVolume,
                icon = when {
                    currentVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeMute
                    currentVolume < 0.50f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                },
                accentColor = WarmIvory,
                isLeft = false,
                onLevelChange = { newLevel ->
                    updateVolume(newLevel)
                    controlsInteractionCounter++
                },
                onInteractionStart = {
                    isAdjustingVolume = true
                    controlsInteractionCounter++
                },
                onInteractionEnd = {
                    scope.launch {
                        delay(1200)
                        isAdjustingVolume = false
                    }
                },
                onIconClick = {
                    val toggled = if (currentVolume > 0.01f) 0f else 0.5f
                    updateVolume(toggled)
                    controlsInteractionCounter++
                }
            )
        }

        // Controls HUD Overlay
        AnimatedVisibility(
            visible = areControlsVisible && !isLoadingStream && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (isScreenLocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                            .padding(top = 18.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            .clickable {
                                isScreenLocked = false
                                areControlsVisible = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Pantalla Bloqueada",
                                tint = TvProYellow,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pantalla Bloqueada â€¢ Toca para desbloquear",
                                color = WarmIvory,
                                fontSize = 12.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x77000000))
                ) {
                    // Top Bar: Back, Title, Server Pill, Speed, Audio/Subtitles, Lock, AspectRatio, PiP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "AtrÃ¡s",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        val ep = currentEpisode
                        val cleanMovieTitle = movie.title.cleanTitle()
                        val displayTitle = if (ep != null) {
                            "$cleanMovieTitle â€¢ T${ep.seasonNumber}:E${ep.episodeNumber}"
                        } else {
                            cleanMovieTitle
                        }

                        Text(
                            text = displayTitle,
                            color = WarmIvory,
                            fontFamily = Inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Playback Speed Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                                .clickable { showSpeedModal = true }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${currentSpeed}x",
                                color = WarmIvory,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Audio & Subtitles button
                        IconButton(
                            onClick = { showTrackModal = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Audio y SubtÃ­tulos",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Lock Screen button
                        IconButton(
                            onClick = {
                                isScreenLocked = true
                                areControlsVisible = false
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Bloquear Pantalla",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Resize mode toggle button
                        IconButton(
                            onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // PiP Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = { enterPictureInPicture() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x8806080C))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "PiP",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                // Center Controls: Rewind 10, Play/Pause / Buffering, Forward 10
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            seekRelative(-10L)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x66000000), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Retroceder 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            togglePlayPauseAction()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(MochiStrawberryPink, MochiWarmRose)
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            seekRelative(10L)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0x66000000), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Adelantar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Bar: Progress Bar & Timers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    val displayFraction = if (totalDurationMs > 0) {
                        if (isDraggingSlider) dragProgressFraction else (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val displayTimeMs = if (isDraggingSlider) (dragProgressFraction * totalDurationMs).toLong() else currentPositionMs

                    Slider(
                        value = displayFraction,
                        onValueChange = { frac ->
                            isDraggingSlider = true
                            dragProgressFraction = frac
                        },
                        onValueChangeFinished = {
                            val target = (dragProgressFraction * totalDurationMs).toLong()
                            seekToTargetMs(target)
                            isDraggingSlider = false
                        },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(MochiStrawberryPink, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(3.5.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = MochiStrawberryPink,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                                ),
                                thumbTrackGapSize = 0.dp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(displayTimeMs),
                            color = WarmIvory,
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium
                        )

                        // Series quick navigation buttons: Anterior, CapÃ­tulos, Siguiente
                        if (isSeriesContent) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (prevEpisode != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x66000000))
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                            .clickable { playEpisode(prevEpisode) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SkipPrevious,
                                                contentDescription = "Anterior",
                                                tint = WarmIvory,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Anterior",
                                                color = WarmIvory,
                                                fontSize = 11.sp,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x66000000))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                        .clickable { showEpisodesModal = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VideoLibrary,
                                            contentDescription = "CapÃ­tulos",
                                            tint = WarmIvory,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "CapÃ­tulos",
                                            color = WarmIvory,
                                            fontSize = 11.sp,
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (nextEpisode != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(MochiStrawberryPink, MochiWarmRose)
                                                )
                                            )
                                            .clickable { playEpisode(nextEpisode) }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Siguiente",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.SkipNext,
                                                contentDescription = "Siguiente",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "-" + formatDuration((totalDurationMs - displayTimeMs).coerceAtLeast(0L)),
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )
                    }
                }
            }
        }

        // Next Episode Countdown Overlay (Shows during last 30s or on video ended)
        AnimatedVisibility(
            visible = showNextEpisodePrompt && nextEpisode != null && !isNextEpisodeDismissed && !isLoadingStream,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(350)) { it / 2 },
            exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(animationSpec = tween(250)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(end = 24.dp, bottom = 28.dp)
        ) {
            val targetNextEp = nextEpisode ?: return@AnimatedVisibility
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF20B0E14)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.width(360.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row: Siguiente Episodio + Countdown Badge + Dismiss button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                tint = TvProYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "SIGUIENTE EPISODIO",
                                color = WarmIvory,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(TvProYellow.copy(alpha = 0.2f))
                                    .border(1.dp, TvProYellow.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "En ${nextEpisodeCountdownSeconds}s",
                                    color = TvProYellow,
                                    fontSize = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    showNextEpisodePrompt = false
                                    isNextEpisodeDismissed = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = SoftSilver,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Episode Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val cleanThumb = com.tvpro.core.data.repository.MovieRepository.sanitizeThumbnailUrl(targetNextEp.image)
                        val epThumb = cleanThumb.ifBlank { seriesDetailCache?.backdrop ?: movie.backdrop ?: movie.image }
                        if (epThumb.isNotBlank()) {
                            AsyncImage(
                                model = epThumb,
                                contentDescription = targetNextEp.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "T${targetNextEp.seasonNumber} : E${targetNextEp.episodeNumber}",
                                color = TvProYellow,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cleanEpisodeTitle(targetNextEp.title, movie.title, targetNextEp.episodeNumber),
                                color = WarmIvory,
                                fontSize = 12.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showNextEpisodePrompt = false
                                playEpisode(targetNextEp)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TvProYellow,
                                contentColor = Color(0xFF0D0F14)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reproducir Ahora",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                showNextEpisodePrompt = false
                                isNextEpisodeDismissed = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                color = WarmIvory,
                                fontFamily = Inter,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Episodes list Modal Bottom Sheet (for series fast episode selection)
        if (showEpisodesModal) {
            val allEpisodes = seriesDetailCache?.episodes ?: emptyList()
            val seasons = remember(allEpisodes) {
                allEpisodes.map { it.seasonNumber }.distinct().sorted()
            }
            var selectedSeasonInModal by remember(currentEpisode, seasons) {
                mutableIntStateOf(currentEpisode?.seasonNumber ?: seasons.firstOrNull() ?: 1)
            }
            val seasonEpisodes = remember(allEpisodes, selectedSeasonInModal) {
                allEpisodes.filter { it.seasonNumber == selectedSeasonInModal }
            }
            val episodeListState = rememberLazyListState()

            // Auto-scroll to currently playing episode on modal open
            LaunchedEffect(selectedSeasonInModal, showEpisodesModal) {
                val currentIdx = seasonEpisodes.indexOfFirst {
                    it.id == currentEpisode?.id ||
                        (it.seasonNumber == currentEpisode?.seasonNumber && it.episodeNumber == currentEpisode?.episodeNumber)
                }
                if (currentIdx >= 0) {
                    episodeListState.scrollToItem((currentIdx - 1).coerceAtLeast(0))
                }
            }

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showEpisodesModal = false },
                sheetState = sheetState,
                containerColor = Color(0xF40D0814),
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 4.dp)
                            .size(width = 42.dp, height = 4.5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .navigationBarsPadding()
                ) {
                    // Header Row: Title, Total Count & Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "CapÃ­tulos",
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = WarmIvory
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MochiPlumSurface)
                                        .border(1.dp, SubtleBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                                ) {
                                    Text(
                                        text = "${allEpisodes.size} en total",
                                        fontSize = 11.sp,
                                        color = SoftSilver,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            val cleanMovieTitle = movie.title.cleanTitle()
                            val curEpNum = currentEpisode?.episodeNumber ?: 1
                            val curSeasonNum = currentEpisode?.seasonNumber ?: 1
                            Text(
                                text = "$cleanMovieTitle â€¢ T$curSeasonNum:E$curEpNum en curso",
                                fontSize = 12.sp,
                                color = MochiLavender,
                                fontFamily = Inter,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { showEpisodesModal = false },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = WarmIvory,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Season selector pills (if more than 1 season)
                    if (seasons.size > 1) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            items(seasons) { sNum ->
                                val isSelected = sNum == selectedSeasonInModal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) MochiStrawberryPink else PillSurface)
                                        .border(1.dp, if (isSelected) MochiStrawberryPink else SubtleBorder, RoundedCornerShape(16.dp))
                                        .clickable { selectedSeasonInModal = sNum }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Temporada $sNum",
                                        color = if (isSelected) Color.White else WarmIvory,
                                        fontFamily = Inter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (allEpisodes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MochiStrawberryPink, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Cargando episodios...",
                                    color = SoftSilver,
                                    fontSize = 13.sp,
                                    fontFamily = Inter
                                )
                            }
                        }
                    } else {
                        val modalListHeight = if (isLandscape) 240.dp else 470.dp
                        LazyColumn(
                            state = episodeListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = modalListHeight),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(seasonEpisodes) { _, ep ->
                                val isCurrent = currentEpisode?.id == ep.id ||
                                    (currentEpisode?.seasonNumber == ep.seasonNumber && currentEpisode?.episodeNumber == ep.episodeNumber)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .then(
                                            if (isCurrent) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            MochiStrawberryPink.copy(alpha = 0.25f),
                                                            Color(0x331E1228)
                                                        )
                                                    )
                                                )
                                            } else {
                                                Modifier.background(PillSurface)
                                            }
                                        )
                                        .border(
                                            width = if (isCurrent) 1.5.dp else 1.dp,
                                            color = if (isCurrent) MochiStrawberryPink.copy(alpha = 0.75f) else SubtleBorder.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            playEpisode(ep)
                                            showEpisodesModal = false
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 16:9 Thumbnail with live audio wave & duration
                                    val cleanEpThumb = com.tvpro.core.data.repository.MovieRepository.sanitizeThumbnailUrl(ep.image)
                                    val epThumb = cleanEpThumb.ifBlank { seriesDetailCache?.backdrop ?: movie.backdrop ?: movie.image }
                                    Box(
                                        modifier = Modifier
                                            .width(108.dp)
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MochiPlumSurface)
                                    ) {
                                        if (epThumb.isNotBlank()) {
                                            AsyncImage(
                                                model = epThumb,
                                                contentDescription = ep.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Dark vignette
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color(0x99000000))
                                                    )
                                                )
                                        )

                                        // Center status indicator
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) MochiStrawberryPink.copy(alpha = 0.90f) else Color(0x66000000))
                                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                AudioWaveIndicator(color = Color.White)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Reproducir",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Badge bottom: duration or episode number
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.75f))
                                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = "45m",
                                                color = WarmIvory,
                                                fontSize = 9.sp,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Episode Info Column
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "CapÃ­tulo ${ep.episodeNumber}",
                                                color = if (isCurrent) MochiStrawberryPink else WarmIvory,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                fontFamily = PlusJakartaSans
                                            )
                                            if (isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MochiStrawberryPink.copy(alpha = 0.20f))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "EN CURSO",
                                                        color = MochiStrawberryPink,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        fontFamily = Inter,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                        }

                                        val cleanSub = cleanEpisodeTitle(ep.title, movie.title, ep.episodeNumber)
                                        if (cleanSub.isNotBlank() && !cleanSub.equals("Episodio ${ep.episodeNumber}", ignoreCase = true)) {
                                            Text(
                                                text = cleanSub,
                                                color = WarmIvory.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                fontFamily = Inter,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (ep.overview.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = ep.overview.cleanHtml(),
                                                color = MutedSlate,
                                                fontSize = 11.sp,
                                                fontFamily = Inter,
                                                maxLines = 2,
                                                lineHeight = 14.sp,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Right Action Button
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrent) MochiStrawberryPink.copy(alpha = 0.15f)
                                                else Color.White.copy(alpha = 0.06f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isCurrent) MochiStrawberryPink else SoftSilver,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Server switcher Modal Bottom Sheet
        if (showServerModal) {
            ModalBottomSheet(
                onDismissRequest = { showServerModal = false },
                containerColor = MochiPlumCard,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Seleccionar Servidor de Streaming",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    streamList.forEachIndexed { idx, s ->
                        val isSelected = idx == selectedStreamIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MochiStrawberryPink.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable {
                                    selectedStreamIndex = idx
                                    resolvedStreamUrl = s.url
                                    showServerModal = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Servidor ${idx + 1} (${s.source ?: "HD Fast Stream"})",
                                color = if (isSelected) MochiStrawberryPink else WarmIvory,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MochiStrawberryPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Playback Speed Modal Bottom Sheet
        if (showSpeedModal) {
            ModalBottomSheet(
                onDismissRequest = { showSpeedModal = false },
                containerColor = MochiPlumCard,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Velocidad de ReproducciÃ³n",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { spd ->
                        val isSelected = currentSpeed == spd
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MochiStrawberryPink.copy(alpha = 0.14f) else Color.Transparent)
                                .clickable {
                                    currentSpeed = spd
                                    exoPlayer.setPlaybackSpeed(spd)
                                    showSpeedModal = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (spd == 1.0f) "Normal (1.0x)" else "${spd}x",
                                color = if (isSelected) MochiStrawberryPink else WarmIvory,
                                fontFamily = Inter,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MochiStrawberryPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Audio & Subtitles Modal Bottom Sheet
        if (showTrackModal) {
            ModalBottomSheet(
                onDismissRequest = { showTrackModal = false },
                containerColor = MochiPlumCard,
                contentColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = "Audio y SubtÃ­tulos",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmIvory
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio tracks
                    val audioGroups = currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList()
                    Text(
                        text = "PISTAS DE AUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MochiLavender,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (audioGroups.isEmpty()) {
                        Text(
                            text = "Audio predeterminado",
                            fontSize = 13.sp,
                            color = SoftSilver,
                            fontFamily = Inter,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        audioGroups.forEach { group ->
                            val trackGroup = group.mediaTrackGroup
                            for (i in 0 until trackGroup.length) {
                                val format = trackGroup.getFormat(i)
                                val isSelected = group.isTrackSelected(i)
                                val trackLabel = format.label ?: format.language ?: "Audio ${i + 1}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) MochiStrawberryPink.copy(alpha = 0.14f) else Color.Transparent)
                                        .clickable {
                                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setOverrideForType(
                                                    TrackSelectionOverride(trackGroup, listOf(i))
                                                )
                                                .build()
                                            showTrackModal = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trackLabel.replaceFirstChar { it.uppercase() },
                                        color = if (isSelected) MochiStrawberryPink else WarmIvory,
                                        fontFamily = Inter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MochiStrawberryPink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Subtitle tracks
                    val subtitleGroups = currentTracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList()
                    Text(
                        text = "SUBTÃTULOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MochiLavender,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Option: Subtitles Off
                    val isSubtitlesDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSubtitlesDisabled) MochiStrawberryPink.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable {
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                    .build()
                                showTrackModal = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Desactivados",
                            color = if (isSubtitlesDisabled) MochiStrawberryPink else WarmIvory,
                            fontFamily = Inter,
                            fontWeight = if (isSubtitlesDisabled) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSubtitlesDisabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MochiStrawberryPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    subtitleGroups.forEach { group ->
                        val trackGroup = group.mediaTrackGroup
                        for (i in 0 until trackGroup.length) {
                            val format = trackGroup.getFormat(i)
                            val isSelected = !isSubtitlesDisabled && group.isTrackSelected(i)
                            val trackLabel = format.label ?: format.language ?: "SubtÃ­tulo ${i + 1}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MochiStrawberryPink.copy(alpha = 0.14f) else Color.Transparent)
                                    .clickable {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(
                                                TrackSelectionOverride(trackGroup, listOf(i))
                                            )
                                            .build()
                                        showTrackModal = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trackLabel.replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) MochiStrawberryPink else WarmIvory,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MochiStrawberryPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
}
