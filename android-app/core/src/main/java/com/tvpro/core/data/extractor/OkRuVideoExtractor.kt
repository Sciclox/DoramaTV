package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.OkRuExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OkRuVideoExtractor : VideoExtractor {
    override val name: String = "Ok.ru"
    override val priority: Int = 2

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("ok.ru") || u.contains("odnoklassniki.ru")
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                val direct = OkRuExtractor.extract(url)
                if (direct == null) {
                    return@withContext Result.failure(IllegalStateException("Failed to extract streams from OkRu: $url"))
                }

                val streams = mutableListOf<ExtractedStream>()
                val reqHeaders = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                    "Referer" to "https://doramasflix.in/"
                )

                direct.hlsUrl?.let {
                    streams.add(
                        ExtractedStream(
                            url = it,
                            format = StreamFormat.HLS,
                            quality = "Auto HLS",
                            serverName = "Ok.ru (HLS)",
                            headers = reqHeaders
                        )
                    )
                }

                direct.hdMp4Url?.let {
                    streams.add(
                        ExtractedStream(
                            url = it,
                            format = StreamFormat.MP4,
                            quality = "720p HD",
                            serverName = "Ok.ru (HD)",
                            headers = reqHeaders
                        )
                    )
                }

                direct.sdMp4Url?.let {
                    streams.add(
                        ExtractedStream(
                            url = it,
                            format = StreamFormat.MP4,
                            quality = "480p SD",
                            serverName = "Ok.ru (SD)",
                            headers = reqHeaders
                        )
                    )
                }

                if (streams.isEmpty()) {
                    Result.failure(IllegalStateException("No valid video stream links extracted from OkRu"))
                } else {
                    Result.success(streams)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
