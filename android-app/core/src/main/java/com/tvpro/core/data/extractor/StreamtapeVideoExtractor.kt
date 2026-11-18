package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object StreamtapeVideoExtractor : VideoExtractor {
    override val name: String = "Streamtape"
    override val priority: Int = 5

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("streamtape.com") ||
                u.contains("streamta.pe") ||
                u.contains("streamtape.to") ||
                u.contains("streamtape.net") ||
                u.contains("stape.fun") ||
                u.contains("shavetape.cash") ||
                u.contains("strtape")
    }

    fun parseStreamFromHtml(html: String): String? {
        // Pattern 1: document.getElementById('robotlink').innerHTML = '...' + ('...').substring(x)
        val concatRegex = Regex(
            """document\.getElementById\(['"](?:no)?robotlink['"]\)\.innerHTML\s*=\s*['"]([^'"]+)['"]\s*\+\s*\(?['"]([^'"]+)['"]\)?\.substring\((\d+)\)"""
        )
        val concatMatch = concatRegex.find(html)
        if (concatMatch != null) {
            val part1 = concatMatch.groupValues[1]
            val part2 = concatMatch.groupValues[2]
            val subStart = concatMatch.groupValues[3].toIntOrNull() ?: 0
            val fullPath = part1 + part2.substring(subStart.coerceAtMost(part2.length))
            val url = if (fullPath.startsWith("//")) "https:$fullPath" else if (!fullPath.startsWith("http")) "https://$fullPath" else fullPath
            return if (url.contains("&stream=1")) url else "$url&stream=1"
        }

        // Pattern 2: id="ideoolink" or id="robotlink" inside div/p
        val idRegex = Regex("""id=['"](?:norobotlink|robotlink|ideoolink|ideoooolink)['"][^>]*>([^<]+)<""")
        val idMatch = idRegex.find(html)
        if (idMatch != null) {
            val raw = idMatch.groupValues[1].trim()
            if (raw.isNotBlank()) {
                val url = if (raw.startsWith("//")) "https:$raw" else if (!raw.startsWith("http")) "https://$raw" else raw
                return if (url.contains("&stream=1")) url else "$url&stream=1"
            }
        }

        return null
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                // Ensure embed format
                val embedUrl = if (url.contains("/v/")) url.replace("/v/", "/e/") else url
                val req = Request.Builder()
                    .url(embedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Referer", embedUrl)
                    .build()

                val resp = ApiClient.streamingOkHttpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code} connecting to Streamtape: $embedUrl"))
                }

                val html = resp.body?.string() ?: ""
                val streamUrl = parseStreamFromHtml(html)
                if (streamUrl != null) {
                    Result.success(
                        listOf(
                            ExtractedStream(
                                url = streamUrl,
                                format = StreamFormat.MP4,
                                quality = "Streamtape 1080p",
                                serverName = "Streamtape (MP4)",
                                headers = mapOf(
                                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                    "Referer" to embedUrl
                                )
                            )
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("Could not extract stream link from Streamtape HTML: $embedUrl"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
