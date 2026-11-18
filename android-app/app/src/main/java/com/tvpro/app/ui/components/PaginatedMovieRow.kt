package com.tvpro.app.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.CatalogPageResult
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.PlusJakartaSans

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PaginatedMovieRow(
    title: String,
    initialItems: List<MovieItem>,
    rowIndex: Int = 0,
    onMovieSelect: (MovieItem) -> Unit,
    onFocusItem: (FocusRequester) -> Unit = {},
    onMovieFocused: ((MovieItem) -> Unit)? = null,
    onFocusedRowChanged: ((Int) -> Unit)? = null,
    firstItemFocusRequester: FocusRequester? = null,
    categoryType: String? = null,
    genreIds: List<Int> = emptyList(),
    orderBy: String = "latest",
    onUpDpad: (() -> Unit)? = null,
    restoreFocusUrl: String? = null,
    restoreFocusRowKey: String? = null,
    onFocusRestored: (() -> Unit)? = null
) {
    val rowListState = rememberLazyListState()
    var rowItems by remember(title, categoryType, genreIds, orderBy) { mutableStateOf(initialItems) }
    var currentPage by remember(title, categoryType, genreIds, orderBy) { mutableIntStateOf(1) }
    var totalPages by remember(title, categoryType, genreIds, orderBy) { mutableIntStateOf(20) }
    var isFetchingMore by remember(title, categoryType, genreIds, orderBy) { mutableStateOf(false) }

    // Keep rowItems synced if initialItems updates from parent
    LaunchedEffect(initialItems) {
        if (initialItems.isNotEmpty()) {
            rowItems = (rowItems + initialItems).distinctBy { it.url }
        }
    }

    // Fetch real page 1 for specific genre/orderBy row so each section has dedicated live titles immediately
    LaunchedEffect(categoryType, genreIds, orderBy) {
        if (categoryType != null && (genreIds.isNotEmpty() || orderBy != "latest")) {
            try {
                val res = MovieRepository.fetchCategoryListing(
                    categoryType = categoryType,
                    page = 1,
                    postsPerPage = 24,
                    genreIds = genreIds,
                    orderBy = orderBy
                )
                if (res.items.isNotEmpty()) {
                    rowItems = (rowItems + res.items).distinctBy {
                        it.title.replace(Regex("""\s*\(\d{4}\)"""), "").lowercase().trim()
                    }
                    totalPages = res.totalPages.coerceAtLeast(1)
                    currentPage = 1
                }
            } catch (_: Exception) {}
        }
    }

    // Infinite horizontal pagination as the user scrolls near the end of the row
    LaunchedEffect(rowListState, categoryType, genreIds, orderBy, rowItems.size, totalPages) {
        if (categoryType == null) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = rowListState.layoutInfo
            val total = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 5
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && !isFetchingMore && currentPage < totalPages) {
                isFetchingMore = true
                try {
                    val nextPage = currentPage + 1
                    val nextRes = if (categoryType == "doramasflix" || categoryType == "populares") {
                        val items = MovieRepository.getDoramasflixCatalog(nextPage, 24)
                        CatalogPageResult(items = items, totalPages = 126, currentPage = nextPage)
                    } else {
                        MovieRepository.fetchCategoryListing(
                            categoryType = categoryType,
                            page = nextPage,
                            postsPerPage = 24,
                            genreIds = genreIds,
                            orderBy = orderBy
                        )
                    }
                    if (nextRes.items.isNotEmpty()) {
                        rowItems = (rowItems + nextRes.items).distinctBy { it.url }
                        currentPage = nextPage
                        if (nextRes.totalPages > 0) totalPages = nextRes.totalPages
                    } else {
                        totalPages = currentPage
                    }
                } catch (_: Exception) {}
                isFetchingMore = false
            }
        }
    }

    val isTargetRow = (restoreFocusRowKey == null || restoreFocusRowKey == title)
    var lastFocusedIndex by remember { mutableIntStateOf(0) }

    // Scroll LazyRow to the target item if it's in this row
    LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
        if (restoreFocusUrl != null && isTargetRow && rowItems.any { it.url == restoreFocusUrl }) {
            val targetIndex = rowItems.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIndex >= 0) {
                try {
                    rowListState.scrollToItem(targetIndex)
                } catch (_: Exception) {}
            }
        }
    }

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
                text = title,
                color = Color(0xFFF5F2EB),
                fontSize = 18.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            state = rowListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 48.dp, top = 6.dp, bottom = 16.dp, end = 48.dp)
        ) {
            val visibleStart = rowListState.firstVisibleItemIndex
            val activeFocusTargetIndex = when {
                lastFocusedIndex in visibleStart..(visibleStart + 8) -> lastFocusedIndex
                else -> visibleStart
            }.coerceIn(0, (rowItems.size - 1).coerceAtLeast(0))

            itemsIndexed(items = rowItems, key = { _, movie -> "$title-${movie.url}" }) { idx, movie ->
                val defaultRequester = remember(movie.url) { FocusRequester() }
                val isFirstFocusTarget = (idx == activeFocusTargetIndex)
                val cardFocusRequester = if (isFirstFocusTarget && firstItemFocusRequester != null) firstItemFocusRequester else defaultRequester

                // When returning from Detail, if this is the clicked card, restore focus to it
                LaunchedEffect(restoreFocusUrl, restoreFocusRowKey) {
                    if (restoreFocusUrl != null && isTargetRow && restoreFocusUrl == movie.url) {
                        kotlinx.coroutines.delay(60)
                        try {
                            cardFocusRequester.requestFocus()
                            onFocusRestored?.invoke()
                        } catch (_: Exception) {}
                    }
                }

                TvMovieCard(
                    movie = movie,
                    onClick = { onMovieSelect(movie) },
                    modifier = Modifier
                        .focusRequester(cardFocusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP && onUpDpad != null) {
                                onUpDpad.invoke()
                                true
                            } else false
                        }
                        .onFocusChanged {
                            if (it.isFocused) {
                                lastFocusedIndex = idx
                                onFocusItem(cardFocusRequester)
                                onMovieFocused?.invoke(movie)
                                onFocusedRowChanged?.invoke(rowIndex)
                            }
                        }
                )
            }
        }
    }
}

