package com.tvpro.app.ui.components

import com.tvpro.core.data.model.WatchHistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TvMovieCardFormattingTest {

    @Test
    fun testCleanHtmlEntities() {
        val raw = "Spider-Man &#8211; A Través &amp; &quot;Más Allá&#039;"
        val cleaned = raw.cleanHtml()
        assertEquals("Spider-Man – A Través & \"Más Allá'", cleaned)
    }

    @Test
    fun testCleanTitleRemovesYearInParentheses() {
        assertEquals("LA LEY DE HERODES", "LA LEY DE HERODES (1999)".cleanTitle())
        assertEquals("Tombstone", "Tombstone (1993)".cleanTitle())
        assertEquals("Madre siniestra", "Madre siniestra (2026)".cleanTitle())
        assertEquals("Scissor Seven", "Scissor Seven (2018)".cleanTitle())
        assertEquals("Breaking Bad", "Breaking Bad".cleanTitle())
        assertEquals("Blade Runner 2049", "Blade Runner 2049".cleanTitle())
        assertEquals("1917", "1917 (2019)".cleanTitle())
    }

    @Test
    fun testFormatRatingValidAndInvalid() {
        // Valid ratings
        assertEquals("8.5", formatRating("8.5"))
        assertEquals("7.0", formatRating("7"))
        assertEquals("9.2", formatRating("9.24"))

        // Invalid / empty ratings must return null to avoid displaying hardcoded values
        assertNull(formatRating(null))
        assertNull(formatRating(""))
        assertNull(formatRating("   "))
        assertNull(formatRating("0"))
        assertNull(formatRating("0.0"))
        assertNull(formatRating("N/A"))
        assertNull(formatRating("n/a"))
    }

    @Test
    fun testExtractSeriesInfoPatternMatching() {
        val historyWithExplicitNumbers = WatchHistoryItem(
            movieUrl = "https://tvpro.app/series/stranger-things",
            title = "Stranger Things",
            image = "",
            rating = "8.7",
            seasonNumber = 4,
            episodeNumber = 7,
            durationMs = 3600000,
            playbackPositionMs = 1200000
        )
        val info1 = extractSeriesInfo(historyWithExplicitNumbers)
        assertNotNull(info1)
        assertEquals(4, info1?.season)
        assertEquals(7, info1?.episode)
        assertEquals("T4 : E7", info1?.formattedLabel)

        val historyWithTitlePattern = WatchHistoryItem(
            movieUrl = "https://tvpro.app/series/loki",
            title = "Loki - T2:E3 1080p",
            image = "",
            rating = null,
            durationMs = 3000000,
            playbackPositionMs = 1500000
        )
        val info2 = extractSeriesInfo(historyWithTitlePattern)
        assertNotNull(info2)
        assertEquals(2, info2?.season)
        assertEquals(3, info2?.episode)
        assertEquals("T2 : E3", info2?.formattedLabel)
    }
}
