package com.tvpro.core.data.extractor

import com.tvpro.core.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object DoodVideoExtractor : VideoExtractor {
    override val name: String = "Doodstream"
    override val priority: Int = 3

    override fun canHandle(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("dood") ||
                u.contains("do7go.com") ||
                u.contains("ds2play.com") ||
                u.contains("doodstream") ||
                u.contains("dood.to") ||
                u.contains("dood.so") ||
                u.contains("doods.pro")
    }

    override suspend fun extract(url: String, headers: Map<String, String>): Result<List<ExtractedStream>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Referer", "https://doramasflix.in/")
                    .build()

                val resp = ApiClient.streamingOkHttpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code} connecting to Doodstream: $url"))
                }

                val html = resp.body?.string() ?: ""
                val passMatch = Regex("""/pass_md5/[^'"]+""").find(html)
                if (passMatch != null) {
                    val host = try { java.net.URI(url).host } catch (_: Exception) { null } ?: "do7go.com"
                    val scheme = if (url.startsWith("https://")) "https://" else "http://"
                    val passUrl = "$scheme$host${passMatch.value}"

                    val passReq = Request.Builder()
                        .url(passUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Referer", url)
                        .build()

                    val passResp = ApiClient.streamingOkHttpClient.newCall(passReq).execute()
                    val rawStreamPart = passResp.body?.string() ?: ""
                    if (rawStreamPart.isNotBlank()) {
                        val token = (System.currentTimeMillis() / 1000).toString()
                        val finalUrl = rawStreamPart.trim() + token + "?token=" + passMatch.value.substringAfterLast("/")

                        return@withContext Result.success(
                            listOf(
                                ExtractedStream(
                                    url = finalUrl,
                                    format = StreamFormat.MP4,
                                    quality = "Doodstream Direct",
                                    serverName = "Doodstream (MP4)",
                                    headers = mapOf(
                                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                                        "Referer" to url
                                    )
                                )
                            )
                        )
                    }
                }

                Result.failure(IllegalStateException("Could not resolve pass_md5 token from Doodstream: $url"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
