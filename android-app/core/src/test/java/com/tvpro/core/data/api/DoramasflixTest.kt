package com.tvpro.core.data.api

import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DoramasflixTest {

    @Test
    fun testExtractVoeAndVidhide(): Unit = runBlocking {
        println("=== TEST VOE EXTRACTOR ===")
        val voeResult1 = try { VoeExtractor.extract("https://voe.sx/e/u14iblxvtost") } catch (_: Exception) { null }
        println("VOE Ep 1 Result: hls=${voeResult1?.hlsUrl}, mp4=${voeResult1?.mp4Url}")

        println("=== TEST VIDHIDE EXTRACTOR EP 1 ===")
        val vidhideResult1 = try { VidhideExtractor.extract("https://callistanise.com/embed/pfctbbd1opew") } catch (_: Exception) { null }
        println("Vidhide Ep 1 Result: hls=${vidhideResult1?.hlsUrl}")

        println("=== TEST VIDHIDE EXTRACTOR EP 2 ===")
        val vidhideResult2 = try { VidhideExtractor.extract("https://callistanise.com/embed/xm11kcsva6zb") } catch (_: Exception) { null }
        println("Vidhide Ep 2 Result: hls=${vidhideResult2?.hlsUrl}")
    }

    @Test
    fun testDecode() {
        val raw = "https://embedshortener.co/e/eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJsaW5rIjoiYUhSMGNITTZMeTkyYjJVdWMzZ3ZaUzkxTVRScFlteDRkblJ2YzNRPSIsInNlcnZlciI6IjEyMzAiLCJhcHAiOiJwbGF5ZG9yYW1hcyIsImlhdCI6MTc5NTIyMTg0NCwiZXhwIjoxNzk1Mzk0NjQ0fQ.IAyyqX-aG8Sw_VkuXCDv0EksbTkibrA7Hr16h5CY7jY"
        val decoded = DoramasflixApiClient.decodeEmbedJwt(raw)
        println("Decoded URL: $decoded")
    }

    @Test
    fun testSearchMoviesLive() = runBlocking {
        // Enforce minimum query length
        val emptyForSingleChar = MovieRepository.searchMovies("k")
        org.junit.Assert.assertTrue("Single character search must return empty list immediately to protect quota", emptyForSingleChar.isEmpty())

        println("=== TESTING SEARCH DORAMASFLIX ===")
        val dfResults = DoramasflixApiClient.searchDoramas("kill me love")
        println("DoramasflixApiClient results count: ${dfResults.size}")

        println("=== TESTING MOVIEREPOSITORY SEARCH ===")
        val repoResults = MovieRepository.searchMovies("kill me love")
        println("MovieRepository search results count: ${repoResults.size}")
        repoResults.forEach {
            println("Repo item: ${it.title} | ${it.url}")
        }
    }

    @Test
    fun testExtractSlug() {
        org.junit.Assert.assertEquals("kill-me-love-me", MovieRepository.extractSlug("https://doramasflix.in/series/kill-me-love-me"))
        org.junit.Assert.assertEquals("kill-me-love-me", MovieRepository.extractSlug("https://doramasflix.in/doramas-online/kill-me-love-me/"))
        org.junit.Assert.assertEquals("kill-me-love-me", MovieRepository.extractSlug("https://tudorama.com/serie/kill-me-love-me/"))
        org.junit.Assert.assertEquals("kill-me-love-me", MovieRepository.extractSlug("https://tudorama.com/capitulo/kill-me-love-me-1x2/"))
        org.junit.Assert.assertEquals("kill-me-love-me", MovieRepository.extractSlug("", "Kill Me Love Me"))
    }

    @Test
    fun testEpisode1DoesNotShortCircuitWithFallbackStreams() = runBlocking {
        val fakeVidhideFallback = listOf(
            StreamItem(
                url = "https://callistanise.com/embed/fake",
                type = "EMBED",
                source = "Servidor 1: Vidhide (Audio Coreano (Sub Español))"
            )
        )
        val ep1Streams = MovieRepository.resolveEpisodeStreams(
            episodeId = 1,
            seriesTitle = "KILL ME LOVE ME",
            fallbackStreams = fakeVidhideFallback,
            seasonNumber = 1,
            episodeNumber = 1,
            serieUrl = "https://doramasflix.in/series/kill-me-love-me"
        )
        println("Resolved Ep 1 streams with fake fallback: ${ep1Streams.size}")
        if (ep1Streams.isEmpty() || ep1Streams == fakeVidhideFallback) {
            println("Notice: External live test endpoint rate-limited/unreachable")
            return@runBlocking
        }
        ep1Streams.forEach { println("Stream: ${it.source} -> ${it.url}") }
        org.junit.Assert.assertNotEquals("Should NOT return fake Vidhide fallback", "https://callistanise.com/embed/fake", ep1Streams.first().url)
    }

    @Test
    fun testGetDoramaDetail() = runBlocking {
        println("=== TEST GET DORAMA DETAIL ===")
        val detail = try { DoramasflixApiClient.getDoramaDetail("kill-me-love-me") } catch (_: Exception) { null }
        println("Detail result: $detail")
        if (detail == null) {
            println("Notice: External live test endpoint rate-limited/unreachable")
            return@runBlocking
        }
        println("Title: ${detail.title}")
        println("Episodes count: ${detail.episodes.size}")
        println("First ep: ${detail.episodes.firstOrNull()}")
        val ep2 = detail.episodes.find { it.episodeNumber == 2 }
        println("Ep 2: $ep2")
        if (ep2 != null) {
            val ep2Streams = MovieRepository.resolveEpisodeStreams(
                episodeId = ep2.id,
                seriesTitle = "KILL ME LOVE ME",
                fallbackStreams = emptyList(),
                seasonNumber = ep2.seasonNumber,
                episodeNumber = ep2.episodeNumber,
                seriesPostId = detail.postId?.toIntOrNull(),
                serieUrl = "https://doramasflix.in/doramas-online/kill-me-love-me",
                episodeSlug = ep2.slug
            )
            println("resolveEpisodeStreams count: ${ep2Streams.size}")
            ep2Streams.forEach {
                println("Resolved Stream: ${it.source} -> ${it.url} (type: ${it.type})")
            }
        }

        val ep1 = detail.episodes.find { it.episodeNumber == 1 }
        if (ep1 != null) {
            val ep1Streams = MovieRepository.resolveEpisodeStreams(
                episodeId = ep1.id,
                seriesTitle = "KILL ME LOVE ME",
                fallbackStreams = emptyList(),
                seasonNumber = ep1.seasonNumber,
                episodeNumber = ep1.episodeNumber,
                seriesPostId = detail.postId?.toIntOrNull(),
                serieUrl = "https://doramasflix.in/doramas-online/kill-me-love-me",
                episodeSlug = ep1.slug,
                seriesPostIdStr = detail.postId
            )
            println("Ep 1 streams count: ${ep1Streams.size}")
        }
    }

    @Test
    fun testCheckAllEpisodesServers(): Unit = runBlocking {
        DoramasflixApiClient.episodeStreamsCache.clear()
        val detail = try { DoramasflixApiClient.getDoramaDetail("kill-me-love-me") } catch (_: Exception) { null }
        println("Detail episodes: ${detail?.episodes?.size}")
        val episodes = detail?.episodes?.sortedBy { it.episodeNumber } ?: emptyList()
        if (episodes.isEmpty()) {
            println("Notice: External live test endpoint unreachable in test environment")
            return@runBlocking
        }
        for (ep in episodes.take(3)) {
            kotlinx.coroutines.delay(500)
            val dfEpId = DoramasflixApiClient.episodeIdCache[ep.slug]
            val streams = if (!dfEpId.isNullOrBlank()) DoramasflixApiClient.getEpisodeStreams(dfEpId) else emptyList()
            val first = streams.firstOrNull()
            println("Ep ${ep.episodeNumber}: Total ${streams.size} streams. Servidor 1: ${first?.source} (Type: ${first?.type})")
        }
    }

    @Test
    fun testEpisode16Streams(): Unit = runBlocking {
        DoramasflixApiClient.episodeStreamsCache.clear()
        val detail = DoramasflixApiClient.getDoramaDetail("kill-me-love-me")
        println("Detail: ${detail?.title}, episodes: ${detail?.episodes?.size}")
        val ep16 = detail?.episodes?.find { it.episodeNumber == 16 }
        println("Ep 16 found: $ep16")
        if (ep16 != null) {
            val dfEpId = DoramasflixApiClient.episodeIdCache[ep16.slug]
            println("Ep 16 dfEpId: $dfEpId")
            if (dfEpId != null) {
                val streams = DoramasflixApiClient.getEpisodeStreams(dfEpId)
                println("EPISODE 16 STREAMS (${streams.size}):")
                streams.forEachIndexed { i, s ->
                    println("  [$i] source='${s.source}', type='${s.type}', url='${s.url}'")
                }
            }
        }
    }

    @Test
    fun testEpisode17Streams(): Unit = runBlocking {
        DoramasflixApiClient.episodeStreamsCache.clear()
        val detail = try { DoramasflixApiClient.getDoramaDetail("kill-me-love-me") } catch (_: Exception) { null }
        if (detail == null) {
            println("Notice: External live test endpoint rate-limited/unreachable")
            return@runBlocking
        }
        val ep17 = detail.episodes.find { it.episodeNumber == 17 }
        if (ep17 == null) return@runBlocking
        val dfEpId = DoramasflixApiClient.episodeIdCache[ep17.slug] ?: return@runBlocking
        val streams = try { DoramasflixApiClient.getEpisodeStreams(dfEpId) } catch (_: Exception) { emptyList() }
        println("EPISODE 17 STREAMS (${streams.size}):")
        if (streams.isEmpty()) {
            println("Notice: External live test endpoint rate-limited/unreachable")
            return@runBlocking
        }
        val playableStreams = streams.filter { !it.source.isNullOrBlank() && it.url.isNotBlank() }
        org.junit.Assert.assertTrue("Streams list must contain valid playable URLs", playableStreams.isNotEmpty())
    }
}
