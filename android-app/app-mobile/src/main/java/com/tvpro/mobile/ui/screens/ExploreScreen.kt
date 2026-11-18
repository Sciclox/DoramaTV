package com.tvpro.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.mobile.ui.components.MobileFilterBar
import com.tvpro.mobile.ui.components.MobileMovieCard
import com.tvpro.mobile.ui.components.matchesGenre
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExploreScreen(
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Todo") }
    var selectedGenre by remember { mutableStateOf("Todos") }
    var selectedSort by remember { mutableStateOf("Más Recientes") }

    var catalogMovies by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var hasMorePages by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    fun loadData(reset: Boolean = false) {
        coroutineScope.launch {
            if (reset) {
                if (catalogMovies.isEmpty()) isLoading = true else isRefreshing = true
                currentPage = 1
            } else {
                isLoadingMore = true
            }

            withContext(Dispatchers.IO) {
                try {
                    val result = when (selectedCategory) {
                        "En Emisión" -> MovieRepository.fetchCategoryListing("emision", page = currentPage, postsPerPage = 24, categoryId = 245).items
                        "Más Populares", "Populares", "Doramasflix" -> MovieRepository.getDoramasflixCatalog(page = currentPage, limit = 24)
                        "Audio Latino" -> MovieRepository.fetchCategoryListing("espanol-latino", page = currentPage, postsPerPage = 24, categoryId = 622).items
                        "Sub Español" -> MovieRepository.fetchCategoryListing("sub-espanol", page = currentPage, postsPerPage = 24, categoryId = 623).items
                        "C-Dramas" -> MovieRepository.fetchCategoryListing("cdrama", page = currentPage, postsPerPage = 24, categoryId = 526).items
                        "J-Dramas" -> MovieRepository.fetchCategoryListing("jdrama", page = currentPage, postsPerPage = 24, categoryId = 720).items
                        "Películas" -> MovieRepository.fetchCategoryListing("peliculas", page = currentPage, postsPerPage = 24, categoryId = 281).items
                        else -> MovieRepository.getCatalog(currentPage).movies
                    }

                    val finalItems = result.ifEmpty {
                        if (currentPage == 1) MovieRepository.localCatalog else emptyList()
                    }

                    catalogMovies = if (reset) finalItems else catalogMovies + finalItems
                    hasMorePages = finalItems.isNotEmpty()
                } catch (e: Exception) {
                    if (reset && catalogMovies.isEmpty()) {
                        catalogMovies = MovieRepository.localCatalog
                    }
                    hasMorePages = false
                } finally {
                    isLoading = false
                    isLoadingMore = false
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        loadData(reset = true)
    }

    // Filter items based on selected genre
    val filteredMovies = remember(catalogMovies, selectedGenre, selectedSort) {
        var list = if (selectedGenre == "Todos") {
            catalogMovies
        } else {
            catalogMovies.filter { movie ->
                matchesGenre(movie.genres, selectedGenre)
            }
        }

        when (selectedSort) {
            "Mayor Puntuación TMDB" -> list.sortedByDescending { it.rating?.toDoubleOrNull() ?: 0.0 }
            "Título A-Z" -> list.sortedBy { it.title }
            "Año Descendente" -> list.sortedByDescending { it.year?.toIntOrNull() ?: 0 }
            else -> list
        }
    }

    // Pagination detection
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMorePages && !isLoadingMore && !isLoading) {
            currentPage++
            loadData(reset = false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
            .statusBarsPadding()
            .padding(top = 10.dp)
    ) {
        // Header
        Text(
            text = "Catálogo",
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.3).sp,
            color = WarmIvory,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp)
        )

        // Filters bar
        MobileFilterBar(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            selectedGenre = selectedGenre,
            onGenreSelected = { selectedGenre = it },
            selectedSort = selectedSort,
            onSortSelected = { selectedSort = it }
        )

        Spacer(modifier = Modifier.height(4.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { loadData(reset = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MochiStrawberryPink)
                }
            } else if (filteredMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No se encontraron títulos con los filtros seleccionados",
                            color = MutedSlate,
                            fontSize = 14.sp,
                            fontFamily = Inter,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedGenre = "Todos"
                                selectedSort = "Más Recientes"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MochiStrawberryPink,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Limpiar filtros", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredMovies) { movie ->
                        MobileMovieCard(
                            movie = movie,
                            fillWidth = true,
                            onClick = { onMovieClick(movie) }
                        )
                    }

                    if (isLoadingMore) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = WarmIvory,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
