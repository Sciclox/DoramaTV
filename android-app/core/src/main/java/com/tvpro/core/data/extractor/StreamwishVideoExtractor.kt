package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object StreamwishVideoExtractor : VideoExtractor {
    override val name: String = "Streamwish"
    override val priority: Int = 4

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("streamwish") ||
                u.contains("strwish") ||
                u.contains("awish.pro") ||
                u.contains("wishembed") ||
                u.contains("flaswish") ||
                u.contains("embedwish") ||
                u.contains("sfastwish")
    }

    fun parseStreamFromHtml(html: String): String? {
        val directMatch = Regex("""file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""").find(html)
        if (directMatch != null) return directMatch.groupValues[1]

        val unpacked = JsUnpacker.unpackPackedJs(html)
        if (unpacked != null) {
            val unpackedMatch = Regex("""file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)
                ?: Regex("""["']?(?:hls|file|source)["']?\s*:\s*["'](https?://[^"']+)["']""").find(unpacked)
            if (unpackedMatch != null) return unpackedMatch.groupValues[1]
        }

        return null
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Referer", url)
                    .build()

                val resp = ApiClient.streamingOkHttpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code} connecting to Streamwish: $url"))
                }

                val html = resp.body?.string() ?: ""
                val streamUrl = parseStreamFromHtml(html)
                if (streamUrl != null) {
                    Result.success(
                        listOf(
                            ExtractedStream(
                                url = streamUrl,
                                format = StreamFormat.HLS,
                                quality = "Streamwish HLS",
                                serverName = "Streamwish (HLS)",
                                headers = mapOf(
                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                    "Referer" to url
                                )
                            )
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Could not extract HLS stream from Streamwish: $url"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
