package com.tvpro.core.data.extractor

import org.junit.Assert.*
import org.junit.Test

class ExtractorRegistryTest {

    @Test
    fun testFindExtractor_VoeUrls() {
        val voeUrls = listOf(
            "https://voe.sx/e/u14iblxvtost",
            "https://megankid.com/e/abc12345",
            "https://arianathankspecial.com/e/xyz987"
        )

        for (url in voeUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("VOE", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_OkRuUrls() {
        val okUrls = listOf(
            "https://ok.ru/videoembed/789123456",
            "https://odnoklassniki.ru/video/12345"
        )

        for (url in okUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Ok.ru", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_DoodUrls() {
        val doodUrls = listOf(
            "https://do7go.com/e/b5ovrqpcsfws",
            "https://dood.to/e/12345",
            "https://doodstream.com/d/abcde",
            "https://ds2play.com/e/test99"
        )

        for (url in doodUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Doodstream", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_VidhideUrls() {
        val vidhideUrls = listOf(
            "https://vidhide.com/v/xyz123",
            "https://dramiyos.com/embed/abc"
        )

        for (url in vidhideUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Vidhide", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_UnknownUrlReturnsNull() {
        val unknownUrl = "https://example.com/video/123"
        val extractor = ExtractorRegistry.findExtractor(unknownUrl)
        assertNull("Unknown URL should return null extractor", extractor)
    }

    @Test
    fun testExtractorPriorityOrdering() {
        val voe = ExtractorRegistry.findExtractor("https://voe.sx/e/123")
        val okru = ExtractorRegistry.findExtractor("https://ok.ru/videoembed/123")
        val dood = ExtractorRegistry.findExtractor("https://do7go.com/e/123")
        val vidhide = ExtractorRegistry.findExtractor("https://vidhide.com/v/123")

        assertTrue("VOE priority must be higher than OkRu", voe!!.priority < okru!!.priority)
        assertTrue("OkRu priority must be higher than Doodstream", okru.priority < dood!!.priority)
        assertTrue("Doodstream priority must be higher than Vidhide", dood.priority < vidhide!!.priority)
    }

    @Test
    fun testToStreamItemMapping() {
        val extracted = ExtractedStream(
            url = "https://cdn.example.com/master.m3u8",
            format = StreamFormat.HLS,
            quality = "1080p Full HD",
            serverName = "VOE (HLS)"
        )

        val streamItem = with(ExtractorRegistry) { extracted.toStreamItem("Servidor 1: VOE") }

        assertEquals("HLS", streamItem.type)
        assertEquals("https://cdn.example.com/master.m3u8", streamItem.url)
        assertEquals("Servidor 1: VOE", streamItem.source)
        assertEquals("ExtractorRegistry (VOE (HLS))", streamItem.foundVia)
    }
}
