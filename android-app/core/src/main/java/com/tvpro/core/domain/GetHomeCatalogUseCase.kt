package com.tvpro.core.domain

import com.tvpro.core.data.api.DoramasflixApiClient
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.repository.MovieRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class HomeCatalog(
    val featured: List<MovieItem>,
    val top10: List<MovieItem>,
    val doramasEnEmision: List<MovieItem>,
    val popularDoramas: List<MovieItem>,
    val audioLatino: List<MovieItem>,
    val subEspanol: List<MovieItem>,
    val cdramasChinos: List<MovieItem>,
    val jdramasJaponeses: List<MovieItem>,
    val peliculasAsiaticas: List<MovieItem>,
    val romanceComedia: List<MovieItem>,
    val misterioCrimen: List<MovieItem>,
    val fantasiaEpicos: List<MovieItem>,
    val allMovies: List<MovieItem>
)

class GetHomeCatalogUseCase(
    private val movieRepository: MovieRepository = MovieRepository,
    private val apiClient: DoramasflixApiClient = DoramasflixApiClient
) {

    suspend fun execute(): HomeCatalog = coroutineScope {
        try {
            // Fetch live taxonomies in parallel
            val emisionDeferred = async { try { movieRepository.fetchCategoryListing("emision", page = 1, postsPerPage = 24, categoryId = 245) } catch (_: Exception) { null } }
            val popularDeferred = async { try { apiClient.getDoramas(page = 1, limit = 24) } catch (_: Exception) { emptyList() } }
            val latinoDeferred = async { try { movieRepository.fetchCategoryListing("espanol-latino", page = 1, postsPerPage = 24, categoryId = 622) } catch (_: Exception) { null } }
            val subDeferred = async { try { movieRepository.fetchCategoryListing("sub-espanol", page = 1, postsPerPage = 24, categoryId = 623) } catch (_: Exception) { null } }
            val cdramaDeferred = async { try { movieRepository.fetchCategoryListing("cdrama", page = 1, postsPerPage = 24, categoryId = 526) } catch (_: Exception) { null } }
            val jdramaDeferred = async { try { movieRepository.fetchCategoryListing("jdrama", page = 1, postsPerPage = 24, categoryId = 720) } catch (_: Exception) { null } }
            val peliculasDeferred = async { try { movieRepository.fetchCategoryListing("peliculas", page = 1, postsPerPage = 24, categoryId = 281) } catch (_: Exception) { null } }

            val emisionRes = emisionDeferred.await()
            val popularItems = popularDeferred.await()
            val latinoRes = latinoDeferred.await()
            val subRes = subDeferred.await()
            val cdramaRes = cdramaDeferred.await()
            val jdramaRes = jdramaDeferred.await()
            val peliculasRes = peliculasDeferred.await()

            val local = movieRepository.localCatalog

            val emisionItems = (emisionRes?.items?.ifEmpty { null } ?: local.filter { it.type == "series" }).distinctByUrl()
            val latinoItems = (latinoRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true }).distinctByUrl()
            val subItems = (subRes?.items?.ifEmpty { null } ?: local.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true }).distinctByUrl()
            val cdramaItems = (cdramaRes?.items?.ifEmpty { null } ?: local.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } || it.title.contains("Moon", ignoreCase = true) }).distinctByUrl()
            val jdramaItems = (jdramaRes?.items?.ifEmpty { null } ?: local.filter { it.genres.any { g -> g.contains("J-Drama", ignoreCase = true) } }).distinctByUrl()
            val peliculasItems = (peliculasRes?.items?.ifEmpty { null } ?: local.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } }).distinctByUrl()

            val masterCatalog = (emisionItems + popularItems + latinoItems + subItems + cdramaItems + jdramaItems + peliculasItems + local).distinctByUrl()

            val allWithBackdrops = masterCatalog
                .filter { !it.backdrop.isNullOrBlank() || !it.image.isNullOrBlank() }
                .distinctByUrl()

            val featuredRaw = allWithBackdrops.take(6).ifEmpty { masterCatalog.take(6) }
            val featured = featuredRaw.map { movie ->
                val highResBackdrop = (movie.backdrop?.takeIf { it.isNotBlank() } ?: movie.image)
                    .replace("/t/p/w500/", "/t/p/original/")
                    .replace("/t/p/w780/", "/t/p/original/")
                    .replace("/t/p/w1280/", "/t/p/original/")
                    .replace(Regex("""-\d+x\d+(\.(?:jpg|jpeg|png|webp))""", RegexOption.IGNORE_CASE), "$1")
                movie.copy(backdrop = highResBackdrop)
            }

            val top10 = (featured + emisionItems.take(3) + popularItems.take(4) + latinoItems.take(3))
                .distinctByUrl()
                .take(10)

            val romanceComedia = masterCatalog
                .filter { it.genres.any { g -> g.contains("Romance", true) || g.contains("Comedia", true) } }
                .distinctByUrl()

            val misterioCrimen = masterCatalog
                .filter { it.genres.any { g -> g.contains("Misterio", true) || g.contains("Crimen", true) || g.contains("Terror", true) } }
                .distinctByUrl()

            val fantasiaEpicos = masterCatalog
                .filter { it.genres.any { g -> g.contains("Fantasía", true) || g.contains("Sci-Fi", true) || g.contains("Acción", true) } }
                .distinctByUrl()

            HomeCatalog(
                featured = featured,
                top10 = top10,
                doramasEnEmision = emisionItems,
                popularDoramas = popularItems,
                audioLatino = latinoItems,
                subEspanol = subItems,
                cdramasChinos = cdramaItems,
                jdramasJaponeses = jdramaItems,
                peliculasAsiaticas = peliculasItems,
                romanceComedia = romanceComedia,
                misterioCrimen = misterioCrimen,
                fantasiaEpicos = fantasiaEpicos,
                allMovies = masterCatalog
            )
        } catch (_: Exception) {
            getOfflineFallback()
        }
    }

    fun getOfflineFallback(): HomeCatalog {
        val fallback = movieRepository.localCatalog
        val fallbackEmision = fallback.filter { it.type == "series" }.distinctByUrl()
        val fallbackLatino = fallback.filter { it.languages?.any { l -> l.contains("Latino", ignoreCase = true) } == true }.distinctByUrl()
        val fallbackSub = fallback.filter { it.languages?.any { l -> l.contains("Sub", ignoreCase = true) } == true }.distinctByUrl()
        val fallbackCdrama = fallback.filter { it.genres.any { g -> g.contains("C-Drama", ignoreCase = true) } }.distinctByUrl()
        val fallbackPeliculas = fallback.filter { it.type == "normal" || it.genres.any { g -> g.contains("Película", ignoreCase = true) } }.distinctByUrl()

        return HomeCatalog(
            featured = fallback.take(6),
            top10 = fallback.take(10),
            doramasEnEmision = fallbackEmision,
            popularDoramas = emptyList(),
            audioLatino = fallbackLatino,
            subEspanol = fallbackSub,
            cdramasChinos = fallbackCdrama,
            jdramasJaponeses = emptyList(),
            peliculasAsiaticas = fallbackPeliculas,
            romanceComedia = emptyList(),
            misterioCrimen = emptyList(),
            fantasiaEpicos = emptyList(),
            allMovies = fallback
        )
    }

    private fun List<MovieItem>.distinctByUrl(): List<MovieItem> {
        val seen = HashSet<String>()
        return filter { movie ->
            val key = movie.url.ifBlank { movie.title }
            seen.add(key)
        }
    }
}
