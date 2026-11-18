package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.app.ui.theme.*

@Composable
fun DetailActionButtons(
    isContentAvailable: Boolean,
    isEpisodeResolving: Boolean,
    hasSavedProgress: Boolean,
    isFavorite: Boolean,
    primaryLabel: String,
    isBackgroundLoading: Boolean,
    playButtonFocusRequester: FocusRequester,
    restartButtonFocusRequester: FocusRequester,
    myListFocusRequester: FocusRequester,
    backButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onRestartClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warmIvory = MochiCream
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isContentAvailable) {
                // Primary CTA: Play / Resume
                var isPlayFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayFocused) warmIvory else pillSurface,
                        contentColor = if (isPlayFocused) Color.Black else warmIvory
                    ),
                    border = if (isPlayFocused) BorderStroke(2.dp, warmIvory)
                    else BorderStroke(1.dp, subtleBorder),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .focusRequester(playButtonFocusRequester)
                        .focusProperties {
                            up = backButtonFocusRequester
                            down = downFocusRequester
                            right = if (hasSavedProgress) restartButtonFocusRequester else myListFocusRequester
                        }
                        .onFocusChanged { isPlayFocused = it.isFocused }
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                true
                            } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                try { downFocusRequester.requestFocus() } catch (_: Exception) {}
                                true
                            } else false
                        }
                ) {
                    if (isEpisodeResolving) {
                        CircularProgressIndicator(
                            color = if (isPlayFocused) Color.Black else warmIvory,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ABRIENDO...",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.6.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = primaryLabel,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.6.sp
                        )
                    }
                }

                // Secondary Button: Restart from beginning
                if (hasSavedProgress) {
                    var isRestartFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onRestartClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRestartFocused) warmIvory else pillSurface,
                            contentColor = if (isRestartFocused) Color.Black else warmIvory
                        ),
                        border = if (isRestartFocused) BorderStroke(2.dp, warmIvory)
                        else BorderStroke(1.dp, subtleBorder),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .focusRequester(restartButtonFocusRequester)
                            .focusProperties {
                                up = backButtonFocusRequester
                                down = downFocusRequester
                                left = playButtonFocusRequester
                                right = myListFocusRequester
                            }
                            .onFocusChanged { isRestartFocused = it.isFocused }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                    try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                                    try { downFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else false
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Reiniciar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REINICIAR",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp
                        )
                    }
                }
            } else {
                // Badge: AÚN NO DISPONIBLE
                Surface(
                    color = MochiPlumCard,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.2.dp, MochiStrawberryPink.copy(alpha = 0.7f)),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MochiStrawberryPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AÚN NO DISPONIBLE",
                            color = warmIvory,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 1.4.sp
                        )
                    }
                }
            }

            // My List Button
            var isFavoriteFocused by remember { mutableStateOf(false) }
            Button(
                onClick = onFavoriteClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isFavoriteFocused -> warmIvory
                        isFavorite -> Color(0xFF1A202C)
                        else -> pillSurface
                    },
                    contentColor = if (isFavoriteFocused) Color.Black else warmIvory
                ),
                border = when {
                    isFavoriteFocused -> BorderStroke(2.dp, warmIvory)
                    isFavorite -> BorderStroke(1.2.dp, warmIvory.copy(alpha = 0.5f))
                    else -> BorderStroke(1.dp, subtleBorder)
                },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(42.dp)
                    .focusRequester(myListFocusRequester)
                    .focusProperties {
                        up = backButtonFocusRequester
                        down = downFocusRequester
                        left = if (isContentAvailable) {
                            if (hasSavedProgress) restartButtonFocusRequester else playButtonFocusRequester
                        } else FocusRequester.Default
                    }
                    .onFocusChanged { isFavoriteFocused = it.isFocused }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                            try { backButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                            try { downFocusRequester.requestFocus() } catch (_: Exception) {}
                            true
                        } else false
                    }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isFavorite) "Quitar de Mi Lista" else "Añadir a Mi Lista",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFavorite) "EN MI LISTA" else "MI LISTA",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp
                )
            }
        }

        // Banner explicativo cuando el contenido aún no está disponible
        if (!isBackgroundLoading && !isContentAvailable) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MochiStrawberryPink.copy(alpha = 0.12f))
                    .border(1.dp, MochiStrawberryPink.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MochiStrawberryPink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Aún no disponemos de enlaces para reproducir este contenido en los servidores. ¡Agrégalo a tu lista para estar al tanto de su estreno!",
                    color = warmIvory,
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
