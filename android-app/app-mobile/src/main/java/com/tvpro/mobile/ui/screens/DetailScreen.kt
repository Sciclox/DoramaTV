package com.tvpro.mobile.ui.screens

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.MobileMovieCard
import com.tvpro.mobile.ui.components.cleanHtml
import com.tvpro.mobile.ui.components.cleanTitle
import com.tvpro.mobile.ui.components.cleanEpisodeTitle
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    movie: MovieItem,
    onBack: () -> Unit,
    onPlay: (MovieItem, EpisodeItem?, String?) -> Unit,
    onMovieClick: (MovieItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initialCached = remember(movie.url) { MovieRepository.getCachedDetail(movie.url, movie.title) }
    var movieDetail by remember(movie.url) { mutableStateOf<MovieDetailResponse?>(initialCached) }
    var isLoading by remember(movie.url) { mutableStateOf(initialCached == null) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var isInMyList by remember { mutableStateOf(WatchHistoryManager.isInMyList(context, movie.url)) }
    var isLiked by remember {
        mutableStateOf(
            context.getSharedPreferences("tvpro_ratings", android.content.Context.MODE_PRIVATE)
                .getBoolean("liked_${movie.url}", false)
        )
    }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var isOverviewExpanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("episodes") } // "episodes" or "similar"
    var seasonEpisodes by remember(movie.url) {
        mutableStateOf<List<EpisodeItem>>(initialCached?.episodes?.filter { it.seasonNumber == 1 } ?: initialCached?.episodes ?: emptyList())
    }
    var isSeasonLoading by remember { mutableStateOf(false) }
    var showSeasonBottomSheet by remember { mutableStateOf(false) }

    val cleanTitle = remember(movie.title) { movie.title.cleanTitle() }

    LaunchedEffect(movie.url, reloadTrigger) {
        val cached = MovieRepository.getCachedDetail(movie.url, movie.title)
        if (cached != null) {
            movieDetail = cached
            isLoading = false
            hasNetworkError = false
            if (seasonEpisodes.isEmpty() && cached.episodes.isNotEmpty() && selectedSeason == 1) {
                seasonEpisodes = cached.episodes.filter { it.seasonNumber == 1 }.ifEmpty { cached.episodes }
            }
        } else {
            isLoading = true
        }
        try {
            val detail = MovieRepository.extractMovie(movie, forceRefresh = (reloadTrigger > 0))
            movieDetail = detail
            if (seasonEpisodes.isEmpty() && detail.episodes.isNotEmpty() && selectedSeason == 1) {
                seasonEpisodes = detail.episodes.filter { it.seasonNumber == 1 }.ifEmpty { detail.episodes }
            }
            hasNetworkError = false
        } catch (_: Exception) {
            if (movieDetail == null) {
                hasNetworkError = true
            }
        } finally {
            isLoading = false
        }
    }

    val isSeries = remember(movieDetail, movie) {
        movieDetail?.isSeries == true || movie.type == "series" || movie.url.contains("/series/") || movie.url.contains("/animes/")
    }

    LaunchedEffect(selectedSeason, movieDetail?.postId) {
        val detail = movieDetail ?: return@LaunchedEffect
        if (!isSeries) return@LaunchedEffect

        val localMatches = detail.episodes.filter { it.seasonNumber == selectedSeason }
        if (selectedSeason == 1 && localMatches.isNotEmpty() && seasonEpisodes.isNotEmpty()) {
            seasonEpisodes = localMatches
            return@LaunchedEffect
        }

        isSeasonLoading = true
        try {
            val postIdInt = detail.postId?.toIntOrNull() ?: 0
            val eps = MovieRepository.fetchEpisodesForSeason(
                postId = postIdInt,
                season = selectedSeason,
                fallbackTitle = detail.title ?: movie.title,
                fallbackBackdrop = detail.backdrop ?: movie.backdrop ?: movie.image,
                fallbackGallery = detail.gallery ?: emptyList(),
                serieUrl = movie.url,
                postIdStr = detail.postId
            )
            if (eps.isNotEmpty()) {
                seasonEpisodes = eps
            } else if (localMatches.isNotEmpty()) {
                seasonEpisodes = localMatches
            }
        } catch (_: Exception) {
        } finally {
            isSeasonLoading = false
        }
    }

    val backdropUrl = remember(movieDetail, movie) {
        val raw = movieDetail?.backdrop ?: movie.backdrop ?: movie.image
        raw.replace("/t/p/w500/", "/t/p/original/")
            .replace("/t/p/w780/", "/t/p/original/")
            .replace("/t/p/w1280/", "/t/p/original/")
    }

    // Dynamic ambient palette glow
    var ambientColor by remember { mutableStateOf(MochiPlumCard) }
    val animatedAmbient by animateColorAsState(
        targetValue = ambientColor,
        animationSpec = tween(800),
        label = "detailAmbient"
    )

    LaunchedEffect(backdropUrl) {
        if (backdropUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(backdropUrl)
                        .size(240, 135)
                        .allowHardware(false)
                        .target { drawable ->
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null && !bitmap.isRecycled) {
                                Palette.from(bitmap).generate { palette ->
                                    val vibrant = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
                                    vibrant?.let {
                                        val hsl = FloatArray(3)
                                        ColorUtils.colorToHSL(it, hsl)
                                        hsl[2] = 0.50f
                                        ambientColor = Color(ColorUtils.HSLToColor(hsl))
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

    val similar = remember(movieDetail, isSeries) {
        val fromDetail = movieDetail?.similarMovies ?: emptyList()
        if (fromDetail.isNotEmpty()) fromDetail
        else if (movieDetail != null) {
            val pool = if (isSeries) MovieRepository.localSeriesCatalog else MovieRepository.localCatalog
            pool.filter { it.url != movie.url && !it.title.equals(cleanTitle, ignoreCase = true) }.take(10)
        } else emptyList()
    }
    val isSimilarLoading = isLoading && similar.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Edge-to-edge Backdrop Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = cleanTitle,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Ambient glow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        animatedAmbient.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    startY = 50f
                                )
                            )
                    )

                    // Dark gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x9906080C),
                                        Color.Transparent,
                                        Color(0x8806080C),
                                        TvProDarkBg
                                    ),
                                    startY = 0f
                                )
                            )
                    )

                    // Top Bar: Back & Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x8806080C))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Mira ${movie.title} en Mochi")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x88130D19))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Info, Badges & Actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Logo or Title
                    val logoUrl = movieDetail?.logo ?: movie.logo
                    if (!logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = cleanTitle,
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth(0.6f),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.CenterStart
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Text(
                            text = cleanTitle,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = WarmIvory,
                            lineHeight = 32.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Metadata Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // TMDB Pill
                        val rating = movieDetail?.rating ?: movie.rating ?: "8.3"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(3.5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "TMDB",
                                    color = MutedSlate,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter
                                )
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = TvProYellow,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = rating,
                                    color = WarmIvory,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter
                                )
                            }
                        }

                        // Year Pill
                        val year = movie.year ?: "2024"
                        Text(
                            text = year,
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )

                        // Quality Pill
                        val quality = movie.quality ?: "4K Ultra HD"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PillSurface)
                                .border(1.dp, WarmIvory.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = if (quality.contains("4K", ignoreCase = true)) "4K" else "HD",
                                color = WarmIvory,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter
                            )
                        }

                        // Audio Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "LAT",
                                color = SoftSilver,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter
                            )
                        }

                        // Runtime / Seasons
                        val durationText = if (isSeries) {
                            val count = movieDetail?.seasonsCount ?: 1
                            if (count > 1) "$count Temporadas" else "1 Temporada"
                        } else {
                            movieDetail?.runtime ?: movie.runtime ?: "1h 52m"
                        }
                        Text(
                            text = "• $durationText",
                            color = MutedSlate,
                            fontSize = 12.sp,
                            fontFamily = Inter
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val hasStreams = remember(movieDetail, isSeries, seasonEpisodes, movie.postId) {
                        if (movieDetail == null) true
                        else if (isSeries) seasonEpisodes.isNotEmpty() || movieDetail?.episodes?.isNotEmpty() == true || (movie.postId ?: 0) > 0
                        else movieDetail?.streams?.isNotEmpty() == true || (movie.postId ?: 0) > 0 || (movieDetail?.postId?.toIntOrNull() ?: 0) > 0
                    }

                    if (!isLoading && hasNetworkError) {
                        Surface(
                            color = MochiPlumCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reloadTrigger++ }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Error de red al sincronizar • Toca para reintentar",
                                    color = WarmIvory,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (!isLoading && !hasStreams) {
                        Surface(
                            color = MochiPlumCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MochiStrawberryPink.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MochiStrawberryPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aún no disponible",
                                    color = WarmIvory,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Play & Trailer Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val firstEpisode = seasonEpisodes.firstOrNull()
                                        ?: movieDetail?.episodes?.firstOrNull { it.seasonNumber == selectedSeason }
                                        ?: movieDetail?.episodes?.firstOrNull()
                                    onPlay(movie, firstEpisode, null)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MochiStrawberryPink,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val targetEpNum = seasonEpisodes.firstOrNull()?.episodeNumber ?: 1
                                Text(
                                    text = if (isSeries) "Reproducir T$selectedSeason:E$targetEpNum" else "Reproducir",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 15.sp
                                )
                            }

                            // Trailer Button
                            OutlinedButton(
                                onClick = {
                                    val trailerUrl = movieDetail?.trailer?.takeIf { it.isNotBlank() }
                                        ?: "https://www.youtube.com/results?search_query=${Uri.encode(movie.title + " trailer oficial latino")}"
                                    val finalUrl = if (trailerUrl.startsWith("http")) trailerUrl else "https://www.youtube.com/watch?v=$trailerUrl"
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No se pudo abrir el trailer", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmIvory),
                                border = BorderStroke(1.dp, SubtleBorder),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = "Trailer",
                                    tint = WarmIvory,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trailer",
                                    color = WarmIvory,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = Inter,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Icons Row: Mi Lista, Calificar, Compartir, Descargar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mi Lista
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isInMyList = WatchHistoryManager.toggleMyList(context, movie)
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "Mi Lista",
                                tint = if (isInMyList) WarmIvory else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isInMyList) "Guardado" else "Mi Lista",
                                color = if (isInMyList) WarmIvory else MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Calificar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    isLiked = !isLiked
                                    context.getSharedPreferences("tvpro_ratings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("liked_${movie.url}", isLiked)
                                        .apply()
                                    Toast.makeText(
                                        context,
                                        if (isLiked) "¡Añadido a tus recomendaciones!" else "Calificación removida",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = "Calificar",
                                tint = if (isLiked) TvProYellow else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isLiked) "Te gusta" else "Calificar",
                                color = if (isLiked) TvProYellow else MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Compartir
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Mira ${movie.title} en Mochi")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Compartir",
                                color = MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }

                        // Descargar
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    Toast.makeText(context, "Descargas offline próximamente en Mochi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Descargar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Descargar",
                                color = MutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Overview Synopsis
                    val overview = (movieDetail?.overview?.takeIf { it.isNotBlank() }
                        ?: movie.overview?.takeIf { it.isNotBlank() }
                        ?: MovieRepository.getCachedOverview(movie.url)
                        ?: MovieRepository.getCachedOverview(MovieRepository.extractSlug(movie.url, movie.title)))
                    if (!overview.isNullOrBlank()) {
                        Text(
                            text = overview,
                            color = WarmIvory.copy(alpha = 0.88f),
                            fontSize = 13.sp,
                            fontFamily = Inter,
                            lineHeight = 19.sp,
                            maxLines = if (isOverviewExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { isOverviewExpanded = !isOverviewExpanded }
                        )
                        if (!isOverviewExpanded && overview.length > 120) {
                            Text(
                                text = "Más información...",
                                color = MutedSlate,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                modifier = Modifier
                                    .clickable { isOverviewExpanded = true }
                                    .padding(top = 2.dp)
                            )
                        }
                    }

                    // Genres Row
                    val genres = movieDetail?.genres?.ifEmpty { movie.genres } ?: movie.genres
                    if (genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Géneros: " + genres.joinToString(", "),
                            color = MutedSlate,
                            fontSize = 11.5.sp,
                            fontFamily = Inter
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            // 3. Series Episodes or Similar Movies Section
            if (isSeries) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episodios",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                letterSpacing = (-0.2).sp,
                                color = WarmIvory
                            )

                            // Seasons Selector Button (opens BottomSheet with Mochi theme)
                            val totalSeasons = maxOf(
                                movieDetail?.seasonsCount ?: 1,
                                movieDetail?.episodes?.maxOfOrNull { it.seasonNumber } ?: 1
                            ).coerceAtLeast(1)
                            if (totalSeasons > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PillSurface)
                                        .border(1.dp, SubtleBorder, RoundedCornerShape(8.dp))
                                        .clickable { showSeasonBottomSheet = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Temporada $selectedSeason ▾",
                                        color = WarmIvory,
                                        fontSize = 12.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Episodes list items or skeletons
                if (isSeasonLoading) {
                    items(4) {
                        MobileEpisodeSkeletonCard()
                    }
                } else if (seasonEpisodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron episodios para la Temporada $selectedSeason",
                                color = MutedSlate,
                                fontSize = 13.sp,
                                fontFamily = Inter
                            )
                        }
                    }
                } else {
                    items(seasonEpisodes) { episode ->
                        MobileEpisodeCard(
                            episode = episode,
                            seriesTitle = cleanTitle,
                            onClick = { onPlay(movie, episode, null) }
                        )
                    }
                }
            }

            // 4. Similar / Recommended Titles
            if (isSimilarLoading || similar.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "TÍTULOS SIMILARES",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.2).sp,
                        color = WarmIvory,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSimilarLoading) {
                            items(5) {
                                MobileMovieCardSkeleton()
                            }
                        } else {
                            items(similar) { simMovie ->
                                MobileMovieCard(
                                    movie = simMovie,
                                    onClick = {
                                        onMovieClick(simMovie)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
                Spacer(modifier = Modifier.height(56.dp))
            }
        }

        // Season Selector BottomSheet (Matches official Mochi theme)
        if (showSeasonBottomSheet) {
            val totalSeasons = maxOf(
                movieDetail?.seasonsCount ?: 1,
                movieDetail?.episodes?.maxOfOrNull { it.seasonNumber } ?: 1
            ).coerceAtLeast(1)

            ModalBottomSheet(
                onDismissRequest = { showSeasonBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MochiPlumSurface,
                scrimColor = Color.Black.copy(alpha = 0.65f),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MutedSlate.copy(alpha = 0.4f))
                    )
                },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "Temporadas",
                        color = WarmIvory,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlusJakartaSans,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )

                    (1..totalSeasons).forEach { sNum ->
                        val isSelected = sNum == selectedSeason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSeason = sNum
                                    showSeasonBottomSheet = false
                                }
                                .background(if (isSelected) MochiStrawberryPink.copy(alpha = 0.12f) else Color.Transparent)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Temporada $sNum",
                                color = if (isSelected) MochiStrawberryPink else WarmIvory,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = Inter
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MochiStrawberryPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileEpisodeSkeletonCard(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "epSkeleton")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "epShimmer"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MochiPlumSurface,
            MochiPlumCard,
            MochiPlumSurface
        ),
        start = Offset(translateAnim - 150f, translateAnim - 150f),
        end = Offset(translateAnim + 150f, translateAnim + 150f)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
fun MobileMovieCardSkeleton(
    modifier: Modifier = Modifier,
    cardWidth: androidx.compose.ui.unit.Dp = 135.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 195.dp
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MochiPlumSurface,
            MochiPlumCard,
            MochiPlumSurface
        ),
        start = Offset(translateAnim - 150f, translateAnim - 150f),
        end = Offset(translateAnim + 150f, translateAnim + 150f)
    )

    val cardShape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(cardShape)
                .background(shimmerBrush)
                .border(BorderStroke(1.dp, SubtleBorder.copy(alpha = 0.4f)), shape = cardShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

@Composable
fun MobileEpisodeCard(
    episode: EpisodeItem,
    seriesTitle: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail with play overlay
            Box(
                modifier = Modifier
                    .width(135.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(6.dp))
                    .background(TvProCardBg)
            ) {
                AsyncImage(
                    model = com.tvpro.core.data.repository.MovieRepository.sanitizeThumbnailUrl(episode.image),
                    contentDescription = episode.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0x9906080C))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            val epCleanTitle = remember(episode.title, seriesTitle, episode.episodeNumber) {
                cleanEpisodeTitle(episode.title, seriesTitle, episode.episodeNumber)
            }
            val displayEpText = if (epCleanTitle.startsWith("Episodio", ignoreCase = true) || epCleanTitle.startsWith("Capítulo", ignoreCase = true)) {
                epCleanTitle
            } else {
                "${episode.episodeNumber}. $epCleanTitle"
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayEpText,
                    color = WarmIvory,
                    fontSize = 13.5.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.runtime,
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontFamily = Inter
                )
            }

            IconButton(onClick = {
                Toast.makeText(localContext, "Descargas de episodios próximamente en Mochi", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = MutedSlate,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Episode synopsis if available
        if (episode.overview.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = episode.overview.cleanHtml(),
                color = MutedSlate,
                fontSize = 11.5.sp,
                fontFamily = Inter,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
