package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.material3.Icon as M3Icon
import androidx.tv.material3.*
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.components.*
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState

import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (MovieItem, String) -> Unit,
    onPlayClick: (MovieItem) -> Unit,
    playButtonFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    selectedNavId: String = "home",
    onFocusItem: (FocusRequester) -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    shouldRequestInitialFocus: Boolean = false,
    onInitialFocusLanded: () -> Unit = {},
    restoreFocusUrl: String? = null,
    restoreFocusRowKey: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    val listState = rememberTvLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val watchHistory = remember { mutableStateListOf<WatchHistoryItem>() }
    val firstRowFocusRequester = remember { FocusRequester() }

    // Reload history on launch and whenever returning to Home (e.g. from Detail/Player)
    LaunchedEffect(context, restoreFocusUrl, restoreFocusRowKey) {
        watchHistory.clear()
        watchHistory.addAll(WatchHistoryManager.getHistory(context))
    }

    // Scroll vertical list to row containing restoreFocusUrl when returning from Detail
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, uiState) {
        val url = restoreFocusUrl ?: return@LaunchedEffect
        val rowKey = restoreFocusRowKey ?: return@LaunchedEffect
        val state = uiState as? HomeUiState.Success ?: return@LaunchedEffect

        val hasHistory = watchHistory.isNotEmpty()

        val targetRowIndex = when (rowKey) {
            "hero" -> 0
            "history" -> if (hasHistory) 1 else 0
            "Doramas en Emisión" -> if (hasHistory) 2 else 1
            "top10" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                idx
            }
            "Audio Latino" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                idx
            }
            "Sub Español" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.audioLatino.isNotEmpty()) idx++
                idx
            }
            "C-Dramas Chinos" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.audioLatino.isNotEmpty()) idx++
                if (state.subEspanol.isNotEmpty()) idx++
                idx
            }
            "Películas Asiáticas", "Películas recién añadidas" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.audioLatino.isNotEmpty()) idx++
                if (state.subEspanol.isNotEmpty()) idx++
                if (state.cdramasChinos.isNotEmpty()) idx++
                idx
            }
            "Próximos Estrenos 2026" -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.audioLatino.isNotEmpty()) idx++
                if (state.subEspanol.isNotEmpty()) idx++
                if (state.cdramasChinos.isNotEmpty()) idx++
                if (state.peliculasAsiaticas.isNotEmpty()) idx++
                idx
            }
            else -> {
                var idx = if (hasHistory) 2 else 1
                if (state.doramasEnEmision.isNotEmpty()) idx++
                if (state.top10.isNotEmpty()) idx++
                if (state.audioLatino.isNotEmpty()) idx++
                if (state.subEspanol.isNotEmpty()) idx++
                if (state.cdramasChinos.isNotEmpty()) idx++
                if (state.peliculasAsiaticas.isNotEmpty()) idx++
                if (state.proximosEstrenos.isNotEmpty()) idx++
                var found: Int? = null
                for (col in state.collections) {
                    if (col.movies.isNotEmpty()) {
                        if (col.title == rowKey) {
                            found = idx
                            break
                        }
                        idx++
                    }
                }
                found
            }
        }

        if (targetRowIndex != null) {
            try {
                listState.scrollToItem(targetRowIndex, 0)
            } catch (_: Exception) {}
        }
    }

    // Land initial focus on Hero Play button ONLY ONCE on initial app launch
    LaunchedEffect(shouldRequestInitialFocus, uiState) {
        if (shouldRequestInitialFocus && uiState is HomeUiState.Success) {
            delay(120)
            try {
                playButtonFocusRequester.requestFocus()
                onInitialFocusLanded()
            } catch (_: Exception) {}
        }
    }

    // React to top navigation selection (smooth scroll jump to corresponding section on Home)
    LaunchedEffect(selectedNavId, uiState) {
        if (restoreFocusUrl != null) return@LaunchedEffect
        val offset = if (watchHistory.isNotEmpty()) 1 else 0
        val emisionIdx = 1 + offset
        val popularIdx = 3 + offset
        val latinoIdx = 4 + offset
        val subIdx = 5 + offset
        val cdramaIdx = 6 + offset
        val peliculasIdx = 7 + offset

        when (selectedNavId) {
            "home" -> {
                try { listState.animateScrollToItem(0, 0) } catch (_: Exception) {}
            }
            "emision" -> {
                try { listState.animateScrollToItem(emisionIdx, 0) } catch (_: Exception) {}
            }
            "series" -> {
                try { listState.animateScrollToItem(popularIdx, 0) } catch (_: Exception) {}
            }
            "latino" -> {
                try { listState.animateScrollToItem(latinoIdx, 0) } catch (_: Exception) {}
            }
            "sub" -> {
                try { listState.animateScrollToItem(subIdx, 0) } catch (_: Exception) {}
            }
            "cdrama", "animes" -> {
                try { listState.animateScrollToItem(cdramaIdx, 0) } catch (_: Exception) {}
            }
            "movies", "peliculas" -> {
                try { listState.animateScrollToItem(peliculasIdx, 0) } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MochiDeepPlum)
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                SkeletonHomeScreen()
            }
            is HomeUiState.Error -> {
                TvConnectionErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.loadCatalog() }
                )
            }
            is HomeUiState.Success -> {
                val featured = state.featured
                val doramasEnEmision = state.doramasEnEmision
                val popularDoramas = state.popularDoramas
                val audioLatino = state.audioLatino
                val subEspanol = state.subEspanol
                val cdramasChinos = state.cdramasChinos
                val peliculasAsiaticas = state.peliculasAsiaticas
                val top10Movies = state.top10
                val collections = state.collections

                var selectedType by rememberSaveable { mutableStateOf("Todos") }
                var selectedGenre by rememberSaveable { mutableStateOf("Todos") }
                val filterBarFocusRequester = remember { FocusRequester() }

                val matchesGenre: (MovieItem) -> Boolean = { movie ->
                    if (selectedGenre == "Todos") true
                    else {
                        val keywords = when (selectedGenre) {
                            "Romance" -> listOf("Romance")
                            "Comedia" -> listOf("Comedia", "Comedy")
                            "Drama" -> listOf("Drama")
                            "Acción" -> listOf("Acción", "Accion", "Action")
                            "Fantasía" -> listOf("Fantasía", "Fantasia", "Fantasy")
                            "Ciencia Ficción" -> listOf("Ciencia ficción", "Ciencia Ficcion", "Sci-Fi", "SciFi")
                            "Misterio" -> listOf("Misterio", "Mystery")
                            "Crimen" -> listOf("Crimen", "Crime")
                            "Suspenso" -> listOf("Suspenso", "Suspense", "Thriller")
                            "Aventura" -> listOf("Aventura", "Adventure")
                            "Histórico" -> listOf("Histórico", "Historico", "Historia", "History")
                            "Familia" -> listOf("Familia", "Family")
                            "Terror" -> listOf("Terror", "Horror")
                            else -> listOf(selectedGenre)
                        }
                        movie.genres.any { g ->
                            keywords.any { kw -> g.contains(kw, ignoreCase = true) }
                        }
                    }
                }

                val isSeriesType = selectedType == "Series"
                val isMoviesType = selectedType == "Películas"

                val filteredEmision = if (!isMoviesType) doramasEnEmision.filter(matchesGenre) else emptyList()
                val filteredTop10 = if (!isMoviesType) top10Movies.filter(matchesGenre) else emptyList()
                val filteredPopulares = if (!isMoviesType) popularDoramas.filter(matchesGenre) else emptyList()
                val filteredLatino = if (!isMoviesType) audioLatino.filter(matchesGenre) else emptyList()
                val filteredSub = if (!isMoviesType) subEspanol.filter(matchesGenre) else emptyList()
                val filteredCdramas = if (!isMoviesType) cdramasChinos.filter(matchesGenre) else emptyList()
                val filteredPeliculas = if (!isSeriesType) peliculasAsiaticas.filter(matchesGenre) else emptyList()

                val heroSource = when (selectedType) {
                    "Películas" -> peliculasAsiaticas.filter(matchesGenre)
                    "Series" -> (featured + doramasEnEmision).filter { it.type == "series" }.filter(matchesGenre)
                    else -> featured.filter(matchesGenre)
                }
                val heroMovies = heroSource.take(5).ifEmpty { featured.take(5) }

                val showContinueWatching = watchHistory.isNotEmpty() && (selectedType == "Todos" || selectedType == "Series")
                val needFirstFocusOnEmision = !showContinueWatching && filteredEmision.isNotEmpty()
                val needFirstFocusOnTop10 = !showContinueWatching && filteredEmision.isEmpty() && filteredTop10.isNotEmpty()
                val needFirstFocusOnPopulares = !showContinueWatching && filteredEmision.isEmpty() && filteredTop10.isEmpty() && filteredPopulares.isNotEmpty()
                val needFirstFocusOnPeliculas = !showContinueWatching && filteredEmision.isEmpty() && filteredTop10.isEmpty() && filteredPopulares.isEmpty() && filteredPeliculas.isNotEmpty()

                val historyListState = rememberLazyListState()
                val historyCardRequesters = remember { mutableMapOf<String, FocusRequester>() }
                var lastFocusedHistoryIndex by remember { mutableIntStateOf(0) }

                val top10ListState = rememberLazyListState()
                val top10CardRequesters = remember { mutableMapOf<String, FocusRequester>() }
                var lastFocusedTop10Index by remember { mutableIntStateOf(0) }

                // Restore focus to Hero Play button ONLY if Hero was clicked
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                    if (restoreFocusUrl != null && restoreFocusRowKey == "hero") {
                        delay(100)
                        try {
                            playButtonFocusRequester.requestFocus()
                            onFocusRestored()
                        } catch (_: Exception) {}
                    }
                }

                // Restore focus to Continue Watching card if clicked
                val isHistoryTarget = (restoreFocusRowKey == "history" || (restoreFocusRowKey == null && watchHistory.any { it.movieUrl == restoreFocusUrl }))
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, watchHistory.toList()) {
                    if (restoreFocusUrl != null && isHistoryTarget && watchHistory.any { it.movieUrl == restoreFocusUrl }) {
                        val targetIdx = watchHistory.indexOfFirst { it.movieUrl == restoreFocusUrl }
                        if (targetIdx >= 0) {
                            try { historyListState.scrollToItem(targetIdx) } catch (_: Exception) {}
                        }
                    }
                }

                // Restore focus to Top 10 card if clicked
                val isTop10Target = (restoreFocusRowKey == "top10" || (restoreFocusRowKey == null && top10Movies.any { it.url == restoreFocusUrl }))
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, top10Movies) {
                    if (restoreFocusUrl != null && isTop10Target && top10Movies.any { it.url == restoreFocusUrl }) {
                        val targetIdx = top10Movies.indexOfFirst { it.url == restoreFocusUrl }
                        if (targetIdx >= 0) {
                            try { top10ListState.scrollToItem(targetIdx) } catch (_: Exception) {}
                        }
                    }
                }

                // Silent background preload for Continue Watching and Top items
                LaunchedEffect(watchHistory.toList(), top10Movies) {
                    withContext(Dispatchers.IO) {
                        watchHistory.take(4).forEach { item ->
                            try {
                                MovieRepository.preloadMovie(
                                    MovieItem(
                                        title = item.title,
                                        url = item.movieUrl,
                                        image = item.image,
                                        rating = item.rating,
                                        postId = item.postId,
                                        slug = item.slug,
                                        type = if (item.isSeries) "series" else "movie"
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                        top10Movies.take(2).forEach { m ->
                            try {
                                MovieRepository.preloadMovie(m)
                            } catch (_: Exception) {}
                        }
                    }
                }

                val isHeroScrolledDown by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
                    }
                }

                TvLazyColumn(
                    state = listState,
                    pivotOffsets = PivotOffsets(parentFraction = 0.22f),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 64.dp)
                ) {
                    // 1. FULL-BLEED CINEMATIC HERO BANNER (Edge-to-Edge)
                    item(key = "hero_banner") {
                        TvHeroBanner(
                            heroList = heroMovies,
                            onMovieSelect = { movie -> onMovieClick(movie, "hero") },
                            onFocusItem = onFocusItem,
                            firstKeyFocusRequester = playButtonFocusRequester,
                            navFocusRequester = navFocusRequester,
                            onHeroFocusChanged = { isFocused ->
                                if (isFocused) {
                                    scope.launch {
                                        try {
                                            listState.animateScrollToItem(0, 0)
                                        } catch (_: Exception) {}
                                    }
                                }
                            },
                            onDownDpad = {
                                scope.launch {
                                    try {
                                        filterBarFocusRequester.requestFocus()
                                    } catch (_: Exception) {}
                                }
                            },
                            scrollToRow = {},
                            isScrolledDown = isHeroScrolledDown
                        )
                    }

                    // 2. MODERN TV FILTER BAR
                    item(key = "home_filter_bar") {
                        TvHomeFilterBar(
                            selectedType = selectedType,
                            onTypeSelected = { selectedType = it },
                            selectedGenre = selectedGenre,
                            onGenreSelected = { selectedGenre = it },
                            firstChipFocusRequester = filterBarFocusRequester,
                            onUpDpad = {
                                try { playButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                            },
                            onDownDpad = {
                                scope.launch {
                                    if (showContinueWatching) {
                                        val targetCard = historyCardRequesters.values.firstOrNull()
                                        if (targetCard != null) {
                                            targetCard.requestFocus()
                                        } else {
                                            firstRowFocusRequester.requestFocus()
                                        }
                                    } else {
                                        firstRowFocusRequester.requestFocus()
                                    }
                                }
                            }
                        )
                    }

                    // 3. CONTINUAR VIENDO (Rendered only when history exists)
                    if (showContinueWatching) {
                        item(key = "continue_watching") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFF5F2EB))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Continuar viendo para Ti",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 18.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LazyRow(
                                    state = historyListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(310.dp)
                                        .focusGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 48.dp, top = 6.dp, bottom = 16.dp, end = 48.dp)
                                ) {
                                    itemsIndexed(items = watchHistory.toList(), key = { _, h -> "history-${h.movieUrl}" }) { idx, historyItem ->
                                        val defaultRequester = remember(historyItem.movieUrl) { FocusRequester() }
                                        val cardFocusRequester = if (idx == 0) firstRowFocusRequester else defaultRequester

                                        DisposableEffect(historyItem.movieUrl) {
                                            historyCardRequesters[historyItem.movieUrl] = cardFocusRequester
                                            onDispose {
                                                historyCardRequesters.remove(historyItem.movieUrl)
                                            }
                                        }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isHistoryTarget && restoreFocusUrl == historyItem.movieUrl) {
                                                kotlinx.coroutines.delay(60)
                                                try {
                                                    cardFocusRequester.requestFocus()
                                                    onFocusRestored()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        TvHistoryCard(
                                            historyItem = historyItem,
                                            onClick = {
                                                onMovieClick(
                                                    MovieItem(
                                                        title = historyItem.title,
                                                        url = historyItem.movieUrl,
                                                        image = historyItem.image,
                                                        rating = historyItem.rating,
                                                        postId = historyItem.postId,
                                                        slug = historyItem.slug,
                                                        type = if (historyItem.isSeries) "series" else "movie"
                                                    ),
                                                    "history"
                                                )
                                            },
                                            modifier = Modifier
                                                .focusRequester(cardFocusRequester)
                                                .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                        try {
                                                            filterBarFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } else false
                                                }
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        lastFocusedHistoryIndex = idx
                                                        onFocusItem(cardFocusRequester)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. ROW: Doramas en Emisión
                    if (filteredEmision.isNotEmpty()) {
                        item(key = "row_emision") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "Doramas en Emisión" else "Doramas de $selectedGenre en Emisión",
                                initialItems = filteredEmision,
                                rowIndex = 1,
                                onMovieSelect = { movie -> onMovieClick(movie, "Doramas en Emisión") },
                                onFocusItem = { req ->
                                    onFocusItem(req)
                                },
                                firstItemFocusRequester = if (needFirstFocusOnEmision) firstRowFocusRequester else null,
                                onUpDpad = if (needFirstFocusOnEmision) {
                                    { try { filterBarFocusRequester.requestFocus() } catch (_: Exception) {} }
                                } else null,
                                categoryType = "emision",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 5. LOS 10 MÁS POPULARES (Tendencias de TuDorama con números grandes)
                    if (filteredTop10.isNotEmpty()) {
                        item(key = "top10") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.5.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFF5F2EB))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tendencias y más populares",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 18.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LazyRow(
                                    state = top10ListState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(295.dp)
                                        .focusGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(start = 48.dp, top = 6.dp, bottom = 16.dp, end = 48.dp)
                                ) {
                                    itemsIndexed(items = filteredTop10, key = { _, movie -> "top10-${movie.url}" }) { rankIndex, movie ->
                                        val cardFocusRequester = remember(movie.url) { FocusRequester() }

                                        LaunchedEffect(movie.url, cardFocusRequester) {
                                            top10CardRequesters[movie.url] = cardFocusRequester
                                        }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isTop10Target && restoreFocusUrl == movie.url) {
                                                kotlinx.coroutines.delay(60)
                                                try {
                                                    cardFocusRequester.requestFocus()
                                                    onFocusRestored()
                                                } catch (_: Exception) {}
                                            }
                                        }

                                        TvTop10Card(
                                            movie = movie,
                                            rank = rankIndex + 1,
                                            onClick = { onMovieClick(movie, "top10") },
                                            modifier = Modifier
                                                .focusRequester(cardFocusRequester)
                                                .onPreviewKeyEvent { event ->
                                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP && needFirstFocusOnTop10) {
                                                        try {
                                                            filterBarFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } else false
                                                }
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        lastFocusedTop10Index = rankIndex
                                                        onFocusItem(cardFocusRequester)
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. ROW: Doramas Más Populares
                    if (filteredPopulares.isNotEmpty()) {
                        item(key = "row_populares") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "Doramas Más Populares" else "Doramas de $selectedGenre Más Populares",
                                initialItems = filteredPopulares,
                                rowIndex = 2,
                                onMovieSelect = { movie -> onMovieClick(movie, "Doramas Más Populares") },
                                onFocusItem = onFocusItem,
                                firstItemFocusRequester = if (needFirstFocusOnPopulares) firstRowFocusRequester else null,
                                onUpDpad = if (needFirstFocusOnPopulares) {
                                    { try { filterBarFocusRequester.requestFocus() } catch (_: Exception) {} }
                                } else null,
                                categoryType = "populares",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 7. ROW: Audio Latino
                    if (filteredLatino.isNotEmpty()) {
                        item(key = "row_audio_latino") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "Doramas en Audio Latino" else "Audio Latino - $selectedGenre",
                                initialItems = filteredLatino,
                                rowIndex = 3,
                                onMovieSelect = { movie -> onMovieClick(movie, "Doramas en Audio Latino") },
                                onFocusItem = onFocusItem,
                                categoryType = "espanol-latino",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 8. ROW: Sub Español
                    if (filteredSub.isNotEmpty()) {
                        item(key = "row_sub_espanol") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "Doramas Sub Español" else "Sub Español - $selectedGenre",
                                initialItems = filteredSub,
                                rowIndex = 4,
                                onMovieSelect = { movie -> onMovieClick(movie, "Doramas Sub Español") },
                                onFocusItem = onFocusItem,
                                categoryType = "sub-espanol",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 9. ROW: C-Dramas Chinos
                    if (filteredCdramas.isNotEmpty()) {
                        item(key = "row_cdramas") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "C-Dramas Chinos" else "C-Dramas de $selectedGenre",
                                initialItems = filteredCdramas,
                                rowIndex = 5,
                                onMovieSelect = { movie -> onMovieClick(movie, "C-Dramas Chinos") },
                                onFocusItem = onFocusItem,
                                categoryType = "cdrama",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // 10. ROW: Películas Asiáticas
                    if (filteredPeliculas.isNotEmpty()) {
                        item(key = "row_peliculas") {
                            PaginatedMovieRow(
                                title = if (selectedGenre == "Todos") "Películas Asiáticas" else "Películas de $selectedGenre",
                                initialItems = filteredPeliculas,
                                rowIndex = 6,
                                onMovieSelect = { movie -> onMovieClick(movie, "Películas Asiáticas") },
                                onFocusItem = onFocusItem,
                                firstItemFocusRequester = if (needFirstFocusOnPeliculas) firstRowFocusRequester else null,
                                onUpDpad = if (needFirstFocusOnPeliculas) {
                                    { try { filterBarFocusRequester.requestFocus() } catch (_: Exception) {} }
                                } else null,
                                categoryType = "peliculas",
                                orderBy = "latest",
                                restoreFocusUrl = restoreFocusUrl,
                                restoreFocusRowKey = restoreFocusRowKey,
                                onFocusRestored = onFocusRestored
                            )
                        }
                    }

                    // Empty filter state
                    if (filteredEmision.isEmpty() && filteredTop10.isEmpty() && filteredPopulares.isEmpty() &&
                        filteredLatino.isEmpty() && filteredSub.isEmpty() && filteredCdramas.isEmpty() && filteredPeliculas.isEmpty()) {
                        item(key = "empty_filter_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron títulos disponibles para el filtro seleccionado.",
                                    color = Color(0xFFAAAAAA),
                                    fontFamily = Inter,
                                    fontSize = 15.sp
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
fun TvConnectionErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    val retryFocusRequester = remember { FocusRequester() }
    var isRetryFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        try { retryFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MochiDeepPlum),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            // Elegant Wi-Fi Off Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF141923))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            ) {
                M3Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = Color(0xFFF5F2EB),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "SIN CONEXIÓN A INTERNET",
                color = Color(0xFFF5F2EB),
                fontSize = 24.sp,
                fontFamily = BebasNeue,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message.ifBlank { "No se pudo conectar con los servidores de Mochi. Comprueba tu conexión de red local e inténtalo de nuevo." },
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp,
                fontFamily = Inter,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.widthIn(max = 520.dp)
            )

            Spacer(modifier = Modifier.height(26.dp))

            M3Button(
                onClick = onRetry,
                colors = M3ButtonDefaults.buttonColors(
                    containerColor = if (isRetryFocused) Color(0xFFF5F2EB) else Color(0xBF0B0E14),
                    contentColor = if (isRetryFocused) Color.Black else Color(0xFFF5F2EB)
                ),
                border = if (isRetryFocused) BorderStroke(2.dp, Color(0xFFF5F2EB))
                else BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .focusRequester(retryFocusRequester)
                    .onFocusChanged { isRetryFocused = it.isFocused }
            ) {
                M3Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isRetryFocused) Color.Black else Color(0xFFF5F2EB)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REINTENTAR CONEXIÓN",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

