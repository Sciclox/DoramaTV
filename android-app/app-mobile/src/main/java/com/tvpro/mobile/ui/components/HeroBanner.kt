package com.tvpro.mobile.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.core.data.model.MovieItem
import com.tvpro.mobile.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun MobileHeroBanner(
    heroList: List<MovieItem>,
    isInMyList: Boolean,
    onPlayClick: (MovieItem) -> Unit,
    onDetailClick: (MovieItem) -> Unit,
    onToggleMyList: (MovieItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (heroList.isEmpty()) return

    val context = LocalContext.current
    val cleanHeroList = remember(heroList) { heroList.take(5) }
    var currentHeroIndex by remember { mutableIntStateOf(0) }

    // Auto-advance hero banner every 7 seconds
    LaunchedEffect(cleanHeroList) {
        if (cleanHeroList.size > 1) {
            while (true) {
                delay(7000)
                currentHeroIndex = (currentHeroIndex + 1) % cleanHeroList.size
            }
        }
    }

    val activeHero = cleanHeroList.getOrNull(currentHeroIndex) ?: cleanHeroList.first()

    val rawHeroUrl = activeHero.backdrop?.takeIf { it.isNotBlank() } ?: activeHero.image
    val heroImageUrl = rawHeroUrl
        .replace("/t/p/w500/", "/t/p/original/")
        .replace("/t/p/w780/", "/t/p/original/")
        .replace("/t/p/w1280/", "/t/p/original/")

    // Ambient glow color extraction (whisper of color, subtle like TV app)
    var ambientColor by remember { mutableStateOf(Color(0xFF281830)) }
    val animatedAmbient by animateColorAsState(
        targetValue = ambientColor,
        animationSpec = tween(durationMillis = 800),
        label = "heroAmbient"
    )

    LaunchedEffect(heroImageUrl) {
        if (heroImageUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context)
                        .data(heroImageUrl)
                        .size(240, 135)
                        .allowHardware(false)
                        .target { drawable ->
                            val bitmap = (drawable as? BitmapDrawable)?.bitmap
                            if (bitmap != null && !bitmap.isRecycled) {
                                Palette.from(bitmap).generate { palette ->
                                    val vibrant = palette?.dominantSwatch?.rgb ?: palette?.vibrantSwatch?.rgb
                                    vibrant?.let {
                                        val hsl = FloatArray(3)
                                        ColorUtils.colorToHSL(it, hsl)
                                        hsl[2] = 0.40f
                                        ambientColor = Color(ColorUtils.HSLToColor(hsl))
                                    }
                                }
                            }
                        }
                        .build()
                    loader.enqueue(req)
                } catch (_: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(510.dp)
            .background(LaMovieDarkBg)
    ) {
        // 1. Edge-to-Edge Hero Image
        Crossfade(
            targetState = heroImageUrl,
            animationSpec = tween(600),
            label = "heroCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { targetUrl ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(targetUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = activeHero.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Subtle Ambient color glow layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            animatedAmbient.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = 100f,
                        endY = 600f
                    )
                )
        )

        // 3. Cinematic Vignettes into OLED Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99130D19),
                            Color.Transparent,
                            Color(0x55130D19),
                            Color(0xDD130D19),
                            LaMovieDarkBg
                        ),
                        startY = 0f
                    )
                )
        )

        // 4. Horizontal edge gradient to add depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            LaMovieDarkBg.copy(alpha = 0.45f),
                            Color.Transparent,
                            LaMovieDarkBg.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        // 5. Pager indicators (Capsule pills matching TV version)
        if (cleanHeroList.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cleanHeroList.indices.forEach { index ->
                    val isSelected = index == currentHeroIndex
                    Box(
                        modifier = Modifier
                            .height(3.5.dp)
                            .width(if (isSelected) 18.dp else 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isSelected) WarmIvory else Color.White.copy(alpha = 0.28f))
                    )
                }
            }
        }

        // 6. Bottom Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Editorial Category Badge / TOP 10 Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PillSurface)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 3.5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MochiGold,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "DESTACADO HOY EN MOCHI",
                        color = WarmIvory,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Heading Title in Warm Ivory
            Text(
                text = activeHero.title.cleanTitle(),
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = WarmIvory,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                letterSpacing = (-0.4).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Refined Metadata Badges Strip (TMDB, LAT, 4K, Year)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // TMDB Rating Pill
                val ratingValue = activeHero.rating?.takeIf { it.isNotBlank() && it != "0" } ?: "8.5"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "TMDB",
                            color = MutedSlate,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter
                        )
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarmIvory,
                            modifier = Modifier.size(8.5.dp)
                        )
                        Text(
                            text = ratingValue,
                            color = WarmIvory,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Inter
                        )
                    }
                }

                // LAT Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PillSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LAT",
                        color = SoftSilver,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter
                    )
                }

                // 4K HDR Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PillSurface)
                        .border(1.dp, WarmIvory.copy(alpha = 0.30f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "4K HDR",
                        color = WarmIvory,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Inter
                    )
                }

                // Year
                val heroYear = activeHero.year
                if (!heroYear.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PillSurface)
                            .border(1.dp, SubtleBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = heroYear,
                            color = SoftSilver,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Inter
                        )
                    }
                }
            }

            // Genres
            if (activeHero.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activeHero.genres.take(3).joinToString("  •  "),
                    color = MutedSlate,
                    fontSize = 11.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row (Cohesive luxury capsule pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mi Lista Pill Button
                Button(
                    onClick = { onToggleMyList(activeHero) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PillSurface,
                        contentColor = WarmIvory
                    ),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = if (isInMyList) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = WarmIvory,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isInMyList) "Guardado" else "Mi Lista",
                        color = WarmIvory,
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Play Primary Button (Solid Warm Ivory with Play Icon)
                Button(
                    onClick = { onPlayClick(activeHero) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmIvory,
                        contentColor = Color.Black
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Reproducir",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Info / Detalles Pill Button
                Button(
                    onClick = { onDetailClick(activeHero) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PillSurface,
                        contentColor = WarmIvory
                    ),
                    border = BorderStroke(1.dp, SubtleBorder),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WarmIvory,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Detalles",
                        color = WarmIvory,
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
