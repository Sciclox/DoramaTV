package com.tvpro.core.data.extractor

import android.util.Base64
import org.json.JSONObject

object JsUnpacker {

    fun unescapeHtml(raw: String): String {
        return raw.replace("&amp;", "&")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#038;", "&")
            .replace("&#039;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    fun rot13(str: String): String {
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

    fun decodeVoeConfig(encoded: String): JSONObject? {
        return try {
            var decoded = rot13(encoded)
            val markers = listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&")
            for (m in markers) {
                decoded = decoded.replace(m, "")
            }
            val firstBytes = Base64.decode(decoded, Base64.DEFAULT)
            val shifted = ByteArray(firstBytes.size)
            for (i in firstBytes.indices) {
                val b = firstBytes[i].toInt() and 0xFF
                shifted[i] = (b - 3).toByte()
            }
            val reversed = shifted.reversedArray()
            val reversedBase64 = String(reversed, Charsets.UTF_8)
            val jsonBytes = Base64.decode(reversedBase64, Base64.DEFAULT)
            val jsonStr = String(jsonBytes, Charsets.UTF_8)
            JSONObject(jsonStr)
        } catch (_: Exception) {
            null
        }
    }

    fun encodeBase(c: Int, a: Int): String {
        return if (c < a) {
            if (c > 35) (c + 29).toChar().toString() else c.toString(36)
        } else {
            encodeBase(c / a, a) + encodeBase(c % a, a)
        }
    }

    fun unpackPackedJs(htmlOrJs: String): String? {
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
}
