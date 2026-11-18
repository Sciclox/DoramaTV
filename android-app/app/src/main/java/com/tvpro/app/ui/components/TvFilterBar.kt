package com.tvpro.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.tvpro.core.data.repository.MovieRepository
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ActiveFilterDialog {
    NONE,
    SORT,
    GENRE,
    PROVIDER,
    COUNTRY,
    YEAR,
    TMDB
}

@Composable
fun TvFilterBar(
    categoryType: String, // "movies", "series", "animes"
    filterState: MovieRepository.FilterState,
    onFilterChange: (MovieRepository.FilterState) -> Unit,
    firstItemFocusRequester: FocusRequester,
    onUpDpad: () -> Unit = {},
    onDownDpad: () -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeDialog by remember { mutableStateOf(ActiveFilterDialog.NONE) }
    var lastOpenedDialog by remember { mutableStateOf<ActiveFilterDialog?>(null) }

    val genreFocusRequester = remember { FocusRequester() }
    val providerFocusRequester = remember { FocusRequester() }
    val countryFocusRequester = remember { FocusRequester() }
    val yearFocusRequester = remember { FocusRequester() }
    val tmdbFocusRequester = remember { FocusRequester() }

    // When any filter dialog closes, immediately restore focus to that specific filter pill
    // instead of allowing focus to escape to the top navigation bar or search button
    LaunchedEffect(activeDialog) {
        if (activeDialog == ActiveFilterDialog.NONE && lastOpenedDialog != null) {
            val target = when (lastOpenedDialog) {
                ActiveFilterDialog.SORT -> firstItemFocusRequester
                ActiveFilterDialog.GENRE -> genreFocusRequester
                ActiveFilterDialog.PROVIDER -> providerFocusRequester
                ActiveFilterDialog.COUNTRY -> countryFocusRequester
                ActiveFilterDialog.YEAR -> yearFocusRequester
                ActiveFilterDialog.TMDB -> tmdbFocusRequester
                else -> null
            }
            lastOpenedDialog = null
            delay(50)
            try {
                target?.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val genreList = remember(categoryType) {
        when (categoryType) {
            "series" -> MovieRepository.seriesGenres
            "animes" -> MovieRepository.animeGenres
            else -> MovieRepository.movieGenres
        }
    }

    val sortLabel = remember(filterState.orderBy) {
        MovieRepository.sortOptions.firstOrNull { it.first == filterState.orderBy }?.second ?: "Más recientes"
    }

    val isInverse = filterState.order.equals("asc", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // 1. ORDEN
            item {
                FilterPillButton(
                    label = sortLabel,
                    badgeText = if (isInverse) "Asc" else null,
                    isActive = filterState.orderBy != "latest" || isInverse,
                    hasDropdown = true,
                    focusRequester = firstItemFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.SORT
                        activeDialog = ActiveFilterDialog.SORT
                    }
                )
            }

            // 2. GÉNERO
            item {
                val selectedName = filterState.selectedGenre?.name
                val label = if (selectedName != null) "Género(1): $selectedName" else "Género"
                FilterPillButton(
                    label = label,
                    isActive = filterState.selectedGenre != null,
                    hasDropdown = true,
                    focusRequester = genreFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.GENRE
                        activeDialog = ActiveFilterDialog.GENRE
                    }
                )
            }

            // 3. PROVEEDOR / PLATAFORMA
            item {
                val selectedProv = filterState.selectedProvider?.name
                val label = if (selectedProv != null) "Proveedor(1): $selectedProv" else "Proveedor"
                FilterPillButton(
                    label = label,
                    isActive = filterState.selectedProvider != null,
                    hasDropdown = true,
                    focusRequester = providerFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.PROVIDER
                        activeDialog = ActiveFilterDialog.PROVIDER
                    }
                )
            }

            // 4. PAÍS
            item {
                val selectedCountry = filterState.selectedCountry?.name
                val label = if (selectedCountry != null) "País(1): $selectedCountry" else "País"
                FilterPillButton(
                    label = label,
                    isActive = filterState.selectedCountry != null,
                    hasDropdown = true,
                    focusRequester = countryFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.COUNTRY
                        activeDialog = ActiveFilterDialog.COUNTRY
                    }
                )
            }

            // 5. AÑO
            item {
                val selectedYear = filterState.selectedYear?.name
                val label = if (selectedYear != null) "Año(1): $selectedYear" else "Año"
                FilterPillButton(
                    label = label,
                    isActive = filterState.selectedYear != null,
                    hasDropdown = true,
                    focusRequester = yearFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.YEAR
                        activeDialog = ActiveFilterDialog.YEAR
                    }
                )
            }

            // 6. TMDB
            item {
                val tmdbMin = filterState.selectedTmdbMin
                val label = if (tmdbMin != null && tmdbMin > 0.0) "TMDB: ${String.format(java.util.Locale.US, "%.1f", tmdbMin)}+" else "TMDB"
                FilterPillButton(
                    label = label,
                    isActive = filterState.selectedTmdbMin != null,
                    hasDropdown = true,
                    focusRequester = tmdbFocusRequester,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        lastOpenedDialog = ActiveFilterDialog.TMDB
                        activeDialog = ActiveFilterDialog.TMDB
                    }
                )
            }

            // 7. INVERTIR ORDEN (TOGGLE DIRECTO)
            item {
                FilterPillButton(
                    label = if (isInverse) "Orden inverso (Activo)" else "Orden inverso",
                    isActive = isInverse,
                    hasDropdown = false,
                    onUpDpad = onUpDpad,
                    onDownDpad = onDownDpad,
                    onClick = {
                        val newOrder = if (isInverse) "desc" else "asc"
                        onFilterChange(filterState.copy(order = newOrder))
                    }
                )
            }

            // 8. LIMPIAR FILTROS (Visible solo si hay filtros activos)
            if (filterState.hasActiveFilters) {
                item {
                    FilterPillButton(
                        label = "Limpiar (${filterState.activeFiltersCount})",
                        isActive = false,
                        isDanger = true,
                        hasDropdown = false,
                        onUpDpad = onUpDpad,
                        onDownDpad = onDownDpad,
                        onClick = {
                            onFilterChange(MovieRepository.FilterState())
                        }
                    )
                }
            }
        }
    }

    // MODAL DIALOGS
    when (activeDialog) {
        ActiveFilterDialog.SORT -> {
            TvFilterSelectDialog(
                title = "Ordenar por",
                options = MovieRepository.sortOptions.map { it.first to it.second },
                currentSelectedKey = filterState.orderBy,
                onSelect = { selectedKey ->
                    onFilterChange(filterState.copy(orderBy = selectedKey))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.GENRE -> {
            TvFilterSelectDialog(
                title = "Filtrar por Género",
                options = listOf("" to "Todos los géneros") + genreList.map { it.id.toString() to it.name },
                currentSelectedKey = filterState.selectedGenre?.id?.toString() ?: "",
                onSelect = { selectedKey ->
                    val chosenGenre = if (selectedKey.isBlank()) null else genreList.firstOrNull { it.id.toString() == selectedKey }
                    onFilterChange(filterState.copy(selectedGenre = chosenGenre))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.PROVIDER -> {
            TvFilterSelectDialog(
                title = "Filtrar por Plataforma / Proveedor",
                options = listOf("" to "Todas las plataformas") + MovieRepository.providers.map { it.id.toString() to it.name },
                currentSelectedKey = filterState.selectedProvider?.id?.toString() ?: "",
                onSelect = { selectedKey ->
                    val chosenProv = if (selectedKey.isBlank()) null else MovieRepository.providers.firstOrNull { it.id.toString() == selectedKey }
                    onFilterChange(filterState.copy(selectedProvider = chosenProv))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.COUNTRY -> {
            TvFilterSelectDialog(
                title = "Filtrar por País de origen",
                options = listOf("" to "Todos los países") + MovieRepository.countries.map { it.id.toString() to it.name },
                currentSelectedKey = filterState.selectedCountry?.id?.toString() ?: "",
                onSelect = { selectedKey ->
                    val chosenCountry = if (selectedKey.isBlank()) null else MovieRepository.countries.firstOrNull { it.id.toString() == selectedKey }
                    onFilterChange(filterState.copy(selectedCountry = chosenCountry))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.YEAR -> {
            TvFilterSelectDialog(
                title = "Filtrar por Año de lanzamiento",
                options = listOf("" to "Todos los años") + MovieRepository.years.map { it.id.toString() to it.name },
                currentSelectedKey = filterState.selectedYear?.id?.toString() ?: "",
                onSelect = { selectedKey ->
                    val chosenYear = if (selectedKey.isBlank()) null else MovieRepository.years.firstOrNull { it.id.toString() == selectedKey }
                    onFilterChange(filterState.copy(selectedYear = chosenYear))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.TMDB -> {
            TvFilterSelectDialog(
                title = "Calificación mínima TMDB",
                options = listOf("" to "Cualquier calificación") + MovieRepository.tmdbScoreOptions.map { it.first.toString() to it.second },
                currentSelectedKey = filterState.selectedTmdbMin?.toString() ?: "",
                onSelect = { selectedKey ->
                    val chosenTmdb = selectedKey.toDoubleOrNull()
                    onFilterChange(filterState.copy(selectedTmdbMin = chosenTmdb))
                    activeDialog = ActiveFilterDialog.NONE
                },
                onDismiss = { activeDialog = ActiveFilterDialog.NONE }
            )
        }
        ActiveFilterDialog.NONE -> {}
    }
}

@Composable
private fun FilterPillButton(
    label: String,
    badgeText: String? = null,
    isActive: Boolean = false,
    isDanger: Boolean = false,
    hasDropdown: Boolean = false,
    leadingIcon: ImageVector? = null,
    focusRequester: FocusRequester? = null,
    onUpDpad: () -> Unit = {},
    onDownDpad: () -> Boolean = { false },
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(50)

    Box(
        modifier = Modifier
            .zIndex(if (isFocused) 10f else 1f)
            .shadow(
                elevation = if (isFocused) 8.dp else 0.dp,
                shape = shape,
                spotColor = Color.White.copy(alpha = 0.40f),
                ambientColor = Color.White.copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(
                when {
                    isFocused -> Color(0xFFF5F2EB)
                    isDanger -> Color(0xFF3B1214)
                    isActive -> Color(0xFF1E2433)
                    else -> Color(0xBF0B0E14)
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    isDanger -> MochiStrawberryPink
                    isActive -> Color(0xFFF5F2EB).copy(alpha = 0.65f)
                    else -> Color.White.copy(alpha = 0.14f)
                },
                shape = shape
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            onUpDpad()
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onDownDpad()
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = when {
                        isFocused -> MochiDeepPlum
                        isDanger -> MochiStrawberryPink
                        isActive -> Color(0xFFF5F2EB)
                        else -> Color(0xFF9CA3AF)
                    },
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = label,
                color = when {
                    isFocused -> MochiDeepPlum
                    isDanger -> MochiStrawberryPink
                    isActive -> Color(0xFFF5F2EB)
                    else -> Color(0xFF9CA3AF)
                },
                fontSize = 12.sp,
                fontFamily = Inter,
                fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.Medium
            )

            if (badgeText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) Color.Black.copy(alpha = 0.15f) else Color(0xFF263044))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (isFocused) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hasDropdown) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TvFilterSelectDialog(
    title: String,
    options: List<Pair<String, String>>, // key to display name
    currentSelectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val selectedIndex = options.indexOfFirst { it.first == currentSelectedKey }.coerceAtLeast(0)
        delay(60)
        try {
            listState.scrollToItem(selectedIndex)
            firstFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .heightIn(max = 500.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0E131C))
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = Color(0xFFF5F2EB),
                    fontSize = 18.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(options) { index, (key, label) ->
                        val isSelected = key == currentSelectedKey
                        var isItemFocused by remember { mutableStateOf(false) }

                        val itemShape = RoundedCornerShape(10.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(itemShape)
                                .background(
                                    when {
                                        isItemFocused -> Color(0xFFF5F2EB)
                                        isSelected -> Color(0xFF1B2333)
                                        else -> Color(0x660B0E14)
                                    }
                                )
                                .border(
                                    width = if (isItemFocused) 2.dp else 1.dp,
                                    color = when {
                                        isItemFocused -> Color.White
                                        isSelected -> Color(0xFFF5F2EB).copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    },
                                    shape = itemShape
                                )
                                .then(if (index == 0) Modifier.focusRequester(firstFocusRequester) else Modifier)
                                .onFocusChanged { isItemFocused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                                onSelect(key)
                                                true
                                            }
                                            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                                                onDismiss()
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .clickable { onSelect(key) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = when {
                                        isItemFocused -> Color.Black
                                        isSelected -> Color(0xFFF5F2EB)
                                        else -> Color(0xFFB0B7C3)
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected || isItemFocused) FontWeight.Bold else FontWeight.Normal
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = if (isItemFocused) Color.Black else MochiStrawberryPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

