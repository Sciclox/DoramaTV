package com.tvpro.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val NetflixTvColorScheme = darkColorScheme(
    primary = NetflixRed,
    onPrimary = NetflixWhite,
    secondary = NetflixDarkGrey,
    onSecondary = NetflixWhite,
    background = NetflixBlack,
    onBackground = NetflixWhite,
    surface = NetflixDarkGrey,
    onSurface = NetflixWhite
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NetflixTvColorScheme,
        content = content
    )
}
