package com.tvpro.app.ui.components.player

import androidx.media3.common.Tracks
import com.tvpro.core.data.model.StreamItem

data class ExoAudioTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean,
    val isSpanish: Boolean,
    val isEnglish: Boolean = false
)

data class ExoSubtitleTrackItem(
    val group: Tracks.Group,
    val trackIndex: Int,
    val name: String,
    val language: String,
    val isSelected: Boolean,
    val isSpanish: Boolean,
    val isEnglish: Boolean = false
)

data class DisplayLanguageOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val onSelect: () -> Unit
)

fun getCleanServerDisplayName(stream: StreamItem): String {
    val src = (stream.source ?: "").lowercase()
    val u = stream.url.lowercase()
    return when {
        src.contains("primeload") || u.contains("primeload") -> "★ Primeload [MEJOR]"
        src.contains("ok.ru") || u.contains("ok.ru") -> "Ok.ru (Full HD)"
        src.contains("voe") || u.contains("voe") -> "VOE (HD)"
        src.contains("dood") || u.contains("dood") || u.contains("playmogo") || u.contains("do7go") -> "Doodstream"
        src.contains("streamtape") || u.contains("streamtape") -> "Streamtape"
        src.contains("flaswish") || src.contains("flashwish") || u.contains("flaswish") -> "FlashWish"
        src.contains("uqload") || u.contains("uqload") -> "Uqload"
        src.contains("mp4upload") || u.contains("mp4upload") -> "Mp4Upload"
        src.contains("filemoon") || u.contains("filemoon") || u.contains("byse") -> "Filemoon"
        src.contains("vidhide") || u.contains("vidhide") || u.contains("callistanise") -> "Vidhide"
        src.contains("mixdrop") || u.contains("mixdrop") -> "Mixdrop"
        else -> {
            val clean = (stream.source ?: "Servidor")
                .replace(Regex("^Servidor\\s*\\d+\\s*:\\s*", RegexOption.IGNORE_CASE), "")
                .split("(")[0].trim()
            if (clean.isBlank()) "Servidor" else clean
        }
    }
}
