package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.VidhideExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VidhideVideoExtractor : VideoExtractor {
    override val name: String = "Vidhide"
    override val priority: Int = 4

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("vidhide") || u.contains("dramiyos") || u.contains("filelions")
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                val direct = VidhideExtractor.extract(url)
                if (direct?.hlsUrl != null) {
                    Result.success(
                        listOf(
                            ExtractedStream(
                                url = direct.hlsUrl,
                                format = StreamFormat.HLS,
                                quality = "Auto HLS",
                                serverName = "Vidhide (HLS)"
                            )
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Vidhide CDN is unavailable or stream extraction failed for: $url"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
