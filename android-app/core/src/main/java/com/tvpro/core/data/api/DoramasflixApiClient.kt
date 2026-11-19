package com.tvpro.core.data.api

import android.util.Base64
import com.tvpro.core.data.extractor.ExtractorRegistry
import com.tvpro.core.data.extractor.StreamFormat
import com.tvpro.core.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object DoramasflixApiClient {

    private const val GRAPHQL_ENDPOINT = "https://user-api.seriesapi.co/graphql"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // Cache episode id by slug for quick stream resolution
    val episodeIdCache = ConcurrentHashMap<String, String>()
    val serieIdCache = ConcurrentHashMap<String, String>()
    val episodeStreamsCache = ConcurrentHashMap<String, List<StreamItem>>()

    private fun postGraphQL(query: String, variables: JSONObject = JSONObject(), operationName: String? = null): JSONObject? {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
            if (operationName != null) {
                put("operationName", operationName)
            }
        }
        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(GRAPHQL_ENDPOINT)
            .header("Origin", "https://doramasflix.in")
            .header("Referer", "https://doramasflix.in/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .post(requestBody)
            .build()

        return try {
            val response = ApiClient.okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) JSONObject(bodyStr) else null
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    fun decodeEmbedJwt(jwtUrl: String): String? {
        return try {
            if (!jwtUrl.contains("/e/")) return jwtUrl
            val token = jwtUrl.substringAfter("/e/").substringBefore("&")
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payloadBytes = pureB64Decode(parts[1])
            val jsonStr = String(payloadBytes, Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val rawLinkB64 = json.optString("link", "")
            if (rawLinkB64.isNotBlank()) {
                val linkBytes = pureB64Decode(rawLinkB64)
                var decoded = String(linkBytes, Charsets.UTF_8).trim()
                if (!decoded.startsWith("http://") && !decoded.startsWith("https://")) {
                    try {
                        val secondBytes = pureB64Decode(decoded)
                        val secondDecoded = String(secondBytes, Charsets.UTF_8).trim()
                        if (secondDecoded.startsWith("http://") || secondDecoded.startsWith("https://")) {
                            decoded = secondDecoded
                        }
                    } catch (_: Exception) {}
                }
                decoded
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchDoramas(query: String, limit: Int = 20): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<MovieItem>()

        val gql = """
            query SearchFullDoramas(${"$"}input: String!, ${"$"}page: Int, ${"$"}perPage: Int) {
              searchFullDoramas(input: ${"$"}input, page: ${"$"}page, perPage: ${"$"}perPage, fuzzy: true) {
                items {
                  _id
                  slug
                  name
                  name_es
                  poster_path
                  backdrop_path
                  rating
                  overview
                  genres {
                    name
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("input", query.trim())
            put("page", 1)
            put("perPage", limit)
        }

        val res = postGraphQL(gql, variables, "SearchFullDoramas") ?: return@withContext emptyList()
        val itemsArr = res.optJSONObject("data")?.optJSONObject("searchFullDoramas")?.optJSONArray("items") ?: return@withContext emptyList()

        for (i in 0 until itemsArr.length()) {
            val item = itemsArr.optJSONObject(i) ?: continue
            val id = item.optString("_id", "")
            val slug = item.optString("slug", "")
            val name = item.optString("name", "").ifBlank { item.optString("name_es", "") }
            if (name.isBlank() || slug.isBlank()) continue

            val posterPath = item.optString("poster_path", "")
            if (posterPath.isBlank()) continue

            val backdropPath = item.optString("backdrop_path", "")
            val posterUrl = "https://media.themoviedb.org/t/p/w500$posterPath"
            val backdropUrl = if (backdropPath.isNotBlank()) "https://media.themoviedb.org/t/p/w1280$backdropPath" else posterUrl

            val ratingVal = item.optDouble("rating", 4.8)
            val scaledRating = if (ratingVal in 0.1..5.0) ratingVal * 2.0 else ratingVal
            val ratingStr = if (scaledRating > 0.0) String.format(java.util.Locale.US, "%.1f", scaledRating) else "8.8"

            val genresList = mutableListOf<String>()
            val genresArr = item.optJSONArray("genres")
            if (genresArr != null) {
                for (gi in 0 until genresArr.length()) {
                    genresArr.optJSONObject(gi)?.optString("name")?.let { if (it.isNotBlank()) genresList.add(it) }
                }
            }
            if (genresList.isEmpty()) genresList.add("Dorama")

            if (id.isNotBlank() && slug.isNotBlank()) {
                serieIdCache[slug] = id
            }

            results.add(
                MovieItem(
                    title = name,
                    url = "https://doramasflix.in/doramas-online/$slug",
                    image = posterUrl,
                    backdrop = backdropUrl.ifBlank { null },
                    type = "series",
                    rating = ratingStr,
                    year = "2026",
                    quality = "Full HD",
                    languages = listOf("Sub Español", "Latino"),
                    overview = item.optString("overview", "").ifBlank { null },
                    runtime = "45m",
                    genres = genresList,
                    slug = slug,
                    extractUrl = id
                )
            )
        }

        results
    }

    suspend fun getDoramas(page: Int = 1, limit: Int = 20): List<MovieItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MovieItem>()

        val gql = """
            query PaginationDorama(${"$"}page: Int, ${"$"}limit: Int) {
              paginationDorama(page: ${"$"}page, limit: ${"$"}limit, sort: VIEWS_COUNT_DESC) {
                count
                items {
                  _id
                  slug
                  name
                  name_es
                  poster_path
                  backdrop_path
                  rating
                  overview
                  number_of_episodes
                  genres {
                    name
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", page)
            put("limit", limit)
        }

        val res = postGraphQL(gql, variables, "PaginationDorama") ?: return@withContext emptyList()
        val itemsArr = res.optJSONObject("data")?.optJSONObject("paginationDorama")?.optJSONArray("items") ?: return@withContext emptyList()

        for (i in 0 until itemsArr.length()) {
            val item = itemsArr.optJSONObject(i) ?: continue
            val id = item.optString("_id", "")
            val slug = item.optString("slug", "")
            val name = item.optString("name", "").ifBlank { item.optString("name_es", "") }
            if (name.isBlank() || slug.isBlank()) continue

            // Ensure title has active episodes and valid poster so unplayable titles never appear
            val numEpisodes = item.optInt("number_of_episodes", 0)
            if (numEpisodes <= 0 && !slug.contains("movie", ignoreCase = true)) continue

            val posterPath = item.optString("poster_path", "")
            if (posterPath.isBlank()) continue

            val backdropPath = item.optString("backdrop_path", "")
            val posterUrl = "https://media.themoviedb.org/t/p/w500$posterPath"
            val backdropUrl = if (backdropPath.isNotBlank()) "https://media.themoviedb.org/t/p/w1280$backdropPath" else posterUrl

            val ratingVal = item.optDouble("rating", 4.8)
            val scaledRating = if (ratingVal in 0.1..5.0) ratingVal * 2.0 else ratingVal
            val ratingStr = if (scaledRating > 0.0) String.format(java.util.Locale.US, "%.1f", scaledRating) else "8.8"

            val genresList = mutableListOf<String>()
            val genresArr = item.optJSONArray("genres")
            if (genresArr != null) {
                for (gi in 0 until genresArr.length()) {
                    genresArr.optJSONObject(gi)?.optString("name")?.let { if (it.isNotBlank()) genresList.add(it) }
                }
            }
            if (genresList.isEmpty()) genresList.add("Dorama")

            if (id.isNotBlank() && slug.isNotBlank()) {
                serieIdCache[slug] = id
            }

            results.add(
                MovieItem(
                    title = name,
                    url = "https://doramasflix.in/doramas-online/$slug",
                    image = posterUrl,
                    backdrop = backdropUrl.ifBlank { null },
                    type = "series",
                    rating = ratingStr,
                    year = "2026",
                    quality = "Full HD",
                    languages = listOf("Sub Español", "Latino"),
                    overview = item.optString("overview", "").ifBlank { null },
                    runtime = "45m",
                    genres = genresList,
                    slug = slug,
                    extractUrl = id
                )
            )
        }

        results
    }

    suspend fun getDoramaDetail(slug: String): MovieDetailResponse? = withContext(Dispatchers.IO) {
        val cleanSlug = slug.trim()
            .replace(Regex("^https?://[^/]+/(doramas-online|series|dorama|serie|tvshows)/"), "")
            .replace(Regex("-\\d+x\\d+$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("-\\d+$"), "")
            .removeSuffix("/")

        val gql = """
            query DetailDoramaSlug(${"$"}slug: String!) {
              detailDorama(filter: { slug: ${"$"}slug }) {
                _id
                name
                name_es
                slug
                overview
                poster_path
                backdrop_path
                rating
                number_of_seasons
                number_of_episodes
                genres_en: localizedGenres(locale: en) { name }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("slug", cleanSlug)
        }

        val res = postGraphQL(gql, variables, "DetailDoramaSlug") ?: return@withContext null
        val dorama = res.optJSONObject("data")?.optJSONObject("detailDorama") ?: return@withContext null

        val id = dorama.optString("_id", "")
        val title = dorama.optString("name", "").ifBlank { dorama.optString("name_es", "") }
        val posterPath = dorama.optString("poster_path", "")
        val backdropPath = dorama.optString("backdrop_path", "")
        val posterUrl = if (posterPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""
        val backdropUrl = if (backdropPath.isNotBlank()) "https://image.tmdb.org/t/p/w1280$backdropPath" else posterUrl

        val ratingVal = dorama.optDouble("rating", 8.8)
        val ratingStr = if (ratingVal > 0.0) String.format("%.1f", ratingVal) else "8.8"

        val genresList = mutableListOf<String>()
        val genresArr = dorama.optJSONArray("genres_en")
        if (genresArr != null) {
            for (gi in 0 until genresArr.length()) {
                genresArr.optJSONObject(gi)?.optString("name")?.let { if (it.isNotBlank()) genresList.add(it) }
            }
        }
        if (genresList.isEmpty()) genresList.add("Dorama")

        val totalSeasons = dorama.optInt("number_of_seasons", 1).coerceAtLeast(1)

        if (id.isNotBlank()) {
            serieIdCache[cleanSlug] = id
        }

        val episodes = if (id.isNotBlank()) {
            getEpisodes(id, 1)
        } else {
            emptyList()
        }

        val firstEpStreams = if (episodes.isNotEmpty()) {
            val firstEpId = episodeIdCache[episodes.first().slug] ?: ""
            if (firstEpId.isNotBlank()) getEpisodeStreams(firstEpId) else emptyList()
        } else {
            emptyList()
        }

        MovieDetailResponse(
            status = "success",
            postId = id,
            title = title,
            overview = dorama.optString("overview", "").ifBlank { null },
            rating = ratingStr,
            poster = posterUrl,
            backdrop = backdropUrl,
            genres = genresList,
            streams = firstEpStreams,
            streamsCount = firstEpStreams.size,
            isSeries = true,
            seasonsCount = totalSeasons,
            episodes = episodes
        )
    }

    suspend fun getEpisodes(serieId: String, seasonNumber: Int = 1): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<EpisodeItem>()

        val gql = """
            query EpisodesPagination(${"$"}page: Int!, ${"$"}serie_id: ID!, ${"$"}season_number: Int!, ${"$"}limit: Int!) {
              paginationEpisode(page: ${"$"}page, limit: ${"$"}limit, filter: { serie_id: ${"$"}serie_id, season_number: ${"$"}season_number }, sort: NUMBER_ASC) {
                count
                items {
                  _id
                  name
                  slug
                  episode_number
                  season_number
                  still_path
                  overview
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("page", 1)
            put("limit", 50)
            put("serie_id", serieId)
            put("season_number", seasonNumber)
        }

        val res = postGraphQL(gql, variables, "EpisodesPagination") ?: return@withContext emptyList()
        val itemsArr = res.optJSONObject("data")?.optJSONObject("paginationEpisode")?.optJSONArray("items") ?: return@withContext emptyList()

        for (i in 0 until itemsArr.length()) {
            val it = itemsArr.optJSONObject(i) ?: continue
            val epId = it.optString("_id", "")
            val epNum = it.optInt("episode_number", i + 1)
            val sNum = it.optInt("season_number", seasonNumber)
            val epSlug = it.optString("slug", "ep-$epNum")
            val epName = it.optString("name", "Capítulo $epNum")
            val stillPath = it.optString("still_path", "")
            val stillUrl = if (stillPath.isNotBlank()) "https://image.tmdb.org/t/p/w500$stillPath" else ""

            if (epId.isNotBlank() && epSlug.isNotBlank()) {
                episodeIdCache[epSlug] = epId
            }

            results.add(
                EpisodeItem(
                    id = epNum,
                    episodeNumber = epNum,
                    seasonNumber = sNum,
                    title = epName,
                    overview = it.optString("overview", ""),
                    image = stillUrl,
                    runtime = "45m",
                    slug = epSlug
                )
            )
        }

        // Sort ascending by episode number
        results.sortBy { it.episodeNumber }
        results
    }

    private fun extractDomainBrand(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: ""
            val parts = host.removePrefix("www.").split(".")
            if (parts.size >= 2) {
                val brand = if (parts.size > 2 && parts.last().length <= 3 && parts[parts.size - 2].length <= 3) {
                    parts[parts.size - 3]
                } else {
                    parts[parts.size - 2]
                }
                brand.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }
            } else {
                host.ifBlank { "Alternativo" }
            }
        } catch (_: Exception) {
            "Alternativo"
        }
    }

    private fun resolveServerInfo(serverCode: String, url: String): Pair<String, Int> {
        val lower = url.lowercase()
        return when {
            serverCode == "1113" || lower.contains("ok.ru") || lower.contains("odnoklassniki") ->
                Pair("Ok.ru (Full HD)", 1)
            serverCode == "4721" || lower.contains("primeload") ->
                Pair("Primeload (Oficial HD)", 2)
            serverCode == "1230" || lower.contains("voe.sx") || lower.contains("voe-network") || lower.contains("arianathankspecial") || lower.contains("delivery") ->
                Pair("VOE", 3)
            serverCode == "7286" || lower.contains("dood") || lower.contains("playmogo") || lower.contains("do7go") ->
                Pair("Doodstream", 4)
            serverCode == "38585" || lower.contains("flaswish") || lower.contains("streamwish") || lower.contains("wishembed") ->
                Pair("FlashWish", 5)
            serverCode == "576857" || lower.contains("callistanise") || lower.contains("vidhide") ->
                Pair("Vidhide", 6)
            serverCode == "8309" || lower.contains("streamtape") || lower.contains("strtape") ->
                Pair("Streamtape", 7)
            serverCode == "958695" || lower.contains("filemoon") || lower.contains("byse") ->
                Pair("Filemoon", 8)
            serverCode == "1233" || lower.contains("uqload") ->
                Pair("Uqload", 9)
            serverCode == "1234" || lower.contains("mp4upload") ->
                Pair("Mp4Upload", 10)
            serverCode == "1239" || lower.contains("sbembed") || lower.contains("watchsb") || lower.contains("streamsb") ->
                Pair("StreamSB", 11)
            serverCode == "3889" || lower.contains("mixdrop") || lower.contains("m1xdrop") ->
                Pair("Mixdrop", 12)
            else -> {
                val brand = extractDomainBrand(url)
                Pair(if (brand != "Alternativo") brand else "Servidor $serverCode", 25)
            }
        }
    }

    private data class ParsedStreamCandidate(
        val url: String,
        val serverName: String,
        val langLabel: String,
        val priority: Int,
        val isRecommended: Boolean
    )

    suspend fun getEpisodeStreams(episodeId: String, forceRefresh: Boolean = false): List<StreamItem> = withContext(Dispatchers.IO) {
        android.util.Log.e("TVPRO_PLAY", "getEpisodeStreams CALLED for episodeId=$episodeId, forceRefresh=$forceRefresh")
        if (forceRefresh) {
            episodeStreamsCache.remove(episodeId)
        } else {
            val cached = episodeStreamsCache[episodeId]
            if (cached != null && cached.isNotEmpty()) {
                android.util.Log.e("TVPRO_PLAY", "getEpisodeStreams CACHE HIT for episodeId=$episodeId, count=${cached.size}")
                return@withContext cached
            }
        }

        val variables = JSONObject().apply {
            put("episode_id", episodeId)
        }

        // Doramasflix shards servers across partitions:
        // Priority tier: null (Web default: Primeload, VOE) and "playdoramas" (Ok.ru [1113], Primeload, Filemoon)
        // Secondary tier: "doramasyt" (Streamtape), "doramasflix", "com.asiapp.doramasgo" (Doodstream)
        val primaryPartitions = listOf(null, "playdoramas")
        val secondaryPartitions = listOf("doramasyt", "doramasflix", "com.asiapp.doramasgo")
        val rawLinks = mutableListOf<JSONObject>()

        fun queryPartition(appPart: String?) {
            val gql = if (appPart == null) {
                """
                    query EpisodeLinksOnlineWeb(${"$"}episode_id: ID!) {
                      getEpisodeLinks(id: ${"$"}episode_id) {
                        links_online {
                          server
                          lang
                          link
                          _id
                          is_recommended
                        }
                      }
                    }
                """.trimIndent()
            } else {
                """
                    query EpisodeLinksOnlineApp(${"$"}episode_id: ID!) {
                      getEpisodeLinks(id: ${"$"}episode_id, app: "$appPart") {
                        links_online {
                          server
                          lang
                          link
                          _id
                          is_recommended
                        }
                      }
                    }
                """.trimIndent()
            }
            val opName = if (appPart == null) "EpisodeLinksOnlineWeb" else "EpisodeLinksOnlineApp"
            val res = postGraphQL(gql, variables, opName)
            val arr = res?.optJSONObject("data")?.optJSONObject("getEpisodeLinks")?.optJSONArray("links_online")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { rawLinks.add(it) }
                }
            }
        }

        // Query primary partitions first (covers Ok.ru and Primeload)
        for (appPart in primaryPartitions) {
            queryPartition(appPart)
        }

        // Only query secondary partitions if primary didn't yield enough working options (< 2 servers)
        if (rawLinks.size < 2) {
            for (appPart in secondaryPartitions) {
                queryPartition(appPart)
                if (rawLinks.size >= 3) break
            }
        }

        if (rawLinks.isEmpty()) {
            return@withContext emptyList()
        }

        val candidates = mutableListOf<ParsedStreamCandidate>()

        for (linkObj in rawLinks) {
            val rawLink = linkObj.optString("link", "")
            val serverCode = linkObj.optString("server", "")
            val langCode = linkObj.optInt("lang", 0)
            val isRecommended = linkObj.optBoolean("is_recommended", false)

            if (rawLink.isBlank()) continue

            // Decode the real stream link
            var decodedUrl = decodeEmbedJwt(rawLink) ?: rawLink
            if (decodedUrl.isBlank() || decodedUrl.contains("embedshortener.co", ignoreCase = true)) continue

            // Normalize Byse / Filemoon /d/ URLs to /e/ embed URLs:
            if (decodedUrl.contains("bysefujedu.com/d/")) {
                decodedUrl = decodedUrl.replace("/d/", "/e/")
            } else if (decodedUrl.contains("filemoon.") && decodedUrl.contains("/d/")) {
                decodedUrl = decodedUrl.replace("/d/", "/e/")
            }

            val (serverName, priority) = resolveServerInfo(serverCode, decodedUrl)
            val langLabel = if (langCode == 38) "Español Latino" else "Audio Coreano (Sub Español)"

            candidates.add(
                ParsedStreamCandidate(
                    url = decodedUrl,
                    serverName = serverName,
                    langLabel = langLabel,
                    priority = priority,
                    isRecommended = isRecommended
                )
            )
        }

        // Sort candidates: Recommended (★ MEJOR e.g. Primeload) first, then cleanest priority (Ok.ru -> Primeload -> VOE -> ...),
        // then audio latino
        val sortedCandidates = candidates
            .distinctBy { it.url }
            .sortedWith(
                compareBy(
                    { if (it.isRecommended) 0 else 1 },
                    { it.priority },
                    { if (it.langLabel.contains("Latino")) 0 else 1 }
                )
            )

        val streamItems = mutableListOf<StreamItem>()
        for (candidate in sortedCandidates) {
            var extractedAny = false
            val extractor = ExtractorRegistry.findExtractor(candidate.url)
            if (extractor != null) {
                try {
                    val result = extractor.extract(candidate.url)
                    val extractedList = result.getOrNull()
                    if (!extractedList.isNullOrEmpty()) {
                        for (extracted in extractedList) {
                            extractedAny = true
                            streamItems.add(
                                StreamItem(
                                    url = extracted.url,
                                    type = if (extracted.format == StreamFormat.HLS) "HLS" else "MP4",
                                    source = "${extracted.serverName} (${if (extracted.format == StreamFormat.HLS) "HLS" else "Direct Full HD"}) (${candidate.langLabel})",
                                    foundVia = "Doramasflix ExtractorRegistry (${extracted.serverName})",
                                    subtitleTracks = extracted.subtitles
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DoramasflixApiClient", "Extractor failed for ${candidate.url}: ${e.message}")
                }
            }

            // Exclude dead hosts when direct extraction fails (e.g. 404 VOE or dead 502 Vidhide)
            val isDeadVoe = candidate.serverName.contains("VOE", ignoreCase = true) && !extractedAny
            val isDeadVidhide = candidate.serverName.contains("Vidhide", ignoreCase = true) && !extractedAny

            if (!isDeadVoe && !isDeadVidhide) {
                streamItems.add(
                    StreamItem(
                        url = candidate.url,
                        type = "EMBED",
                        source = "${candidate.serverName} (Web Player) (${candidate.langLabel})",
                        foundVia = "Doramasflix ${candidate.serverName}"
                    )
                )
            }
        }

        // Prioritize DIRECT native streams (MP4/HLS) first for high-performance instant playback in ExoPlayer,
        // followed by Ok.ru and Primeload (Official Doramasflix HD CDN)
        val prioritizedStreams = streamItems.sortedWith(
            compareBy(
                { if (it.type == "MP4" || it.type == "HLS") 0 else 1 },
                { if (it.source?.contains("Ok.ru", ignoreCase = true) == true) 0 else 1 },
                { if (it.source?.contains("Primeload", ignoreCase = true) == true) 0 else 1 },
                { if (it.source?.contains("VOE", ignoreCase = true) == true) 0 else 1 },
                { if (it.source?.contains("Latino", ignoreCase = true) == true) 0 else 1 }
            )
        )

        // Assign clean progressive server numbering: Servidor 1, Servidor 2, etc.
        val finalStreams = prioritizedStreams.mapIndexed { index, item ->
            val cleanSource = item.source?.replace(Regex("^(Servidor\\s*\\d+\\s*:\\s*)+", RegexOption.IGNORE_CASE), "")?.trim() ?: "Servidor HD"
            item.copy(source = "Servidor ${index + 1}: $cleanSource")
        }

        if (finalStreams.isNotEmpty()) {
            episodeStreamsCache[episodeId] = finalStreams
        }
        android.util.Log.e("TVPRO_PLAY", "finalStreams count=${finalStreams.size}: ${finalStreams.map { "${it.source}: ${it.type} -> ${it.url.take(60)}" }}")

        finalStreams
    }
}
