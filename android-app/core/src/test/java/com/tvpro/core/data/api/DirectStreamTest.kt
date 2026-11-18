package com.tvpro.core.data.api

import kotlinx.coroutines.runBlocking
import org.junit.Test

class DirectStreamTest {
    @Test
    fun testDirectStreamEp1() = runBlocking {
        println("=== TEST DIRECT EP 1 ===")
        val ep1Id = "670b7518a35a6c33cfba1435"
        val streams = DoramasflixApiClient.getEpisodeStreams(ep1Id)
        println("Streams returned: ${streams.size}")
        streams.forEach {
            println("Stream: [${it.type}] ${it.source} -> ${it.url}")
        }
    }

    @Test
    fun testDirectStreamEp2() = runBlocking {
        println("=== TEST DIRECT EP 2 ===")
        val ep2Id = "670b752ba35a6c33cfba1440"
        val streams = DoramasflixApiClient.getEpisodeStreams(ep2Id)
        println("Streams returned: ${streams.size}")
        streams.forEach {
            println("Stream: [${it.type}] ${it.source} -> ${it.url}")
        }
    }
}
