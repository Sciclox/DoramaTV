package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.MochiCream

@Composable
fun DetailLightboxOverlay(
    sceneUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val warmIvory = MochiCream

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF206080C))
            .clickable { onClose() }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(sceneUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Vista previa de escena",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .fillMaxHeight(0.78f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, warmIvory.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PRESIONA OK O ATRÁS PARA CERRAR LA VISTA PREVIA",
                color = warmIvory.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )
        }
    }
}
