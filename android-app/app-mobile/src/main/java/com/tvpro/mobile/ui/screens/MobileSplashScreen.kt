package com.tvpro.mobile.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.R
import com.tvpro.mobile.ui.theme.TvProDarkBg
import kotlinx.coroutines.delay

@Composable
fun MobileSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Smooth Entrance Alpha (stays at 1.0f once entered, avoiding premature exit fading)
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 650,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashAlpha"
    )

    // Gentle Breathing / Living Scale Animation
    val infiniteTransition = rememberInfiniteTransition(label = "livingArt")
    val livingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livingScale"
    )

    // Progress Bar Animation
    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2100) // Display full splash experience
        onSplashFinished() // Directly hand over to navigation crossfade without blank gap
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvProDarkBg), // Seamless match with the rest of the app
        contentAlignment = Alignment.Center
    ) {
        // 1. Full-screen Official Mochi Artwork (Lettering 3D + Pulpito en la nube con corazón)
        Image(
            painter = painterResource(id = R.drawable.splash_mochi_bg),
            contentDescription = "Mochi: Historias que abrazan el corazón",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = if (startAnimation) livingScale else 0.96f
                    scaleY = if (startAnimation) livingScale else 0.96f
                },
            contentScale = ContentScale.Crop
        )

        // 2. Subtle Bottom Ambient Vignette & Cute Pastel Progress Bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 44.dp)
                .graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minimalist Pastel Loading Bar
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.55f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFFB7C5),
                                        Color(0xFFFF8DA1),
                                        Color(0xFFE8D7F1)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = "Cargando tus historias...",
                    color = Color(0xFF9C7787),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
