package com.tvpro.mobile.ui.components

import com.tvpro.core.data.model.WatchHistoryItem

fun String.cleanHtml(): String {
    return this
        .replace("&#8211;", "–")
        .replace("&#8212;", "—")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

fun String.cleanTitle(): String {
    return this
        .cleanHtml()
        .replace(Regex("""\s*\(\s*\d{4}\s*\)\s*"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

fun cleanEpisodeTitle(rawTitle: String, seriesTitle: String, episodeNumber: Int): String {
    var s = rawTitle.cleanHtml().trim()
    val nameCandidates = listOf(
        seriesTitle.cleanHtml(),
        seriesTitle.cleanTitle(),
        seriesTitle
    ).filter { it.isNotBlank() }.distinct()

    nameCandidates.forEach { name ->
        s = s.replace(Regex("^\\Q$name\\E\\s*[:\\-·/]?\\s*", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("\\Q$name\\E", RegexOption.IGNORE_CASE), "")
    }
    s = s.replace(Regex("""(?i)\b(?:Temporada|Temp|T)\s*\d+\b\s*[:\-·/]?\s*"""), "")
    s = s.replace(Regex("""(?i)\b(?:Episodio|Capítulo|Cap|E)\s*\d+\b\s*[:\-·/]?\s*"""), "")
    s = s.trim().trim(':', '-', '·', ' ', '/', '.')

    return if (s.isBlank() ||
        s.equals("Capítulo $episodeNumber", ignoreCase = true) ||
        s.equals("Episodio $episodeNumber", ignoreCase = true) ||
        s.equals("Episodio", ignoreCase = true) ||
        s.equals("Capítulo", ignoreCase = true) ||
        nameCandidates.any { candidate -> s.contains(candidate, ignoreCase = true) }
    ) {
        "Episodio $episodeNumber"
    } else {
        s
    }
}

data class SeriesEpisodeInfo(
    val season: Int?,
    val episode: Int?,
    val formattedLabel: String
)

fun extractSeriesInfo(historyItem: WatchHistoryItem): SeriesEpisodeInfo? {
    if (historyItem.seasonNumber != null && historyItem.episodeNumber != null) {
        return SeriesEpisodeInfo(
            season = historyItem.seasonNumber,
            episode = historyItem.episodeNumber,
            formattedLabel = "T${historyItem.seasonNumber} : E${historyItem.episodeNumber}"
        )
    }
    if (historyItem.episodeNumber != null) {
        val s = historyItem.seasonNumber ?: 1
        return SeriesEpisodeInfo(
            season = s,
            episode = historyItem.episodeNumber,
            formattedLabel = if (historyItem.seasonNumber != null) "T$s : E${historyItem.episodeNumber}" else "E${historyItem.episodeNumber}"
        )
    }

    val title = historyItem.title
    val url = historyItem.movieUrl

    val tPattern = Regex("""T(\d+)\s*[:\-·x]?\s*E(\d+)""", RegexOption.IGNORE_CASE)
    val tMatch = tPattern.find(title)
    if (tMatch != null) {
        val s = tMatch.groupValues[1].toIntOrNull() ?: 1
        val e = tMatch.groupValues[2].toIntOrNull() ?: 1
        return SeriesEpisodeInfo(s, e, "T$s : E$e")
    }

    val tempEpPattern = Regex("""Temporada\s*(\d+).*?(?:Episodio|Cap[ií]tulo|Cap\.?)\s*(\d+)""", RegexOption.IGNORE_CASE)
    val tempEpMatch = tempEpPattern.find(title)
    if (tempEpMatch != null) {
        val s = tempEpMatch.groupValues[1].toIntOrNull() ?: 1
        val e = tempEpMatch.groupValues[2].toIntOrNull() ?: 1
        return SeriesEpisodeInfo(s, e, "T$s : E$e")
    }

    val isSeriesUrl = url.contains("/series/", ignoreCase = true) ||
            url.contains("/animes/", ignoreCase = true) ||
            url.contains("/novelas/", ignoreCase = true) ||
            historyItem.isSeries
    if (isSeriesUrl) {
        return SeriesEpisodeInfo(1, 1, "T1 : E1")
    }

    return null
}
