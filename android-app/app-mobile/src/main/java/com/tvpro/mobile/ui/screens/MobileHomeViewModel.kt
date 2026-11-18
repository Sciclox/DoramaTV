package com.tvpro.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.api.DoramasflixApiClient
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MobileHomeUiState {
    data object Loading : MobileHomeUiState
    data class Success(
        val featured: List<MovieItem>,
        val top10: List<MovieItem>,
        val doramasEnEmision: List<MovieItem>,
        val popularDoramas: List<MovieItem> = emptyList(),
        val audioLatino: List<MovieItem>,
        val subEspanol: List<MovieItem>,
        val cdramasChinos: List<MovieItem>,
        val jdramasJaponeses: List<MovieItem>,
        val peliculasAsiaticas: List<MovieItem>,
        val romanceComedia: List<MovieItem>,
        val misterioCrimen: List<MovieItem>,
        val fantasiaEpicos: List<MovieItem>,
        val proximosEstrenos: List<MovieItem> = emptyList(),
        val allCatalog: List<MovieItem>,
        // Backwards compatibility aliases
        val doramasflixCatalog: List<MovieItem> = popularDoramas,
        val recentMovies: List<MovieItem> = peliculasAsiaticas,
        val topRated: List<MovieItem> = top10,
        val trendingSeries: List<MovieItem> = doramasEnEmision,
        val trendingAnimes: List<MovieItem> = cdramasChinos,
        val actionMovies: List<MovieItem> = peliculasAsiaticas,
        val sciFiEpics: List<MovieItem> = fantasiaEpicos,
        val horrorThrillers: List<MovieItem> = misterioCrimen,
        val comedies: List<MovieItem> = romanceComedia,
        val dramas: List<MovieItem> = audioLatino,
        val collections: List<MovieRepository.MovieCollectionSection> = emptyList()
    ) : MobileHomeUiState
    data class Error(val message: String) : MobileHomeUiState
}

class MobileHomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<MobileHomeUiState>(MobileHomeUiState.Loading)
    val uiState: StateFlow<MobileHomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    private fun List<MovieItem>.distinctByUrl(): List<MovieItem> {
        val seen = HashSet<String>()
        return filter { movie ->
            val key = movie.url.ifBlank { movie.title }
            seen.add(key)
        }
    }

    private fun MovieItem.ratingValue(): Double {
        return rating?.toDoubleOrNull() ?: 0.0
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = MobileHomeUiState.Loading
            try {
                // Fetch TuDorama live taxonomies and Doramasflix in parallel
                val emisionDeferred = async { try { MovieRepository.fetchCategoryListing("emision", page = 1, postsPerPage = 24, categoryId = 245) } catch (_: Exception) { null } }
                val doramasflixDeferred = async { try { DoramasflixApiClient.getDoramas(page = 1, limit = 24) } catch (_: Exception) { emptyList() } }
                val latinoDeferred = async { try { MovieRepository.fetchCategoryListing("espanol-latino", page = 1, postsPerPage = 24, categoryId = 622) } catch (_: Exception) { null } }
                val subDeferred = async { try { MovieRepository.fetchCategoryListing("sub-espanol", page = 1, postsPerPage = 24, categoryId = 623) } catch (_: Exception) { null } }
                val cdramaDeferred = async { try { MovieRepository.fetchCategoryListing("cdrama", page = 1, postsPerPage = 24, categoryId = 526) } catch (_: Exception) { null } }
                val jdramaDeferred = async { try { MovieRepository.fetchCategoryListing("jdrama", page = 1, postsPerPage = 24, categoryId = 720) } catch (_: Exception) { null } }
                val peliculasDeferred = async { try { MovieRepository.fetchCategoryListing("peliculas", page = 1, postsPerPage = 24, categoryId = 281) } catch (_: Exception) { null } }

                val emisionRes = emisionDeferred.await()
                val doramasflixItems = doramasflixDeferred.await()
                val latinoRes = latinoDeferred.await()
                val subRes = subDeferred.await()
                val cdramaRes = cdramaDeferred.await()
                val jdramaRes = jdramaDeferred.await()
                val peliculasRes = peliculasDeferred.await()

                val local = MovieRepository.localCatalog

                val emisionItems = (emisionRes?.items?.ifEmpty { null } ?: local.filter { it.type == "series" }).distinctByUrl()
                val latinoItems = (latinoRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true }).distinctByUrl()
                val subItems = (subRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true }).distinctByUrl()
                val cdramaItems = (cdramaRes?.items?.ifEmpty { null } ?: local.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } || it.title.contains("Moon", ignoreCase = true) }).distinctByUrl()
                val jdramaItems = (jdramaRes?.items?.ifEmpty { null } ?: local.filter { it.genres.any { g -> g.contains("J-Drama", ignoreCase = true) } }).distinctByUrl()
                val peliculasItems = (peliculasRes?.items?.ifEmpty { null } ?: local.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } }).distinctByUrl()

                val masterCatalog = (emisionItems + doramasflixItems + latinoItems + subItems + cdramaItems + jdramaItems + peliculasItems + local).distinctByUrl()

                // High-resolution backdrop optimization for Featured Hero Carousel
                val allWithBackdrops = masterCatalog
                    .filter { !it.backdrop.isNullOrBlank() || !it.image.isNullOrBlank() }
                    .distinctByUrl()

                val featuredRaw = allWithBackdrops.take(6).ifEmpty { masterCatalog.take(6) }
                val featured = featuredRaw.map { movie ->
                    val highResBackdrop = (movie.backdrop?.takeIf { it.isNotBlank() } ?: movie.image)
                        .replace("/t/p/w500/", "/t/p/original/")
                        .replace("/t/p/w780/", "/t/p/original/")
                        .replace("/t/p/w1280/", "/t/p/original/")
                        .replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")
                    movie.copy(backdrop = highResBackdrop)
                }

                // Top 10 Monumental
                val top10List = (featured + emisionItems.take(3) + doramasflixItems.take(4) + latinoItems.take(3))
                    .distinctByUrl()
                    .take(10)

                // Thematic genre collections
                val romanceComedia = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Romance", true) || g.contains("Comedia", true) } }
                    .distinctByUrl()

                val misterioCrimen = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Misterio", true) || g.contains("Crimen", true) || g.contains("Terror", true) } }
                    .distinctByUrl()

                val fantasiaEpicos = masterCatalog
                    .filter { it.genres.any { g -> g.contains("Fantasía", true) || g.contains("Sci-Fi", true) || g.contains("Acción", true) } }
                    .distinctByUrl()

                _uiState.value = MobileHomeUiState.Success(
                    featured = featured,
                    top10 = top10List,
                    doramasEnEmision = emisionItems,
                    popularDoramas = doramasflixItems,
                    doramasflixCatalog = doramasflixItems,
                    audioLatino = latinoItems,
                    subEspanol = subItems,
                    cdramasChinos = cdramaItems,
                    jdramasJaponeses = jdramaItems,
                    peliculasAsiaticas = peliculasItems,
                    romanceComedia = romanceComedia,
                    misterioCrimen = misterioCrimen,
                    fantasiaEpicos = fantasiaEpicos,
                    proximosEstrenos = emptyList(),
                    allCatalog = masterCatalog
                )
            } catch (e: Exception) {
                // Fallback to local offline catalog
                val fallback = MovieRepository.localCatalog
                val fallbackEmision = fallback.filter { it.type == "series" }
                val fallbackLatino = fallback.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true }
                val fallbackSub = fallback.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true }
                val fallbackCdrama = fallback.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } }
                val fallbackPeliculas = fallback.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } }

                _uiState.value = MobileHomeUiState.Success(
                    featured = fallback.take(5),
                    top10 = fallback.take(10),
                    doramasEnEmision = fallbackEmision,
                    audioLatino = fallbackLatino,
                    subEspanol = fallbackSub,
                    cdramasChinos = fallbackCdrama,
                    jdramasJaponeses = emptyList(),
                    peliculasAsiaticas = fallbackPeliculas,
                    romanceComedia = fallback.filter { it.genres.any { g -> g.contains("Romance", true) } },
                    misterioCrimen = fallback.filter { it.genres.any { g -> g.contains("Misterio", true) } },
                    fantasiaEpicos = fallback.filter { it.genres.any { g -> g.contains("Fantasía", true) } },
                    proximosEstrenos = fallback.filter { it.year == "2026" },
                    allCatalog = fallback
                )
            }
        }
    }
}
