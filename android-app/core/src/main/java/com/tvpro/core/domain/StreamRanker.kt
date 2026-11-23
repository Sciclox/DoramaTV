package com.tvpro.core.domain

import com.tvpro.core.data.model.StreamItem

object StreamRanker {

    private val DEAD_KEYWORDS = listOf(
        "232001",
        "no se puede reproducir",
        "archivo de video no se puede reproducir",
        "video file cannot be played",
        "error loading media",
        "the media playback was aborted",
        "expired or has been deleted",
        "no longer available",
        "file was deleted",
        "video deleted",
        "contenido no disponible",
        "not a robot",
        "click allow",
        "e-captcha",
        "verify you are human",
        "verify you're a human",
        "verify you are a human",
        "verify that you are human",
        "in order to verify",
        "click play button in order",
        "click play button to verify",
        "click play button"
    )

    /**
     * Checks if a stream error message or page content indicates a dead or scam stream.
     */
    fun isDeadStream(contentOrError: String): Boolean {
        val lower = contentOrError.lowercase()
        return DEAD_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Calculates the sorting priority of a stream item (lower is faster / more reliable).
     */
    fun getStreamPriority(stream: StreamItem): Int {
        val u = stream.url.lowercase()
        val src = (stream.source ?: "").lowercase()
        return when {
            stream.type.equals("HLS", ignoreCase = true) ||
            stream.type.equals("MP4", ignoreCase = true) ||
            u.contains(".m3u8") ||
            u.contains(".mp4") -> 0
            u.contains("ok.ru") || src.contains("ok.ru") -> 1
            u.contains("primeload") || src.contains("primeload") -> 2
            u.contains("flaswish") || src.contains("flashwish") -> 3
            u.contains("voe") || u.contains("megankid") || u.contains("arianathankspecial") || src.contains("voe") -> 4
            u.contains("abyss") || src.contains("abyss") -> 5
            u.contains("minochinos") || u.contains("earnvids") || src.contains("earnvids") -> 6
            u.contains("uqload") || src.contains("uqload") -> 7
            u.contains("mp4upload") || src.contains("mp4upload") -> 8
            u.contains("mixdrop") || src.contains("mixdrop") -> 9
            u.contains("streamtape") || src.contains("streamtape") -> 14
            u.contains("dood") || u.contains("do7go") || u.contains("playmogo") || src.contains("dood") -> 15
            u.contains("callistanise") || u.contains("vidhide") || src.contains("vidhide") -> 16
            u.contains("vimeos") -> 17
            u.contains("filemoon") || u.contains("byse") || u.contains("bysesukior") || src.contains("filemoon") || src.contains("byse") -> 90
            else -> 10
        }
    }

    /**
     * Sorts available streams placing direct/native streams and fast hosts first.
     */
    fun rankStreams(streams: List<StreamItem>): List<StreamItem> {
        return streams.sortedWith(compareBy { getStreamPriority(it) })
    }

    /**
     * Picks the next alive stream that has not failed yet.
     */
    fun findNextAvailableStream(streams: List<StreamItem>, failedUrls: Set<String>): StreamItem? {
        val alive = streams.filter { it.url !in failedUrls }
        return rankStreams(alive).firstOrNull()
    }
}
