package com.tvpro.app.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.WatchHistoryManager
import com.tvpro.app.ui.components.TvMovieCard
import com.tvpro.app.ui.components.cleanHtml
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun MovieItem.getItemCategory(): String {
    return when {
        type == "movie" || type == "normal" || url.contains("/pelicula/", ignoreCase = true) -> "Películas"
        languages?.any { it.contains("Latino", ignoreCase = true) } == true -> "Audio Latino"
        genres.any { it.contains("Emisión", ignoreCase = true) } || year == "2026" -> "En Emisión"
        else -> "Doramas"
    }
}

@Composable
fun MyListScreen(
    onMovieClick: (MovieItem) -> Unit,
    navFocusRequester: FocusRequester = remember { FocusRequester() },
    firstItemFocusRequester: FocusRequester = remember { FocusRequester() },
    restoreFocusUrl: String? = null,
    onFocusRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var myList by remember { mutableStateOf(WatchHistoryManager.getMyList(context)) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var focusedMovie by remember { mutableStateOf<MovieItem?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val filterListState = rememberLazyListState()

    val firstChipFocusRequester = remember { FocusRequester() }
    val clearBtnFocusRequester = remember { FocusRequester() }
    val emptyExploreFocusRequester = remember { FocusRequester() }

    // Mochi Theme Tokens
    val deepBackground = MochiDeepPlum
    val warmIvory = MochiCream
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder
    val mutedSlate = MochiMutedLilac

    // Re-sync with storage when launched or screen regains visibility
    LaunchedEffect(Unit) {
        myList = WatchHistoryManager.getMyList(context)
        if (focusedMovie == null || myList.none { it.url == focusedMovie?.url }) {
            focusedMovie = myList.firstOrNull()
        }
    }

    // Category count statistics
    val moviesCount = remember(myList) { myList.count { it.getItemCategory() == "Películas" } }
    val doramasCount = remember(myList) { myList.count { it.getItemCategory() == "Doramas" } }
    val latinoCount = remember(myList) { myList.count { it.getItemCategory() == "Audio Latino" } }
    val emisionCount = remember(myList) { myList.count { it.getItemCategory() == "En Emisión" } }

    val availableFilters = remember(myList, moviesCount, doramasCount, latinoCount, emisionCount) {
        buildList {
            add("Todos" to myList.size)
            if (doramasCount > 0) add("Doramas" to doramasCount)
            if (emisionCount > 0) add("En Emisión" to emisionCount)
            if (latinoCount > 0) add("Audio Latino" to latinoCount)
            if (moviesCount > 0) add("Películas" to moviesCount)
        }
    }

    // Filtered items list
    val filteredList = remember(myList, selectedFilter) {
        when (selectedFilter) {
            "Películas" -> myList.filter { it.getItemCategory() == "Películas" }
            "Doramas" -> myList.filter { it.getItemCategory() == "Doramas" }
            "En Emisión" -> myList.filter { it.getItemCategory() == "En Emisión" }
            "Audio Latino" -> myList.filter { it.getItemCategory() == "Audio Latino" }
            else -> myList
        }
    }

    // Single-row chunking: 4 large widescreen cards filling remaining screen space
    val itemsPerRow = 4
    val rows = remember(filteredList) { filteredList.chunked(itemsPerRow) }
    var currentRowIndex by remember { mutableIntStateOf(0) }
    var focusedColIndex by remember { mutableIntStateOf(0) }

    val safeRowIndex = if (rows.isNotEmpty()) currentRowIndex.coerceIn(0, rows.size - 1) else 0

    // 4 focus requesters corresponding to the 4 card slots in the active row
    val cardFocusRequesters = remember { List(itemsPerRow) { FocusRequester() } }
    val firstCardFocusRequester = cardFocusRequesters[0]

    // Reset pagination to first row when changing filter
    LaunchedEffect(selectedFilter) {
        currentRowIndex = 0
        focusedColIndex = 0
    }

    // Keep focusedMovie synchronized with current row and focused column
    LaunchedEffect(filteredList, safeRowIndex, focusedColIndex) {
        if (rows.isNotEmpty() && safeRowIndex in rows.indices) {
            val itemsInRow = rows[safeRowIndex]
            val safeCol = focusedColIndex.coerceIn(0, itemsInRow.lastIndex)
            focusedMovie = itemsInRow[safeCol]
        } else {
            focusedMovie = filteredList.firstOrNull() ?: myList.firstOrNull()
        }
    }

    // Auto-focus active slot when changing row via D-pad Up/Down
    LaunchedEffect(safeRowIndex) {
        if (rows.isNotEmpty() && safeRowIndex in rows.indices) {
            val itemsInRow = rows[safeRowIndex]
            val safeCol = focusedColIndex.coerceIn(0, itemsInRow.lastIndex)
            delay(35)
            try {
                cardFocusRequesters[safeCol].requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Restore focus to previously clicked movie card when returning from DetailScreen
    LaunchedEffect(restoreFocusUrl, filteredList) {
        if (restoreFocusUrl != null && filteredList.any { it.url == restoreFocusUrl }) {
            val targetIdx = filteredList.indexOfFirst { it.url == restoreFocusUrl }
            if (targetIdx >= 0) {
                val targetRow = targetIdx / itemsPerRow
                val targetCol = targetIdx % itemsPerRow
                currentRowIndex = targetRow
                focusedColIndex = targetCol
                delay(80)
                try {
                    cardFocusRequesters[targetCol].requestFocus()
                    onFocusRestored()
                } catch (_: Exception) {}
            }
        }
    }

    val activeBackdropMovie = focusedMovie ?: filteredList.firstOrNull() ?: myList.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepBackground)
    ) {
        // =========================================================================
        // 1. DYNAMIC AMBIENT CINEMATIC BACKDROP
        // =========================================================================
        Crossfade(
            targetState = activeBackdropMovie,
            animationSpec = tween(400),
            label = "ambient_backdrop_fade"
        ) { movie ->
            if (movie != null) {
                val bgUrl = movie.backdrop?.takeIf { it.isNotBlank() } ?: movie.image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bgUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.30f)
                    )
                    // Vertical vignette fading down to deep background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x6606080C),
                                        Color(0xB306080C),
                                        deepBackground
                                    )
                                )
                            )
                    )
                    // Horizontal vignette for subtle edge shadows
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xE606080C),
                                        Color.Transparent,
                                        Color(0xE606080C)
                                    )
                                )
                            )
                    )
                }
            }
        }

        // =========================================================================
        // 2. MAIN CONTENT CONTAINER (Padded under NetflixTopNav)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 48.dp, top = 72.dp, bottom = 12.dp)
        ) {
            // --- HEADER ROW: Accent Bar + Title + Counter Badge + Row Indicator + Clear Action ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                // Vertical accent pill
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(warmIvory)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TU COLECCIÓN PERSONAL",
                    fontSize = 28.sp,
                    fontFamily = BebasNeue,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp,
                    color = warmIvory
                )
                Spacer(modifier = Modifier.width(14.dp))

                // Titles counter pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x99161B26))
                        .border(1.dp, subtleBorder, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = warmIvory,
                            modifier = Modifier.size(12.dp)
                        )
                        val countText = if (myList.size == 1) "1 TÍTULO" else "${myList.size} TÍTULOS"
                        Text(
                            text = countText,
                            color = warmIvory,
                            fontSize = 11.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Row indicator badge (if more than 1 row)
                if (rows.size > 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x99161B26))
                            .border(1.dp, subtleBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "FILA ${safeRowIndex + 1} DE ${rows.size}",
                                color = mutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            if (safeRowIndex < rows.size - 1) {
                                Text(
                                    text = "▼",
                                    color = warmIvory.copy(alpha = 0.65f),
                                    fontSize = 8.5.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // "Vaciar lista" Action Button (Only if list contains items)
                if (myList.isNotEmpty()) {
                    var isClearFocused by remember { mutableStateOf(false) }
                    val clearScaleAnim by animateFloatAsState(
                        targetValue = if (isClearFocused) 1.08f else 1.0f,
                        animationSpec = tween(120),
                        label = "clearScale"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = clearScaleAnim
                                scaleY = clearScaleAnim
                            }
                            .shadow(
                                elevation = if (isClearFocused) 10.dp else 0.dp,
                                shape = RoundedCornerShape(50),
                                spotColor = Color.White.copy(alpha = 0.5f)
                            )
                            .clip(RoundedCornerShape(50))
                            .background(if (isClearFocused) warmIvory else pillSurface)
                            .border(
                                width = if (isClearFocused) 2.dp else 1.dp,
                                color = if (isClearFocused) Color.White else subtleBorder,
                                shape = RoundedCornerShape(50)
                            )
                            .focusRequester(clearBtnFocusRequester)
                            .onFocusChanged { isClearFocused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            try {
                                                if (rows.isNotEmpty()) {
                                                    val targetCol = minOf(focusedColIndex, rows[safeRowIndex].lastIndex)
                                                    cardFocusRequesters[targetCol].requestFocus()
                                                    true
                                                } else false
                                            } catch (_: Exception) {
                                                false
                                            }
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try { firstChipFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                            showClearConfirmDialog = true
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .focusable()
                            .clickable { showClearConfirmDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Vaciar",
                                tint = if (isClearFocused) deepBackground else mutedSlate,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "VACIAR LISTA",
                                color = if (isClearFocused) deepBackground else mutedSlate,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }

            // --- FOCUSED MOVIE SPOTLIGHT INFO BAR ---
            if (activeBackdropMovie != null && myList.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = activeBackdropMovie.title.cleanHtml().uppercase(),
                        color = warmIvory,
                        fontSize = 14.5.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- FILTER CHIPS BAR (Todos, Películas, Series, Animes) ---
            if (myList.isNotEmpty() && availableFilters.size > 1) {
                LazyRow(
                    state = filterListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    itemsIndexed(availableFilters) { idx, (catName, count) ->
                        val isSelected = catName == selectedFilter
                        var isChipFocused by remember(catName) { mutableStateOf(false) }
                        val chipRequester = if (idx == 0) firstChipFocusRequester else remember(catName) { FocusRequester() }

                        val chipScaleAnim by animateFloatAsState(
                            targetValue = if (isChipFocused) 1.08f else 1.0f,
                            animationSpec = tween(durationMillis = 140),
                            label = "chipScale_$catName"
                        )

                        val chipShape = RoundedCornerShape(50)

                        Box(
                            modifier = Modifier
                                .zIndex(if (isChipFocused) 10f else 1f)
                                .graphicsLayer {
                                    scaleX = chipScaleAnim
                                    scaleY = chipScaleAnim
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
                                .focusRequester(chipRequester)
                                .then(
                                    if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                )
                                .onFocusChanged { focusState ->
                                    isChipFocused = focusState.isFocused
                                    if (focusState.isFocused) {
                                        scope.launch {
                                            try { filterListState.animateScrollToItem(idx) } catch (_: Exception) {}
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
                                                if (rows.isNotEmpty()) {
                                                    try {
                                                        val targetCol = minOf(focusedColIndex, rows[safeRowIndex].lastIndex)
                                                        cardFocusRequesters[targetCol].requestFocus()
                                                        true
                                                    } catch (_: Exception) {
                                                        false
                                                    }
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (idx == availableFilters.size - 1 && myList.isNotEmpty()) {
                                                    try { clearBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    true
                                                } else false
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                                selectedFilter = catName
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .clickable { selectedFilter = catName }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = catName.uppercase(),
                                    color = when {
                                        isChipFocused -> deepBackground
                                        isSelected -> warmIvory
                                        else -> mutedSlate
                                    },
                                    fontSize = 11.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected || isChipFocused) FontWeight.Bold else FontWeight.SemiBold,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "($count)",
                                    color = when {
                                        isChipFocused -> deepBackground.copy(alpha = 0.70f)
                                        isSelected -> warmIvory.copy(alpha = 0.70f)
                                        else -> mutedSlate.copy(alpha = 0.60f)
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. MOVIE GRID OR LUXURY EMPTY STATE
            // =========================================================================
            if (myList.isEmpty()) {
                // Entire list is empty
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, bottom = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0x80161B26))
                                .border(1.dp, subtleBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = warmIvory.copy(alpha = 0.75f),
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Text(
                            text = "TU LISTA ESTÁ VACÍA",
                            fontSize = 24.sp,
                            fontFamily = BebasNeue,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp,
                            color = warmIvory
                        )

                        Text(
                            text = "Aún no has añadido títulos a tu lista personal.\nExplora el catálogo y pulsa el botón '+ Mi lista' para guardarlos aquí.",
                            color = mutedSlate,
                            fontSize = 13.5.sp,
                            fontFamily = Inter,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.widthIn(max = 500.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        var isExploreFocused by remember { mutableStateOf(false) }
                        val btnScale by animateFloatAsState(
                            targetValue = if (isExploreFocused) 1.08f else 1.0f,
                            animationSpec = tween(120),
                            label = "exploreScale"
                        )

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = btnScale
                                    scaleY = btnScale
                                }
                                .shadow(
                                    elevation = if (isExploreFocused) 14.dp else 0.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = Color.White.copy(alpha = 0.65f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(if (isExploreFocused) warmIvory else Color(0xFF1E2433))
                                .border(
                                    width = if (isExploreFocused) 2.5.dp else 1.dp,
                                    color = if (isExploreFocused) Color.White else warmIvory.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                                .focusRequester(emptyExploreFocusRequester)
                                .focusRequester(firstItemFocusRequester)
                                .onFocusChanged { isExploreFocused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                                try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .clickable {
                                    try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (isExploreFocused) deepBackground else warmIvory,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "EXPLORAR CATÁLOGO",
                                    fontSize = 12.5.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExploreFocused) deepBackground else warmIvory
                                )
                            }
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                // Filtered category is empty
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "NO TIENES ${selectedFilter.uppercase()} EN TU LISTA",
                            fontSize = 20.sp,
                            fontFamily = BebasNeue,
                            color = warmIvory,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Cambia de filtro o agrega más títulos a tu lista.",
                            color = mutedSlate,
                            fontSize = 13.sp,
                            fontFamily = Inter
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { selectedFilter = "Todos" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2433),
                                contentColor = warmIvory
                            ),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Ver todos los títulos (${myList.size})", fontFamily = Inter, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Widescreen Single-Row Paged Display (4 large cards filling all remaining vertical & horizontal space)
                val currentRowItems = rows.getOrElse(safeRowIndex) { emptyList() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0 until itemsPerRow) {
                        if (col < currentRowItems.size) {
                            val movie = currentRowItems[col]
                            val cardRequester = cardFocusRequesters[col]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                TvMovieCard(
                                    movie = movie,
                                    onClick = { onMovieClick(movie) },
                                    fillContainer = true,
                                    onMovieFocused = {
                                        focusedMovie = it
                                        focusedColIndex = col
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .focusRequester(cardRequester)
                                        .then(
                                            if (safeRowIndex == 0 && col == 0 && availableFilters.size <= 1) {
                                                Modifier.focusRequester(firstItemFocusRequester)
                                            } else Modifier
                                        )
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.nativeKeyEvent.keyCode) {
                                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                                        if (safeRowIndex > 0) {
                                                            focusedColIndex = minOf(col, rows[safeRowIndex - 1].lastIndex)
                                                            currentRowIndex = safeRowIndex - 1
                                                            true
                                                        } else {
                                                            try {
                                                                if (availableFilters.size > 1) {
                                                                    firstChipFocusRequester.requestFocus()
                                                                } else {
                                                                    navFocusRequester.requestFocus()
                                                                }
                                                            } catch (_: Exception) {}
                                                            true
                                                        }
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                        if (safeRowIndex < rows.size - 1) {
                                                            focusedColIndex = minOf(col, rows[safeRowIndex + 1].lastIndex)
                                                            currentRowIndex = safeRowIndex + 1
                                                            true
                                                        } else {
                                                            true
                                                        }
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        if (col == 0) true else false
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                        if (col == currentRowItems.lastIndex) true else false
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                )
                            }
                        } else {
                            // Blank placeholder to keep uniform card width across rows
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 4. CONFIRMATION MODAL DIALOG: VACIAR LISTA
        // =========================================================================
        if (showClearConfirmDialog) {
            val cancelFocusRequester = remember { FocusRequester() }
            val confirmFocusRequester = remember { FocusRequester() }

            LaunchedEffect(Unit) {
                delay(80)
                try { cancelFocusRequester.requestFocus() } catch (_: Exception) {}
            }

            Dialog(onDismissRequest = { showClearConfirmDialog = false }) {
                Box(
                    modifier = Modifier
                        .width(440.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F131C))
                        .border(1.dp, subtleBorder, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "¿VACIAR MI LISTA?",
                            fontSize = 22.sp,
                            fontFamily = BebasNeue,
                            color = warmIvory,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = "Se eliminarán todos los títulos guardados (${myList.size}) de tu lista personal. Esta acción no se puede deshacer.",
                            color = mutedSlate,
                            fontSize = 13.sp,
                            fontFamily = Inter,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var isCancelFocused by remember { mutableStateOf(false) }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isCancelFocused) warmIvory else Color(0xFF1E2433))
                                    .border(
                                        width = if (isCancelFocused) 2.dp else 1.dp,
                                        color = if (isCancelFocused) Color.White else subtleBorder,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .focusRequester(cancelFocusRequester)
                                    .onFocusChanged { isCancelFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                            showClearConfirmDialog = false
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable { showClearConfirmDialog = false }
                            ) {
                                Text(
                                    text = "CANCELAR",
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCancelFocused) deepBackground else warmIvory
                                )
                            }

                            var isConfirmFocused by remember { mutableStateOf(false) }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isConfirmFocused) MochiStrawberryPink else MochiStrawberryPink.copy(alpha = 0.5f))
                                    .border(
                                        width = if (isConfirmFocused) 2.dp else 1.dp,
                                        color = if (isConfirmFocused) Color.White else MochiStrawberryPink.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .focusRequester(confirmFocusRequester)
                                    .onFocusChanged { isConfirmFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown && (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                                            WatchHistoryManager.clearMyList(context)
                                            myList = emptyList()
                                            focusedMovie = null
                                            showClearConfirmDialog = false
                                            true
                                        } else false
                                    }
                                    .focusable()
                                    .clickable {
                                        WatchHistoryManager.clearMyList(context)
                                        myList = emptyList()
                                        focusedMovie = null
                                        showClearConfirmDialog = false
                                    }
                            ) {
                                Text(
                                    text = "VACIAR TODO",
                                    fontSize = 12.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

