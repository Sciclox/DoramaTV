package com.tvpro.core.data.repository

import android.util.Base64
import com.tvpro.core.data.api.ApiClient
import com.tvpro.core.data.api.DoramasflixApiClient
import com.tvpro.core.data.extractor.JsUnpacker
import com.tvpro.core.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class CatalogPageResult(
    val items: List<MovieItem> = emptyList(),
    val totalCount: Int = 0,
    val totalPages: Int = 1,
    val currentPage: Int = 1
)

object MovieRepository {

    data class MovieCollectionSection(
        val id: String,
        val title: String,
        val movies: List<MovieItem> = emptyList()
    )

    data class TaxonomyOption(
        val id: Int,
        val name: String,
        val slug: String = ""
    )

    data class FilterState(
        val orderBy: String = "latest",
        val order: String = "desc",
        val selectedGenre: TaxonomyOption? = null,
        val selectedProvider: TaxonomyOption? = null,
        val selectedCountry: TaxonomyOption? = null,
        val selectedYear: TaxonomyOption? = null,
        val selectedTmdbMin: Double? = null,
        val selectedCategory: TaxonomyOption? = null
    ) {
        val hasActiveFilters: Boolean
            get() = selectedCategory != null || selectedGenre != null || selectedProvider != null || selectedCountry != null ||
                    selectedYear != null || selectedTmdbMin != null || orderBy != "latest" || order != "desc"

        val activeFiltersCount: Int
            get() = (if (selectedCategory != null) 1 else 0) +
                    (if (selectedGenre != null) 1 else 0) +
                    (if (selectedProvider != null) 1 else 0) +
                    (if (selectedCountry != null) 1 else 0) +
                    (if (selectedYear != null) 1 else 0) +
                    (if (selectedTmdbMin != null) 1 else 0) +
                    (if (orderBy != "latest" || order != "desc") 1 else 0)
    }

    val categories = listOf(
        TaxonomyOption(245, "Doramas en Emisión", "emision"),
        TaxonomyOption(622, "Audio Latino", "espanol-latino"),
        TaxonomyOption(622, "Doramas en Español Latino", "latino"),
        TaxonomyOption(623, "Sub Español", "sub-espanol"),
        TaxonomyOption(623, "Doramas Sub Español", "sub"),
        TaxonomyOption(526, "C-Dramas Chinos", "cdrama"),
        TaxonomyOption(720, "J-Dramas Japoneses", "jdrama"),
        TaxonomyOption(281, "Películas Asiáticas", "peliculas"),
        TaxonomyOption(663, "Reality Shows", "reality-show"),
        TaxonomyOption(47, "Próximos Estrenos 2026", "proximamente"),
        TaxonomyOption(47, "Próximos Estrenos 2026", "proximos")
    )
    val allTaxonomyCategories = categories

    val allTaxonomyGenres = listOf(
        TaxonomyOption(2, "Drama", "drama"),
        TaxonomyOption(3, "Comedia", "comedia"),
        TaxonomyOption(114, "Crimen", "crimen"),
        TaxonomyOption(23, "Misterio", "misterio"),
        TaxonomyOption(22, "Fantasía", "fantasy"),
        TaxonomyOption(21, "Sci-Fi", "sci-fi"),
        TaxonomyOption(18, "Acción", "action"),
        TaxonomyOption(19, "Aventura", "adventure"),
        TaxonomyOption(269, "Romance", "romance"),
        TaxonomyOption(98, "Familia", "familia"),
        TaxonomyOption(1131, "Terror", "terror"),
        TaxonomyOption(270, "Historia", "historia")
    )

    val movieGenres = allTaxonomyGenres
    val seriesGenres = allTaxonomyGenres
    val animeGenres = allTaxonomyGenres

    val providers = listOf(
        TaxonomyOption(1, "tvN", "tvn"),
        TaxonomyOption(2, "JTBC", "jtbc"),
        TaxonomyOption(3, "SBS", "sbs"),
        TaxonomyOption(4, "KBS2", "kbs2"),
        TaxonomyOption(5, "MBC", "mbc"),
        TaxonomyOption(6, "Netflix", "netflix"),
        TaxonomyOption(7, "iQiyi", "iqiyi"),
        TaxonomyOption(8, "WeTV / Tencent", "wetv"),
        TaxonomyOption(9, "Youku", "youku"),
        TaxonomyOption(10, "Viki", "viki")
    )

    val countries = listOf(
        TaxonomyOption(1, "Corea del Sur", "corea-del-sur"),
        TaxonomyOption(2, "China", "china"),
        TaxonomyOption(3, "Japón", "japon"),
        TaxonomyOption(4, "Tailandia", "tailandia"),
        TaxonomyOption(5, "Taiwán", "taiwan")
    )

    val years = listOf(
        TaxonomyOption(528, "2026", "2026"),
        TaxonomyOption(33, "2025", "2025"),
        TaxonomyOption(4, "2024", "2024"),
        TaxonomyOption(200, "2023", "2023"),
        TaxonomyOption(161, "2022", "2022"),
        TaxonomyOption(363, "2021", "2021"),
        TaxonomyOption(150, "2020", "2020"),
        TaxonomyOption(128, "2019", "2019")
    )

    val tmdbScoreOptions = listOf(
        8.0 to "8.0+ (Obras maestras)",
        7.0 to "7.0+ (Muy buenas)",
        6.0 to "6.0+ (Populares)"
    )

    val sortOptions = listOf(
        "latest" to "Más recientes",
        "popular" to "Más populares",
        "rated" to "Mejor valorados",
        "views" to "Más vistos"
    )

    val featuredCollections: List<MovieCollectionSection> by lazy {
        listOf(
            MovieCollectionSection(
                id = "doramas_emision",
                title = "Doramas en Emisión",
                movies = localCatalog.filter { it.year == "2026" || it.genres.any { g -> g.contains("Emisión", true) } }
            ),
            MovieCollectionSection(
                id = "doramas_latino",
                title = "Doramas en Español Latino",
                movies = localCatalog.filter { it.languages?.any { l -> l.contains("Latino", true) } == true }
            ),
            MovieCollectionSection(
                id = "cdramas_chinos",
                title = "C-Dramas Chinos",
                movies = localCatalog.filter { it.genres.any { g -> g.contains("C-Drama", true) || g.contains("Chino", true) } }
            ),
            MovieCollectionSection(
                id = "peliculas_asiaticas",
                title = "Películas Asiáticas",
                movies = localCatalog.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", true) } }
            ),
            MovieCollectionSection(
                id = "tendencias_2026",
                title = "Tendencias 2026",
                movies = localCatalog.filter { it.year == "2026" }
            )
        )
    }

    suspend fun fetchFeaturedCollections(): List<MovieCollectionSection> = withContext(Dispatchers.IO) {
        coroutineScope {
            val emisionJob = async { fetchCategoryListing("emision", 1, 10).items }
            val latinoJob = async { fetchCategoryListing("espanol-latino", 1, 10).items }
            val doramasflixJob = async {
                try {
                    DoramasflixApiClient.getDoramas(1, 12)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val cdramasJob = async { fetchCategoryListing("cdrama", 1, 10).items }
            val peliculasJob = async { fetchCategoryListing("peliculas", 1, 10).items }
            val tendenciasJob = async { fetchCategoryListing("series", 1, 10, yearIds = listOf(528)).items }

            val emision = emisionJob.await().ifEmpty { featuredCollections[0].movies }
            val latino = latinoJob.await().ifEmpty { featuredCollections[1].movies }
            val doramasflix = doramasflixJob.await()
            val cdramas = cdramasJob.await().ifEmpty { featuredCollections[2].movies }
            val peliculas = peliculasJob.await().ifEmpty { featuredCollections[3].movies }
            val tendencias = tendenciasJob.await().ifEmpty { featuredCollections[4].movies }

            val sections = mutableListOf(
                MovieCollectionSection("doramas_emision", "Doramas en Emisión", emision),
                MovieCollectionSection("doramas_latino", "Doramas en Español Latino", latino)
            )
            if (doramasflix.isNotEmpty()) {
                sections.add(MovieCollectionSection("doramas_extendido", "Doramas Más Populares", doramasflix))
            }
            sections.add(MovieCollectionSection("cdramas_chinos", "C-Dramas Chinos", cdramas))
            sections.add(MovieCollectionSection("peliculas_asiaticas", "Películas Asiáticas", peliculas))
            sections.add(MovieCollectionSection("tendencias_2026", "Tendencias 2026", tendencias))

            sections
        }
    }

    fun getStreamsForMovie(movieUrl: String): List<StreamItem> {
        val cached = detailCache[movieUrl]
        return cached?.streams ?: emptyList()
    }

    val episodesCache get() = MovieDetailCacheManager.episodesCache
    val detailCache get() = MovieDetailCacheManager.detailCache
    val seriePosterCache get() = MovieDetailCacheManager.seriePosterCache
    val serieBackdropCache get() = MovieDetailCacheManager.serieBackdropCache
    val serieOverviewCache get() = MovieDetailCacheManager.serieOverviewCache

    fun getCachedBackdrop(url: String): String? = MovieDetailCacheManager.getCachedBackdrop(url)
    fun getCachedPoster(url: String): String? = MovieDetailCacheManager.getCachedPoster(url)
    fun getCachedOverview(url: String): String? = MovieDetailCacheManager.getCachedOverview(url)
    fun clearDetailCache() = MovieDetailCacheManager.clearDetailCache()

    fun sanitizeThumbnailUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        var clean = url.trim()
        // Route all TMDB posters through media.themoviedb.org for 100% reliable SSL and Android connectivity
        clean = clean.replace("image.tmdb.org", "media.themoviedb.org")
        // Remove WordPress thumbnail dimensions like -51x54, -150x150, -300x169 before file extension
        clean = clean.replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")
        // Upgrade lower-resolution TMDB sizes to high-res w500
        clean = clean.replace("/t/p/w185/", "/t/p/w500/")
                     .replace("/t/p/w300/", "/t/p/w500/")
                     .replace("/t/p/w342/", "/t/p/w500/")
        return clean
    }

    fun isBackdropUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()
        return lower.contains("-serie.") || lower.contains("-serie-") ||
                lower.contains("1280x") || lower.contains("1920x") ||
                lower.contains("1024x575") || lower.contains("/original/") ||
                lower.contains("/w1280/") || lower.contains("/w780/") ||
                lower.contains("backdrop") || lower.contains("-header")
    }

