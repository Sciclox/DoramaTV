package com.tvpro.app.ui.components.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.tvpro.app.ui.theme.Inter
import com.tvpro.app.ui.theme.MochiCream
import com.tvpro.app.ui.theme.PillSurface
import com.tvpro.app.ui.theme.SubtleBorder

@Composable
fun DetailTopBar(
    onBackClick: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    isRestoringFocus: Boolean,
    isBackgroundLoading: Boolean,
    isEpisodeResolving: Boolean,
    modifier: Modifier = Modifier
) {
    val warmIvory = MochiCream
    val pillSurface = PillSurface
    val subtleBorder = SubtleBorder

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        var isBackFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBackFocused) warmIvory else pillSurface,
                contentColor = if (isBackFocused) Color.Black else warmIvory
            ),
            border = if (isBackFocused) BorderStroke(2.dp, warmIvory)
            else BorderStroke(1.dp, subtleBorder),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            modifier = Modifier
                .height(38.dp)
                .focusRequester(backButtonFocusRequester)
                .focusProperties {
                    down = downFocusRequester
                    canFocus = !isRestoringFocus
                }
                .onFocusChanged { isBackFocused = it.isFocused }
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown) {
                        try { downFocusRequester.requestFocus() } catch (_: Exception) {}
                        true
                    } else false
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Atrás",
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ATRÁS",
                fontSize = 11.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp
            )
        }

        if (isBackgroundLoading || isEpisodeResolving) {
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pillSurface)
                    .border(1.dp, subtleBorder, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = warmIvory,
                    strokeWidth = 1.8.dp,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEpisodeResolving) "PREPARANDO SECUENCIA..." else "SINCRONIZANDO METADATOS...",
                    color = warmIvory,
                    fontSize = 10.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}
