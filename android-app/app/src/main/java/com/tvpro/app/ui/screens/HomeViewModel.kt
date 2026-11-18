package com.tvpro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.api.DoramasflixApiClient
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val featured: List<MovieItem>,
        val doramasEnEmision: List<MovieItem>,
        val popularDoramas: List<MovieItem> = emptyList(),
        val audioLatino: List<MovieItem>,
        val subEspanol: List<MovieItem>,
        val cdramasChinos: List<MovieItem>,
        val peliculasAsiaticas: List<MovieItem>,
        val proximosEstrenos: List<MovieItem> = emptyList(),
        val top10: List<MovieItem>,
        val collections: List<MovieRepository.MovieCollectionSection> = emptyList(),
        val allMovies: List<MovieItem>,
        // Backwards compatibility aliases
        val doramasflixCatalog: List<MovieItem> = popularDoramas,
        val recentMovies: List<MovieItem> = peliculasAsiaticas,
        val recentSeries: List<MovieItem> = doramasEnEmision,
        val recentAnimes: List<MovieItem> = cdramasChinos,
        val dramas: List<MovieItem> = audioLatino
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Fetch live categories with guaranteed active episodes and streaming servers
                val emisionDeferred = async { try { MovieRepository.fetchCategoryListing("emision", page = 1, postsPerPage = 24, categoryId = 245) } catch (_: Exception) { null } }
                val popularDeferred = async { try { DoramasflixApiClient.getDoramas(page = 1, limit = 24) } catch (_: Exception) { emptyList() } }
                val latinoDeferred = async { try { MovieRepository.fetchCategoryListing("espanol-latino", page = 1, postsPerPage = 24, categoryId = 622) } catch (_: Exception) { null } }
                val subDeferred = async { try { MovieRepository.fetchCategoryListing("sub-espanol", page = 1, postsPerPage = 24, categoryId = 623) } catch (_: Exception) { null } }
                val cdramaDeferred = async { try { MovieRepository.fetchCategoryListing("cdrama", page = 1, postsPerPage = 24, categoryId = 526) } catch (_: Exception) { null } }
                val peliculasDeferred = async { try { MovieRepository.fetchCategoryListing("peliculas", page = 1, postsPerPage = 24, categoryId = 281) } catch (_: Exception) { null } }

                val emisionRes = emisionDeferred.await()
                val popularItems = popularDeferred.await()
                val latinoRes = latinoDeferred.await()
                val subRes = subDeferred.await()
                val cdramaRes = cdramaDeferred.await()
                val peliculasRes = peliculasDeferred.await()

                val local = MovieRepository.localCatalog

                val emisionItems = emisionRes?.items?.ifEmpty { null } ?: local.filter { it.type == "series" }
                val latinoItems = latinoRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true }
                val subItems = subRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true }
                val cdramaItems = cdramaRes?.items?.ifEmpty { null } ?: local.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } || it.title.contains("Moon", ignoreCase = true) }
                val peliculasItems = peliculasRes?.items?.ifEmpty { null } ?: local.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } }

                val combinedAll = (emisionItems + popularItems + latinoItems + subItems + cdramaItems + peliculasItems + local)
                    .distinctBy { it.title.lowercase().trim() }

                if (combinedAll.isEmpty()) {
                    _uiState.value = HomeUiState.Error("Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.")
                    return@launch
                }

                // High-resolution backdrop optimization for Featured Hero Carousel
                val allWithBackdrops = combinedAll
                    .filter { !it.backdrop.isNullOrBlank() || !it.image.isNullOrBlank() }
                    .distinctBy { it.title.lowercase().trim() }

                val featuredRaw = allWithBackdrops.take(6).ifEmpty { combinedAll.take(6) }
                val featuredDeferred = featuredRaw.map { movie ->
                    async(Dispatchers.IO) {
                        var trueBackdrop = movie.backdrop?.takeIf { it.isNotBlank() && MovieRepository.isBackdropUrl(it) }
                        var trueOverview = movie.overview?.takeIf { it.isNotBlank() }
                        if (trueBackdrop == null) {
                            try {
                                if (movie.url.contains("tudorama") || movie.url.contains("/serie/")) {
                                    MovieRepository.fetchEpisodesForSerie(movie.url)
                                    val cb = MovieRepository.getCachedBackdrop(movie.url)
                                    if (cb != null && MovieRepository.isBackdropUrl(cb)) {
                                        trueBackdrop = cb
                                    }
                                }
                                if (trueBackdrop == null) {
                                    val details = MovieRepository.extractMovie(movie)
                                    val extracted = details.gallery.firstOrNull { MovieRepository.isBackdropUrl(it) }
                                        ?: details.backdrop?.takeIf { it.isNotBlank() && MovieRepository.isBackdropUrl(it) }
                                        ?: details.backdrop?.takeIf { it.isNotBlank() }
                                    if (extracted != null) {
                                        trueBackdrop = extracted
                                    }
                                    if (trueOverview == null && !details.overview.isNullOrBlank()) {
                                        trueOverview = details.overview
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        val chosenBackdrop = (trueBackdrop ?: movie.backdrop?.takeIf { it.isNotBlank() && MovieRepository.isBackdropUrl(it) } ?: "")
                            .replace("/t/p/w500/", "/t/p/original/")
                            .replace("/t/p/w780/", "/t/p/original/")
                            .replace("/t/p/w1280/", "/t/p/original/")
                            .replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")
                        movie.copy(
                            backdrop = chosenBackdrop.ifBlank { null },
                            overview = trueOverview ?: movie.overview
                        )
                    }
                }
                val featured = featuredDeferred.awaitAll()

                val top10 = (featured + emisionItems.take(3) + popularItems.take(4) + latinoItems.take(3))
                    .distinctBy { it.title.lowercase().trim() }
                    .take(10)

                _uiState.value = HomeUiState.Success(
                    featured = featured,
                    doramasEnEmision = emisionItems,
                    popularDoramas = popularItems,
                    audioLatino = latinoItems,
                    subEspanol = subItems,
                    cdramasChinos = cdramaItems,
                    peliculasAsiaticas = peliculasItems,
                    proximosEstrenos = emptyList(),
                    top10 = top10,
                    collections = emptyList(),
                    allMovies = combinedAll
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.")
            }
        }
    }
}
