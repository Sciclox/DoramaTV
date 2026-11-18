package com.tvpro.app.ui.components

import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.tv.material3.*
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester

@OptIn(ExperimentalTvMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TvHeroBanner(
    heroList: List<MovieItem>,
    onMovieSelect: (MovieItem) -> Unit,
    onFocusItem: (FocusRequester) -> Unit,
    firstKeyFocusRequester: FocusRequester,
    navFocusRequester: FocusRequester,
    scrollToRow: (Int) -> Unit,
    categoryBadgeText: String = "PELÍCULA DESTACADA",
    onHeroFocusChanged: ((Boolean) -> Unit)? = null,
    onDownDpad: (() -> Unit)? = null,
    isScrolledDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (heroList.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val cleanHeroList = remember(heroList) {
        heroList.take(5)
    }

    var currentHeroIndex by remember { mutableIntStateOf(0) }
    var isHeroFocused by remember { mutableStateOf(false) }

    // Auto-rotate Hero Banner every 8 seconds when not focused
    LaunchedEffect(cleanHeroList, isHeroFocused) {
        if (cleanHeroList.size > 1 && !isHeroFocused) {
            while (true) {
                delay(8000)
                currentHeroIndex = (currentHeroIndex + 1) % cleanHeroList.size
            }
        }
    }

    val activeHero = cleanHeroList.getOrNull(currentHeroIndex) ?: cleanHeroList.firstOrNull()
    val fetchedOverviews = remember { mutableStateMapOf<String, String>() }
    val fetchedBackdrops = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(activeHero?.url) {
        val hero = activeHero ?: return@LaunchedEffect
        val heroOv = hero.overview
        val needsOverview = (heroOv.isNullOrBlank() || heroOv.contains("está disponible en calidad de streaming")) &&
                !fetchedOverviews.containsKey(hero.url)
        val hasValidBackdrop = fetchedBackdrops.containsKey(hero.url) ||
                (hero.backdrop?.let { com.tvpro.core.data.repository.MovieRepository.isBackdropUrl(it) } == true)

        if (needsOverview || !hasValidBackdrop) {
            withContext(Dispatchers.IO) {
                try {
                    val details = com.tvpro.core.data.repository.MovieRepository.extractMovie(hero)
                    val realOv = details.overview?.takeIf {
                        it.isNotBlank() && !it.contains("está disponible en calidad de streaming")
                    }
                    if (realOv != null) {
                        fetchedOverviews[hero.url] = realOv
                    }
                    val realBackdrop = details.gallery.firstOrNull { com.tvpro.core.data.repository.MovieRepository.isBackdropUrl(it) }
                        ?: details.backdrop?.takeIf { it.isNotBlank() && com.tvpro.core.data.repository.MovieRepository.isBackdropUrl(it) }
                        ?: details.backdrop?.takeIf { it.isNotBlank() }
                    if (realBackdrop != null) {
                        fetchedBackdrops[hero.url] = realBackdrop
                    }
                } catch (_: Exception) {}
            }
        }
    }

    var dominantColor by remember { mutableStateOf(MochiStrawberryPink) }
    val animatedAmbientColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800),
        label = "ambientColor"
    )

    val rawHeroUrl = activeHero?.let { fetchedBackdrops[it.url] }
        ?: activeHero?.backdrop?.takeIf { it.isNotBlank() && com.tvpro.core.data.repository.MovieRepository.isBackdropUrl(it) }
        ?: activeHero?.backdrop?.takeIf { it.isNotBlank() }
        ?: activeHero?.image
        ?: ""
    val heroImageUrl = rawHeroUrl
        .replace("/t/p/w500/", "/t/p/original/")
        .replace("/t/p/w780/", "/t/p/original/")
        .replace("/t/p/w1280/", "/t/p/original/")
        .replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")

    // Dynamic color extraction for ambient glow
    LaunchedEffect(heroImageUrl) {
        if (heroImageUrl.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(heroImageUrl)
                    .size(300, 169)
                    .allowHardware(false)
                    .target { drawable ->
                        val bitmap = (drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null && !bitmap.isRecycled) {
                            Palette.from(bitmap).generate { palette ->
                                val vibrant = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
                                vibrant?.let {
                                    val hsl = FloatArray(3)
                                    ColorUtils.colorToHSL(it, hsl)
                                    hsl[2] = 0.55f
                                    val brightColor = Color(ColorUtils.HSLToColor(hsl))
                                    dominantColor = brightColor
                                }
                            }
                        }
                    }
                    .build()
                loader.enqueue(req)
            }
        }
    }

    if (activeHero != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(430.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged { focusState ->
                    isHeroFocused = focusState.hasFocus
                    onHeroFocusChanged?.invoke(focusState.hasFocus)
                    if (focusState.hasFocus) {
                        scope.launch {
                            try {
                                bringIntoViewRequester.bringIntoView()
                            } catch (_: Exception) {}
                        }
                    }
                }
                .background(MochiDeepPlum)
        ) {
            val deepBackground = MochiDeepPlum
            val warmIvory = MochiCream
            val softSilver = MochiLavender
            val mutedSlate = MochiMutedLilac
            val pillSurface = PillSurface
            val subtleBorder = SubtleBorder

            val contentAlpha by animateFloatAsState(
                targetValue = if (isScrolledDown) 0f else 1f,
                animationSpec = tween(durationMillis = 250),
                label = "heroContentAlpha"
            )

            // Edge-to-edge cinematic hero backdrop
            Crossfade(
                targetState = heroImageUrl,
                animationSpec = tween(durationMillis = 500),
                label = "heroCrossfade"
            ) { targetUrl ->
                key(targetUrl) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(targetUrl)
                            .size(coil.size.Size.ORIGINAL)
                            .allowRgb565(false)
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .crossfade(true)
                            .build(),
                        contentDescription = activeHero.title,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Layer 1: Left-to-right horizontal dark gradient for optimal text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to deepBackground.copy(alpha = 0.95f),
                                0.40f to deepBackground.copy(alpha = 0.82f),
                                0.65f to deepBackground.copy(alpha = 0.35f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )

            // Layer 2: Subtle ambient color glow matching the movie palette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedAmbientColor.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(250f, 400f),
                            radius = 800f
                        )
                    )
            )

            // Layer 3: Vertical top and bottom gradients for seamless blend
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to deepBackground.copy(alpha = 0.70f),
                                0.22f to Color.Transparent,
                                0.60f to Color.Transparent,
                                0.85f to deepBackground.copy(alpha = 0.85f),
                                1.0f to deepBackground
                            )
                        )
                    )
            )

            // Capsule Pills Indicator (Bottom Right)
            if (cleanHeroList.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 20.dp, end = 48.dp)
                        .graphicsLayer { alpha = contentAlpha }
                        .clip(RoundedCornerShape(50))
                        .background(pillSurface)
                        .border(1.dp, subtleBorder, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cleanHeroList.indices.forEach { index ->
                            val isSelected = index == currentHeroIndex
                            val capsuleWidth by animateDpAsState(
                                targetValue = if (isSelected) 26.dp else 10.dp,
                                animationSpec = tween(durationMillis = 350),
                                label = "capsuleWidth"
                            )
                            Box(
                                modifier = Modifier
                                    .width(capsuleWidth)
                                    .height(5.5.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) warmIvory else Color.White.copy(alpha = 0.28f)
                                    )
                            )
                        }
                    }
                }
            }

            // Cinematic Content: Title, metadata chips, synopsis and action buttons
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 48.dp, bottom = 18.dp, end = 48.dp)
                    .width(540.dp)
                    .graphicsLayer { alpha = contentAlpha }
            ) {
                // Pre-title Editorial Kicker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryBadgeText,
                        color = mutedSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Inter,
                        letterSpacing = 2.0.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Cinematic Monumental Title
                Text(
                    text = activeHero.title.cleanTitle().uppercase(),
                    color = warmIvory,
                    fontSize = 42.sp,
                    fontFamily = BebasNeue,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 44.sp,
                    letterSpacing = 2.0.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Refined Metadata Badges Strip (Editorial Glass & Warm Ivory)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // TMDB Rating Pill
                    val ratingValue = activeHero.rating?.takeIf { it.isNotBlank() } ?: "8.5"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pillSurface)
                            .border(1.dp, subtleBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TMDB",
                                color = mutedSlate,
                                fontSize = 9.5.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = warmIvory,
                                modifier = Modifier.size(10.5.dp)
                            )
                            Text(
                                text = ratingValue,
                                color = warmIvory,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Inter,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Release Year
                    Text(
                        text = activeHero.year?.takeIf { it.isNotBlank() } ?: "2024",
                        color = warmIvory,
                        fontSize = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Maturity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(pillSurface)
                            .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "16+",
                            color = warmIvory,
                            fontSize = 10.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Quality Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(PillSurface)
                            .border(1.dp, warmIvory.copy(alpha = 0.30f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4K HDR",
                            color = warmIvory,
                            fontSize = 9.5.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )
                    }

                    // Audio Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(pillSurface)
                            .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LATINO",
                            color = softSilver,
                            fontSize = 9.5.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.0.sp
                        )
                    }

                    // 5.1 Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(pillSurface)
                            .border(1.dp, subtleBorder, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "5.1",
                            color = Color(0xFFD1D5DB),
                            fontSize = 9.5.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Real Synopsis
                val realSynopsis = activeHero.overview?.takeIf { it.isNotBlank() }
                    ?: fetchedOverviews[activeHero.url]
                if (!realSynopsis.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = realSynopsis,
                        color = Color(0xFFD1D5DB),
                        fontSize = 13.5.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        lineHeight = 20.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row (Capsule pills matching DetailScreen)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    var isPlayFocused by remember { mutableStateOf(false) }
                    var isInfoFocused by remember { mutableStateOf(false) }

                    // Button 1: "REPRODUCIR"
                    M3Button(
                        onClick = {
                            val heroToPlay = if (heroImageUrl.isNotBlank()) activeHero.copy(backdrop = heroImageUrl) else activeHero
                            onMovieSelect(heroToPlay)
                        },
                        modifier = Modifier
                            .height(42.dp)
                            .focusRequester(firstKeyFocusRequester)
                            .onFocusChanged { focusState ->
                                isPlayFocused = focusState.isFocused
                                isHeroFocused = focusState.isFocused || isInfoFocused
                                onHeroFocusChanged?.invoke(isHeroFocused)
                                if (focusState.isFocused) {
                                    onFocusItem(firstKeyFocusRequester)
                                    scrollToRow(0)
                                }
                            }
                            .focusProperties {
                                up = navFocusRequester
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (onDownDpad != null) {
                                                onDownDpad.invoke()
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                        colors = M3ButtonDefaults.buttonColors(
                            containerColor = if (isPlayFocused) warmIvory else pillSurface,
                            contentColor = if (isPlayFocused) Color.Black else warmIvory
                        ),
                        border = if (isPlayFocused) BorderStroke(2.dp, warmIvory)
                        else BorderStroke(1.dp, subtleBorder),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isPlayFocused) Color.Black else warmIvory,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "REPRODUCIR",
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.6.sp,
                                color = if (isPlayFocused) Color.Black else warmIvory
                            )
                        }
                    }

                    // Button 2: "MÁS INFORMACIÓN"
                    M3Button(
                        onClick = {
                            val heroToOpen = if (heroImageUrl.isNotBlank()) activeHero.copy(backdrop = heroImageUrl) else activeHero
                            onMovieSelect(heroToOpen)
                        },
                        modifier = Modifier
                            .height(42.dp)
                            .onFocusChanged { focusState ->
                                isInfoFocused = focusState.isFocused
                                isHeroFocused = isPlayFocused || focusState.isFocused
                                onHeroFocusChanged?.invoke(isHeroFocused)
                                if (focusState.isFocused) {
                                    onFocusItem(firstKeyFocusRequester)
                                    scrollToRow(0)
                                }
                            }
                            .focusProperties {
                                up = navFocusRequester
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            try { navFocusRequester.requestFocus() } catch (_: Exception) {}
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (onDownDpad != null) {
                                                onDownDpad.invoke()
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                        colors = M3ButtonDefaults.buttonColors(
                            containerColor = if (isInfoFocused) warmIvory else pillSurface,
                            contentColor = if (isInfoFocused) Color.Black else warmIvory
                        ),
                        border = if (isInfoFocused) BorderStroke(2.dp, warmIvory)
                        else BorderStroke(1.dp, subtleBorder),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isInfoFocused) Color.Black else warmIvory,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "MÁS INFORMACIÓN",
                                fontFamily = Inter,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.6.sp,
                                color = if (isInfoFocused) Color.Black else warmIvory
                            )
                        }
                    }
                }
            }
        }
    }
}

