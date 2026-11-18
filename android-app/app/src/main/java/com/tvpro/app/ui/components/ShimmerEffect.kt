package com.tvpro.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tvpro.app.ui.theme.*

@Composable
fun ShimmerBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MochiDeepPlum,
        MochiPlumSurface,
        MochiPlumCard,
        MochiPlumSurface,
        MochiDeepPlum
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value + 200f, 0f)
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun SkeletonHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MochiDeepPlum)
            .padding(start = 24.dp, top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ShimmerBox(width = 900.dp, height = 420.dp)
        ShimmerBox(width = 180.dp, height = 18.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) {
                ShimmerBox(width = 160.dp, height = 240.dp)
            }
        }
        ShimmerBox(width = 220.dp, height = 18.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) {
                ShimmerBox(width = 160.dp, height = 240.dp)
            }
        }
    }
}
