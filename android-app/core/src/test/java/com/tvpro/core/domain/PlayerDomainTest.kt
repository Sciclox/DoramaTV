package com.tvpro.core.domain

import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.StreamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDomainTest {

    @Test
    fun testPlayerTimeUtilsFormatting() {
        assertEquals("00:00:00", PlayerTimeUtils.formatTimeHms(0L))
        assertEquals("00:01:05", PlayerTimeUtils.formatTimeHms(65L))
        assertEquals("01:01:01", PlayerTimeUtils.formatTimeHms(3661L))

        assertEquals("00:00", PlayerTimeUtils.formatTimeSec(0L))
        assertEquals("01:05", PlayerTimeUtils.formatTimeSec(65L))
        assertEquals("1:01:01", PlayerTimeUtils.formatTimeSec(3661L))
    }

    @Test
    fun testDeadStreamDetection() {
        assertTrue(StreamRanker.isDeadStream("Este archivo de video no se puede reproducir. (Código de Error: 232001)"))
        assertTrue(StreamRanker.isDeadStream("This video is expired or has been deleted from server"))
        assertTrue(StreamRanker.isDeadStream("Click Allow if you are not a robot"))
        assertTrue(StreamRanker.isDeadStream("Click play button in order to verify you're a human"))
        assertFalse(StreamRanker.isDeadStream("Reproduciendo Efectos Colaterales Temporada 1"))
    }

    @Test
    fun testStreamRankingPrioritizesNativeAndFastHosts() {
        val mixedStreams = listOf(
            StreamItem(url = "https://slow-embed.com/play/1", type = "EMBED", source = "Servidor 1: Lento"),
            StreamItem(url = "https://voe.sx/embed/fast", type = "EMBED", source = "Servidor 2: Voe"),
            StreamItem(url = "https://direct.cdn.com/stream.m3u8", type = "HLS", source = "Servidor 3: Directo"),
            StreamItem(url = "https://ok.ru/videoembed/123", type = "EMBED", source = "Servidor 4: Ok.ru")
        )

        val ranked = StreamRanker.rankStreams(mixedStreams)
        assertEquals("https://direct.cdn.com/stream.m3u8", ranked[0].url)
        assertEquals("https://ok.ru/videoembed/123", ranked[1].url)
        assertEquals("https://voe.sx/embed/fast", ranked[2].url)
    }

    @Test
    fun testFailoverPicksNextAliveServer() {
        val streams = listOf(
            StreamItem(url = "https://server1.com/embed/1", source = "S1"),
            StreamItem(url = "https://server2.com/embed/2", source = "S2")
        )
        val failed = setOf("https://server1.com/embed/1")

        val next = StreamRanker.findNextAvailableStream(streams, failed)
        assertNotNull(next)
        assertEquals("https://server2.com/embed/2", next?.url)
    }

    @Test
    fun testEpisodeNavigation() {
        val episodes = listOf(
            EpisodeItem(id = 1, episodeNumber = 1, seasonNumber = 1, title = "E1", overview = "", image = ""),
            EpisodeItem(id = 2, episodeNumber = 2, seasonNumber = 1, title = "E2", overview = "", image = ""),
            EpisodeItem(id = 3, episodeNumber = 1, seasonNumber = 2, title = "S2E1", overview = "", image = "")
        )

        val nextSameSeason = EpisodeNavigator.findNextEpisode(1, 1, 2, episodes)
        assertEquals(2, nextSameSeason?.episodeNumber)

        val nextAcrossSeason = EpisodeNavigator.findNextEpisode(1, 2, 2, episodes)
        assertEquals(2, nextAcrossSeason?.seasonNumber)
        assertEquals(1, nextAcrossSeason?.episodeNumber)

        val nextAtEnd = EpisodeNavigator.findNextEpisode(2, 1, 2, episodes)
        assertNull(nextAtEnd)
    }
}
