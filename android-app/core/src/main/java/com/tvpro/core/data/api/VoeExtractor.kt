package com.tvpro.core.data.api

import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class VoeDirectStreams(
    val hlsUrl: String?,
    val mp4Url: String?,
    val title: String? = null
)

object VoeExtractor {

    private fun rot13(str: String): String {
        val sb = StringBuilder(str.length)
        for (c in str) {
            when (c) {
                in 'a'..'z' -> sb.append(((c - 'a' + 13) % 26 + 'a'.code).toChar())
                in 'A'..'Z' -> sb.append(((c - 'A' + 13) % 26 + 'A'.code).toChar())
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun pureB64Decode(input: String): ByteArray {
        val clean = input.replace("\n", "").replace("\r", "").replace("=", "").trim()
        val isUrl = clean.contains("-") || clean.contains("_")
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val alphabet = if (isUrl) chars + "-_" else chars + "+/"
        val fallback = if (isUrl) chars + "+/" else chars + "-_"

        val out = java.io.ByteArrayOutputStream(clean.length * 3 / 4)
        var buffer = 0
        var bitsLeft = 0

        for (c in clean) {
            var idx = alphabet.indexOf(c)
            if (idx < 0) idx = fallback.indexOf(c)
            if (idx < 0) continue
            buffer = (buffer shl 6) or idx
            bitsLeft += 6
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.write((buffer shr bitsLeft) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    private fun decodeVoeConfig(encoded: String): JSONObject? {
        return try {
            var decoded = rot13(encoded)
            val markers = listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&")
            for (m in markers) {
                decoded = decoded.replace(m, "")
            }
            val firstBytes = pureB64Decode(decoded)
            val shifted = ByteArray(firstBytes.size)
            for (i in firstBytes.indices) {
                val b = firstBytes[i].toInt() and 0xFF
                shifted[i] = (b - 3).toByte()
            }
            val reversed = shifted.reversedArray()
            val reversedBase64 = String(reversed, Charsets.UTF_8)
            val jsonBytes = pureB64Decode(reversedBase64)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            JSONObject(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun extract(embedUrl: String): VoeDirectStreams? {
        return try {
            var targetUrl = embedUrl
            val req1 = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://voe.sx/")
                .build()

            val resp1 = ApiClient.streamingOkHttpClient.newCall(req1).execute()
            if (!resp1.isSuccessful) return null
            var body = resp1.body?.string() ?: return null

            // Check for JS redirect (e.g. window.location.href = 'https://arianathankspecial.com/e/...')
            val redirectMatch = Regex("""window\.location(?:\.href)?\s*=\s*['"](https?://[^'"]+)['"]""").find(body)
                ?: Regex("""['"](https?://[a-zA-Z0-9.-]+/e/[a-zA-Z0-9]+)['"]""").find(body)

            if (redirectMatch != null) {
                targetUrl = redirectMatch.groupValues[1]
                val req2 = Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://voe.sx/")
                    .build()

                val resp2 = ApiClient.streamingOkHttpClient.newCall(req2).execute()
                if (resp2.isSuccessful) {
                    body = resp2.body?.string() ?: ""
                }
            }

            val scriptPattern = Regex("""<script\b[^>]*type=["']application/json["'][^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val jsonMatches = scriptPattern.findAll(body)
            for (match in jsonMatches) {
                val scriptContent = match.groupValues[1].trim()
                try {
                    val arr = JSONArray(scriptContent)
                    for (i in 0 until arr.length()) {
                        val encoded = arr.optString(i, "")
                        if (encoded.length > 20) {
                            val config = decodeVoeConfig(encoded)
                            if (config != null) {
                                val hlsSource = config.optString("source", "").ifBlank { null }
                                val mp4Source = config.optString("direct_access_url", "").ifBlank { null }
                                val title = config.optString("title", "").ifBlank { null }

                                if (hlsSource != null || mp4Source != null) {
                                    return VoeDirectStreams(
                                        hlsUrl = hlsSource,
                                        mp4Url = mp4Source,
                                        title = title
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
