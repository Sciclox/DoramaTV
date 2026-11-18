package com.tvpro.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.core.domain.GetHomeCatalogUseCase
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

class HomeViewModel(
    private val getHomeCatalogUseCase: GetHomeCatalogUseCase = GetHomeCatalogUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val catalog = getHomeCatalogUseCase.execute()
                _uiState.value = HomeUiState.Success(
                    featured = catalog.featured,
                    doramasEnEmision = catalog.doramasEnEmision,
                    popularDoramas = catalog.popularDoramas,
                    audioLatino = catalog.audioLatino,
                    subEspanol = catalog.subEspanol,
                    cdramasChinos = catalog.cdramasChinos,
                    peliculasAsiaticas = catalog.peliculasAsiaticas,
                    proximosEstrenos = emptyList(),
                    top10 = catalog.top10,
                    collections = emptyList(),
                    allMovies = catalog.allMovies
                )
            } catch (_: Exception) {
                val fallback = getHomeCatalogUseCase.getOfflineFallback()
                _uiState.value = HomeUiState.Success(
                    featured = fallback.featured,
                    doramasEnEmision = fallback.doramasEnEmision,
                    popularDoramas = emptyList(),
                    audioLatino = fallback.audioLatino,
                    subEspanol = fallback.subEspanol,
                    cdramasChinos = fallback.cdramasChinos,
                    peliculasAsiaticas = fallback.peliculasAsiaticas,
                    proximosEstrenos = emptyList(),
                    top10 = fallback.top10,
                    collections = emptyList(),
                    allMovies = fallback.allMovies
                )
            }
        }
    }
}

