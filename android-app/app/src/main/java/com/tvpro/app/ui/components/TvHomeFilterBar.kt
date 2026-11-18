package com.tvpro.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.MochiStrawberryPink
import com.tvpro.app.ui.theme.MochiWarmRose

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeFilterBar(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    firstChipFocusRequester: FocusRequester? = null,
    onUpDpad: (() -> Unit)? = null,
    onDownDpad: (() -> Unit)? = null
) {
    val types = listOf("Todos", "Series", "Películas")
    val genres = listOf(
        "Todos", "Romance", "Comedia", "Drama", "Acción",
        "Fantasía", "Ciencia Ficción", "Misterio", "Crimen",
        "Suspenso", "Aventura", "Histórico", "Familia", "Terror"
    )

    val typeRowState = rememberLazyListState()
    val genreRowState = rememberLazyListState()

    val genreFirstRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ROW 1: TIPO
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TIPO:",
                color = Color(0xFFAAAAAA),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp)
            )

            LazyRow(
                state = typeRowState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(types) { index, typeName ->
                    val isSelected = selectedType == typeName
                    val chipFocusRequester = if (index == 0 && firstChipFocusRequester != null) firstChipFocusRequester else remember { FocusRequester() }

                    TvFilterPill(
                        text = typeName,
                        isSelected = isSelected,
                        onClick = { onTypeSelected(typeName) },
                        focusRequester = chipFocusRequester,
                        onUpDpad = onUpDpad,
                        onDownDpad = {
                            try {
                                genreFirstRequester.requestFocus()
                            } catch (_: Exception) {
                                onDownDpad?.invoke()
                            }
                        }
                    )
                }
            }
        }

        // ROW 2: GÉNEROS
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "GÉNERO:",
                color = Color(0xFFAAAAAA),
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 4.dp)
            )

            LazyRow(
                state = genreRowState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(genres) { index, genreName ->
                    val isSelected = selectedGenre == genreName
                    val chipFocusRequester = if (index == 0) genreFirstRequester else remember { FocusRequester() }

                    TvFilterPill(
                        text = genreName,
                        isSelected = isSelected,
                        onClick = { onGenreSelected(genreName) },
                        focusRequester = chipFocusRequester,
                        onUpDpad = {
                            try {
                                firstChipFocusRequester?.requestFocus()
                            } catch (_: Exception) {
                                onUpDpad?.invoke()
                            }
                        },
                        onDownDpad = onDownDpad
                    )
                }
            }
        }
    }
}

@Composable
fun TvFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUpDpad: (() -> Unit)? = null,
    onDownDpad: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> Color(0xFFE50914).copy(alpha = 0.35f)
            else -> Color(0xFF1E1A24)
        },
        label = "pillBgColor"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.Black
            isSelected -> Color.White
            else -> Color(0xFFD0CBD6)
        },
        label = "pillTextColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> Color.White
            isSelected -> Color(0xFFFF3366)
            else -> Color(0xFF382E42)
        },
        label = "pillBorderColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(16.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if (onUpDpad != null) {
                                onUpDpad()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (onDownDpad != null) {
                                onDownDpad()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = Inter,
            fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
