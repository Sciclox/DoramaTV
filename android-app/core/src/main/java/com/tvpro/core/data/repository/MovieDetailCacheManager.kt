package com.tvpro.core.data.repository

import com.tvpro.core.data.model.EpisodeItem
import com.tvpro.core.data.model.MovieDetailResponse
import java.util.concurrent.ConcurrentHashMap

object MovieDetailCacheManager {

    val episodesCache = ConcurrentHashMap<String, List<EpisodeItem>>()
    val detailCache = ConcurrentHashMap<String, MovieDetailResponse>()
    val seriePosterCache = ConcurrentHashMap<String, String>()
    val serieBackdropCache = ConcurrentHashMap<String, String>()
    val serieOverviewCache = ConcurrentHashMap<String, String>()

    fun getCachedBackdrop(url: String): String? = serieBackdropCache[url]
    fun getCachedPoster(url: String): String? = seriePosterCache[url]
    fun getCachedOverview(url: String): String? = serieOverviewCache[url]

    fun getCachedDetail(url: String, title: String? = null): MovieDetailResponse? {
        val cached = detailCache[url] ?: (title?.let { detailCache[it] })
        if (cached != null) {
            val overview = serieOverviewCache[url] ?: (title?.let { serieOverviewCache[it] })
            if (!overview.isNullOrBlank()) {
                val updated = cached.copy(overview = overview)
                detailCache[url] = updated
                if (!title.isNullOrBlank()) detailCache[title] = updated
                return updated
            }
        }
        return cached
    }

    fun cacheDetail(url: String, title: String?, response: MovieDetailResponse) {
        detailCache[url] = response
        if (!title.isNullOrBlank()) {
            detailCache[title] = response
        }
    }

    fun clearDetailCache() {
        detailCache.clear()
        episodesCache.clear()
        seriePosterCache.clear()
        serieBackdropCache.clear()
        serieOverviewCache.clear()
    }
}
