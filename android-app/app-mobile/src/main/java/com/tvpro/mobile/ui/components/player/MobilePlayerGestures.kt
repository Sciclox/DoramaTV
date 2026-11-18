package com.tvpro.mobile.ui.components.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.ui.theme.Inter
import com.tvpro.mobile.ui.theme.MochiStrawberryPink
import kotlin.math.roundToInt

@Composable
fun DoubleTapSeekOverlay(
    side: String,
    seconds: Int,
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val isForward = side == "RIGHT"

    // Infinite wave animation for 3 chevrons
    val transition = rememberInfiniteTransition(label = "double_tap_wave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Tactile spring bounce on each tap
    val scaleAnim = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        scaleAnim.snapTo(1.15f)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f)
        )
    }

    // Outer half-screen curved arc
    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.46f)
            .clip(
                if (isForward) RoundedCornerShape(topStartPercent = 100, bottomStartPercent = 100)
                else RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100)
            )
            .background(
                Brush.horizontalGradient(
                    colors = if (isForward) {
                        listOf(
                            Color.Transparent,
                            Color(0x14FFFFFF),
                            Color(0x30FFFFFF)
                        )
                    } else {
                        listOf(
                            Color(0x30FFFFFF),
                            Color(0x14FFFFFF),
                            Color.Transparent
                        )
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating glass capsule badge
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0x8805080E))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3 Cascading Chevrons Wave
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp)
                ) {
                    val chevronIndices = if (isForward) listOf(0, 1, 2) else listOf(2, 1, 0)
                    for (i in chevronIndices) {
                        val distance = (wavePhase - i).mod(3f)
                        val alpha = if (distance in 0f..1.1f) 1.0f else 0.28f
                        val chevronScale = if (distance in 0f..1.1f) 1.12f else 0.88f

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = alpha),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = if (isForward) chevronScale else -chevronScale
                                    scaleY = chevronScale
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "${seconds} segundos",
                    color = Color.White,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

@Composable
fun InteractiveVerticalBar(
    level: Float,
    icon: ImageVector,
    accentColor: Color,
    isLeft: Boolean,
    onLevelChange: (Float) -> Unit,
    onInteractionStart: () -> Unit,
    onInteractionEnd: () -> Unit,
    onIconClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cleanLevel = level.coerceIn(0f, 1f)
    var trackHeightPx by remember { mutableFloatStateOf(120f) }
    var trackTopInBoxPx by remember { mutableFloatStateOf(40f) }

    Box(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(if (isLeft) WindowInsetsSides.Start else WindowInsetsSides.End))
            .padding(start = if (isLeft) 28.dp else 0.dp, end = if (!isLeft) 28.dp else 0.dp)
            .width(48.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xD906080C))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
            .pointerInput(trackHeightPx, trackTopInBoxPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onInteractionStart()

                    fun calculateFraction(posY: Float): Float {
                        val safeHeight = trackHeightPx.coerceAtLeast(1f)
                        val relY = (posY - trackTopInBoxPx).coerceIn(0f, safeHeight)
                        return (1f - (relY / safeHeight)).coerceIn(0f, 1f)
                    }

                    onLevelChange(calculateFraction(down.position.y))

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        change.consume()
                        onLevelChange(calculateFraction(change.position.y))
                    }
                    onInteractionEnd()
                }
            }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon button at the top
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(enabled = onIconClick != null) {
                        onIconClick?.invoke()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Vertical Track and Level Fill
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                        trackTopInBoxPx = coordinates.positionInParent().y
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(cleanLevel)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                )
            }

            // Percentage text at the bottom
            Text(
                text = "${(cleanLevel * 100).roundToInt()}%",
                color = Color.White,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AudioWaveIndicator(
    color: Color = MochiStrawberryPink,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")
    val b1 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(490, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )

    Row(
        modifier = modifier.size(width = 16.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight(b1).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(b2).clip(RoundedCornerShape(1.dp)).background(color))
        Box(modifier = Modifier.weight(1f).fillMaxHeight(b3).clip(RoundedCornerShape(1.dp)).background(color))
    }
}
