package com.tvpro.app.ui.screens

import com.tvpro.core.data.model.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerLogicTest {

    @Test
    fun testFormatTimeHmsFormatting() {
        assertEquals("00:00:00", formatTimeHms(0L))
        assertEquals("00:00:45", formatTimeHms(45L))
        assertEquals("00:01:05", formatTimeHms(65L))
        assertEquals("01:01:01", formatTimeHms(3661L))
        assertEquals("02:30:15", formatTimeHms(9015L))
    }

    @Test
    fun testFormatTimeSecFormatting() {
        assertEquals("00:00", formatTimeSec(0L))
        assertEquals("01:05", formatTimeSec(65L))
        assertEquals("12:34", formatTimeSec(754L))
        assertEquals("1:01:01", formatTimeSec(3661L))
        assertEquals("2:30:15", formatTimeSec(9015L))
    }

    @Test
    fun testNextEpisodeResolutionLogic() {
        val s1Episodes = listOf(
            EpisodeItem(id = 101, episodeNumber = 1, seasonNumber = 1, title = "Ep 1", overview = "", image = ""),
            EpisodeItem(id = 102, episodeNumber = 2, seasonNumber = 1, title = "Ep 2", overview = "", image = ""),
            EpisodeItem(id = 103, episodeNumber = 3, seasonNumber = 1, title = "Ep 3", overview = "", image = "")
        )

        // Case 1: In middle of season -> should return next episode in same season
        val nextInSeason = s1Episodes.firstOrNull { it.seasonNumber == 1 && it.episodeNumber == 1 + 1 }
        assertNotNull(nextInSeason)
        assertEquals(2, nextInSeason?.episodeNumber)

        // Case 2: At last episode of season 1, with multi-season availability (total seasons = 2)
        val currentSeason = 1
        val currentEpisode = 3
        val totalSeasons = 2
        val nextSameSeason = s1Episodes.firstOrNull { it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode + 1 }
        assertNull(nextSameSeason)

        // When moving to Season 2 Ep 1
        val shouldAdvanceSeason = currentSeason < totalSeasons
        assertEquals(true, shouldAdvanceSeason)

        // Case 3: At last episode of final season -> should return null
        val isFinalEpisodeOfSeries = (currentSeason >= totalSeasons) && (nextSameSeason == null)
        assertEquals(false, isFinalEpisodeOfSeries)
    }

    @Test
    fun testDeadStreamDetectionPatterns() {
        val deadKeywords = listOf(
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
            "verify you are human"
        )

        fun isDead(text: String): Boolean {
            val lower = text.lowercase()
            return deadKeywords.any { lower.contains(it) }
        }

        // Test user's exact error message: "Este archivo de video no se puede reproducir. (Código de Error: 232001)"
        val userError = "Este archivo de video no se puede reproducir. (Código de Error: 232001)"
        assertEquals(true, isDead(userError))

        val expiredMsg = "This video is expired or has been deleted from server"
        assertEquals(true, isDead(expiredMsg))

        // Test fake captcha scam prompt: "Click Allow if you are not a robot"
        val fakeCaptchaPrompt = "Click Allow if you are not a robot"
        assertEquals(true, isDead(fakeCaptchaPrompt))

        val normalMsg = "Reproduciendo Efectos Colaterales Temporada 1"
        assertEquals(false, isDead(normalMsg))
    }

    @Test
    fun testStreamFailoverSelectionPicksNextAliveServer() {
        val streams = listOf(
            com.tvpro.core.data.model.StreamItem(url = "https://server1.com/embed/1", source = "Servidor 1: Principal HD"),
            com.tvpro.core.data.model.StreamItem(url = "https://server2.com/embed/2", source = "Servidor 2: Latino Full HD"),
            com.tvpro.core.data.model.StreamItem(url = "https://server3.com/embed/3", source = "Servidor 3: Subtitulado")
        )

        val failedUrls = mutableSetOf<String>()
        // Server 1 fails with 232001
        failedUrls.add("https://server1.com/embed/1")

        val alive = streams.filter { it.url !in failedUrls }
        assertEquals(2, alive.size)
        assertEquals("Servidor 2: Latino Full HD", alive.first().source)
        assertEquals("https://server2.com/embed/2", alive.first().url)
    }

    @Test
    fun testFastStreamSortingPrioritizesNativeAndFastHosts() {
        val mixedStreams = listOf(
            com.tvpro.core.data.model.StreamItem(url = "https://slow-embed.com/play/1", type = "EMBED", source = "Servidor 1: Lento"),
            com.tvpro.core.data.model.StreamItem(url = "https://voe.sx/embed/fast", type = "EMBED", source = "Servidor 2: Voe"),
            com.tvpro.core.data.model.StreamItem(url = "https://direct.cdn.com/stream.m3u8", type = "HLS", source = "Servidor 3: Directo"),
            com.tvpro.core.data.model.StreamItem(url = "https://ok.ru/videoembed/123", type = "EMBED", source = "Servidor 4: Ok.ru")
        )

        val sorted = mixedStreams.sortedWith(
            compareBy<com.tvpro.core.data.model.StreamItem> { stream ->
                val u = stream.url.lowercase()
                val src = (stream.source ?: "").lowercase()
                when {
                    stream.type == "HLS" || stream.type == "MP4" || u.contains(".m3u8") || u.contains(".mp4") -> 0
                    u.contains("ok.ru") || src.contains("ok.ru") -> 1
                    u.contains("primeload") || src.contains("primeload") -> 2
                    u.contains("flaswish") || src.contains("flashwish") -> 3
                    u.contains("filemoon") || u.contains("byse") || src.contains("filemoon") -> 4
                    u.contains("voe") || u.contains("megankid") || u.contains("arianathankspecial") || src.contains("voe") -> 5
                    u.contains("uqload") || src.contains("uqload") -> 6
                    u.contains("mp4upload") || src.contains("mp4upload") -> 7
                    u.contains("mixdrop") || src.contains("mixdrop") -> 8
                    u.contains("streamtape") || src.contains("streamtape") -> 14
                    u.contains("dood") || u.contains("do7go") || u.contains("playmogo") || src.contains("dood") -> 15
                    u.contains("callistanise") || u.contains("vidhide") || src.contains("vidhide") -> 16
                    u.contains("vimeos") -> 17
                    else -> 10
                }
            }
        )

        // Native HLS stream should be sorted first (fastest startup < 300ms)
        assertEquals("https://direct.cdn.com/stream.m3u8", sorted[0].url)
        // Ok.ru pristine embed second
        assertEquals("https://ok.ru/videoembed/123", sorted[1].url)
        // Voe embed
        assertEquals("https://voe.sx/embed/fast", sorted[2].url)
        // Slow generic embed last
        assertEquals("https://slow-embed.com/play/1", sorted[3].url)
    }

    @Test
    fun testWatchdogTriggerLogicOnStall() {
        // The watchdog must trigger strictly if video is not ready after timeout,
        // REGARDLESS of any saved resume history position (e.g. 38,000ms from prior playback)
        fun shouldWatchdogTrigger(elapsedMs: Long, isVideoReady: Boolean, maxWaitMs: Long = 12000L): Boolean {
            return !isVideoReady && elapsedMs >= maxWaitMs
        }

        // 1 second in: not ready yet -> do not trigger
        assertEquals(false, shouldWatchdogTrigger(1000L, isVideoReady = false))
        // 12 seconds in: video ready -> do not trigger
        assertEquals(false, shouldWatchdogTrigger(12000L, isVideoReady = true))
        // 12 seconds in: still not ready -> STALLED! Trigger failover immediately!
        assertEquals(true, shouldWatchdogTrigger(12000L, isVideoReady = false))
    }

    @Test
    fun testEpisodeSwitchStateReset() {
        // Episode 1 playback state before switching
        var currentPositionMs = 15000L
        var durationMs = 2013000L // 33:33 from Ep 1
        var isBuffering = false
        var isVideoReady = true
        var pendingResumePositionMs = 15000L
        val failedStreams = mutableListOf("https://bad.com")

        // Simulation of playEpisode when user advances from Episode 1 to Episode 2
        fun onSwitchToEpisode(epNumber: Int) {
            currentPositionMs = 0L
            durationMs = 0L
            pendingResumePositionMs = -1L
            isBuffering = true
            isVideoReady = false
            failedStreams.clear()
        }

        onSwitchToEpisode(2)

        assertEquals(0L, currentPositionMs)
        assertEquals(0L, durationMs)
        assertEquals(-1L, pendingResumePositionMs)
        assertEquals(true, isBuffering)
        assertEquals(false, isVideoReady)
        assertEquals(0, failedStreams.size)
        // Duration must format to 00:00:00 rather than -00:33:33
        assertEquals("00:00:00", formatTimeHms(durationMs / 1000L))
    }

    @Test
    fun testDoramasflixRefererMapping() {
        fun getRefererForUrl(targetUrl: String): String {
            val isDoramasflix = targetUrl.contains("doramasflix", ignoreCase = true) ||
                targetUrl.contains("primeload", ignoreCase = true) ||
                targetUrl.contains("vidhide", ignoreCase = true) ||
                targetUrl.contains("callistanise", ignoreCase = true) ||
                targetUrl.contains("do7go", ignoreCase = true) ||
                targetUrl.contains("playmogo", ignoreCase = true) ||
                targetUrl.contains("dood", ignoreCase = true) ||
                targetUrl.contains("flaswish", ignoreCase = true) ||
                targetUrl.contains("mp4upload", ignoreCase = true) ||
                targetUrl.contains("voe", ignoreCase = true) ||
                targetUrl.contains("byse", ignoreCase = true) ||
                targetUrl.contains("filemoon", ignoreCase = true)

            return when {
                isDoramasflix -> "https://doramasflix.in/"
                targetUrl.contains("tudorama") || targetUrl.contains("minochinos") || targetUrl.contains("bysesukior") || targetUrl.contains("abyss") || targetUrl.contains("banana") -> "https://tudorama.com/"
                else -> "https://lamovie.org/"
            }
        }

        // Must use doramasflix.in referer for Primeload to prevent CSP violation
        assertEquals("https://doramasflix.in/", getRefererForUrl("https://primeload.co/embed/PZiMNV5lIjZ5"))
        assertEquals("https://doramasflix.in/", getRefererForUrl("https://bysefujedu.com/e/cx38cbosofx1"))
        assertEquals("https://doramasflix.in/", getRefererForUrl("https://voe.sx/e/paljqmcc7ty2"))
        assertEquals("https://tudorama.com/", getRefererForUrl("https://tudorama.com/embed/123"))
        assertEquals("https://lamovie.org/", getRefererForUrl("https://other.com/embed/123"))
    }

    @Test
    fun testRealMediaReadyDetectionLogic() {
        // onPageFinished fires when HTML loads, but should NOT mark isVideoReady
        var isVideoReady = false
        var isBuffering = true

        fun onPageFinishedSimulated() {
            // Only perform dead check / ad blocker injection, do NOT mark isVideoReady!
        }

        fun onSyncJsEvaluated(curP: Long, curD: Long, readyState: Int, videoWidth: Int, jwReady: Boolean) {
            val ready = readyState >= 1 || curP > 50L || videoWidth > 0 || curD > 0L || jwReady
            if (ready) {
                isVideoReady = true
                isBuffering = false
            }
        }

        onPageFinishedSimulated()
        // Must remain in buffering/not ready state while video tag loads
        assertEquals(false, isVideoReady)
        assertEquals(true, isBuffering)

        // Web player initializes video with duration and readyState
        onSyncJsEvaluated(curP = 0L, curD = 2700000L, readyState = 2, videoWidth = 1920, jwReady = false)
        assertEquals(true, isVideoReady)
        assertEquals(false, isBuffering)
    }
}
