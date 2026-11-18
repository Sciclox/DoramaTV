package com.tvpro.mobile.ui

import com.tvpro.core.data.model.WatchHistoryItem
import com.tvpro.mobile.ui.components.cleanEpisodeTitle
import com.tvpro.mobile.ui.components.cleanHtml
import com.tvpro.mobile.ui.components.cleanTitle
import com.tvpro.mobile.ui.components.extractSeriesInfo
import com.tvpro.mobile.ui.components.matchesGenre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieUiUtilsTest {

    @Test
    fun testCleanHtmlEntities() {
        val input = "Dorama &amp; Romance &#8211; Capitulo &quot;Especial&quot; &#039;2024&#039;"
        val expected = "Dorama & Romance – Capitulo \"Especial\" '2024'"
        assertEquals(expected, input.cleanHtml())
    }

    @Test
    fun testCleanTitleRemovesYear() {
        val input = "Goblin (2016)"
        assertEquals("Goblin", input.cleanTitle())

        val inputWithSpaces = "Descendants of the Sun   ( 2016 )  "
        assertEquals("Descendants of the Sun", inputWithSpaces.cleanTitle())
    }

    @Test
    fun testCleanEpisodeTitleRedundancy() {
        // Redundant series title in episode title
        val cleaned = cleanEpisodeTitle(
            rawTitle = "Goblin - Episodio 1",
            seriesTitle = "Goblin",
            episodeNumber = 1
        )
        assertEquals("Episodio 1", cleaned)

        // Custom title
        val customCleaned = cleanEpisodeTitle(
            rawTitle = "Goblin - El primer encuentro",
            seriesTitle = "Goblin",
            episodeNumber = 1
        )
        assertEquals("El primer encuentro", customCleaned)
    }

    @Test
    fun testExtractSeriesInfoFromHistoryItem() {
        val itemWithExplicitNumbers = WatchHistoryItem(
            movieUrl = "https://tudorama.net/series/goblin",
            title = "Goblin",
            image = "https://example.com/poster.jpg",
            rating = "9.5",
            playbackPositionMs = 1000L,
            durationMs = 3600000L,
            timestamp = System.currentTimeMillis(),
            isSeries = true,
            seasonNumber = 1,
            episodeNumber = 5
        )

        val info = extractSeriesInfo(itemWithExplicitNumbers)
        assertNotNull(info)
        assertEquals(1, info?.season)
        assertEquals(5, info?.episode)
        assertEquals("T1 : E5", info?.formattedLabel)
    }

    @Test
    fun testMatchesGenreFiltering() {
        val genres = listOf("K-Drama", "Romance", "Comedia Romántica")

        assertTrue(matchesGenre(genres, "Todos"))
        assertTrue(matchesGenre(genres, "Romance"))
        assertTrue(matchesGenre(genres, "Comedia"))
        assertFalse(matchesGenre(genres, "Terror"))
        assertFalse(matchesGenre(genres, "Acción"))
    }
}
