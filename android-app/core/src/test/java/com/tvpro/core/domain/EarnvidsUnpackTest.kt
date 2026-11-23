package com.tvpro.core.domain

import com.tvpro.core.data.extractor.JsUnpacker
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.StreamItem
import com.tvpro.core.data.repository.MovieRepository
import org.junit.Assert.*
import org.junit.Test

class EarnvidsUnpackTest {

    @Test
    fun testUnpackWithParamR() {
        val packed = """eval(function(p,a,c,k,e,r){e=String;if(!''.replace(/^/,String)){while(c--)r[c]=k[c]||c;k=[function(e){return r[e]}];e=function(){return'\\w+'};c=1};while(c--)if(k[c])p=p.replace(new RegExp('\\b'+e(c)+'\\b','g'),k[c]);return p}('0://1.2/3.4',5,5,'https|domain|com|video|m3u8'.split('|'),0,{}))"""
        val unpacked = JsUnpacker.unpackPackedJs(packed)
        assertNotNull(unpacked)
        assertTrue(unpacked!!.contains("https://domain.com/video.m3u8"))
    }

    @Test
    fun testResolveEpisodeStreamsHumanVapor() = kotlinx.coroutines.runBlocking {
        val detail = MovieRepository.extractMovie(MovieItem(
            title = "El Hombre Vapor",
            url = "https://tudorama.com/serie/human-vapor/",
            image = ""
        ))
        println("DETAIL: isSeries=${detail.isSeries}, episodes=${detail.episodes.size}")
        val firstEp = detail.episodes.firstOrNull()
        println("FIRST EP: $firstEp")
        if (firstEp != null) {
            val streams = MovieRepository.resolveEpisodeStreams(
                episodeId = firstEp.id,
                seriesTitle = "El Hombre Vapor",
                fallbackStreams = emptyList(),
                seasonNumber = firstEp.seasonNumber,
                episodeNumber = firstEp.episodeNumber,
                seriesPostId = detail.postId?.toIntOrNull(),
                serieUrl = "https://tudorama.com/serie/human-vapor/",
                episodeSlug = firstEp.slug
            )
            println("RESOLVED STREAMS COUNT: ${streams.size}")
            streams.forEachIndexed { idx, s ->
                println("STREAM [$idx]: type=${s.type}, prio=${StreamRanker.getStreamPriority(s)}, source=${s.source}, url=${s.url}")
            }
        }
    }

    @Test
    fun testFilemoonDeprioritizedOverNativeAndVoe() {
        val streams = listOf(
            StreamItem(url = "https://bysesukior.com/e/ec3lf1gctck7", type = "EMBED", source = "Filemoon"),
            StreamItem(url = "https://cdn.example.com/master.m3u8", type = "HLS", source = "Earnvids Nativo HLS"),
            StreamItem(url = "https://abyssplayer.com/6XbyS9YGxn", type = "EMBED", source = "Abyss"),
            StreamItem(url = "https://minochinos.com/embed/e0nrdq2a78dz", type = "EMBED", source = "Earnvids Player")
        )

        val ranked = StreamRanker.rankStreams(streams)
        assertEquals("https://cdn.example.com/master.m3u8", ranked[0].url)
        assertEquals(0, StreamRanker.getStreamPriority(ranked[0]))

        assertEquals("https://abyssplayer.com/6XbyS9YGxn", ranked[1].url)
        assertEquals(5, StreamRanker.getStreamPriority(ranked[1]))

        assertEquals("https://minochinos.com/embed/e0nrdq2a78dz", ranked[2].url)
        assertEquals(6, StreamRanker.getStreamPriority(ranked[2]))

        assertEquals("https://bysesukior.com/e/ec3lf1gctck7", ranked[3].url)
        assertEquals(90, StreamRanker.getStreamPriority(ranked[3]))
    }
}
