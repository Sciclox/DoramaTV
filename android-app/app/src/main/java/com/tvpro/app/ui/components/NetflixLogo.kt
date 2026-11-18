package com.tvpro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tvpro.app.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LaMovieLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Cute Mochi Mascot mini emoji
        Text(
            text = "🍡",
            fontSize = 20.sp
        )

        // "Mochi" Brand Title
        Text(
            text = "Mochi",
            color = MochiCream,
            fontFamily = PlusJakartaSans,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        // Cute Asian Hangul capsule badge "모찌"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MochiStrawberryPink.copy(alpha = 0.20f))
                .border(
                    0.5.dp,
                    MochiStrawberryPink.copy(alpha = 0.40f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "모찌",
                color = MochiStrawberryPink,
                fontSize = 9.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NetflixLogo(modifier: Modifier = Modifier) {
    LaMovieLogo(modifier = modifier)
}
