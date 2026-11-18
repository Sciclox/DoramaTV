package com.tvpro.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.mobile.ui.components.*
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onMovieClick: (MovieItem) -> Unit,
    onPlayMovie: (MovieItem) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: MobileHomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedCategory by remember { mutableStateOf("Todo") }
    var selectedGenre by remember { mutableStateOf("Todos") }
    var selectedSort by remember { mutableStateOf("Más Recientes") }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val watchHistory = remember { mutableStateListOf<WatchHistoryItem>() }

    fun refreshHistory() {
        watchHistory.clear()
        watchHistory.addAll(WatchHistoryManager.getHistory(context))
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
    ) {
        when (val state = uiState) {
            is MobileHomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WarmIvory)
                }
            }

            is MobileHomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MutedSlate,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadCatalog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarmIvory,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reintentar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is MobileHomeUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        coroutineScope.launch {
                            isRefreshing = true
                            viewModel.loadCatalog()
                            refreshHistory()
                            kotlinx.coroutines.delay(650)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // Top Bar (Category Filter Pills, Genre Dropdown, Sort Dropdown & Search)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 10.dp, bottom = 4.dp)
                        ) {
                            MobileFilterBar(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { 
                                    selectedCategory = it
                                    selectedGenre = "Todos"
                                },
                                selectedGenre = selectedGenre,
                                onGenreSelected = { selectedGenre = it },
                                selectedSort = selectedSort,
                                onSortSelected = { selectedSort = it },
                                onSearchClick = onSearchClick
                            )
                        }
                    }

                    // Mode 1: Genre Filter Active (When user selects a specific genre)
                    if (selectedGenre != "Todos") {
                        val filteredItems = state.allCatalog
                            .filter { matchesGenre(it.genres, selectedGenre) }
                            .applySort(selectedSort)

                        item {
                            // Active Filter Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "GÉNERO SELECCIONADO",
                                        color = MutedSlate,
                                        fontFamily = Inter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "$selectedGenre (${filteredItems.size} Títulos)",
                                        color = WarmIvory,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        letterSpacing = (-0.2).sp
                                    )
                                }
                                IconButton(
                                    onClick = { selectedGenre = "Todos" },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MochiPlumCard)
                                        .border(1.dp, SubtleBorder, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Limpiar filtro",
                                        tint = SoftSilver,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (filteredItems.isNotEmpty()) {
                            // Filtered Top Highlights
                            val filteredTop = filteredItems.filter { (it.rating?.toDoubleOrNull() ?: 0.0) >= 8.0 }
                            if (filteredTop.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Lo Mejor en $selectedGenre",
                                        items = filteredTop,
                                        badgeText = "★ DESTACADOS",
                                        subtitle = "Las producciones mejor valoradas del género",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Filtered Movies
                            val genreMovies = filteredItems.filter { it.type != "series" && it.type != "anime" && it.type != "drama" }
                            if (genreMovies.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Películas de $selectedGenre",
                                        items = genreMovies,
                                        badgeText = "PELÍCULAS",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Filtered Doramas & Series
                            val genreSeries = filteredItems.filter { it.type == "series" || it.type == "anime" || it.type == "drama" }
                            if (genreSeries.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Doramas & Series de $selectedGenre",
                                        items = genreSeries,
                                        badgeText = "DORAMAS",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No se encontraron títulos en $selectedGenre",
                                            color = SoftSilver,
                                            fontFamily = Inter,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { selectedGenre = "Todos" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = WarmIvory,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Text("Ver todo el catálogo", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mode 2: Standard Curated Feed (Netflix / Max Style Layout)
                    else {
                        // 1. Hero Banner (Adaptive to category)
                        val heroList = when (selectedCategory) {
                            "En Emisión" -> state.doramasEnEmision.take(5).ifEmpty { state.featured }
                            "Audio Latino" -> state.audioLatino.take(5).ifEmpty { state.featured }
                            "Sub Español" -> state.subEspanol.take(5).ifEmpty { state.featured }
                            "C-Dramas" -> state.cdramasChinos.take(5).ifEmpty { state.featured }
                            "J-Dramas" -> state.jdramasJaponeses.take(5).ifEmpty { state.featured }
                            "Películas" -> state.peliculasAsiaticas.take(5).ifEmpty { state.featured }
                            else -> state.featured
                        }

                        if (heroList.isNotEmpty()) {
                            item {
                                val currentHero = heroList.first()
                                var isInMyList by remember(currentHero.url) {
                                    mutableStateOf(WatchHistoryManager.isInMyList(context, currentHero.url))
                                }

                                MobileHeroBanner(
                                    heroList = heroList,
                                    isInMyList = isInMyList,
                                    onPlayClick = { movie -> onPlayMovie(movie) },
                                    onDetailClick = { movie -> onMovieClick(movie) },
                                    onToggleMyList = { movie ->
                                        isInMyList = WatchHistoryManager.toggleMyList(context, movie)
                                    }
                                )
                            }
                        }

                        // 2. Continuar Viendo Row (Filtered by category if selected)
                        val filteredHistory = when (selectedCategory) {
                            "Películas" -> watchHistory.filter { !it.isSeries }
                            "Series" -> watchHistory.filter { it.isSeries && !it.movieUrl.contains("/animes/") }
                            "Animes" -> watchHistory.filter { it.movieUrl.contains("/animes/") }
                            else -> watchHistory
                        }
                        if (filteredHistory.isNotEmpty()) {
                            item {
                                MobileContinueWatchingSection(
                                    history = filteredHistory,
                                    onPlayMovie = onPlayMovie
                                )
                            }
                        }

                        // === SECTIONS FOR: "TODO" (Comprehensive Master Feed) ===
                        if (selectedCategory == "Todo") {
                            // Section 1: Top 10 Hoy en Mochi (Monumental Numbers Row)
                            if (state.top10.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Hoy en Mochi",
                                        items = state.top10.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "EN TENDENCIA",
                                        subtitle = "Lo más reproducido en las últimas 24 horas"
                                    )
                                }
                            }

                            // Section 2: Doramas en Emisión
                            if (state.doramasEnEmision.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Doramas en Emisión",
                                        items = state.doramasEnEmision.applySort(selectedSort),
                                        badgeText = "NUEVOS CAPÍTULOS",
                                        subtitle = "Estrenos semanales en simultáneo con Corea",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 2.5: Doramas Más Populares
                            if (state.popularDoramas.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Doramas Más Populares",
                                        items = state.popularDoramas.applySort(selectedSort),
                                        badgeText = "POPULARES",
                                        subtitle = "Los doramas más aclamados y vistos de todos los tiempos",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 3: Doramas en Español Latino
                            if (state.audioLatino.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Doramas en Español Latino",
                                        items = state.audioLatino.applySort(selectedSort),
                                        badgeText = "AUDIO LATINO",
                                        subtitle = "Doblajes oficiales para disfrutar en tu idioma",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 4: Doramas Sub Español
                            if (state.subEspanol.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Doramas Sub Español",
                                        items = state.subEspanol.applySort(selectedSort),
                                        badgeText = "SUB ESPAÑOL",
                                        subtitle = "Capítulos con audio original y subtítulos en español",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 5: C-Dramas Chinos Imperdibles
                            if (state.cdramasChinos.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "C-Dramas Chinos Imperdibles",
                                        items = state.cdramasChinos.applySort(selectedSort),
                                        badgeText = "C-DRAMA",
                                        subtitle = "Superproducciones chinas, romances y fantasía",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 6: J-Dramas Japoneses Populares
                            if (state.jdramasJaponeses.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "J-Dramas Japoneses Populares",
                                        items = state.jdramasJaponeses.applySort(selectedSort),
                                        badgeText = "J-DRAMA",
                                        subtitle = "Historias cautivadoras de la televisión nipona",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 7: Películas Asiáticas Destacadas
                            if (state.peliculasAsiaticas.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Películas Asiáticas Destacadas",
                                        items = state.peliculasAsiaticas.applySort(selectedSort),
                                        badgeText = "CINE ASIÁTICO",
                                        subtitle = "Lo más premiado de Corea, Japón y China",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 8: Romance & Comedia Romántica
                            if (state.romanceComedia.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Romance & Comedia Romántica",
                                        items = state.romanceComedia.applySort(selectedSort),
                                        badgeText = "AMOR & RISAS",
                                        subtitle = "Historias inolvidables para reír y enamorarse",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 9: Misterio, Crimen & Thriller Coreano
                            if (state.misterioCrimen.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Misterio, Crimen & Thriller",
                                        items = state.misterioCrimen.applySort(selectedSort),
                                        badgeText = "ALTA TENSIÓN",
                                        subtitle = "Giros inesperados, investigaciones policiales y suspenso",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }

                            // Section 10: Fantasía, Wuxia & Épicos
                            if (state.fantasiaEpicos.isNotEmpty()) {
                                item {
                                    MobileRowSection(
                                        title = "Fantasía, Wuxia & Épicos",
                                        items = state.fantasiaEpicos.applySort(selectedSort),
                                        badgeText = "MUNDOS MÁGICOS",
                                        subtitle = "Viajes en el tiempo, reencarnaciones y batallas legendarias",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // === SECTIONS FOR: "EN EMISIÓN" ===
                        else if (selectedCategory == "En Emisión") {
                            val emisionItems = state.doramasEnEmision.ifEmpty { state.allCatalog.filter { it.type == "series" } }
                            if (emisionItems.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 en Emisión",
                                        items = emisionItems.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "EN VIVO",
                                        subtitle = "Capítulos de estreno más seguidos de la semana"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "Todos los Doramas en Emisión",
                                        items = emisionItems.applySort(selectedSort),
                                        badgeText = "SEMANALES",
                                        subtitle = "Nuevos episodios cada semana",
                                        onMovieClick = onMovieClick
                                    )
                                }
                                val romEmision = emisionItems.filter { it.genres.any { g -> g.contains("Romance", true) } }
                                if (romEmision.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Romance en Emisión",
                                            items = romEmision.applySort(selectedSort),
                                            badgeText = "ROMANCE",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                                val thrillerEmision = emisionItems.filter { it.genres.any { g -> g.contains("Crimen", true) || g.contains("Misterio", true) } }
                                if (thrillerEmision.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Suspenso en Emisión",
                                            items = thrillerEmision.applySort(selectedSort),
                                            badgeText = "SUSPENSO",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        // === SECTIONS FOR: "AUDIO LATINO" ===
                        else if (selectedCategory == "Audio Latino") {
                            val latinoItems = state.audioLatino.ifEmpty { state.allCatalog.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true } }
                            if (latinoItems.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 en Español Latino",
                                        items = latinoItems.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "DOBLAJE",
                                        subtitle = "Los doramas en latino más reproducidos"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "Catálogo Completo en Latino",
                                        items = latinoItems.applySort(selectedSort),
                                        badgeText = "AUDIO LATINO",
                                        subtitle = "Sin subtítulos, directo en español",
                                        onMovieClick = onMovieClick
                                    )
                                }
                                val romLatino = latinoItems.filter { it.genres.any { g -> g.contains("Romance", true) } }
                                if (romLatino.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Romances Doblados al Latino",
                                            items = romLatino.applySort(selectedSort),
                                            badgeText = "AMOR",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        // === SECTIONS FOR: "SUB ESPAÑOL" ===
                        else if (selectedCategory == "Sub Español") {
                            val subItems = state.subEspanol.ifEmpty { state.allCatalog.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true } }
                            if (subItems.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Sub Español",
                                        items = subItems.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "SUBTITULADOS",
                                        subtitle = "Con su audio original coreano"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "Doramas Subtitulados al Español",
                                        items = subItems.applySort(selectedSort),
                                        badgeText = "SUBTITULADO",
                                        subtitle = "Traducciones precisas y en alta definición",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // === SECTIONS FOR: "C-DRAMAS" ===
                        else if (selectedCategory == "C-Dramas") {
                            val cdramaItems = state.cdramasChinos.ifEmpty { state.allCatalog.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } } }
                            if (cdramaItems.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 C-Dramas Chinos",
                                        items = cdramaItems.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "C-DRAMA",
                                        subtitle = "Las mejores series de China"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "C-Dramas Recomendados",
                                        items = cdramaItems.applySort(selectedSort),
                                        badgeText = "POPULARES",
                                        onMovieClick = onMovieClick
                                    )
                                }
                                val wuxiaItems = cdramaItems.filter { it.genres.any { g -> g.contains("Fantasía", true) || g.contains("Histór", true) } }
                                if (wuxiaItems.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Fantasía Histórica & Wuxia",
                                            items = wuxiaItems.applySort(selectedSort),
                                            badgeText = "WUXIA",
                                            subtitle = "Vestuarios majestuosos, artes marciales e inmortales",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        // === SECTIONS FOR: "J-DRAMAS" ===
                        else if (selectedCategory == "J-Dramas") {
                            val jdramaItems = state.jdramasJaponeses.ifEmpty { state.allCatalog.filter { it.genres.any { g -> g.contains("J-Drama", ignoreCase = true) } } }
                            if (jdramaItems.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 J-Dramas Japoneses",
                                        items = jdramaItems.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "J-DRAMA",
                                        subtitle = "Dramas de la televisión japonesa"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "Dramas Japoneses Destacados",
                                        items = jdramaItems.applySort(selectedSort),
                                        badgeText = "JAPÓN",
                                        onMovieClick = onMovieClick
                                    )
                                }
                            }
                        }

                        // === SECTIONS FOR: "PELÍCULAS" ===
                        else if (selectedCategory == "Películas") {
                            val movieCatalog = state.peliculasAsiaticas.ifEmpty { state.allCatalog.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } } }
                            if (movieCatalog.isNotEmpty()) {
                                item {
                                    MobileTop10Section(
                                        title = "Top 10 Películas Asiáticas",
                                        items = movieCatalog.applySort(selectedSort),
                                        onMovieClick = onMovieClick,
                                        badgeText = "CINE ASIÁTICO",
                                        subtitle = "Las películas más vistas"
                                    )
                                }
                                item {
                                    MobileRowSection(
                                        title = "Películas Recién Añadidas",
                                        items = movieCatalog.applySort(selectedSort),
                                        badgeText = "4K UHD",
                                        subtitle = "Últimos estrenos de cine asiático",
                                        onMovieClick = onMovieClick
                                    )
                                }
                                val actionMovies = movieCatalog.filter { it.genres.any { g -> g.contains("Acción", true) } }
                                if (actionMovies.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Acción & Artes Marciales",
                                            items = actionMovies.applySort(selectedSort),
                                            badgeText = "ACCIÓN",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                                val romanceMovies = movieCatalog.filter { it.genres.any { g -> g.contains("Romance", true) || g.contains("Drama", true) } }
                                if (romanceMovies.isNotEmpty()) {
                                    item {
                                        MobileRowSection(
                                            title = "Romance & Drama en Cine",
                                            items = romanceMovies.applySort(selectedSort),
                                            badgeText = "DRAMA",
                                            onMovieClick = onMovieClick
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom padding for bottom navigation bar
                        item {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }
}
}

private fun List<MovieItem>.applySort(sortOption: String): List<MovieItem> {
    return when (sortOption) {
        "Mayor Puntuación TMDB" -> sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }
        "Año Descendente" -> sortedByDescending { it.year?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0 }
        "Título A-Z" -> sortedBy { it.title.lowercase() }
        else -> this // "Más Recientes" - preserve curated placement
    }
}

@Composable
fun MobileRowSection(
    title: String,
    items: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = title,
            badgeText = badgeText,
            subtitle = subtitle,
            actionText = actionText,
            onActionClick = onActionClick
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = items,
                key = { it.url.ifBlank { it.title } }
            ) { movie ->
                MobileMovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileTop10Section(
    title: String,
    items: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = "EN TENDENCIA",
    subtitle: String? = "Lo más reproducido hoy en Mochi"
) {
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = title,
            badgeText = badgeText,
            subtitle = subtitle
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = items.take(10),
                key = { _, item -> item.url.ifBlank { item.title } }
            ) { index, movie ->
                MobileTop10Card(
                    movie = movie,
                    rank = index + 1,
                    onClick = { onMovieClick(movie) }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileContinueWatchingSection(
    history: List<WatchHistoryItem>,
    onPlayMovie: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        MobileSectionHeader(
            title = "Continuar Viendo",
            badgeText = "${history.size}",
            subtitle = "Retoma donde lo dejaste"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = history,
                key = { it.movieUrl }
            ) { historyItem ->
                MobileContinueWatchingCard(
                    item = historyItem,
                    onClick = {
                        val movieItem = MovieItem(
                            title = historyItem.title,
                            url = historyItem.movieUrl,
                            image = historyItem.image,
                            rating = historyItem.rating,
                            postId = historyItem.postId,
                            slug = historyItem.slug,
                            type = if (historyItem.isSeries) "series" else "movie"
                        )
                        onPlayMovie(movieItem)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun MobileSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = title,
                    color = WarmIvory,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.5.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 2,
                    lineHeight = 22.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MochiPlumCard)
                            .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = SoftSilver,
                            fontFamily = Inter,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionText,
                    color = SoftSilver,
                    fontFamily = Inter,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = MutedSlate,
                fontFamily = Inter,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
fun MobileContinueWatchingCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (item.durationMs > 0) {
        (item.playbackPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val seriesInfo = remember(item) { extractSeriesInfo(item) }

    Column(
        modifier = modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, SubtleBorder), RoundedCornerShape(8.dp))
                .background(TvProCardBg)
        ) {
            AsyncImage(
                model = item.image,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            )

            // Play icon in center
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color(0x99130D19))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Continuar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Series Badge if applicable
            if (seriesInfo != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = seriesInfo.formattedLabel,
                        color = WarmIvory,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter
                    )
                }
            }

            // Progress bar at bottom (Matching TV version: Warm Ivory on translucent track)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(WarmIvory)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = item.title.cleanTitle(),
            color = WarmIvory,
            fontFamily = Inter,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
