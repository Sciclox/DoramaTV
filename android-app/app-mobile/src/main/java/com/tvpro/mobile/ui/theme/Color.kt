package com.tvpro.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Official Mochi Asian Drama Design System
// =========================================================================

// Core Mochi Palette
val MochiStrawberryPink = Color(0xFFFF7597)    // Primary brand & action color (Play, active indicators)
val MochiWarmRose = Color(0xFFFF8DA1)          // Secondary accent, progress sliders, loved badges
val MochiLavender = Color(0xFFE8D7F1)          // Soft dreamy lilac (Subtitles, metadata, secondary pills)
val MochiDeepPlum = Color(0xFF130D19)          // Atmospheric night base background (OLED-friendly dark)
val MochiPlumCard = Color(0xFF211529)          // Card, surface & modal background
val MochiPlumSurface = Color(0xFF1A1121)       // Elevated modal / drawer background
val MochiPlumBorder = Color(0xFF3B2544)        // Delicate glassmorphic border
val MochiCream = Color(0xFFFFF0F5)             // Primary high-prominence text (Vanilla Cream)
val MochiMutedLilac = Color(0xFFC8B8CA)        // Synopsis, tertiary info, unselected tabs
val MochiGold = Color(0xFFFFD166)              // Golden stars, ratings, highlight badges

// Mapped Official Design Tokens (Backwards-compatibility across all screens & modals)
val LaMovieDarkBg = MochiDeepPlum
val LaMovieCardBg = MochiPlumCard
val LaMovieSurface = MochiPlumSurface
val LaMovieMuted = MochiMutedLilac
val LaMovieBorder = MochiPlumBorder
val LaMovieRed = MochiStrawberryPink

// Luxury Tokens mapped to Mochi
val WarmIvory = MochiCream
val SoftSilver = MochiLavender
val MutedSlate = MochiMutedLilac
val DeepObsidian = Color(0xFF0E0812)
val PillSurface = Color(0xD91E1324)
val SubtleBorder = Color(0x33FFB7C5) // Translucent strawberry pastel border
val CardGradientStart = Color(0xFF261830)
val CardGradientMid = Color(0xFF1C1224)
val CardGradientEnd = Color(0xFF130D19)

// Direct aliases
val TvProDarkBg = LaMovieDarkBg
val TvProPureBlack = DeepObsidian
val TvProSurface = LaMovieSurface
val TvProCardBg = LaMovieCardBg
val TvProBorder = LaMovieBorder
val TvProMuted = LaMovieMuted
val TvProWhite = Color(0xFFFFFFFF)
val TvProYellow = MochiGold
val TvProRed = LaMovieRed

// Legacy compatibility
val NetflixRed = LaMovieRed
val NetflixDarkRed = Color(0xFFE0567A)
val NetflixBlack = LaMovieDarkBg
val NetflixPureBlack = DeepObsidian
val NetflixDarkGrey = LaMovieSurface
val NetflixMediumGrey = LaMovieCardBg
val NetflixLightGrey = LaMovieMuted
val NetflixWhite = Color(0xFFFFFFFF)
