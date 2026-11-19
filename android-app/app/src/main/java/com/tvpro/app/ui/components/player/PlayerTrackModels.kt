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
    val type = (stream.type ?: "").uppercase()
    val isHls = type == "HLS" || src.contains("(hls)") || u.contains(".m3u8")
    val isDirectMp4 = type == "MP4" || src.contains("direct") || u.contains(".mp4")

    return when {
        src.contains("primeload") || u.contains("primeload") -> "★ Primeload [MEJOR]"
        src.contains("ok.ru") || u.contains("ok.ru") || src.contains("odnoklassniki") -> {
            when {
                src.contains("(hd)") -> "Ok.ru [720p HD]"
                src.contains("(sd)") -> "Ok.ru [480p SD]"
                isHls -> "Ok.ru [Auto HLS]"
                isDirectMp4 -> "Ok.ru [Direct HD]"
                else -> "Ok.ru [Web]"
            }
        }
        src.contains("voe") || u.contains("voe") -> {
            when {
                isHls -> "VOE [Auto HLS]"
                isDirectMp4 -> "VOE [Direct HD]"
                else -> "VOE [HD]"
            }
        }
        src.contains("dood") || u.contains("dood") || u.contains("playmogo") || u.contains("do7go") -> {
            if (isDirectMp4) "Doodstream [Direct]" else "Doodstream"
        }
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
