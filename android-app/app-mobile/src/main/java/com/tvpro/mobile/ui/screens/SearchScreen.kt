package com.tvpro.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.mobile.ui.components.MobileMovieCard
import com.tvpro.mobile.ui.theme.*
import com.tvpro.mobile.util.SearchHistoryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val SEARCH_BAR_SHAPE = RoundedCornerShape(12.dp)

@Composable
fun SearchScreen(
    onMovieClick: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val localContext = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var recentQueries by remember { mutableStateOf(SearchHistoryManager.getRecentQueries(localContext)) }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    fun performSearch(text: String, saveToHistory: Boolean = false) {
        searchJob?.cancel()
        if (text.isBlank()) {
            results = emptyList()
            isSearching = false
            return
        }
        if (saveToHistory) {
            SearchHistoryManager.addQuery(localContext, text)
            recentQueries = SearchHistoryManager.getRecentQueries(localContext)
        }
        searchJob = coroutineScope.launch {
            delay(280)
            isSearching = true
            try {
                results = MovieRepository.searchMovies(text)
            } catch (e: Exception) {
                results = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg)
            .statusBarsPadding()
            .padding(top = 10.dp)
    ) {
        // Search Header Bar
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                performSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 6.dp),
            shape = SEARCH_BAR_SHAPE,
            placeholder = {
                Text(
                    text = "Buscar doramas, películas, actores, géneros...",
                    color = MutedSlate,
                    fontSize = 13.5.sp,
                    fontFamily = Inter
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MutedSlate,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                performSearch(query, saveToHistory = true)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PillSurface,
                unfocusedContainerColor = PillSurface,
                focusedBorderColor = MochiStrawberryPink,
                unfocusedBorderColor = SubtleBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MochiStrawberryPink
            )
        )

        // Empty state: Recent queries
        if (query.isBlank() && recentQueries.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                // Recent Searches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Búsquedas recientes",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = (-0.2).sp,
                        color = WarmIvory
                    )
                    TextButton(
                        onClick = {
                            SearchHistoryManager.clearAll(localContext)
                            recentQueries = emptyList()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Borrar todo",
                            color = SoftSilver,
                            fontSize = 11.5.sp,
                            fontFamily = Inter
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(recentQueries) { recentTag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PillSurface)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    query = recentTag
                                    performSearch(recentTag, saveToHistory = true)
                                }
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = recentTag,
                                    color = WarmIvory,
                                    fontSize = 12.sp,
                                    fontFamily = Inter
                                )
                                IconButton(
                                    onClick = {
                                        SearchHistoryManager.removeQuery(localContext, recentTag)
                                        recentQueries = SearchHistoryManager.getRecentQueries(localContext)
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Eliminar",
                                        tint = MutedSlate,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WarmIvory)
            }
        } else if (query.isNotBlank() && results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No encontramos resultados para \"$query\"",
                    color = MutedSlate,
                    fontSize = 14.sp,
                    fontFamily = Inter,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (results.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { movie ->
                    MobileMovieCard(
                        movie = movie,
                        fillWidth = true,
                        onClick = {
                            SearchHistoryManager.addQuery(localContext, query)
                            onMovieClick(movie)
                        }
                    )
                }
            }
        }
    }
}
