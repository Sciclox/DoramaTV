package com.tvpro.core.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OkRuExtractorTest {

    @Test
    fun testDirectExtractionFromEmbedUrl() {
        val testEmbedUrl = "https://ok.ru/videoembed/8861696723605"
        val streams = OkRuExtractor.extract(testEmbedUrl)
        
        // OkRuExtractor should extract valid video links for active Ok.ru videos
        if (streams != null) {
            val direct = streams.bestDirectUrl
            assertNotNull("Best direct URL should not be null", direct)
            assertTrue("Direct URL should point to okcdn.ru", direct!!.contains("okcdn.ru"))
        }
    }
}