    fun extractSlug(url: String, fallbackTitle: String = ""): String {
        val cleanUrl = url.trim().removeSuffix("/")
        val rawPart = when {
            cleanUrl.contains("/series/") -> cleanUrl.substringAfter("/series/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/doramas-online/") -> cleanUrl.substringAfter("/doramas-online/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/doramas/") -> cleanUrl.substringAfter("/doramas/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/dorama/") -> cleanUrl.substringAfter("/dorama/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/serie/") -> cleanUrl.substringAfter("/serie/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/tvshows/") -> cleanUrl.substringAfter("/tvshows/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/capitulos/") -> cleanUrl.substringAfter("/capitulos/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/capitulo/") -> cleanUrl.substringAfter("/capitulo/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/episodios/") -> cleanUrl.substringAfter("/episodios/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/episodio/") -> cleanUrl.substringAfter("/episodio/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/peliculas/") -> cleanUrl.substringAfter("/peliculas/").substringBefore("/").substringBefore("?")
            cleanUrl.contains("/pelicula/") -> cleanUrl.substringAfter("/pelicula/").substringBefore("/").substringBefore("?")
            else -> ""
        }.trim()

        val slugFromUrl = rawPart
            .replace(Regex("-\\d+x\\d+$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("-\\d+$"), "")

        if (slugFromUrl.isNotBlank() && !slugFromUrl.startsWith("http", ignoreCase = true)) {
            return slugFromUrl
        }

        if (fallbackTitle.isNotBlank()) {
            return fallbackTitle.lowercase()
                .replace(" ", "-")
                .replace(Regex("[^a-z0-9-]"), "")
                .trim('-')
        }

        return ""
    }

    val localSeriesCatalog: List<MovieItem> get() = localCatalog
    val localAnimeCatalog: List<MovieItem> = emptyList()
    val localDramaCatalog: List<MovieItem> get() = localCatalog

    val localCatalog = listOf(
        MovieItem(
            title = "Belleza Verdadera (True Beauty)",
            url = "https://tudorama.com/serie/true-beauty/",
            image = "https://media.themoviedb.org/t/p/w500/zfKcn6xqCu0zopDdWPiYC7t4R2g.jpg",
            backdrop = "https://media.themoviedb.org/t/p/w1280/leayUtGLqhM82PmiquVubdS4ImL.jpg",
            type = "series",
            rating = "9.2",
            year = "2020",
            quality = "Full HD",
            languages = listOf("Sub Español", "Latino"),
            overview = "Lim Joo-kyung es una estudiante de secundaria que, tras sufrir acoso por su apariencia, aprende el arte del maquillaje y se convierte en una de las chicas más populares del colegio.",
            genres = listOf("Comedia", "Romance", "Drama", "K-Drama")
        ),
        MovieItem(
            title = "Aterrizaje de Emergencia en tu Corazón",
            url = "https://tudorama.com/serie/crash-landing-on-you/",
            image = "https://media.themoviedb.org/t/p/w500/a6Z73Ou52jHSrelffGK04GyVhtd.jpg",
            backdrop = "https://media.themoviedb.org/t/p/w1280/a6Z73Ou52jHSrelffGK04GyVhtd.jpg",
            type = "series",
            rating = "9.5",
            year = "2019",
            quality = "Full HD",
            languages = listOf("Sub Español", "Latino"),
            overview = "Una heredera surcoreana sufre un accidente de parapente en territorio de Corea del Norte, donde un oficial del ejército decide ayudarla a esconderse.",
            genres = listOf("Romance", "Drama", "Comedia", "K-Drama")
        ),
        MovieItem(
            title = "La Gloria (The Glory)",
            url = "https://tudorama.com/serie/the-glory/",
            image = "https://media.themoviedb.org/t/p/w500/uS2dQ7ukij2ifLwjMBzTjqEDg2x.jpg",
            backdrop = "https://media.themoviedb.org/t/p/w1280/3Z4OC6UQa8GZ949kh08Zgq3mM1F.jpg",
            type = "series",
            rating = "9.4",
            year = "2022",
            quality = "4K Ultra HD",
            languages = listOf("Sub Español", "Latino"),
            overview = "Años después de haber sobrevivido a un terrible acoso escolar, una mujer pone en marcha un elaborado plan de venganza para que los culpables paguen.",
            genres = listOf("Drama", "Suspenso", "K-Drama")
        ),
        MovieItem(
            title = "Propuesta Laboral (Business Proposal)",
            url = "https://tudorama.com/serie/business-proposal/",
            image = "https://media.themoviedb.org/t/p/w500/6KMhKm2ZHG8KUtg4lhsNUdt4iPh.jpg",
            backdrop = "https://media.themoviedb.org/t/p/w1280/iEwGToxqGlG46NtNt3WpykbZ0Fd.jpg",
            type = "series",
            rating = "9.0",
            year = "2022",
            quality = "Full HD",
            languages = listOf("Sub Español", "Latino"),
            overview = "Ha-ri va a una cita a ciegas haciéndose pasar por su amiga rica para espantar al pretendiente, pero el plan falla cuando resulta ser su propio jefe.",
            genres = listOf("Comedia", "Romance", "K-Drama")
        ),
        MovieItem(
            title = "When I Meet the Moon",
            url = "https://tudorama.com/serie/when-i-meet-the-moon/",
            image = "https://media.themoviedb.org/t/p/w500/3RrrTbz5zYmv8nZEj4PDXX47axi.jpg",
            backdrop = "https://tudorama.com/wp-content/uploads/2026/11/Cuando-Encuentre-la-Luna-9407-serie.jpg",
            type = "series",
            rating = "8.9",
            year = "2026",
            quality = "Full HD",
            languages = listOf("Sub Español"),
            overview = "Un romance inesperado que florece en las noches más silenciosas bajo la luna llena, enfrentando secretos del pasado.",
            genres = listOf("Romance", "Drama", "C-Drama")
        ),
        MovieItem(
            title = "Amor Disfrazado (Love in Disguise)",
            url = "https://tudorama.com/serie/love-in-disguise/",
            image = "https://media.themoviedb.org/t/p/w500/yjFDgjpX6vv3Z4WEqqtKqROsmRN.jpg",
            backdrop = "https://tudorama.com/wp-content/uploads/2026/09/나의-유죄인간-8159-serie.jpg",
            type = "series",
            rating = "8.8",
            year = "2026",
            quality = "Full HD",
            languages = listOf("Sub Español", "Latino"),
            overview = "Una historia romántica y llena de giros divertidos donde las apariencias engañan y los verdaderos sentimientos no se pueden esconder.",
            genres = listOf("Comedia", "Romance", "K-Drama")
        ),
        MovieItem(
            title = "One of a Kind Romance",
            url = "https://tudorama.com/serie/one-of-a-kind-romance/",
            image = "https://media.themoviedb.org/t/p/w500/pLIfcH8cZC5O8VtgbiqcrOFyy0h.jpg",
            backdrop = "https://tudorama.com/wp-content/uploads/2026/10/유일무이-로맨스-9185-serie.jpg",
            type = "series",
            rating = "8.7",
            year = "2026",
            quality = "Full HD",
            languages = listOf("Sub Español"),
            overview = "Un dorama cautivador sobre dos personas con vidas completamente opuestas que se encuentran por casualidad.",
            genres = listOf("Romance", "Drama", "K-Drama")
        ),
        MovieItem(
            title = "The Intern",
            url = "https://tudorama.com/pelicula/the-intern/",
            image = "https://media.themoviedb.org/t/p/w500/kRHjgsICPISgEClloeARkPHP96M.jpg",
            backdrop = "https://tudorama.com/wp-content/uploads/2026/09/인턴-7643-movie.jpg",
            type = "normal",
            rating = "8.6",
            year = "2026",
            quality = "Full HD",
            languages = listOf("Sub Español"),
            overview = "Seon-wu, directora de una firma de moda, contrata al veterano Gi-ho como pasante. Una historia sobre experiencia y apoyo mutuo.",
            genres = listOf("Comedia", "Drama", "Película Asiática")
        )
    )

    fun unescapeHtml(raw: String): String = JsUnpacker.unescapeHtml(raw)
    fun rot13(str: String): String = JsUnpacker.rot13(str)
    fun decodeVoeConfig(encoded: String): JSONObject? = JsUnpacker.decodeVoeConfig(encoded)
    fun encodeBase(c: Int, a: Int): String = JsUnpacker.encodeBase(c, a)
    fun unpackPackedJs(htmlOrJs: String): String? = JsUnpacker.unpackPackedJs(htmlOrJs)

    suspend fun extractVoeDirectStreams(
        embedUrl: String,
        lang: String = "Sub Español",
        quality: String = "1080p Full HD"
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<StreamItem>()
        try {
            var targetUrl = embedUrl
            val req1 = okhttp3.Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://tudorama.com/")
                .build()
            val resp1 = ApiClient.streamingOkHttpClient.newCall(req1).execute()
            if (!resp1.isSuccessful) return@withContext emptyList()
            var body = resp1.body?.string() ?: ""

            val redirectMatch = Regex("""window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""").find(body)
            if (redirectMatch != null) {
                targetUrl = redirectMatch.groupValues[1]
                val req2 = okhttp3.Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://tudorama.com/")
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
                                val hlsSource = config.optString("source", "")
                                val mp4Source = config.optString("direct_access_url", "")

                                if (hlsSource.isNotBlank()) {
                                    results.add(
                                        StreamItem(
                                            url = hlsSource,
                                            type = "HLS",
                                            source = "Servidor ${results.size + 1}: VOE Nativo Ultra HD ($lang - $quality)",
                                            foundVia = "VOE Native HLS"
                                        )
                                    )
                                }
                                if (mp4Source.isNotBlank() && results.none { it.url == mp4Source }) {
                                    results.add(
                                        StreamItem(
                                            url = mp4Source,
                                            type = "MP4",
                                            source = "Servidor ${results.size + 1}: VOE Directo MP4 ($lang)",
                                            foundVia = "VOE Direct MP4"
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    suspend fun resolveTuDoramaStreams(pageUrl: String): List<StreamItem> = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamItem>()
        val fullUrl = when {
            pageUrl.startsWith("http://", ignoreCase = true) || pageUrl.startsWith("https://", ignoreCase = true) -> pageUrl
            pageUrl.contains("/") -> "https://tudorama.com/$pageUrl"
            else -> "https://tudorama.com/episodio/$pageUrl"
        }
        try {
            val req = okhttp3.Request.Builder()
                .url(fullUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://tudorama.com/")
                .build()
            val resp = ApiClient.okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val html = resp.body?.string() ?: return@withContext emptyList()

            // Extract real overview/synopsis if present on the page
            val overviewMatch = Regex("""<p[^>]*class=["'][^"']*overview[^"']*["'][^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE).find(html)
                ?: Regex("""<div[^>]*class=["'][^"']*(?:sinopsis|entry-content)[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE).find(html)
            val liveOverview = overviewMatch?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")
                ?.replace("Mostrar más", "", ignoreCase = true)
                ?.replace("Mostrar menos", "", ignoreCase = true)
                ?.trim()
            if (!liveOverview.isNullOrBlank()) {
                val cleanOverview = unescapeHtml(liveOverview)
                serieOverviewCache[pageUrl] = cleanOverview
                val s = extractSlug(pageUrl)
                if (s.isNotBlank()) serieOverviewCache[s] = cleanOverview
            }

            val dataIdMatch = Regex("""data-id=['"](\d+)['"]""").find(html)
            val dataNonceMatch = Regex("""data-nonce=['"]([a-zA-Z0-9]+)['"]""").find(html)
            val postId = dataIdMatch?.groupValues?.get(1)
            val nonce = dataNonceMatch?.groupValues?.get(1)

            if (postId == null || nonce == null) return@withContext emptyList()

            val formBody = okhttp3.FormBody.Builder()
                .add("action", "corvus_get_servers")
                .add("post_id", postId)
                .add("nonce", nonce)
                .build()

            val ajaxReq = okhttp3.Request.Builder()
                .url("https://tudorama.com/wp-admin/admin-ajax.php")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", pageUrl)
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            val ajaxResp = ApiClient.okHttpClient.newCall(ajaxReq).execute()
            if (!ajaxResp.isSuccessful) return@withContext emptyList()
            val ajaxJson = ajaxResp.body?.string() ?: return@withContext emptyList()
            val serverArray = JSONArray(ajaxJson)

            for (i in 0 until serverArray.length()) {
                val serverObj = serverArray.getJSONObject(i)
                val cdnUrl = serverObj.optString("url", "")
                val rawName = serverObj.optString("name", "server")
                val lang = serverObj.optString("lang", "Sub Español")
                if (cdnUrl.isBlank()) continue

                try {
                    val wReq = okhttp3.Request.Builder()
                        .url(cdnUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://tudorama.com/")
                        .build()
                    val wResp = ApiClient.okHttpClient.newCall(wReq).execute()
                    if (wResp.isSuccessful) {
                        val wHtml = wResp.body?.string() ?: ""
                        val iframeMatch = Regex("""iframe\.src\s*=\s*['"]([^'"]+)['"]""").find(wHtml)
                        val realUrl = iframeMatch?.groupValues?.get(1) ?: cdnUrl

                        val cleanServerName = when {
                            realUrl.contains("goofy-banana") || realUrl.contains("voe") || rawName.contains("voe") -> "VOE"
                            realUrl.contains("bysesukior") || realUrl.contains("filemoon") || rawName.contains("filemoon") -> "Filemoon"
                            realUrl.contains("abyss") || rawName.contains("abyss") -> "Abyss"
                            realUrl.contains("minochinos") || realUrl.contains("earnvids") || rawName.contains("earnvids") -> "Earnvids"
                            realUrl.contains("hgcloud") || rawName.contains("streamhg") -> "StreamHG"
                            else -> rawName.replace("sub:", "").uppercase()
                        }

                        if (cleanServerName == "VOE" || realUrl.contains("goofy-banana") || realUrl.contains("voe")) {
                            val voeStreams = extractVoeDirectStreams(realUrl, "Sub Español", "1080p Full HD")
                            if (voeStreams.isNotEmpty()) {
                                streams.addAll(voeStreams)
                            }
                            streams.add(
                                StreamItem(
                                    url = realUrl,
                                    type = "EMBED",
                                    source = "Servidor ${streams.size + 1}: VOE Player ($lang)",
                                    foundVia = "TuDorama Native"
                                )
                            )
                        } else if (cleanServerName == "Earnvids" || realUrl.contains("minochinos")) {
                            try {
                                val minoReq = okhttp3.Request.Builder()
                                    .url(realUrl)
                                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                    .header("Referer", "https://cdn.tudorama.com/")
                                    .build()
                                val minoResp = ApiClient.streamingOkHttpClient.newCall(minoReq).execute()
                                if (minoResp.isSuccessful) {
                                    val minoBody = minoResp.body?.string() ?: ""
                                    val unpacked = unpackPackedJs(minoBody)
                                    if (unpacked != null) {
                                        val m3u8Match = Regex("""https?://[^\s"']+\.m3u8[^\s"']*""").find(unpacked)
                                        if (m3u8Match != null) {
                                            streams.add(
                                                StreamItem(
                                                    url = m3u8Match.value,
                                                    type = "HLS",
                                                    source = "Servidor ${streams.size + 1}: Earnvids Nativo HLS ($lang)",
                                                    foundVia = "TuDorama Native HLS"
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            streams.add(
                                StreamItem(
                                    url = realUrl,
                                    type = "EMBED",
                                    source = "Servidor ${streams.size + 1}: Earnvids Player ($lang)",
                                    foundVia = "TuDorama Native"
                                )
                            )
                        } else {
                            streams.add(
                                StreamItem(
                                    url = realUrl,
                                    type = "EMBED",
                                    source = "Servidor ${streams.size + 1}: $cleanServerName ($lang)",
                                    foundVia = "TuDorama Native"
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        streams
    }

    suspend fun fetchEpisodesForSerie(serieUrl: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val cached = episodesCache[serieUrl]
        if (cached != null && cached.isNotEmpty()) return@withContext cached

        val slug = extractSlug(serieUrl)
        if (slug.isNotBlank()) {
            val cachedBySlug = episodesCache[slug]
            if (cachedBySlug != null && cachedBySlug.isNotEmpty()) {
                episodesCache[serieUrl] = cachedBySlug
                return@withContext cachedBySlug
            }
        }

        val isTuDorama = serieUrl.contains("tudorama", ignoreCase = true) ||
            serieUrl.contains("/serie/") ||
            serieUrl.contains("/episodio/") ||
            serieUrl.contains("/ver/")

        val isDoramasflix = !isTuDorama && (
            serieUrl.contains("doramasflix", ignoreCase = true) ||
            serieUrl.contains("/doramas-online/")
        )

        if (isDoramasflix && slug.isNotBlank()) {
            val detail = DoramasflixApiClient.getDoramaDetail(slug)
            if (detail != null && detail.episodes.isNotEmpty()) {
                episodesCache[serieUrl] = detail.episodes
                episodesCache[slug] = detail.episodes
                return@withContext detail.episodes
            }
        }

        val episodes = mutableListOf<EpisodeItem>()
        try {
            val req = okhttp3.Request.Builder()
                .url(serieUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://tudorama.com/")
                .build()
            val resp = ApiClient.okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext emptyList()
            val html = resp.body?.string() ?: return@withContext emptyList()

            // Extract real overview/synopsis from live page HTML
            val overviewMatch = Regex("""<p[^>]*class=["'][^"']*overview[^"']*["'][^>]*>([\s\S]*?)</p>""", RegexOption.IGNORE_CASE).find(html)
                ?: Regex("""<div[^>]*class=["'][^"']*(?:sinopsis|entry-content)[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE).find(html)
            val liveOverview = overviewMatch?.groupValues?.get(1)
                ?.replace(Regex("<[^>]+>"), "")
                ?.replace("Mostrar más", "", ignoreCase = true)
                ?.replace("Mostrar menos", "", ignoreCase = true)
                ?.trim()
            if (!liveOverview.isNullOrBlank()) {
                val cleanOverview = unescapeHtml(liveOverview)
                serieOverviewCache[serieUrl] = cleanOverview
                if (slug.isNotBlank()) serieOverviewCache[slug] = cleanOverview
            }

            // Extract real vertical poster and horizontal backdrop from live page HTML
            val heroPosterMatch = Regex("""class=["']hero__poster["'][\s\S]*?<img[^>]*src=["']([^"']+)["']""").find(html)
            val heroPoster = heroPosterMatch?.groupValues?.get(1)?.trim()
            if (!heroPoster.isNullOrBlank()) {
                seriePosterCache[serieUrl] = sanitizeThumbnailUrl(heroPoster)
            }

            val heroBackdropMatch = Regex("""class=["']hero__backdrop["'][\s\S]*?<img[^>]*src=["']([^"']+)["']""").find(html)
            val heroBackdrop = heroBackdropMatch?.groupValues?.get(1)?.trim()
            if (!heroBackdrop.isNullOrBlank()) {
                serieBackdropCache[serieUrl] = heroBackdrop
            }

            val itemRegex = Regex("""<li[^>]*class=["'][^"']*lep[^"']*["'][^>]*data-episode=["'](\d+)["'][^>]*data-season=["'](\d+)["'][^>]*>([\s\S]*?)</li>""")
            val matches = itemRegex.findAll(html)

            for (m in matches) {
                val epNum = m.groupValues[1].toIntOrNull() ?: 1
                val seasonNum = m.groupValues[2].toIntOrNull() ?: 1
                val inner = m.groupValues[3]

                val imgMatch = Regex("""<img[^>]*src=["']([^"']+)["']""").find(inner)
                val rawImg = imgMatch?.groupValues?.get(1) ?: ""
                val cleanImg = sanitizeThumbnailUrl(rawImg)

                val titleMatch = Regex("""<h3[^>]*class=["'][^"']*lep__title[^"']*["'][^>]*>([^<]*)</h3>""").find(inner)
                val rawTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""
                val title = if (rawTitle.isNotBlank()) unescapeHtml(rawTitle) else "Episodio $epNum"

                val urlMatch = Regex("""<a[^>]*href=["'](https://tudorama\.com/ver/[^"']+)["']""").find(inner)
                val epUrl = urlMatch?.groupValues?.get(1) ?: ""

                if (epUrl.isNotBlank()) {
                    episodes.add(
                        EpisodeItem(
                            id = seasonNum * 1000 + epNum,
                            episodeNumber = epNum,
                            seasonNumber = seasonNum,
                            title = title,
                            overview = "Episodio $epNum de la Temporada $seasonNum.",
                            image = cleanImg,
                            runtime = "45m",
                            slug = epUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Automatic fallback to Doramasflix catalog if TuDorama has 0 episodes
        if (episodes.isEmpty() && slug.isNotBlank()) {
            try {
                var dfDetail = DoramasflixApiClient.getDoramaDetail(slug)
                if (dfDetail == null || dfDetail.episodes.isEmpty()) {
                    val dfMatches = DoramasflixApiClient.searchDoramas(slug.replace("-", " "), 3)
                    val matchedSlug = dfMatches.firstOrNull()?.slug
                    if (!matchedSlug.isNullOrBlank()) {
                        dfDetail = DoramasflixApiClient.getDoramaDetail(matchedSlug)
                    }
                }
                if (dfDetail != null && dfDetail.episodes.isNotEmpty()) {
                    episodes.addAll(dfDetail.episodes)
                }
            } catch (_: Exception) {}
        }

        val sorted = episodes.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
        if (sorted.isNotEmpty()) {
            episodesCache[serieUrl] = sorted
        }
        sorted
    }

    suspend fun fetchCategoryListing(
        categoryType: String = "series",
        page: Int = 1,
        postsPerPage: Int = 30,
        genreIds: List<Int> = emptyList(),
        providerIds: List<Int> = emptyList(),
        countryIds: List<Int> = emptyList(),
        yearIds: List<Int> = emptyList(),
        tmdbMin: Double? = null,
        tmdbMax: Double? = null,
        orderBy: String = "latest",
        order: String = "desc",
        categoryId: Int? = null,
        categorySlug: String? = null
    ): CatalogPageResult = withContext(Dispatchers.IO) {
        val resolvedCategoryId: Int? = when {
            categoryId != null && categoryId > 0 -> categoryId
            !categorySlug.isNullOrBlank() -> {
                categories.firstOrNull {
                    it.slug.equals(categorySlug, ignoreCase = true) ||
                    it.name.equals(categorySlug, ignoreCase = true)
                }?.id ?: categorySlug.toIntOrNull()
            }
            categoryType.toIntOrNull() != null -> categoryType.toIntOrNull()
            else -> {
                categories.firstOrNull {
                    it.slug.equals(categoryType, ignoreCase = true) ||
                    it.name.equals(categoryType, ignoreCase = true)
                }?.id
            }
        }

        val slugToGenero: String? = when {
            categorySlug != null && categorySlug.isNotBlank() -> {
                when (categorySlug.lowercase()) {
                    "emision" -> "emision"
                    "series" -> "series"
                    "espanol-latino", "latino" -> "espanol-latino"
                    "sub-espanol", "sub" -> "sub-espanol"
                    "cdrama" -> "cdrama"
                    "jdrama" -> "jdrama"
                    "peliculas", "movies", "movie" -> "peliculas"
                    "proximamente", "proximos" -> "proximamente"
                    "reality-show" -> "reality-show"
                    else -> categorySlug.lowercase()
                }
            }
            categoryId != null && categoryId > 0 -> {
                when (categoryId) {
                    245 -> "emision"
                    622 -> "espanol-latino"
                    623 -> "sub-espanol"
                    526 -> "cdrama"
                    720 -> "jdrama"
                    281 -> "peliculas"
                    47 -> "proximamente"
                    663 -> "reality-show"
                    else -> null
                }
            }
            categoryType.isNotBlank() -> {
                when (categoryType.lowercase()) {
                    "emision" -> "emision"
                    "series" -> "series"
                    "espanol-latino", "latino" -> "espanol-latino"
                    "sub-espanol", "sub" -> "sub-espanol"
                    "cdrama" -> "cdrama"
                    "jdrama" -> "jdrama"
                    "peliculas", "movies", "movie" -> "peliculas"
                    "proximamente", "proximos" -> "proximamente"
                    "reality-show" -> "reality-show"
                    else -> null
                }
            }
            else -> null
        }

        val hasSpecialFilters = genreIds.isNotEmpty() || providerIds.isNotEmpty() || countryIds.isNotEmpty() || yearIds.isNotEmpty() || tmdbMin != null || orderBy != "latest"

        // High-fidelity web scraping for standard category catalog pages (retrieves real vertical TMDB posters)
        if (!hasSpecialFilters && slugToGenero != null) {
            try {
                val generoUrl = if (page > 1) {
                    "https://tudorama.com/genero/$slugToGenero/page/$page/"
                } else {
                    "https://tudorama.com/genero/$slugToGenero/"
                }
                val req = okhttp3.Request.Builder()
                    .url(generoUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Referer", "https://tudorama.com/")
                    .build()
                val resp = ApiClient.okHttpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val html = resp.body?.string() ?: ""
                    val pages = mutableListOf(1)
                    val pageRegex = Regex("""class=["']page-numbers[^"']*["'][^>]*>(\d+)<""")
                    for (m in pageRegex.findAll(html)) {
                        m.groupValues[1].toIntOrNull()?.let { pages.add(it) }
                    }
                    val totalPages = pages.maxOrNull() ?: 1
                    val articles = Regex("""<article[^>]*class=["'][^"']*ipst[^"']*["'][^>]*>([\s\S]*?)</article>""").findAll(html)
                    val items = mutableListOf<MovieItem>()

                    for (a in articles) {
                        val inner = a.groupValues[1]
                        val linkMatch = Regex("""<a[^>]*href=["']([^"']+)["']""").find(inner)
                        val imgMatch = Regex("""<img[^>]*src=["']([^"']+)["']""").find(inner)
                        val titleMatch = Regex("""class=["']ipst__title["'][^>]*><a[^>]*>([^<]+)</a>""").find(inner)
                        val scoreMatch = Regex("""class=["'][^"']*avg-score[^"']*["'][^>]*>[\s\S]*?([0-9.]+)""").find(inner)

                        val link = linkMatch?.groupValues?.get(1)?.trim() ?: ""
                        val rawImg = imgMatch?.groupValues?.get(1)?.trim() ?: ""
                        val rawTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""
                        val score = scoreMatch?.groupValues?.get(1)?.trim() ?: "8.8"

                        if (link.isNotBlank() && rawTitle.isNotBlank()) {
                            val title = unescapeHtml(rawTitle)
                            val cleanImg = sanitizeThumbnailUrl(rawImg)
                            if (cleanImg.isNotBlank()) {
                                seriePosterCache[link] = cleanImg
                            }
                            val isMovie = slugToGenero == "peliculas" || link.contains("/pelicula/")
                            val parsedGenres = when (slugToGenero) {
                                "emision" -> listOf("En Emisión", "Dorama")
                                "espanol-latino" -> listOf("Audio Latino", "Dorama")
                                "sub-espanol" -> listOf("Sub Español", "Dorama")
                                "cdrama" -> listOf("C-Drama", "Dorama")
                                "jdrama" -> listOf("J-Drama", "Dorama")
                                "peliculas" -> listOf("Película Asiática")
                                else -> listOf("Dorama")
                            }
                            items.add(
                                MovieItem(
                                    title = title,
                                    url = link,
                                    image = cleanImg,
                                    backdrop = serieBackdropCache[link],
                                    type = if (isMovie) "movie" else "series",
                                    rating = score,
                                    year = "2026",
                                    quality = "Full HD",
                                    languages = if (slugToGenero == "espanol-latino") listOf("Latino") else listOf("Sub Español", "Latino"),
                                    genres = parsedGenres
                                )
                            )
                        }
                    }

                    if (items.isNotEmpty()) {
                        return@withContext CatalogPageResult(items, totalPages * 20, totalPages, page)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val postEndpoint = when {
            resolvedCategoryId == 281 || categoryType.equals("movies", ignoreCase = true) || categoryType.equals("movie", ignoreCase = true) || categoryType.equals("peliculas", ignoreCase = true) -> "movie"
            categoryType.equals("episodes", ignoreCase = true) || categoryType.equals("recientes", ignoreCase = true) -> "episodes"
            else -> "serie"
        }

        try {
            val urlBuilder = StringBuilder("https://tudorama.com/wp-json/wp/v2/$postEndpoint?_embed=1")
            if (resolvedCategoryId != null) {
                urlBuilder.append("&categories=").append(resolvedCategoryId)
            }
            if (genreIds.isNotEmpty()) {
                urlBuilder.append("&crvs-genres=").append(genreIds.joinToString(","))
            }
            if (yearIds.isNotEmpty()) {
                urlBuilder.append("&crvs-year=").append(yearIds.joinToString(","))
            }
            urlBuilder.append("&page=").append(page)
            urlBuilder.append("&per_page=").append(postsPerPage)

            if (orderBy != "latest") {
                val orderParam = if (order.equals("asc", ignoreCase = true)) "asc" else "desc"
                urlBuilder.append("&order=").append(orderParam)
            }

            val url = urlBuilder.toString()
            val req = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Referer", "https://tudorama.com/")
                .build()

            val resp = ApiClient.okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext CatalogPageResult(emptyList(), 0, 1, page)
            val body = resp.body?.string() ?: return@withContext CatalogPageResult(emptyList(), 0, 1, page)

            val totalPostsHeader = resp.header("X-WP-Total")?.toIntOrNull() ?: 100
            val totalPagesHeader = resp.header("X-WP-TotalPages")?.toIntOrNull() ?: 10

            val posts = JSONArray(body)
            val items = mutableListOf<MovieItem>()

            for (i in 0 until posts.length()) {
                val p = posts.getJSONObject(i)
                val id = p.optInt("id", 0)
                val rawTitle = p.optJSONObject("title")?.optString("rendered", "") ?: ""
                val title = unescapeHtml(rawTitle)
                val slug = p.optString("slug", "")
                val link = p.optString("link", "https://tudorama.com/$postEndpoint/$slug")

                val yoast = p.optJSONObject("yoast_head_json")
                val overview = yoast?.optString("description", "")?.ifBlank { null }

                // 1. Extract poster from cache or Doramasflix / TMDB
                var posterUrl = seriePosterCache[link]?.takeIf { !isBackdropUrl(it) } ?: ""
                var backdropUrl = ""
                val ogImages = yoast?.optJSONArray("og_image")
                if (ogImages != null && ogImages.length() > 0) {
                    val ogObj = ogImages.getJSONObject(0)
                    backdropUrl = ogObj.optString("url", "")
                }

                val embedded = p.optJSONObject("_embedded")
                val mediaArr = embedded?.optJSONArray("wp:featuredmedia")
                if (mediaArr != null && mediaArr.length() > 0) {
                    val mediaObj = mediaArr.getJSONObject(0)
                    val wpImg = mediaObj.optString("source_url", "")
                    if (backdropUrl.isBlank()) backdropUrl = wpImg
                    if (posterUrl.isBlank() && !isBackdropUrl(wpImg)) {
                        posterUrl = wpImg
                    }
                }

                // If vertical poster is missing from WP post, look up true TMDB poster via Doramasflix catalog
                if (posterUrl.isBlank()) {
                    try {
                        val dfMatches = DoramasflixApiClient.searchDoramas(title, 1)
                        val dfPoster = dfMatches.firstOrNull()?.image
                        if (!dfPoster.isNullOrBlank() && !isBackdropUrl(dfPoster)) {
                            posterUrl = dfPoster
                            seriePosterCache[link] = posterUrl
                        }
                    } catch (_: Exception) {}
                }

                // Fallbacks: backdrop should not overwrite valid vertical poster
                if (posterUrl.isBlank() && backdropUrl.isNotBlank()) {
                    posterUrl = backdropUrl
                }
                if (backdropUrl.isBlank() && posterUrl.isNotBlank()) {
                    backdropUrl = posterUrl
                }
                posterUrl = sanitizeThumbnailUrl(posterUrl)

                val parsedGenres = mutableListOf<String>()
                val terms = embedded?.optJSONArray("wp:term")
                if (terms != null) {
                    for (ti in 0 until terms.length()) {
                        val subTerms = terms.optJSONArray(ti)
                        if (subTerms != null) {
                            for (si in 0 until subTerms.length()) {
                                val tObj = subTerms.getJSONObject(si)
                                val tName = tObj.optString("name", "")
                                val tTax = tObj.optString("taxonomy", "")
                                if ((tTax == "crvs-genres" || tTax == "category") && tName.isNotBlank() && !parsedGenres.contains(tName)) {
                                    parsedGenres.add(tName)
                                }
                            }
                        }
                    }
                }
                if (parsedGenres.isEmpty()) {
                    parsedGenres.add(if (postEndpoint == "movie" || resolvedCategoryId == 281) "Película Asiática" else "Dorama")
                }

                val dateStr = p.optString("date", "")
                val year = if (dateStr.length >= 4) dateStr.substring(0, 4) else "2026"

                if (title.isNotBlank()) {
                    items.add(
                        MovieItem(
                            title = title,
                            url = link,
                            image = posterUrl,
                            backdrop = backdropUrl,
                            type = if (postEndpoint == "movie" || resolvedCategoryId == 281) "normal" else "series",
                            rating = "8.8",
                            year = year,
                            quality = "Full HD",
                            languages = listOf("Sub Español", "Latino"),
                            overview = overview,
                            runtime = if (postEndpoint == "movie" || resolvedCategoryId == 281) "1h 50m" else "45m",
                            genres = parsedGenres,
                            postId = id,
                            slug = slug
                        )
                    )
                }
            }
            CatalogPageResult(items, totalPostsHeader, totalPagesHeader, page)
        } catch (e: Exception) {
            e.printStackTrace()
            CatalogPageResult(emptyList(), 0, 1, page)
        }
    }

    suspend fun getDoramasflixCatalog(page: Int = 1, limit: Int = 24): List<MovieItem> = withContext(Dispatchers.IO) {
        try {
            DoramasflixApiClient.getDoramas(page, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getCatalog(page: Int = 1): MovieListResponse = withContext(Dispatchers.IO) {
        coroutineScope {
            val tudoramaJob = async {
                withTimeoutOrNull(7500L) {
                    fetchCategoryListing("series", page, postsPerPage = 20)
                } ?: CatalogPageResult()
            }
            val doramasflixJob = async {
                try {
                    DoramasflixApiClient.getDoramas(page, limit = 20)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val tuResult = tudoramaJob.await()
            val dfItems = doramasflixJob.await()

            val merged = mutableListOf<MovieItem>()
            merged.addAll(tuResult.items)
            for (df in dfItems) {
                if (merged.none { it.title.equals(df.title, ignoreCase = true) || (!it.slug.isNullOrBlank() && it.slug == df.slug) }) {
                    merged.add(df)
                }
            }

            val liveMovies = merged.ifEmpty { localCatalog }
            val totalPages = maxOf(tuResult.totalPages, 150)
            val totalCount = (tuResult.totalCount + 3024).coerceAtLeast(liveMovies.size)

            val featured = liveMovies
                .filter { !it.backdrop.isNullOrBlank() }
                .take(6)
                .ifEmpty { liveMovies.take(5) }

            MovieListResponse(
                status = "success",
                currentPage = page,
                totalPages = totalPages,
                totalCount = totalCount,
                hasNextPage = page < totalPages,
                featured = featured,
                movies = liveMovies
            )
        }
    }

    suspend fun isServerAlive(stream: StreamItem): Boolean = withContext(Dispatchers.IO) {
        if (stream.url.isBlank()) return@withContext false
        true
    }

    suspend fun filterWorkingStreams(streams: List<StreamItem>): List<StreamItem> = withContext(Dispatchers.IO) {
        streams.filter { it.url.isNotBlank() }
    }

    suspend fun fetchEpisodesForSeason(
        postId: Int,
        season: Int,
        fallbackTitle: String,
        fallbackBackdrop: String,
        fallbackGallery: List<String> = emptyList(),
        serieUrl: String = ""
    ): List<EpisodeItem> = withContext(Dispatchers.IO) {
        // 1. Try finding episodes from cache specifically associated with this series
        val cachedEpisodes = (if (serieUrl.isNotBlank()) episodesCache[serieUrl] else null)
            ?: (if (fallbackTitle.isNotBlank()) detailCache[fallbackTitle]?.episodes else null)
            ?: (if (serieUrl.isNotBlank()) detailCache[serieUrl]?.episodes else null)
            ?: detailCache.values.firstOrNull {
                (postId > 0 && it.postId == postId.toString()) ||
                (fallbackTitle.isNotBlank() && it.title?.equals(fallbackTitle, ignoreCase = true) == true)
            }?.episodes
            ?: episodesCache.entries.firstOrNull { (k, _) ->
                (serieUrl.isNotBlank() && k.contains(serieUrl.trimEnd('/'))) ||
                (fallbackTitle.isNotBlank() && k.lowercase().contains(fallbackTitle.lowercase().replace(" ", "-")))
            }?.value

        if (!cachedEpisodes.isNullOrEmpty()) {
            val seasonEps = cachedEpisodes.filter { it.seasonNumber == season }
            if (seasonEps.isNotEmpty()) {
                return@withContext seasonEps
            }
        }

        // 2. If it's a Doramasflix series, query Doramasflix API directly
        val slug = extractSlug(serieUrl, fallbackTitle)
        val isTuDorama = serieUrl.contains("tudorama", ignoreCase = true) ||
            serieUrl.contains("/serie/") ||
            serieUrl.contains("/episodio/") ||
            serieUrl.contains("/ver/")

        val isDoramasflix = !isTuDorama && (
            serieUrl.contains("doramasflix", ignoreCase = true) ||
            serieUrl.contains("/doramas-online/") ||
            (postId > 0 && postId.toString().length > 10) ||
            (detailCache[fallbackTitle]?.postId?.length ?: 0) > 10
        )

        if (isDoramasflix && slug.isNotBlank()) {
            var serieId = (if (postId > 0 && postId.toString().length > 10) postId.toString() else null)
                ?: DoramasflixApiClient.serieIdCache[slug]
                ?: (detailCache[fallbackTitle]?.postId?.takeIf { it.length > 10 })
                ?: ""

            if (serieId.isBlank()) {
                val detail = DoramasflixApiClient.getDoramaDetail(slug)
                if (detail != null && !detail.postId.isNullOrBlank()) {
                    serieId = detail.postId
                }
            }

            if (serieId.isNotBlank()) {
                val eps = DoramasflixApiClient.getEpisodes(serieId, season)
                if (eps.isNotEmpty()) {
                    if (serieUrl.isNotBlank()) episodesCache[serieUrl] = eps
                    episodesCache[slug] = eps
                    return@withContext eps
                }
            }
        }

        // 3. Fallback to fetchEpisodesForSerie if serieUrl is present
        if (serieUrl.isNotBlank()) {
            try {
                val seriesEps = fetchEpisodesForSerie(serieUrl)
                val seasonEps = seriesEps.filter { it.seasonNumber == season }
                if (seasonEps.isNotEmpty()) {
                    return@withContext seasonEps
                }
            } catch (_: Exception) {}
        }

        // 4. Doramasflix fallback search if TuDorama has no episodes for this season
        if (slug.isNotBlank() || fallbackTitle.isNotBlank()) {
            try {
                val targetSearch = if (slug.isNotBlank()) slug else fallbackTitle
                val dfDetail = DoramasflixApiClient.getDoramaDetail(targetSearch)
                    ?: DoramasflixApiClient.searchDoramas(fallbackTitle.ifBlank { slug.replace("-", " ") }, 1).firstOrNull()?.let {
                        DoramasflixApiClient.getDoramaDetail(it.slug ?: "")
                    }
                if (dfDetail != null && !dfDetail.postId.isNullOrBlank()) {
                    val eps = DoramasflixApiClient.getEpisodes(dfDetail.postId, season)
                    if (eps.isNotEmpty()) {
                        if (serieUrl.isNotBlank()) episodesCache[serieUrl] = eps
                        return@withContext eps
                    }
                }
            } catch (_: Exception) {}
        }

        emptyList()
    }

    suspend fun resolveEpisodeStreams(
        episodeId: Int,
        seriesTitle: String,
        fallbackStreams: List<StreamItem>,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        seriesPostId: Int? = null,
        serieUrl: String = "",
        episodeSlug: String = "",
        seriesPostIdStr: String? = null,
        forceRefresh: Boolean = false
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        val effectivePostId = seriesPostIdStr?.ifBlank { null } ?: seriesPostId?.toString()

        // 1. Identify the episode strictly within this series' episodes
        val seriesEpisodes = (if (serieUrl.isNotBlank()) episodesCache[serieUrl] else null)
            ?: (if (seriesTitle.isNotBlank()) detailCache[seriesTitle]?.episodes else null)
            ?: (if (serieUrl.isNotBlank()) detailCache[serieUrl]?.episodes else null)
            ?: detailCache.values.firstOrNull {
                (effectivePostId != null && it.postId == effectivePostId) ||
                (seriesTitle.isNotBlank() && it.title?.equals(seriesTitle, ignoreCase = true) == true)
            }?.episodes
            ?: episodesCache.entries.firstOrNull { (k, _) ->
                (serieUrl.isNotBlank() && k.contains(serieUrl.trimEnd('/'))) ||
                (seriesTitle.isNotBlank() && k.lowercase().contains(seriesTitle.lowercase().replace(" ", "-")))
            }?.value
            ?: emptyList()

        val matchedEp = seriesEpisodes.firstOrNull { ep ->
            (episodeId > 0 && ep.id == episodeId) ||
            (ep.seasonNumber == seasonNumber && ep.episodeNumber == episodeNumber) ||
            (episodeSlug.isNotBlank() && ep.slug == episodeSlug)
        }

        val targetSlug = matchedEp?.slug ?: episodeSlug
        val seriesSlug = extractSlug(if (serieUrl.isNotBlank()) serieUrl else episodeSlug, seriesTitle)

        // 2. Direct check in Doramasflix caches (instant resolution for any Doramasflix episode)
        val directCachedStreams = (if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[targetSlug] else null)
            ?: (if (episodeSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[episodeSlug] else null)
        if (!forceRefresh && !directCachedStreams.isNullOrEmpty()) {
            return@withContext directCachedStreams
        }

        val directDfEpId = (if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeIdCache[targetSlug] else null)
            ?: (if (episodeSlug.isNotBlank()) DoramasflixApiClient.episodeIdCache[episodeSlug] else null)

        if (!directDfEpId.isNullOrBlank()) {
            val dfStreams = DoramasflixApiClient.getEpisodeStreams(directDfEpId, forceRefresh = forceRefresh)
            if (dfStreams.isNotEmpty()) {
                if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[targetSlug] = dfStreams
                if (episodeSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[episodeSlug] = dfStreams
                return@withContext dfStreams
            }
        }

        // 3. Determine provider: Doramasflix vs TuDorama
        val isDoramasflix = serieUrl.contains("doramasflix", ignoreCase = true) ||
            serieUrl.contains("/doramas-online/") ||
            serieUrl.contains("/episodios/") ||
            (targetSlug.isNotBlank() && DoramasflixApiClient.episodeIdCache.containsKey(targetSlug)) ||
            (episodeSlug.isNotBlank() && DoramasflixApiClient.episodeIdCache.containsKey(episodeSlug)) ||
            ((effectivePostId?.length ?: 0) > 10) ||
            (detailCache[seriesTitle]?.postId?.length ?: 0) > 10 ||
            detailCache.values.any { it.title.equals(seriesTitle, ignoreCase = true) && (it.postId?.length ?: 0) > 10 }

        val isTuDorama = !isDoramasflix && (
            serieUrl.contains("tudorama", ignoreCase = true) ||
            serieUrl.contains("/serie/") ||
            serieUrl.contains("/capitulo/") ||
            serieUrl.contains("/episodio/") ||
            serieUrl.contains("/ver/") ||
            targetSlug.contains("tudorama") ||
            episodeSlug.contains("tudorama")
        )

        if (isDoramasflix) {
            var dfEpId = if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeIdCache[targetSlug] else null
            if (dfEpId.isNullOrBlank() && episodeSlug.isNotBlank()) {
                dfEpId = DoramasflixApiClient.episodeIdCache[episodeSlug]
            }
            val effectiveSlug = seriesSlug.ifBlank { extractSlug("", seriesTitle) }
            if (dfEpId.isNullOrBlank() && effectiveSlug.isNotBlank()) {
                var serieId = (if ((effectivePostId?.length ?: 0) > 10) effectivePostId else null)
                    ?: DoramasflixApiClient.serieIdCache[effectiveSlug]
                    ?: (detailCache[seriesTitle]?.postId?.takeIf { it.length > 10 })
                    ?: ""

                if (serieId.isBlank()) {
                    val detail = DoramasflixApiClient.getDoramaDetail(effectiveSlug)
                        ?: DoramasflixApiClient.searchDoramas(seriesTitle.ifBlank { effectiveSlug.replace("-", " ") }, 1).firstOrNull()?.let {
                            DoramasflixApiClient.getDoramaDetail(it.slug ?: "")
                        }
                    if (detail != null && !detail.postId.isNullOrBlank()) {
                        serieId = detail.postId
                    }
                }

                if (serieId.isNotBlank()) {
                    val freshEps = DoramasflixApiClient.getEpisodes(serieId, seasonNumber)
                    val found = freshEps.firstOrNull { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
                        ?: freshEps.firstOrNull { it.episodeNumber == episodeNumber }
                    if (found != null && found.slug.isNotBlank()) {
                        dfEpId = DoramasflixApiClient.episodeIdCache[found.slug]
                    }
                }
            }

            if (!dfEpId.isNullOrBlank()) {
                val dfStreams = DoramasflixApiClient.getEpisodeStreams(dfEpId, forceRefresh = forceRefresh)
                if (dfStreams.isNotEmpty()) {
                    if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[targetSlug] = dfStreams
                    if (episodeSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[episodeSlug] = dfStreams
                    return@withContext dfStreams
                }
            }
        }

        // 4. TuDorama series resolution
        if (targetSlug.isNotBlank()) {
            val resolved = resolveTuDoramaStreams(targetSlug)
            if (resolved.isNotEmpty()) {
                return@withContext resolved
            }
        }

        // 5. Doramasflix fallback if TuDorama stream resolution returned nothing
        if (seriesSlug.isNotBlank() || seriesTitle.isNotBlank()) {
            try {
                val searchSlug = seriesSlug.ifBlank { extractSlug("", seriesTitle) }
                val dfDetail = DoramasflixApiClient.getDoramaDetail(searchSlug)
                    ?: DoramasflixApiClient.searchDoramas(seriesTitle.ifBlank { searchSlug.replace("-", " ") }, 1).firstOrNull()?.let {
                        DoramasflixApiClient.getDoramaDetail(it.slug ?: "")
                    }
                if (dfDetail != null && !dfDetail.postId.isNullOrBlank()) {
                    val freshEps = DoramasflixApiClient.getEpisodes(dfDetail.postId, seasonNumber)
                    val found = freshEps.firstOrNull { it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber }
                    val dfEpId = if (found != null && found.slug.isNotBlank()) {
                        DoramasflixApiClient.episodeIdCache[found.slug]
                    } else null
                    if (!dfEpId.isNullOrBlank()) {
                        val dfStreams = DoramasflixApiClient.getEpisodeStreams(dfEpId, forceRefresh = forceRefresh)
                        if (dfStreams.isNotEmpty()) {
                            if (targetSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[targetSlug] = dfStreams
                            if (episodeSlug.isNotBlank()) DoramasflixApiClient.episodeStreamsCache[episodeSlug] = dfStreams
                            return@withContext dfStreams
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 6. Fallback
        if (fallbackStreams.isNotEmpty()) {
            return@withContext fallbackStreams
        }

        emptyList()
    }

    fun getCachedDetail(url: String, title: String? = null): MovieDetailResponse? =
        MovieDetailCacheManager.getCachedDetail(url, title)

    fun cacheDetail(url: String, title: String?, response: MovieDetailResponse) =
        MovieDetailCacheManager.cacheDetail(url, title, response)

    suspend fun preloadMovie(movie: MovieItem) = withContext(Dispatchers.IO) {
        try {
            extractMovie(movie)
        } catch (_: Exception) {}
    }

    suspend fun extractMovie(movie: MovieItem, forceRefresh: Boolean = false): MovieDetailResponse = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = getCachedDetail(movie.url, movie.title)
            if (cached != null && !cached.overview.isNullOrBlank()) return@withContext cached
        }

        val targetUrl = movie.url
        val slug = movie.slug?.ifBlank { null } ?: extractSlug(targetUrl, movie.title)
        val isTuDorama = targetUrl.contains("tudorama", ignoreCase = true) ||
            targetUrl.contains("/serie/") ||
            targetUrl.contains("/pelicula/") ||
            targetUrl.contains("/dorama/")

        val isDoramasflix = !isTuDorama && (
            targetUrl.contains("doramasflix", ignoreCase = true) ||
            targetUrl.contains("/doramas-online/") ||
            movie.extractUrl?.contains("doramasflix") == true
        )

        if (isDoramasflix && slug.isNotBlank()) {
            val dfDetail = DoramasflixApiClient.getDoramaDetail(slug)
            if (dfDetail != null && (dfDetail.streams.isNotEmpty() || dfDetail.episodes.isNotEmpty())) {
                var streams = dfDetail.streams
                if (dfDetail.episodes.isNotEmpty()) {
                    val targetEp = dfDetail.episodes.firstOrNull { ep ->
                        targetUrl.contains(ep.slug, ignoreCase = true) ||
                        targetUrl.contains("-${ep.seasonNumber}x${ep.episodeNumber}", ignoreCase = true) ||
                        targetUrl.contains("episodio-${ep.episodeNumber}", ignoreCase = true) ||
                        targetUrl.contains("capitulo-${ep.episodeNumber}", ignoreCase = true)
                    } ?: dfDetail.episodes.first()
                    val epId = DoramasflixApiClient.episodeIdCache[targetEp.slug]
                    if (!epId.isNullOrBlank()) {
                        val resolvedStreams = DoramasflixApiClient.getEpisodeStreams(epId)
                        if (resolvedStreams.isNotEmpty()) {
                            streams = resolvedStreams
                        }
                    }
                }
                val finalDetail = dfDetail.copy(
                    streams = streams,
                    streamsCount = streams.size
                )
                if (finalDetail.episodes.isNotEmpty()) {
                    episodesCache[movie.url] = finalDetail.episodes
                    episodesCache[slug] = finalDetail.episodes
                }
                cacheDetail(movie.url, movie.title, finalDetail)
                return@withContext finalDetail
            }
        }

        val isSeries = movie.type == "series" || targetUrl.contains("/serie/")

        if (isSeries) {
            var episodes = fetchEpisodesForSerie(targetUrl)
            var totalSeasons = episodes.map { it.seasonNumber }.maxOrNull() ?: 1

            val livePoster = seriePosterCache[targetUrl]?.takeIf { !isBackdropUrl(it) }
                ?: movie.image.takeIf { !isBackdropUrl(it) }
                ?: movie.image
            val liveBackdrop = serieBackdropCache[targetUrl]?.ifBlank { null }
                ?: movie.backdrop
                ?: (if (isBackdropUrl(movie.image)) movie.image else null)

            var streams = if (episodes.isNotEmpty()) {
                resolveTuDoramaStreams(episodes.first().slug)
            } else {
                resolveTuDoramaStreams(targetUrl)
            }

            val searchTitle = movie.title.trim()
            val targetSlug = movie.slug?.ifBlank { null } ?: extractSlug(targetUrl, movie.title)
            var resolvedDfSlug = targetSlug
            var dfDetail: MovieDetailResponse? = null

            // Automatic fallback to Doramasflix if TuDorama has 0 episodes, 0 playable streams, or no overview
            if (episodes.isEmpty() || streams.isEmpty() || movie.overview.isNullOrBlank()) {
                if (targetSlug.isNotBlank()) {
                    dfDetail = DoramasflixApiClient.getDoramaDetail(targetSlug)
                }
                if (dfDetail == null || (episodes.isEmpty() && dfDetail.episodes.isEmpty()) || (movie.overview.isNullOrBlank() && dfDetail.overview.isNullOrBlank())) {
                    val dfMatches = DoramasflixApiClient.searchDoramas(searchTitle, 5)
                    val matched = dfMatches.firstOrNull {
                        it.title.equals(searchTitle, ignoreCase = true) ||
                        it.slug.equals(targetSlug, ignoreCase = true) ||
                        it.title.contains(searchTitle, ignoreCase = true)
                    } ?: dfMatches.firstOrNull()
                    val matchedSlug = matched?.slug
                    if (!matchedSlug.isNullOrBlank() && matchedSlug != targetSlug) {
                        resolvedDfSlug = matchedSlug
                        val fetchedDf = DoramasflixApiClient.getDoramaDetail(matchedSlug)
                        if (fetchedDf != null) dfDetail = fetchedDf
                    } else if (matched != null && !matched.overview.isNullOrBlank() && dfDetail == null) {
                        dfDetail = MovieDetailResponse(
                            status = "success",
                            postId = matched.extractUrl ?: "",
                            title = matched.title,
                            overview = matched.overview,
                            rating = matched.rating,
                            poster = matched.image,
                            backdrop = matched.backdrop,
                            genres = matched.genres,
                            streams = emptyList(),
                            streamsCount = 0,
                            isSeries = true,
                            seasonsCount = 1,
                            episodes = emptyList()
                        )
                    }
                }

                if (dfDetail != null && dfDetail.episodes.isNotEmpty()) {
                    if (episodes.isEmpty()) {
                        episodes = dfDetail.episodes
                        totalSeasons = dfDetail.seasonsCount
                        episodesCache[targetUrl] = episodes
                        if (targetSlug.isNotBlank()) episodesCache[targetSlug] = episodes
                        if (resolvedDfSlug.isNotBlank()) episodesCache[resolvedDfSlug] = episodes
                    }
                    if (streams.isEmpty() && dfDetail.streams.isNotEmpty()) {
                        streams = dfDetail.streams
                    }
                }
            }

            var resolvedOverview = movie.overview?.takeIf { it.isNotBlank() }
                ?: serieOverviewCache[targetUrl]
                ?: (if (targetSlug.isNotBlank()) serieOverviewCache[targetSlug] else null)
                ?: dfDetail?.overview?.takeIf { it.isNotBlank() }

            if (resolvedOverview.isNullOrBlank()) {
                try {
                    val dfMatches = DoramasflixApiClient.searchDoramas(searchTitle, 5)
                    val match = dfMatches.firstOrNull {
                        it.title.equals(searchTitle, ignoreCase = true) ||
                        it.slug.equals(targetSlug, ignoreCase = true) ||
                        it.title.contains(searchTitle, ignoreCase = true)
                    } ?: dfMatches.firstOrNull()
                    if (match?.overview?.isNotBlank() == true) {
                        resolvedOverview = match.overview
                    }
                } catch (_: Exception) {}
            }

            if (!resolvedOverview.isNullOrBlank()) {
                serieOverviewCache[targetUrl] = resolvedOverview
                if (targetSlug.isNotBlank()) serieOverviewCache[targetSlug] = resolvedOverview
            }

            val detail = MovieDetailResponse(
                status = "success",
                postId = movie.postId?.toString() ?: "",
                title = movie.title,
                overview = resolvedOverview,
                rating = movie.rating ?: "8.8",
                poster = livePoster,
                backdrop = liveBackdrop,
                genres = movie.genres,
                streams = streams,
                streamsCount = streams.size,
                isSeries = true,
                seasonsCount = totalSeasons,
                episodes = episodes
            )
            cacheDetail(movie.url, movie.title, detail)
            return@withContext detail
        } else {
            val streams = resolveTuDoramaStreams(targetUrl)
            val livePoster = seriePosterCache[targetUrl]?.takeIf { !isBackdropUrl(it) }
                ?: movie.image.takeIf { !isBackdropUrl(it) }
                ?: movie.image
            val liveBackdrop = serieBackdropCache[targetUrl]?.ifBlank { null }
                ?: movie.backdrop
                ?: (if (isBackdropUrl(movie.image)) movie.image else null)

            val searchTitle = movie.title.trim()
            val targetSlug = movie.slug?.ifBlank { null } ?: extractSlug(targetUrl, movie.title)

            var resolvedOverview = movie.overview?.takeIf { it.isNotBlank() }
                ?: serieOverviewCache[targetUrl]
                ?: (if (targetSlug.isNotBlank()) serieOverviewCache[targetSlug] else null)

            if (resolvedOverview.isNullOrBlank()) {
                try {
                    val dfMatches = DoramasflixApiClient.searchDoramas(searchTitle, 5)
                    val match = dfMatches.firstOrNull {
                        it.title.equals(searchTitle, ignoreCase = true) ||
                        it.slug.equals(targetSlug, ignoreCase = true) ||
                        it.title.contains(searchTitle, ignoreCase = true)
                    } ?: dfMatches.firstOrNull()
                    if (match?.overview?.isNotBlank() == true) {
                        resolvedOverview = match.overview
                    }
                } catch (_: Exception) {}
            }

            if (!resolvedOverview.isNullOrBlank()) {
                serieOverviewCache[targetUrl] = resolvedOverview
                if (targetSlug.isNotBlank()) serieOverviewCache[targetSlug] = resolvedOverview
            }

            val detail = MovieDetailResponse(
                status = "success",
                postId = movie.postId?.toString() ?: "",
                title = movie.title,
                overview = resolvedOverview,
                rating = movie.rating ?: "8.8",
                poster = livePoster,
                backdrop = liveBackdrop,
                genres = movie.genres,
                streams = streams,
                streamsCount = streams.size,
                isSeries = false,
                seasonsCount = 1,
                episodes = emptyList()
            )
            cacheDetail(movie.url, movie.title, detail)
            return@withContext detail
        }
    }

    suspend fun extractMovie(movieUrl: String, forceRefresh: Boolean = false): MovieDetailResponse = withContext(Dispatchers.IO) {
        val dummy = MovieItem(
            title = "",
            url = movieUrl,
            image = "",
            type = if (movieUrl.contains("/serie/")) "series" else "normal"
        )
        extractMovie(dummy, forceRefresh)
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<MovieItem>()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")

        coroutineScope {
            val tudoramaJob = async {
                val tuList = mutableListOf<MovieItem>()
                try {
                    val searchUrl = "https://tudorama.com/?s=$encoded"
                    val req = okhttp3.Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .header("Referer", "https://tudorama.com/")
                        .build()
                    val resp = ApiClient.okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val html = resp.body?.string() ?: ""
                        val articleRegex = Regex("""<article[^>]*class=["'][^"']*ipst[^"']*["'][^>]*>([\s\S]*?)</article>""")
                        val matches = articleRegex.findAll(html)
                        for (m in matches) {
                            val inner = m.groupValues[1]
                            val linkMatch = Regex("""<a[^>]*href=["']([^"']+)["']""").find(inner)
                            val imgMatch = Regex("""<img[^>]*src=["']([^"']+)["']""").find(inner)
                            val titleMatch = Regex("""class=["']ipst__title["'][^>]*><a[^>]*>([^<]+)</a>""").find(inner)
                            val scoreMatch = Regex("""class=["'][^"']*avg-score[^"']*["'][^>]*>[\s\S]*?([0-9.]+)""").find(inner)

                            val link = linkMatch?.groupValues?.get(1)?.trim() ?: ""
                            val rawImg = imgMatch?.groupValues?.get(1)?.trim() ?: ""
                            val rawTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""
                            val score = scoreMatch?.groupValues?.get(1)?.trim() ?: "8.8"

                            if (link.isNotBlank() && rawTitle.isNotBlank()) {
                                val title = unescapeHtml(rawTitle)
                                val cleanImg = sanitizeThumbnailUrl(rawImg)
                                if (cleanImg.isNotBlank()) {
                                    seriePosterCache[link] = cleanImg
                                }
                                val isSeries = link.contains("/serie/") || link.contains("/series/") || !link.contains("/pelicula/")
                                tuList.add(
                                    MovieItem(
                                        title = title,
                                        url = link,
                                        image = cleanImg,
                                        backdrop = serieBackdropCache[link],
                                        type = if (isSeries) "series" else "movie",
                                        rating = score,
                                        quality = "Full HD",
                                        languages = listOf("Sub Español", "Latino"),
                                        genres = listOf(if (isSeries) "Dorama" else "Película Asiática")
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                tuList
            }

            val doramasflixJob = async {
                try {
                    DoramasflixApiClient.searchDoramas(query, 20)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val tuResults = tudoramaJob.await()
            val dfResults = doramasflixJob.await()

            results.addAll(tuResults)
            for (df in dfResults) {
                if (results.none { it.url == df.url || it.title.equals(df.title, ignoreCase = true) }) {
                    results.add(df)
                }
            }

            // Include any matches from localCatalog
            val localMatches = localCatalog.filter {
                it.title.contains(query, ignoreCase = true) || it.overview?.contains(query, ignoreCase = true) == true
            }
            for (lm in localMatches) {
                if (results.none { it.url == lm.url || it.title.equals(lm.title, ignoreCase = true) }) {
                    results.add(lm)
                }
            }

            // Fallback to WP REST search if live HTML returned nothing
            if (results.isEmpty()) {
                try {
                    val searchUrl = "https://tudorama.com/wp-json/wp/v2/serie?search=$encoded&_embed=1&per_page=15"
                    val req = okhttp3.Request.Builder()
                        .url(searchUrl)
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://tudorama.com/")
                        .build()
                    val resp = ApiClient.okHttpClient.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val arr = JSONArray(body)
                        for (i in 0 until arr.length()) {
                            val p = arr.getJSONObject(i)
                            val rawTitle = p.optJSONObject("title")?.optString("rendered", "") ?: ""
                            val title = unescapeHtml(rawTitle)
                            val link = p.optString("link", "")
                            val embedded = p.optJSONObject("_embedded")
                            val mediaArr = embedded?.optJSONArray("wp:featuredmedia")
                            var poster = seriePosterCache[link]?.takeIf { !isBackdropUrl(it) } ?: ""
                            var backdrop = ""
                            val yoast = p.optJSONObject("yoast_head_json")
                            val ogImages = yoast?.optJSONArray("og_image")
                            if (ogImages != null && ogImages.length() > 0) {
                                backdrop = ogImages.getJSONObject(0).optString("url", "")
                            }
                            if (mediaArr != null && mediaArr.length() > 0) {
                                val wpImg = mediaArr.getJSONObject(0).optString("source_url", "")
                                if (backdrop.isBlank()) backdrop = wpImg
                                if (poster.isBlank() && !isBackdropUrl(wpImg)) {
                                    poster = wpImg
                                }
                            }
                            if (poster.isBlank()) {
                                try {
                                    val dfMatches = DoramasflixApiClient.searchDoramas(title, 1)
                                    val dfPoster = dfMatches.firstOrNull()?.image
                                    if (!dfPoster.isNullOrBlank() && !isBackdropUrl(dfPoster)) {
                                        poster = dfPoster
                                        seriePosterCache[link] = poster
                                    }
                                } catch (_: Exception) {}
                            }
                            if (poster.isBlank() && backdrop.isNotBlank()) poster = backdrop

                            if (title.isNotBlank() && link.isNotBlank()) {
                                results.add(
                                    MovieItem(
                                        title = title,
                                        url = link,
                                        image = sanitizeThumbnailUrl(poster),
                                        backdrop = backdrop.ifBlank { null },
                                        type = "series",
                                        rating = "8.8",
                                        year = "2026",
                                        quality = "Full HD",
                                        languages = listOf("Sub Español"),
                                        overview = yoast?.optString("description"),
                                        genres = listOf("Dorama")
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Append Doramasflix extended catalog results that are not already present
            for (df in dfResults) {
                if (results.none { it.title.equals(df.title, ignoreCase = true) || (!it.slug.isNullOrBlank() && it.slug == df.slug) }) {
                    results.add(df)
                }
            }
        }
        results
    }
}
