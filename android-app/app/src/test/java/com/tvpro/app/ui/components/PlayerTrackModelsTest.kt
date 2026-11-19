package com.tvpro.app.ui.components

import com.tvpro.app.ui.components.player.getCleanServerDisplayName
import com.tvpro.core.data.model.StreamItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTrackModelsTest {

    @Test
    fun testGetCleanServerDisplayNameDistinctLabels() {
        val okruHls = StreamItem(
            url = "https://vd409.okcdn.ru/video.m3u8?cmd=videoPlayerCdn",
            type = "HLS",
            source = "Ok.ru (HLS) (HLS) (Audio Coreano)"
        )
        val okruHdMp4 = StreamItem(
            url = "https://vd409.okcdn.ru/?expires=123&type=3",
            type = "MP4",
            source = "Ok.ru (HD) (Direct Full HD) (Audio Coreano)"
        )
        val okruSdMp4 = StreamItem(
            url = "https://vd409.okcdn.ru/?expires=123&type=2",
            type = "MP4",
            source = "Ok.ru (SD) (Direct Full HD) (Audio Coreano)"
        )
        val okruWeb = StreamItem(
            url = "https://ok.ru/videoembed/8731973323413",
            type = "EMBED",
            source = "Ok.ru (Full HD) (Web Player) (Audio Coreano)"
        )
        val primeload = StreamItem(
            url = "https://primeload.co/embed/cg7U8mqxrsYH",
            type = "EMBED",
            source = "Primeload (Oficial HD) (Web Player)"
        )
        val voeHls = StreamItem(
            url = "https://cloudwindow.net/master.m3u8",
            type = "HLS",
            source = "VOE (HLS) (HLS)"
        )
        val streamtape = StreamItem(
            url = "https://streamtape.com/e/olrX1zpXAXFJLKd/",
            type = "EMBED",
            source = "Streamtape (Web Player)"
        )

        assertEquals("Ok.ru [Auto HLS]", getCleanServerDisplayName(okruHls))
        assertEquals("Ok.ru [720p HD]", getCleanServerDisplayName(okruHdMp4))
        assertEquals("Ok.ru [480p SD]", getCleanServerDisplayName(okruSdMp4))
        assertEquals("Ok.ru [Web]", getCleanServerDisplayName(okruWeb))
        assertEquals("Primeload [Web]", getCleanServerDisplayName(primeload))
        assertEquals("VOE [Auto HLS]", getCleanServerDisplayName(voeHls))
        assertEquals("Streamtape", getCleanServerDisplayName(streamtape))
    }
}
