package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.VoeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VoeVideoExtractor : VideoExtractor {
    override val name: String = "VOE"
    override val priority: Int = 1 // Máxima prioridad por estabilidad y calidad 1080p

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("voe.sx") ||
                u.contains("voe-network") ||
                u.contains("megankid") ||
                u.contains("arianathankspecial") ||
                u.contains("goofy-banana") ||
                u.contains("/voe/") ||
                u.contains("/e/") && (u.contains("voe") || u.contains("banana") || u.contains("kid"))
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                val direct = VoeExtractor.extract(url)
                if (direct == null || (direct.hlsUrl.isNullOrBlank() && direct.mp4Url.isNullOrBlank())) {
                    return@withContext Result.failure(IllegalStateException("No direct streams found in VOE response for $url"))
                }

                val streams = mutableListOf<ExtractedStream>()
                val reqHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to "https://voe.sx/"
                )

                direct.hlsUrl?.let { hls ->
                    streams.add(
                        ExtractedStream(
                            url = hls,
                            format = StreamFormat.HLS,
                            quality = "1080p Full HD",
                            serverName = "VOE (HLS)",
                            headers = reqHeaders
                        )
                    )
                }

                direct.mp4Url?.let { mp4 ->
                    streams.add(
                        ExtractedStream(
                            url = mp4,
                            format = StreamFormat.MP4,
                            quality = "Direct MP4",
                            serverName = "VOE (MP4)",
                            headers = reqHeaders
                        )
                    )
                }

                Result.success(streams)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
