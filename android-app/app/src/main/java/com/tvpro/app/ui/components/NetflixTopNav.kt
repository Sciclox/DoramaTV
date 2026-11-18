package com.tvpro.app.ui.components

import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.tvpro.app.ui.theme.Inter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NetflixTopNav(
    selectedId: String,
    onSelect: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    homeFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    emisionFocusRequester: FocusRequester = remember { FocusRequester() },
    seriesFocusRequester: FocusRequester = remember { FocusRequester() },
    moviesFocusRequester: FocusRequester = remember { FocusRequester() },
    latinoFocusRequester: FocusRequester = remember { FocusRequester() },
    navFocusRequesters: Map<String, FocusRequester> = emptyMap(),
    onDownDpad: (() -> Unit)? = null,
    onFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isNavFocused by remember { mutableStateOf(true) }
    var focusedId by remember { mutableStateOf<String?>(null) }
    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var autoNavJob by remember { mutableStateOf<Job?>(null) }

    val focusRequesterForId: (String) -> FocusRequester? = { id ->
        when (id) {
            "search" -> searchFocusRequester
            "home" -> homeFocusRequester
            "emision" -> emisionFocusRequester
            "series" -> seriesFocusRequester
            "movies" -> moviesFocusRequester
            "latino" -> latinoFocusRequester
            "my_list", "mylist" -> myListFocusRequester
            else -> navFocusRequesters[id]
        }
    }

    val handleSelectInstant: (String) -> Unit = { id ->
        autoNavJob?.cancel()
        if (id != selectedId) {
            onSelect(id)
        }
    }

    val handleFocusGained: (String) -> Unit = { id ->
        focusedId = id
        autoNavJob?.cancel()
        if (id != selectedId) {
            autoNavJob = scope.launch {
                // TV ergonomics: 800ms debounce ensures rapid D-pad traversal does not trigger premature network/screen reloads
                delay(800L)
                if (isNavFocused) {
                    onSelect(id)
                    try {
                        focusRequesterForId(id)?.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val navScale = 1.0f

    val pillTargetId = if (isNavFocused) (focusedId ?: selectedId) else selectedId
    val pillBounds = itemBounds[pillTargetId]
    val isSearchPillTarget = pillTargetId == "search"

    val rawPillX = with(density) { (pillBounds?.left ?: 0f).toDp() }
    val rawPillW = with(density) { (pillBounds?.width ?: 0f).toDp() }
    val rawPillH = with(density) { (pillBounds?.height ?: 0f).toDp() }

    val targetPillH = rawPillH
    val targetPillW = if (isSearchPillTarget && rawPillH > 0.dp) rawPillH else rawPillW
    val targetPillX = if (isSearchPillTarget && rawPillW > rawPillH) rawPillX + (rawPillW - rawPillH) / 2 else rawPillX

    val animPillX by animateDpAsState(
        targetValue = targetPillX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillX"
    )
    val animPillW by animateDpAsState(
        targetValue = targetPillW,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillW"
    )
    val animPillH by animateDpAsState(
        targetValue = targetPillH,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillH"
    )

    val pillAlpha by animateFloatAsState(
        targetValue = if (isNavFocused) 1.0f else 0.20f,
        animationSpec = tween(durationMillis = 200),
        label = "pillAlpha"
    )

    val scrollState = rememberScrollState()

    LaunchedEffect(focusedId) {
        focusedId?.let { id ->
            itemBounds[id]?.let { bounds ->
                val targetScroll = (bounds.left - 80f).toInt().coerceAtLeast(0)
                scrollState.animateScrollTo(targetScroll)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 48.dp, top = 10.dp, end = 48.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = navScale
                    scaleY = navScale
                }
        ) {
            if (animPillW > 0.dp && animPillH > 0.dp) {
                Box(
                    modifier = Modifier
                        .offset(x = animPillX)
                        .width(animPillW)
                        .height(animPillH)
                        .background(
                            color = if (isNavFocused) Color(0xFFF5F2EB) else Color.White.copy(alpha = pillAlpha),
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .focusGroup()
                    .onFocusChanged { state ->
                        isNavFocused = state.hasFocus
                        onFocusChanged?.invoke(state.hasFocus)
                    }
            ) {
                // 1. Search
                NavPillButton(
                    id = "search",
                    isSelected = selectedId == "search",
                    isFocusedExternally = isNavFocused && focusedId == "search",
                    isCircular = true,
                    onFocusGained = handleFocusGained,
                    onSelect = onSelect,
                    onSelectInstant = handleSelectInstant,
                    focusRequester = searchFocusRequester,
                    onBoundsChanged = { bounds -> itemBounds["search"] = bounds },
                    onDownDpad = onDownDpad,
                    onLeftDpad = { true }
                ) { contentColor ->
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 2. Inicio
                NavPillButton(
                    id = "home",
                    isSelected = selectedId == "home",
                    isFocusedExternally = isNavFocused && focusedId == "home",
                    onFocusGained = handleFocusGained,
                    onSelect = onSelect,
                    onSelectInstant = handleSelectInstant,
                    focusRequester = homeFocusRequester,
                    onBoundsChanged = { bounds -> itemBounds["home"] = bounds },
                    onDownDpad = onDownDpad
                ) { contentColor ->
                    Text(
                        text = "Inicio",
                        color = contentColor,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // 3. Mi lista
                NavPillButton(
                    id = "my_list",
                    isSelected = selectedId == "my_list" || selectedId == "mylist",
                    isFocusedExternally = isNavFocused && focusedId == "my_list",
                    onFocusGained = handleFocusGained,
                    onSelect = onSelect,
                    onSelectInstant = handleSelectInstant,
                    focusRequester = myListFocusRequester,
                    onBoundsChanged = { bounds -> itemBounds["my_list"] = bounds },
                    onDownDpad = onDownDpad,
                    onRightDpad = { true }
                ) { contentColor ->
                    Text(
                        text = "Mi lista",
                        color = contentColor,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavPillButton(
    id: String,
    isSelected: Boolean,
    isFocusedExternally: Boolean,
    isCircular: Boolean = false,
    onFocusGained: (String) -> Unit,
    onSelect: (String) -> Unit,
    onSelectInstant: (String) -> Unit = onSelect,
    onBoundsChanged: (Rect) -> Unit,
    focusRequester: FocusRequester,
    onDownDpad: (() -> Unit)? = null,
    onLeftDpad: (() -> Boolean)? = null,
    onRightDpad: (() -> Boolean)? = null,
    content: @Composable (contentColor: Color) -> Unit
) {
    val pillShape = if (isCircular) CircleShape else RoundedCornerShape(20.dp)

    val animatedContentColor by animateColorAsState(
        targetValue = when {
            isFocusedExternally -> Color.Black
            isSelected -> Color(0xFFF5F2EB)
            else -> Color(0xFF9CA3AF)
        },
        animationSpec = tween(durationMillis = 150),
        label = "contentColor_$id"
    )

    Button(
        onClick = {
            onSelectInstant(id)
            if (id == "search") {
                onDownDpad?.invoke()
            }
        },
        scale = ButtonDefaults.scale(scale = 1.0f, focusedScale = 1.0f, pressedScale = 0.98f),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = animatedContentColor,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.Black
        ),
        border = ButtonDefaults.border(
            border = Border.None,
            focusedBorder = Border.None
        ),
        shape = ButtonDefaults.shape(shape = pillShape),
        modifier = Modifier
            .focusRequester(focusRequester)
            .onGloballyPositioned { coords ->
                onBoundsChanged(coords.boundsInParent())
            }
            .onFocusChanged { state ->
                if (state.isFocused) onFocusGained(id)
            }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onSelectInstant(id)
                            if (id == "search") {
                                onDownDpad?.invoke()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            onSelectInstant(id)
                            if (onDownDpad != null) {
                                onDownDpad()
                                true
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (onLeftDpad != null) {
                                onLeftDpad()
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (onRightDpad != null) {
                                onRightDpad()
                            } else false
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentPadding = if (isCircular) PaddingValues(8.dp) else PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) {
        content(animatedContentColor)
    }
}
