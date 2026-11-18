package com.tvpro.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WarmIvory,
    onPrimary = Color.Black,
    secondary = LaMovieCardBg,
    onSecondary = WarmIvory,
    background = LaMovieDarkBg,
    onBackground = WarmIvory,
    surface = LaMovieSurface,
    onSurface = WarmIvory
)

@Composable
fun TvProMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
