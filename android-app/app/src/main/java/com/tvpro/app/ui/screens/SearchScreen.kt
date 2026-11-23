package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.app.ui.components.TvMovieCard
import com.tvpro.app.navigation.TvFocusResolver
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onMovieClick: (MovieItem) -> Unit,
    firstItemFocusRequester: FocusRequester = remember { FocusRequester() },
    navFocusRequester: FocusRequester = remember { FocusRequester() },
    restoreFocusUrl: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var searchResults by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var defaultRecommendations by remember { mutableStateOf<List<MovieItem>>(MovieRepository.localCatalog) }
    var isSearching by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val firstChipFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }
    val firstKeyFocusRequester = remember { FocusRequester() }
    val lastRowLeftKeyFocusRequester = remember { FocusRequester() }
    val lastRowMiddleKeyFocusRequester = remember { FocusRequester() }
    val lastRowRightKeyFocusRequester = remember { FocusRequester() }
    val spaceFocusRequester = remember { FocusRequester() }
    val backspaceFocusRequester = remember { FocusRequester() }
    val clearFocusRequester = remember { FocusRequester() }
    val filtersListState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val movieFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    val categories = listOf("Todos", "En Emisión", "Doramas", "Películas", "Audio Latino", "C-Dramas", "Romance", "Comedia")

    // Ergonomic 6x6 Alpha-Numeric TV Keyboard Matrix
    val keyboardRows = listOf(
        listOf("A", "B", "C", "D", "E", "F"),
        listOf("G", "H", "I", "J", "K", "L"),
        listOf("M", "N", "O", "P", "Q", "R"),
        listOf("S", "T", "U", "V", "W", "X"),
        listOf("Y", "Z", "1", "2", "3", "4"),
        listOf("5", "6", "7", "8", "9", "0")
    )

    // Load initial recommendations on launch with safe fallback
    LaunchedEffect(Unit) {
        try {
            val catalog = MovieRepository.getCatalog(1)
            if (catalog.movies.isNotEmpty()) {
                defaultRecommendations = catalog.movies
            }
        } catch (_: Exception) {
            // Keep MovieRepository.localCatalog
        }
    }

    // Debounced search query and category filter
    LaunchedEffect(searchQuery, selectedCategory) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank() && selectedCategory == "Todos") {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        if (trimmed.length == 1 && selectedCategory == "Todos") {
            // Instant local recommendation filter for single char to protect SeriesAPI rate limits
            searchResults = defaultRecommendations.filter {
                it.title.contains(trimmed, ignoreCase = true)
            }
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        delay(450)

        try {
            if (trimmed.isNotBlank()) {
                val results = MovieRepository.searchMovies(trimmed)
                searchResults = if (selectedCategory != "Todos") {
                    results.filter { movie ->
                        when (selectedCategory) {
                            "En Emisión" -> movie.year == "2026" || movie.genres.any { it.contains("Emisión", ignoreCase = true) }
                            "Doramas" -> movie.type == "series" || movie.url.contains("/serie/") || movie.url.contains("/doramas-online/")
                            "Películas" -> movie.type == "movie" || movie.type == "normal" || movie.url.contains("/pelicula/") || movie.genres.any { it.contains("Película", ignoreCase = true) }
                            "Audio Latino" -> movie.languages?.any { it.contains("Latino", ignoreCase = true) } == true || movie.genres.any { it.contains("Latino", ignoreCase = true) }
                            "C-Dramas" -> movie.genres.any { it.contains("C-Drama", ignoreCase = true) || it.contains("Chino", ignoreCase = true) }
                            "Romance" -> movie.genres.any { it.contains("Romance", ignoreCase = true) }
                            "Comedia" -> movie.genres.any { it.contains("Comedia", ignoreCase = true) }
                            else -> true
                        }
                    }
                } else {
                    results
                }
            } else {
                searchResults = when (selectedCategory) {
                    "En Emisión" -> MovieRepository.fetchCategoryListing("emision", 1, 30, categoryId = 245).items
                    "Doramas" -> MovieRepository.fetchCategoryListing("series", 1, 30, orderBy = "popular").items
                    "Películas" -> MovieRepository.fetchCategoryListing("peliculas", 1, 30, categoryId = 281).items
                    "Audio Latino" -> MovieRepository.fetchCategoryListing("espanol-latino", 1, 30, categoryId = 622).items
                    "C-Dramas" -> MovieRepository.fetchCategoryListing("cdrama", 1, 30, categoryId = 526).items
                    "Romance" -> MovieRepository.fetchCategoryListing("series", 1, 30, genreIds = listOf(269)).items
                    "Comedia" -> MovieRepository.fetchCategoryListing("series", 1, 30, genreIds = listOf(3)).items
                    else -> defaultRecommendations
                }
            }
        } catch (_: Exception) {
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    val displayMovies = if (searchQuery.isNotBlank() || selectedCategory != "Todos") {
        searchResults
    } else {
        defaultRecommendations
    }

    var lastFocusedResultIndex by remember { mutableIntStateOf(0) }
    var lastFocusedMovieUrl by remember { mutableStateOf<String?>(null) }

    fun requestGridFocus(): Boolean {
        if (displayMovies.isEmpty()) return false
        // 1. Try last focused movie URL if registered
        if (lastFocusedMovieUrl != null) {
            val req = movieFocusRequesters[lastFocusedMovieUrl]
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        // 2. Try firstResultFocusRequester
        if (TvFocusResolver.safeRequestFocus(firstResultFocusRequester)) return true
        // 3. Try visible item in grid
        val visibleFirst = gridState.firstVisibleItemIndex
        val visibleMovie = displayMovies.getOrNull(visibleFirst)
        if (visibleMovie != null) {
            val req = movieFocusRequesters[visibleMovie.url]
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        // 4. Try any composed requester
        for (req in movieFocusRequesters.values) {
            if (TvFocusResolver.safeRequestFocus(req)) return true
        }
        // 5. Scroll to last focused or top and request
        scope.launch {
            try {
                val target = lastFocusedResultIndex.coerceIn(0, displayMovies.lastIndex)
                gridState.scrollToItem(target)
                delay(40)
                val movie = displayMovies.getOrNull(target)
                if (movie != null) movieFocusRequesters[movie.url]?.requestFocus()
                else firstResultFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
        return true
    }

    // Restore focus to previously clicked movie card in search results
    LaunchedEffect(restoreFocusUrl, displayMovies) {
        if (restoreFocusUrl != null && displayMovies.any { it.url == restoreFocusUrl }) {
            val targetIdx = displayMovies.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIdx >= 0) {
                try {
                    gridState.scrollToItem(targetIdx)
                } catch (_: Exception) {}
                delay(150)
                try {
                    movieFocusRequesters[restoreFocusUrl]?.requestFocus()
                    onFocusRestored()
                } catch (_: Exception) {}
            }
        }
    }

    // Mochi Design Tokens
    val deepBackground = MochiDeepPlum
    val warmIvory = MochiCream
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder
    val mutedSlate = MochiMutedLilac

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground)
            .padding(start = 48.dp, end = 48.dp, top = 82.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        // =========================================================================
        // LEFT PANEL: Search Input Bar + 6x6 TV Remote Keyboard + Actions
        // =========================================================================
        Column(
            modifier = Modifier.width(320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Search Input Bar
            var isSearchBoxFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSearchBoxFocused) MochiPlumCard else pillSurface)
                    .border(
                        width = if (isSearchBoxFocused) 2.dp else 1.dp,
                        color = if (isSearchBoxFocused) warmIvory else subtleBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .focusRequester(firstItemFocusRequester)
                    .onFocusChanged { isSearchBoxFocused = it.isFocused || it.hasFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            val char = event.nativeKeyEvent.unicodeChar.toChar()
                            if (char.isLetterOrDigit() || char == ' ') {
                                searchQuery += char
                                selectedCategory = "Todos"
                                return@onPreviewKeyEvent true
                            } else if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                                if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
                                return@onPreviewKeyEvent true
                            }
                            when (event.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                    try { firstKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP -> {
                                    try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    try { firstKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        try { firstKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = if (isSearchBoxFocused) warmIvory else mutedSlate,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar películas, series...",
                                color = mutedSlate,
                                fontSize = 13.5.sp,
                                fontFamily = Inter
                            )
                        } else {
                            Text(
                                text = searchQuery,
                                color = warmIvory,
                                fontSize = 14.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar texto",
                                tint = warmIvory,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Ergonomic 6x6 TV Remote Virtual Keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x800A0D14))
                    .border(1.dp, subtleBorder, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keyboardRows.forEachIndexed { rowIndex, rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowKeys.forEachIndexed { colIndex, keyChar ->
                            var isKeyFocused by remember(keyChar) { mutableStateOf(false) }
                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isKeyFocused) 1.12f else 1.0f,
                                animationSpec = tween(120),
                                label = "keyScale_$keyChar"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .zIndex(if (isKeyFocused) 10f else 1f)
                                    .graphicsLayer {
                                        scaleX = scaleAnim
                                        scaleY = scaleAnim
                                    }
                                    .shadow(
                                        elevation = if (isKeyFocused) 10.dp else 0.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        spotColor = Color.White.copy(alpha = 0.65f),
                                        ambientColor = Color.White.copy(alpha = 0.35f)
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isKeyFocused) warmIvory else pillSurface
                                    )
                                    .border(
                                        width = if (isKeyFocused) 2.dp else 1.dp,
                                        color = if (isKeyFocused) Color.White else subtleBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .then(
                                        when {
                                            rowIndex == 0 && colIndex == 0 -> Modifier.focusRequester(firstKeyFocusRequester)
                                            rowIndex == keyboardRows.size - 1 && colIndex == 0 -> Modifier.focusRequester(lastRowLeftKeyFocusRequester)
                                            rowIndex == keyboardRows.size - 1 && colIndex == 2 -> Modifier.focusRequester(lastRowMiddleKeyFocusRequester)
                                            rowIndex == keyboardRows.size - 1 && colIndex == rowKeys.size - 1 -> Modifier.focusRequester(lastRowRightKeyFocusRequester)
                                            else -> Modifier
                                        }
                                    )
                                    .onFocusChanged { isKeyFocused = it.isFocused || it.hasFocus }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            when (event.nativeKeyEvent.keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> {
                                                    if (rowIndex == 0) {
                                                        try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        true
                                                    } else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                    if (rowIndex == keyboardRows.size - 1) {
                                                        when {
                                                            colIndex in 0..2 -> {
                                                                try { spaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                            colIndex in 3..4 || searchQuery.isEmpty() -> {
                                                                try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                            else -> {
                                                                try { clearFocusRequester.requestFocus() } catch (_: Exception) {}
                                                                true
                                                            }
                                                        }
                                                    } else false
                                                }
                                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                    if (colIndex == rowKeys.size - 1) {
                                                        if (displayMovies.isNotEmpty()) {
                                                            if (!requestGridFocus()) {
                                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                            }
                                                        } else {
                                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        }
                                                        true
                                                    } else false
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        searchQuery += keyChar.lowercase()
                                        selectedCategory = "Todos"
                                    }
                            ) {
                                Text(
                                    text = keyChar,
                                    color = if (isKeyFocused) MochiDeepPlum else warmIvory,
                                    fontSize = 13.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = if (isKeyFocused) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Compact Utility Action Bar (Espacio, Borrar, Limpiar)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Espacio
                var isSpaceFocused by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSpaceFocused) warmIvory else pillSurface)
                        .border(
                            width = if (isSpaceFocused) 2.dp else 1.dp,
                            color = if (isSpaceFocused) Color.White else subtleBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(spaceFocusRequester)
                        .onFocusChanged { isSpaceFocused = it.isFocused || it.hasFocus }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        try { lastRowLeftKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> true
                                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                                    else -> false
                                }
                            } else false
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { searchQuery += " " }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SpaceBar,
                            contentDescription = "Espacio",
                            tint = if (isSpaceFocused) MochiDeepPlum else warmIvory,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Espacio",
                            fontSize = 11.5.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSpaceFocused) MochiDeepPlum else warmIvory
                        )
                    }
                }

                // Borrar (Backspace)
                var isBackspaceFocused by remember { mutableStateOf(false) }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBackspaceFocused) warmIvory else pillSurface)
                        .border(
                            width = if (isBackspaceFocused) 2.dp else 1.dp,
                            color = if (isBackspaceFocused) Color.White else subtleBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(backspaceFocusRequester)
                        .onFocusChanged { isBackspaceFocused = it.isFocused || it.hasFocus }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                        try { lastRowMiddleKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        try { spaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (searchQuery.isNotEmpty()) {
                                            try { clearFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        } else {
                                            if (displayMovies.isNotEmpty()) {
                                                if (!requestGridFocus()) {
                                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                            } else {
                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                            true
                                        }
                                    }
                                    KeyEvent.KEYCODE_DPAD_DOWN -> true
                                    else -> false
                                }
                            } else false
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Borrar",
                            tint = if (isBackspaceFocused) MochiDeepPlum else warmIvory,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Borrar",
                            fontSize = 11.5.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isBackspaceFocused) MochiDeepPlum else warmIvory
                        )
                    }
                }

                // Limpiar (Clear) - Visible when query has text
                if (searchQuery.isNotEmpty()) {
                    var isClearFocused by remember { mutableStateOf(false) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isClearFocused) warmIvory else pillSurface)
                            .border(
                                width = if (isClearFocused) 2.dp else 1.dp,
                                color = if (isClearFocused) Color.White else subtleBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .focusRequester(clearFocusRequester)
                            .onFocusChanged { isClearFocused = it.isFocused || it.hasFocus }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { lastRowRightKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            if (displayMovies.isNotEmpty()) {
                                                if (!requestGridFocus()) {
                                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                            } else {
                                                try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> true
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                searchQuery = ""
                                try { backspaceFocusRequester.requestFocus() } catch (_: Exception) {}
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = if (isClearFocused) MochiDeepPlum else warmIvory,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Limpiar",
                                fontSize = 11.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isClearFocused) MochiDeepPlum else warmIvory
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // RIGHT PANEL: Category Chips + Live Results Header + Spacious Movie Grid
        // =========================================================================
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Quick Category Filter Chips
            LazyRow(
                state = filtersListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                itemsIndexed(categories) { index, cat ->
                    val isSelected = (selectedCategory == cat && searchQuery.isBlank())
                    var isChipFocused by remember(cat) { mutableStateOf(false) }

                    val scaleAnim by animateFloatAsState(
                        targetValue = if (isChipFocused) 1.08f else 1.0f,
                        animationSpec = tween(durationMillis = 150),
                        label = "chipScale_$cat"
                    )

                    val chipShape = RoundedCornerShape(50)

                    Box(
                        modifier = Modifier
                            .zIndex(if (isChipFocused) 10f else 1f)
                            .graphicsLayer {
                                scaleX = scaleAnim
                                scaleY = scaleAnim
                            }
                            .shadow(
                                elevation = if (isChipFocused) 12.dp else 0.dp,
                                shape = chipShape,
                                spotColor = Color.White.copy(alpha = 0.65f),
                                ambientColor = Color.White.copy(alpha = 0.35f)
                            )
                            .clip(chipShape)
                            .background(
                                when {
                                    isChipFocused -> warmIvory
                                    isSelected -> Color(0xFF1E2433)
                                    else -> pillSurface
                                }
                            )
                            .border(
                                width = if (isChipFocused) 2.5.dp else 1.dp,
                                color = when {
                                    isChipFocused -> Color.White
                                    isSelected -> warmIvory.copy(alpha = 0.60f)
                                    else -> subtleBorder
                                },
                                shape = chipShape
                            )
                            .then(if (index == 0) Modifier.focusRequester(firstChipFocusRequester) else Modifier)
                            .onFocusChanged { focusState ->
                                isChipFocused = focusState.isFocused || focusState.hasFocus
                                if (focusState.isFocused || focusState.hasFocus) {
                                    scope.launch {
                                        try { filtersListState.animateScrollToItem(index) } catch (_: Exception) {}
                                    }
                                }
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (displayMovies.isNotEmpty()) {
                                                requestGridFocus()
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            if (index == 0) {
                                                try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                            selectedCategory = cat
                                            searchQuery = ""
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectedCategory = cat
                                searchQuery = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            color = when {
                                isChipFocused -> MochiDeepPlum
                                isSelected -> warmIvory
                                else -> mutedSlate
                            },
                            fontSize = 12.sp,
                            fontFamily = Inter,
                            fontWeight = if (isSelected || isChipFocused) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Results Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(warmIvory)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSearching -> "Buscando..."
                        searchQuery.isNotBlank() -> "Resultados para \"$searchQuery\""
                        selectedCategory != "Todos" -> "Películas de $selectedCategory"
                        else -> "Títulos recomendados para ti"
                    },
                    color = warmIvory,
                    fontSize = 16.sp,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                )
                if (!isSearching && displayMovies.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "(${displayMovies.size} títulos)",
                        color = mutedSlate,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. Grid of Movie Posters (Adaptive 3-4 Columns with Full Vertical Height)
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = warmIvory, strokeWidth = 2.5.dp)
                }
            } else if (displayMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = mutedSlate.copy(alpha = 0.45f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "No encontramos resultados para \"$searchQuery\""
                                selectedCategory != "Todos" -> "No encontramos títulos en \"$selectedCategory\""
                                else -> "No hay títulos disponibles"
                            },
                            color = warmIvory,
                            fontSize = 15.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Prueba con otro término o selecciona una categoría de la lista."
                                   else "Intenta seleccionar otra categoría o buscar por título.",
                            color = mutedSlate,
                            fontSize = 12.5.sp,
                            fontFamily = Inter
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 142.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayMovies, key = { idx, m -> "${m.url}_$idx" }) { index, movie ->
                        val cardRequester = if (index == 0) firstResultFocusRequester else remember(movie.url) { FocusRequester() }

                        LaunchedEffect(movie.url, cardRequester) {
                            movieFocusRequesters[movie.url] = cardRequester
                        }

                        LaunchedEffect(restoreFocusUrl) {
                            if (restoreFocusUrl != null && restoreFocusUrl == movie.url) {
                                delay(60)
                                try {
                                    cardRequester.requestFocus()
                                    onFocusRestored()
                                } catch (_: Exception) {}
                            }
                        }

                        TvMovieCard(
                            movie = movie,
                            onClick = { onMovieClick(movie) },
                            fillWidth = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(cardRequester)
                                .onFocusChanged {
                                    if (it.isFocused || it.hasFocus) {
                                        lastFocusedResultIndex = index
                                        lastFocusedMovieUrl = movie.url
                                    }
                                }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                if (index < 3) {
                                                    try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (index % 3 == 0) {
                                                    try { firstKeyFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                        )
                    }
                }
            }
        }
    }
}

