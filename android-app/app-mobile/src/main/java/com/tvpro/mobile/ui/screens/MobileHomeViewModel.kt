package com.tvpro.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.domain.GetHomeCatalogUseCase
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

class MobileHomeViewModel(
    private val getHomeCatalogUseCase: GetHomeCatalogUseCase = GetHomeCatalogUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MobileHomeUiState>(MobileHomeUiState.Loading)
    val uiState: StateFlow<MobileHomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = MobileHomeUiState.Loading
            try {
                val catalog = getHomeCatalogUseCase.execute()
                _uiState.value = MobileHomeUiState.Success(
                    featured = catalog.featured,
                    top10 = catalog.top10,
                    doramasEnEmision = catalog.doramasEnEmision,
                    popularDoramas = catalog.popularDoramas,
                    doramasflixCatalog = catalog.popularDoramas,
                    audioLatino = catalog.audioLatino,
                    subEspanol = catalog.subEspanol,
                    cdramasChinos = catalog.cdramasChinos,
                    jdramasJaponeses = catalog.jdramasJaponeses,
                    peliculasAsiaticas = catalog.peliculasAsiaticas,
                    romanceComedia = catalog.romanceComedia,
                    misterioCrimen = catalog.misterioCrimen,
                    fantasiaEpicos = catalog.fantasiaEpicos,
                    proximosEstrenos = emptyList(),
                    allCatalog = catalog.allMovies
                )
            } catch (_: Exception) {
                val fallback = getHomeCatalogUseCase.getOfflineFallback()
                _uiState.value = MobileHomeUiState.Success(
                    featured = fallback.featured,
                    top10 = fallback.top10,
                    doramasEnEmision = fallback.doramasEnEmision,
                    popularDoramas = emptyList(),
                    audioLatino = fallback.audioLatino,
                    subEspanol = fallback.subEspanol,
                    cdramasChinos = fallback.cdramasChinos,
                    jdramasJaponeses = emptyList(),
                    peliculasAsiaticas = fallback.peliculasAsiaticas,
                    romanceComedia = emptyList(),
                    misterioCrimen = emptyList(),
                    fantasiaEpicos = emptyList(),
                    proximosEstrenos = emptyList(),
                    allCatalog = fallback.allMovies
                )
            }
        }
    }
}

