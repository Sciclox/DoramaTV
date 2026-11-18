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
    fun testFindExtractor_StreamtapeUrls() {
        val streamtapeUrls = listOf(
            "https://streamtape.com/e/6qVOgq4bJ1I9G2",
            "https://streamta.pe/v/abc12345",
            "https://streamtape.to/e/xyz",
            "https://stape.fun/e/test1"
        )

        for (url in streamtapeUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Streamtape", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_FilemoonUrls() {
        val filemoonUrls = listOf(
            "https://filemoon.sx/e/kjn28s721",
            "https://filemoon.to/d/abcde",
            "https://moonplayer.xyz/e/xyz987"
        )

        for (url in filemoonUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Filemoon", extractor?.name)
        }
    }

    @Test
    fun testFindExtractor_StreamwishUrls() {
        val streamwishUrls = listOf(
            "https://streamwish.to/e/abc1234",
            "https://strwish.com/e/xyz",
            "https://awish.pro/e/test99"
        )

        for (url in streamwishUrls) {
            val extractor = ExtractorRegistry.findExtractor(url)
            assertNotNull("Extractor should be found for $url", extractor)
            assertEquals("Streamwish", extractor?.name)
        }
    }

    @Test
    fun testStreamtapeHtmlParser() {
        val mockHtml = """
            <html>
            <body>
            <script>
            document.getElementById('robotlink').innerHTML = '//streamtape.com/get_video?id=foobar&token=' + ('abcde12345fghij').substring(5);
            </script>
            </body>
            </html>
        """.trimIndent()

        val parsed = StreamtapeVideoExtractor.parseStreamFromHtml(mockHtml)
        assertNotNull(parsed)
        assertTrue(parsed!!.startsWith("https://streamtape.com/get_video?id=foobar&token=12345fghij"))
        assertTrue(parsed.endsWith("&stream=1"))
    }

    @Test
    fun testFilemoonHtmlParser() {
        val mockHtml = """
            <html>
            <script>
            var player = { file: "https://delivery.filemoon.sx/hls/test/master.m3u8" };
            </script>
            </html>
        """.trimIndent()

        val parsed = FilemoonVideoExtractor.parseStreamFromHtml(mockHtml)
        assertNotNull(parsed)
        assertEquals("https://delivery.filemoon.sx/hls/test/master.m3u8", parsed)
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
        val filemoon = ExtractorRegistry.findExtractor("https://filemoon.sx/e/123")
        val streamwish = ExtractorRegistry.findExtractor("https://streamwish.to/e/123")
        val streamtape = ExtractorRegistry.findExtractor("https://streamtape.com/e/123")
        val dood = ExtractorRegistry.findExtractor("https://do7go.com/e/123")
        val vidhide = ExtractorRegistry.findExtractor("https://vidhide.com/v/123")

        assertTrue("VOE priority must be higher than OkRu", voe!!.priority < okru!!.priority)
        assertTrue("OkRu priority must be higher than Filemoon", okru.priority < filemoon!!.priority)
        assertTrue("Filemoon priority must be higher than Streamwish", filemoon.priority < streamwish!!.priority)
        assertTrue("Streamwish priority must be higher than Streamtape", streamwish.priority < streamtape!!.priority)
        assertTrue("Streamtape priority must be higher than Doodstream", streamtape.priority < dood!!.priority)
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
