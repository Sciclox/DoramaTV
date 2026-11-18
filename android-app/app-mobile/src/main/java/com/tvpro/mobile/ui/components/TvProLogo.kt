package com.tvpro.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tvpro.mobile.ui.theme.*

@Composable
fun TvProLogo(
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
            fontSize = 18.sp
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
                .padding(horizontal = 5.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = "모찌",
                color = MochiStrawberryPink,
                fontSize = 8.5.sp,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
