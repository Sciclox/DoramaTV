package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.app.ui.theme.*

@Composable
fun DetailExtrasContent(
    gallery: List<String>,
    displayTitle: String,
    genresList: List<String>,
    ratingValue: String,
    extrasTabFocusRequester: FocusRequester,
    onSceneClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warmIvory = MochiCream
    val mutedSlate = MochiMutedLilac
    val subtleBorder = SubtleBorder
    val glassSurface = Color(0xD9160E1E)

    Column(modifier = modifier.fillMaxWidth()) {
        if (gallery.isNotEmpty()) {
            Text(
                text = "GALERÍA DE ESCENAS · PRESIONA OK PARA AMPLIAR",
                fontSize = 10.5.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = mutedSlate,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 20.dp, start = 2.dp, end = 24.dp)
            ) {
                items(gallery) { imgUrl ->
                    var isSceneFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .height(135.dp)
                            .width(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSceneFocused) 2.dp else 1.dp,
                                color = if (isSceneFocused) warmIvory else subtleBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .focusProperties {
                                up = extrasTabFocusRequester
                            }
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionUp) {
                                    try { extrasTabFocusRequester.requestFocus() } catch (_: Exception) {}
                                    true
                                } else false
                            }
                            .onFocusChanged { focusState ->
                                isSceneFocused = focusState.isFocused
                            }
                            .clickable { onSceneClick(imgUrl) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imgUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Escena de $displayTitle",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Technical Specifications Card (Focusable for smooth D-Pad scroll)
        var isSpecsFocused by remember { mutableStateOf(false) }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = glassSurface),
            border = BorderStroke(
                width = if (isSpecsFocused) 1.5.dp else 1.dp,
                color = if (isSpecsFocused) warmIvory.copy(alpha = 0.6f) else subtleBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isSpecsFocused = it.isFocused }
                .focusable()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CALIDAD DE VIDEO", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1080p Full HD / 4K HDR10", color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Bitrate adaptativo de alta velocidad", color = mutedSlate, fontSize = 10.5.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUDIO Y SUBTÍTULOS", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Español Latino / Original", color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Subtítulos CC configurables", color = mutedSlate, fontSize = 10.5.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("GÉNEROS", color = mutedSlate, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(genresList.joinToString(", "), color = warmIvory, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Clasificación: 16+ · TMDB ★ $ratingValue", color = mutedSlate, fontSize = 10.5.sp)
                }
            }
        }
    }
}
