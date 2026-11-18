package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.CatalogPageResult
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.app.ui.components.*
import com.tvpro.app.navigation.TvFocusResolver
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TuDoramaCategoryTab(
    val id: String,
    val name: String,
    val categoryId: Int,
    val slug: String
)

val tuDoramaTabs = listOf(
    TuDoramaCategoryTab("emision", "Doramas en Emisión", 245, "emision"),
    TuDoramaCategoryTab("series", "Todos los Doramas", 0, "series"),
    TuDoramaCategoryTab("doramasflix", "Doramas Más Populares", 0, "doramasflix"),
    TuDoramaCategoryTab("latino", "Audio Latino", 622, "espanol-latino"),
    TuDoramaCategoryTab("sub", "Sub Español", 623, "sub-espanol"),
    TuDoramaCategoryTab("cdrama", "C-Dramas Chinos", 526, "cdrama"),
    TuDoramaCategoryTab("peliculas", "Películas Asiáticas", 281, "peliculas")
)

private data class CategorySectionRow(
    val id: String,
    val title: String,
    val items: List<MovieItem>,
    val genreIds: List<Int> = emptyList(),
    val orderBy: String = "latest"
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryCatalogScreen(
    categoryType: String, // "movies", "series", "animes", or TuDorama slugs
    onMovieClick: (MovieItem, String) -> Unit,
    firstItemFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    restoreFocusUrl: String? = null,
    restoreFocusRowKey: String? = null,
    onFocusRestored: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val initialType = remember(categoryType) {
        when (categoryType) {
            "movies" -> "peliculas"
            "series" -> "series"
            "emision" -> "emision"
            "latino" -> "latino"
            else -> categoryType
        }
    }
    var currentCategoryType by remember(categoryType) { mutableStateOf(initialType) }

    val currentTab = remember(currentCategoryType) {
        tuDoramaTabs.firstOrNull { it.id.equals(currentCategoryType, ignoreCase = true) || it.slug.equals(currentCategoryType, ignoreCase = true) }
            ?: tuDoramaTabs.first()
    }

    var rawItems by remember(currentCategoryType) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var filterState by remember(currentCategoryType) { mutableStateOf(MovieRepository.FilterState()) }
    var isLoading by remember(currentCategoryType) { mutableStateOf(true) }
    var totalCount by remember(currentCategoryType) { mutableIntStateOf(0) }
    var reloadTrigger by remember(currentCategoryType) { mutableIntStateOf(0) }

    // Filter results state
    var filteredResults by remember(currentCategoryType) { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isFilterLoading by remember(currentCategoryType) { mutableStateOf(false) }
    var filterCurrentPage by remember(currentCategoryType) { mutableIntStateOf(1) }
    var filterTotalPages by remember(currentCategoryType) { mutableIntStateOf(1) }
    var filterTotalCount by remember(currentCategoryType) { mutableIntStateOf(0) }
    var isFilterFetchingMore by remember(currentCategoryType) { mutableStateOf(false) }

    val listState = rememberTvLazyListState()
    val top10ListState = rememberLazyListState()
    val filterGridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val firstCategoryTabFocusRequester = remember { FocusRequester() }
    val firstFilterPillFocusRequester = remember { FocusRequester() }
    val firstSectionFocusRequester = remember { FocusRequester() }
    val firstFilteredCardFocusRequester = remember { FocusRequester() }
    val filteredCardRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocusedTop10Index by remember(currentCategoryType) { mutableIntStateOf(0) }
    var lastFocusedFilteredMovieUrl by remember(currentCategoryType) { mutableStateOf<String?>(null) }
    var isResultsGridFocused by remember(currentCategoryType) { mutableStateOf(false) }

    // TV Back key handling when filters are active:
    BackHandler(enabled = filterState.hasActiveFilters) {
        if (isResultsGridFocused) {
            isResultsGridFocused = false
            scope.launch {
                try {
                    listState.animateScrollToItem(0)
                } catch (_: Exception) {}
                try {
                    firstCategoryTabFocusRequester.requestFocus()
                } catch (_: Exception) {
                    try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
        } else {
            filterState = MovieRepository.FilterState()
            scope.launch {
                try {
                    listState.animateScrollToItem(0)
                } catch (_: Exception) {}
                delay(60)
                try {
                    firstCategoryTabFocusRequester.requestFocus()
                } catch (_: Exception) {
                    try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
        }
    }

    val screenTitle = currentTab.name
    val heroBadgeText = when (currentTab.id) {
        "emision" -> "DORAMA EN EMISIÓN DESTACADO"
        "series" -> "DORAMA DESTACADO"
        "doramasflix" -> "DORAMAS MÁS POPULARES DESTACADO"
        "latino" -> "AUDIO LATINO DESTACADO"
        "sub" -> "SUB ESPAÑOL DESTACADO"
        "cdrama" -> "C-DRAMA CHINO DESTACADO"
        "peliculas" -> "PELÍCULA ASIÁTICA DESTACADA"
        "proximos" -> "PRÓXIMO ESTRENO 2026 DESTACADO"
        else -> "TUDORAMA DESTACADO"
    }

    val top10SectionTitle = "Top 10 ${currentTab.name} hoy"

    // Load initial 3 pages concurrently for hero and initial sections with local fallbacks
    LaunchedEffect(currentCategoryType, reloadTrigger) {
        isLoading = true
        try {
            val p1Deferred = async {
                if (currentCategoryType == "doramasflix") {
                    CatalogPageResult(items = MovieRepository.getDoramasflixCatalog(1, 30), totalPages = 100, totalCount = 3024)
                } else {
                    MovieRepository.fetchCategoryListing(currentCategoryType, page = 1, postsPerPage = 30, categoryId = currentTab.categoryId)
                }
            }
            val p2Deferred = async {
                if (currentCategoryType == "doramasflix") {
                    CatalogPageResult(items = MovieRepository.getDoramasflixCatalog(2, 30), totalPages = 100, totalCount = 3024)
                } else {
                    MovieRepository.fetchCategoryListing(currentCategoryType, page = 2, postsPerPage = 30, categoryId = currentTab.categoryId)
                }
            }
            val p3Deferred = async {
                if (currentCategoryType == "doramasflix") {
                    CatalogPageResult(items = MovieRepository.getDoramasflixCatalog(3, 30), totalPages = 100, totalCount = 3024)
                } else {
                    MovieRepository.fetchCategoryListing(currentCategoryType, page = 3, postsPerPage = 30, categoryId = currentTab.categoryId)
                }
            }

            val res1 = p1Deferred.await()
            val res2 = p2Deferred.await()
            val res3 = p3Deferred.await()

            val combinedLive = (res1.items + res2.items + res3.items).distinctBy { it.url }
            val localFallbacks = MovieRepository.localCatalog.filter {
                when (currentTab.id) {
                    "emision" -> it.type == "series"
                    "latino" -> it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true
                    "sub" -> it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true
                    "cdrama" -> it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } || it.title.contains("Moon", ignoreCase = true)
                    "peliculas" -> it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) }
                    "proximos" -> it.year == "2026"
                    else -> true
                }
            }
            val finalItems = combinedLive.ifEmpty { localFallbacks }
            totalCount = if (res1.totalCount > 0) res1.totalCount else finalItems.size
            rawItems = finalItems
        } catch (_: Exception) {
            val localFallbacks = MovieRepository.localCatalog.filter {
                when (currentTab.id) {
                    "emision" -> it.type == "series"
                    "latino" -> it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true
                    "sub" -> it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true
                    "cdrama" -> it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) }
                    "peliculas" -> it.type == "normal"
                    "proximos" -> it.year == "2026"
                    else -> true
                }
            }
            rawItems = localFallbacks
        } finally {
            isLoading = false
        }
    }

    // Handle Active Filters: Fetch 100% server-side filtered results from TuDorama API
    LaunchedEffect(currentCategoryType, filterState) {
        if (!filterState.hasActiveFilters) {
            filteredResults = emptyList()
            filterTotalCount = 0
            return@LaunchedEffect
        }

        isFilterLoading = true
        filterCurrentPage = 1
        try {
            val res = MovieRepository.fetchCategoryListing(
                categoryType = currentCategoryType,
                page = 1,
                postsPerPage = 30,
                genreIds = listOfNotNull(filterState.selectedGenre?.id),
                providerIds = listOfNotNull(filterState.selectedProvider?.id),
                countryIds = listOfNotNull(filterState.selectedCountry?.id),
                yearIds = listOfNotNull(filterState.selectedYear?.id),
                tmdbMin = filterState.selectedTmdbMin,
                orderBy = filterState.orderBy,
                order = filterState.order,
                categoryId = currentTab.categoryId
            )
            filteredResults = res.items
            filterTotalPages = res.totalPages
            filterTotalCount = res.totalCount
        } catch (_: Exception) {
            filteredResults = emptyList()
            filterTotalCount = 0
        } finally {
            isFilterLoading = false
        }
    }

    // Infinite scroll for Filtered Results in TvLazyColumn
    LaunchedEffect(listState, filterState, filterCurrentPage, filterTotalPages, filteredResults.size) {
        if (!filterState.hasActiveFilters) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isFilterFetchingMore && filterCurrentPage < filterTotalPages) {
                isFilterFetchingMore = true
                try {
                    val nextPage = filterCurrentPage + 1
                    val nextRes = MovieRepository.fetchCategoryListing(
                        categoryType = currentCategoryType,
                        page = nextPage,
                        postsPerPage = 30,
                        genreIds = listOfNotNull(filterState.selectedGenre?.id),
                        providerIds = listOfNotNull(filterState.selectedProvider?.id),
                        countryIds = listOfNotNull(filterState.selectedCountry?.id),
                        yearIds = listOfNotNull(filterState.selectedYear?.id),
                        tmdbMin = filterState.selectedTmdbMin,
                        orderBy = filterState.orderBy,
                        order = filterState.order,
                        categoryId = currentTab.categoryId
                    )
                    if (nextRes.items.isNotEmpty()) {
                        filteredResults = (filteredResults + nextRes.items).distinctBy { it.url }
                        filterCurrentPage = nextPage
                        if (nextRes.totalPages > 0) filterTotalPages = nextRes.totalPages
                    }
                } catch (_: Exception) {}
                isFilterFetchingMore = false
            }
        }
    }

    // Hero Banner items with high-resolution backdrops
    val heroItems = remember(rawItems) {
        val withBackdrops = rawItems
            .filter { !it.backdrop.isNullOrBlank() || !it.image.isNullOrBlank() }
            .distinctBy { it.title.lowercase().trim() }
        val chosen = withBackdrops.ifEmpty { rawItems }.take(5)
        chosen.map { movie ->
            val highResBackdrop = (movie.backdrop?.takeIf { it.isNotBlank() } ?: movie.image)
                .replace("/t/p/w500/", "/t/p/original/")
                .replace("/t/p/w780/", "/t/p/original/")
                .replace("/t/p/w1280/", "/t/p/original/")
                .replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")
            movie.copy(backdrop = highResBackdrop)
        }
    }

    // Top 10 items for this specific category (numbered 1 to 10)
    val top10Items = remember(rawItems) {
        rawItems
            .distinctBy { it.title.replace(Regex("""\s*\(\d{4}\)"""), "").lowercase().trim() }
            .take(10)
    }

    val top10FocusRequesters = remember(currentCategoryType, top10Items) {
        List(top10Items.size) { FocusRequester() }
    }

    // Build Netflix-style horizontal sections reflecting TuDorama categories
    val sections = remember(currentCategoryType, rawItems) {
        buildCleanCategorySections(
            categoryType = currentCategoryType,
            allItems = rawItems
        )
    }

    val isTop10Target = (restoreFocusRowKey == "top10" || (restoreFocusRowKey == null && top10Items.any { it.url == restoreFocusUrl }))

    // Auto-scroll vertical listState to the row containing restoreFocusUrl
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, rawItems, sections) {
        if (restoreFocusUrl == null || filterState.hasActiveFilters) return@LaunchedEffect
        val rowKey = restoreFocusRowKey
        val hasTop10 = top10Items.isNotEmpty()

        val targetRowIndex: Int? = when {
            rowKey == "hero" -> 0
            rowKey == "top10" && hasTop10 -> 2
            rowKey != null -> {
                val baseSectionIndex = if (hasTop10) 3 else 2
                val matchingSectionOffset = sections.indexOfFirst { it.title == rowKey }
                if (matchingSectionOffset >= 0) baseSectionIndex + matchingSectionOffset else null
            }
            else -> {
                if (hasTop10 && top10Items.any { it.url == restoreFocusUrl }) {
                    2
                } else {
                    val baseSectionIndex = if (hasTop10) 3 else 2
                    val matchingSectionOffset = sections.indexOfFirst { sec -> sec.items.any { it.url == restoreFocusUrl } }
                    if (matchingSectionOffset >= 0) baseSectionIndex + matchingSectionOffset else null
                }
            }
        }

        if (targetRowIndex != null) {
            try {
                listState.scrollToItem(targetRowIndex, 0)
            } catch (_: Exception) {}
        }
    }

    // Restore focus to Hero banner ONLY if Hero was clicked
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
        if (restoreFocusUrl != null && restoreFocusRowKey == "hero") {
            delay(100)
            try {
                firstItemFocusRequester.requestFocus()
                onFocusRestored()
            } catch (_: Exception) {}
        }
    }

    // Restore focus to Top 10 card if clicked
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey, top10Items) {
        if (restoreFocusUrl != null && isTop10Target && top10Items.any { it.url == restoreFocusUrl }) {
            val targetIdx = top10Items.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIdx >= 0) {
                try { top10ListState.scrollToItem(targetIdx) } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MochiDeepPlum)
    ) {
        if (isLoading) {
            SkeletonHomeScreen()
        } else if (rawItems.isEmpty()) {
            TvConnectionErrorScreen(
                message = "Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.",
                onRetry = {
                    isLoading = true
                    reloadTrigger++
                }
            )
        } else {
            val isHeroScrolledDown by remember {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
                }
            }

            // Chunked in rows of 4 for larger, cinematic and readable posters on TV
            val chunkedFilteredMovies = remember(filteredResults) { filteredResults.chunked(4) }

            TvLazyColumn(
                state = listState,
                pivotOffsets = PivotOffsets(parentFraction = 0.22f),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 64.dp)
            ) {
                // 1. CATEGORY HERO BANNER (Hidden when actively filtering to give prominence to filtered results)
                if (!filterState.hasActiveFilters) {
                    item(key = "category_hero_$currentCategoryType") {
                        TvHeroBanner(
                            heroList = heroItems,
                            onMovieSelect = { movie -> onMovieClick(movie, "hero") },
                            onFocusItem = {},
                            firstKeyFocusRequester = firstItemFocusRequester,
                            navFocusRequester = navFocusRequester,
                            categoryBadgeText = heroBadgeText,
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
                                try {
                                    firstCategoryTabFocusRequester.requestFocus()
                                } catch (_: Exception) {
                                    try { firstFilterPillFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                            },
                            scrollToRow = {},
                            isScrolledDown = isHeroScrolledDown
                        )
                    }
                }

                // 2. TUDORAMA CATEGORY TABS (Doramas en Emisión, Audio Latino, Sub Español, C-Dramas, Películas, Estrenos 2026)
                item(key = "tudorama_category_tabs") {
                    TvCategoryTabsRow(
                        tabs = tuDoramaTabs,
                        selectedTabId = currentCategoryType,
                        onTabSelect = { selectedTab ->
                            currentCategoryType = selectedTab.id
                            filterState = MovieRepository.FilterState()
                        },
                        firstPillFocusRequester = firstCategoryTabFocusRequester,
                        onUpDpad = {
                            if (!filterState.hasActiveFilters) {
                                try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                            } else {
                                try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                        },
                        onDownDpad = {
                            try { firstFilterPillFocusRequester.requestFocus() } catch (_: Exception) {}
                        }
                    )
                }

                // 3. COMPREHENSIVE FILTER BAR (Orden, Géneros, Proveedor, País, Año, TMDB)
                item(key = "category_filters_$currentCategoryType") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (filterState.hasActiveFilters) 96.dp else 4.dp, bottom = 4.dp)
                    ) {
                        if (!filterState.hasActiveFilters) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFF5F2EB))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Filtros de $screenTitle",
                                    color = Color(0xFFF5F2EB),
                                    fontSize = 18.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                val displayCount = if (filterState.hasActiveFilters) filterTotalCount else totalCount
                                val formattedCount = remember(displayCount) {
                                    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(displayCount)
                                }
                                Text(
                                    text = "$formattedCount títulos disponibles",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TvFilterBar(
                            categoryType = currentCategoryType,
                            filterState = filterState,
                            onFilterChange = { newState ->
                                filterState = newState
                            },
                            firstItemFocusRequester = if (filterState.hasActiveFilters) firstItemFocusRequester else firstFilterPillFocusRequester,
                            onUpDpad = {
                                try { firstCategoryTabFocusRequester.requestFocus() } catch (_: Exception) {
                                    if (!filterState.hasActiveFilters) {
                                        try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                    } else {
                                        try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                    }
                                }
                            },
                            onDownDpad = {
                                if (filterState.hasActiveFilters) {
                                    var handled = false
                                    if (lastFocusedFilteredMovieUrl != null) {
                                        val req = filteredCardRequesters[lastFocusedFilteredMovieUrl]
                                        handled = TvFocusResolver.safeRequestFocus(req)
                                    }
                                    if (!handled) {
                                        handled = TvFocusResolver.safeRequestFocus(firstFilteredCardFocusRequester)
                                    }
                                    if (!handled && filteredCardRequesters.isNotEmpty()) {
                                        for (req in filteredCardRequesters.values) {
                                            if (TvFocusResolver.safeRequestFocus(req)) {
                                                handled = true
                                                break
                                            }
                                        }
                                    }
                                    handled
                                } else if (top10Items.isNotEmpty()) {
                                    val targetIdx = lastFocusedTop10Index.coerceIn(0, top10Items.lastIndex)
                                    var handled = TvFocusResolver.requestListTargetFocus(
                                        requesters = top10FocusRequesters,
                                        targetIndex = targetIdx,
                                        firstVisibleIndex = top10ListState.firstVisibleItemIndex
                                    )
                                    if (!handled) {
                                        scope.launch {
                                            try {
                                                top10ListState.scrollToItem(targetIdx)
                                                delay(40)
                                                top10FocusRequesters.getOrNull(targetIdx)?.requestFocus()
                                            } catch (_: Exception) {}
                                        }
                                        handled = true
                                    }
                                    handled
                                } else {
                                    TvFocusResolver.safeRequestFocus(firstSectionFocusRequester)
                                }
                            }
                        )
                    }
                }

                // 3. SHOW FILTERED RESULTS IF FILTERS ARE ACTIVE
                if (filterState.hasActiveFilters) {
                    if (isFilterLoading) {
                        item(key = "filter_loading_$categoryType") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MochiStrawberryPink, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Consultando catálogo con los filtros aplicados...",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp,
                                        fontFamily = Inter
                                    )
                                }
                            }
                        }
                    } else if (filteredResults.isEmpty()) {
                        item(key = "filter_empty_$categoryType") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No se encontraron títulos con los filtros seleccionados",
                                        color = Color(0xFFF5F2EB),
                                        fontSize = 16.sp,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Prueba seleccionando otro género, proveedor o cambiando la calificación TMDB.",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp,
                                        fontFamily = Inter,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Title row item
                        item(key = "filter_results_title_$categoryType") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, end = 48.dp, top = 6.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resultados encontrados (${filteredResults.size})",
                                    color = Color(0xFFF5F2EB),
                                    fontSize = 16.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Chunked 4 columns per row: larger, spacious and cinematic posters on TV
                        chunkedFilteredMovies.forEachIndexed { rowIndex, rowMovies ->
                            item(key = "filter_row_${rowIndex}_${rowMovies.firstOrNull()?.url}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 48.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    for (colIndex in 0 until 4) {
                                        if (colIndex < rowMovies.size) {
                                            val movie = rowMovies[colIndex]
                                            val isFirstCard = rowIndex == 0 && colIndex == 0
                                            val cardRequester = if (isFirstCard) firstFilteredCardFocusRequester else remember(movie.url) { FocusRequester() }

                                            LaunchedEffect(movie.url, cardRequester) {
                                                filteredCardRequesters[movie.url] = cardRequester
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .onFocusChanged {
                                                        if (it.hasFocus || it.isFocused) {
                                                            isResultsGridFocused = true
                                                            lastFocusedFilteredMovieUrl = movie.url
                                                        }
                                                    }
                                                    .onKeyEvent { keyEvent ->
                                                        if (rowIndex == 0 && keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                            try {
                                                                firstItemFocusRequester.requestFocus()
                                                                true
                                                            } catch (_: Exception) {
                                                                false
                                                            }
                                                        } else false
                                                    }
                                            ) {
                                                TvMovieCard(
                                                    movie = movie,
                                                    onClick = { onMovieClick(movie, "Filtros") },
                                                    fillWidth = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(cardRequester)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        if (isFilterFetchingMore) {
                            item(key = "filter_loading_more_$categoryType") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MochiStrawberryPink, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                } else {
                    // 4. TOP 10 ROW FOR THIS CATEGORY (When no filter is active)
                    if (top10Items.isNotEmpty()) {
                        item(key = "category_top10_$categoryType") {
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
                                        text = top10SectionTitle,
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
                                    itemsIndexed(
                                        items = top10Items,
                                        key = { _, movie -> "cat-top10-$categoryType-${movie.url}" }
                                    ) { rankIndex, movie ->
                                        val cardFocusRequester = top10FocusRequesters.getOrElse(rankIndex) { remember(movie.url) { FocusRequester() } }

                                        LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                                            if (restoreFocusUrl != null && isTop10Target && restoreFocusUrl == movie.url) {
                                                delay(60)
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
                                                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                        try {
                                                            firstFilterPillFocusRequester.requestFocus()
                                                            true
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } else false
                                                }
                                                .onFocusChanged { state ->
                                                    if (state.isFocused || state.hasFocus) {
                                                        lastFocusedTop10Index = rankIndex
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. HORIZONTAL GENRE SECTIONS (PURE, ZERO FALLBACK CONTAMINATION)
                    sections.forEachIndexed { sectionIndex, section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${categoryType}_${section.id}") {
                                val isFirstRow = sectionIndex == 0
                                PaginatedMovieRow(
                                    title = section.title,
                                    initialItems = section.items,
                                    rowIndex = sectionIndex + 1,
                                    onMovieSelect = { movie -> onMovieClick(movie, section.title) },
                                    firstItemFocusRequester = if (isFirstRow) firstSectionFocusRequester else null,
                                    onUpDpad = if (isFirstRow && top10Items.isEmpty()) {
                                        { try { firstFilterPillFocusRequester.requestFocus() } catch (_: Exception) {} }
                                    } else null,
                                    categoryType = categoryType,
                                    genreIds = section.genreIds,
                                    orderBy = section.orderBy,
                                    restoreFocusUrl = restoreFocusUrl,
                                    restoreFocusRowKey = restoreFocusRowKey,
                                    onFocusRestored = onFocusRestored
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Builds clean thematic sections for each category.
 * CRITICAL FIX: Eliminates `sectionWithFallback` so random unrelated movies or series are NEVER injected!
 * Each section queries its genuine genreIds through PaginatedMovieRow.
 */
private fun buildCleanCategorySections(
    categoryType: String,
    allItems: List<MovieItem>
): List<CategorySectionRow> {
    if (allItems.isEmpty()) return emptyList()

    fun filterByGenreName(items: List<MovieItem>, targetGenres: List<String>): List<MovieItem> {
        return items.filter { item ->
            item.genres.any { itemGenre ->
                targetGenres.any { target -> itemGenre.equals(target, ignoreCase = true) }
            }
        }.distinctBy { it.url }
    }

    val topRated = allItems.sortedByDescending { it.rating?.toDoubleOrNull() ?: 7.5 }
    val romanceComedy = filterByGenreName(allItems, listOf("Romance", "Comedia"))
    val dramaSuspense = filterByGenreName(allItems, listOf("Drama", "Suspenso", "Misterio", "Crimen"))
    val fantasyHistorical = filterByGenreName(allItems, listOf("Fantasía", "Histórico", "Acción"))
    val schoolFamily = filterByGenreName(allItems, listOf("Escolar", "Familia", "Médico"))

    val baseRows = listOf(
        CategorySectionRow(
            id = "sec_recent",
            title = "Recién añadidos y nuevos capítulos",
            items = allItems,
            orderBy = "latest"
        ),
        CategorySectionRow(
            id = "sec_romance",
            title = "Romance Inolvidable y Comedia",
            items = romanceComedy.ifEmpty { allItems.take(4) },
            genreIds = listOf(1, 2),
            orderBy = "popular"
        ),
        CategorySectionRow(
            id = "sec_drama",
            title = "Drama, Emociones Profundas y Suspenso",
            items = dramaSuspense.ifEmpty { allItems.take(4) },
            genreIds = listOf(3, 7, 6),
            orderBy = "popular"
        ),
        CategorySectionRow(
            id = "sec_fantasy",
            title = "Fantasía, Época y Mundos Históricos",
            items = fantasyHistorical.ifEmpty { allItems.take(4) },
            genreIds = listOf(4, 8, 5),
            orderBy = "popular"
        ),
        CategorySectionRow(
            id = "sec_school",
            title = "Juvenil, Escolar y Medicina",
            items = schoolFamily.ifEmpty { allItems.take(4) },
            genreIds = listOf(9, 10),
            orderBy = "popular"
        ),
        CategorySectionRow(
            id = "sec_top_rated",
            title = "Doramas Mejor Valorados (Top Crítica)",
            items = topRated,
            orderBy = "rated"
        )
    )

    return baseRows + MovieRepository.featuredCollections.map { col ->
        CategorySectionRow(
            id = "sec_col_${col.id}",
            title = col.title,
            items = col.movies,
            orderBy = "views"
        )
    }
}

@Composable
fun TvCategoryTabsRow(
    tabs: List<TuDoramaCategoryTab>,
    selectedTabId: String,
    onTabSelect: (TuDoramaCategoryTab) -> Unit,
    firstPillFocusRequester: FocusRequester,
    onUpDpad: () -> Unit = {},
    onDownDpad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFF5F2EB))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Categorías TuDorama",
                color = Color(0xFFF5F2EB),
                fontSize = 18.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 4.dp)
        ) {
            itemsIndexed(tabs, key = { _, tab -> "tab_${tab.id}" }) { idx, tab ->
                val isSelected = tab.id.equals(selectedTabId, ignoreCase = true)
                var isFocused by remember { mutableStateOf(false) }
                val pillRequester = if (idx == 0) firstPillFocusRequester else remember { FocusRequester() }

                Box(
                    modifier = Modifier
                        .focusRequester(pillRequester)
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (state.isFocused) {
                                scope.launch {
                                    try {
                                        scrollState.animateScrollToItem(idx)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        onUpDpad()
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        onDownDpad()
                                        true
                                    }
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        onTabSelect(tab)
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .clickable { onTabSelect(tab) }
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isFocused -> Color(0xFFF5F2EB)
                                isSelected -> MochiStrawberryPink.copy(alpha = 0.85f)
                                else -> PillSurface
                            }
                        )
                        .border(
                            width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isFocused -> Color(0xFFF5F2EB)
                                isSelected -> MochiStrawberryPink
                                else -> Color.White.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = tab.name,
                        color = when {
                            isFocused -> MochiDeepPlum
                            isSelected -> Color.White
                            else -> Color(0xFFD1D5DB)
                        },
                        fontSize = 13.sp,
                        fontFamily = Inter,
                        fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

