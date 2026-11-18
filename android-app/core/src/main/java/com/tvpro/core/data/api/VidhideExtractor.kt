package com.tvpro.core.data.api

import okhttp3.Request

data class VidhideDirectStreams(
    val hlsUrl: String?,
    val title: String? = null
)

object VidhideExtractor {

    private fun encodeBase(c: Int, a: Int): String {
        return if (c < a) {
            if (c > 35) (c + 29).toChar().toString() else c.toString(36)
        } else {
            encodeBase(c / a, a) + encodeBase(c % a, a)
        }
    }

    private fun unpackPackedJs(htmlOrJs: String): String? {
        val pattern = Regex(
            """eval\(function\(p,a,c,k,e,d\)\{.*?\}\s*\(\s*'(.*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL
        )
        val match = pattern.find(htmlOrJs) ?: return null
        return try {
            var p = match.groupValues[1].replace("\\'", "'").replace("\\\\", "\\")
            val a = match.groupValues[2].toInt()
            var c = match.groupValues[3].toInt()
            val k = match.groupValues[4].split("|")
            while (c > 0) {
                c--
                val token = if (c < k.size && k[c].isNotBlank()) k[c] else encodeBase(c, a)
                val baseStr = encodeBase(c, a)
                val wordRegex = Regex("\\b$baseStr\\b")
                p = p.replace(wordRegex, token)
            }
            p
        } catch (_: Exception) {
            null
        }
    }

    fun extract(embedUrl: String): VidhideDirectStreams? {
        // dramiyos-cdn.com CDN is permanently broken (502 Bad Gateway / hangs indefinitely)
        // Vidhide should never be treated as direct native stream.
        return null
    }
}
