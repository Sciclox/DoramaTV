package com.tvpro.core.data.api

import okhttp3.Request
import org.json.JSONObject

data class OkRuDirectStreams(
    val hlsUrl: String?,
    val hdMp4Url: String?,
    val sdMp4Url: String?,
    val lowMp4Url: String?
) {
    val bestDirectUrl: String?
        get() = hdMp4Url ?: sdMp4Url ?: hlsUrl ?: lowMp4Url
}

object OkRuExtractor {

    private fun unescapeHtml(input: String): String {
        return input
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&#34;", "\"")
            .replace("&#38;", "&")
            .replace("&#60;", "<")
            .replace("&#62;", ">")
    }

    fun extract(embedUrl: String): OkRuDirectStreams? {
        return try {
            val req = Request.Builder()
                .url(embedUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .header("Referer", "https://doramasflix.in/")
                .build()

            val resp = ApiClient.streamingOkHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val html = resp.body?.string() ?: return null

            val match = Regex("""data-options="([^"]+)"""").find(html) ?: return null
            val rawOpts = match.groupValues[1]
            val decodedOpts = unescapeHtml(rawOpts)
            val json = JSONObject(decodedOpts)
            val flashvars = json.optJSONObject("flashvars") ?: return null

            val metadataVal = flashvars.opt("metadata") ?: return null
            val metadataObj = when (metadataVal) {
                is JSONObject -> metadataVal
                is String -> if (metadataVal.isNotBlank()) JSONObject(metadataVal) else null
                else -> null
            } ?: return null

            val hlsUrl = metadataObj.optString("hlsManifestUrl", "").ifBlank { null }
            val videosArr = metadataObj.optJSONArray("videos")

            var hdUrl: String? = null
            var sdUrl: String? = null
            var lowUrl: String? = null

            if (videosArr != null) {
                for (i in 0 until videosArr.length()) {
                    val v = videosArr.optJSONObject(i) ?: continue
                    val name = v.optString("name", "").lowercase()
                    val u = v.optString("url", "")
                    if (u.isBlank()) continue
                    when (name) {
                        "hd" -> hdUrl = u
                        "sd" -> sdUrl = u
                        "low", "lowest", "mobile" -> if (lowUrl == null) lowUrl = u
                    }
                }
            }

            if (hlsUrl == null && hdUrl == null && sdUrl == null && lowUrl == null) {
                null
            } else {
                OkRuDirectStreams(
                    hlsUrl = hlsUrl,
                    hdMp4Url = hdUrl,
                    sdMp4Url = sdUrl,
                    lowMp4Url = lowUrl
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
