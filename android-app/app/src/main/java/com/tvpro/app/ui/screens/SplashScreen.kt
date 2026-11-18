package com.tvpro.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.Text
import com.tvpro.app.R
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.NetflixBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    // Smooth Entrance Alpha (stays fully visible until transition)
    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 750,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        ),
        label = "splashAlpha"
    )

    // Cinematic Living Scale Animation
    val infiniteTransition = rememberInfiniteTransition(label = "livingArtTV")
    val livingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livingScaleTV"
    )

    // Progress Bar Animation
    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onSplashFinished() // Smooth direct handover
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NetflixBlack), // Seamless cinematic dark background
        contentAlignment = Alignment.Center
    ) {
        // 1. Widescreen Official Mochi Artwork (Lettering 3D + Mascota en la nube)
        Image(
            painter = painterResource(id = R.drawable.splash_mochi_bg),
            contentDescription = "Mochi TV Splash",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = if (startAnimation) livingScale else 0.96f
                    scaleY = if (startAnimation) livingScale else 0.96f
                },
            contentScale = ContentScale.Crop
        )

        // 2. Bottom Ambient TV Progress Bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 52.dp)
                .graphicsLayer { alpha = contentAlpha },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.35f))
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
                    text = "HISTORIAS QUE ABRAZAN EL CORAZÓN",
                    color = Color(0xFFF5E6ED),
                    fontSize = 11.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp
                )
            }
        }
    }
}
