package com.tvpro.core.data.extractor

import com.tvpro.core.data.model.SubtitleTrackData

enum class StreamFormat {
    HLS,
    MP4,
    DASH,
    EMBED
}

data class ExtractedStream(
    val url: String,
    val format: StreamFormat = StreamFormat.HLS,
    val quality: String = "Auto",
    val serverName: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitles: List<SubtitleTrackData> = emptyList()
)
