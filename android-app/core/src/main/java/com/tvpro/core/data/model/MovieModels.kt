package com.tvpro.core.data.model

import com.google.gson.annotations.SerializedName

data class MovieListResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("currentPage") val currentPage: Int = 1,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("remainingPages") val remainingPages: Int = 0,
    @SerializedName("hasNextPage") val hasNextPage: Boolean = false,
    @SerializedName("hasPrevPage") val hasPrevPage: Boolean = false,
    @SerializedName("featuredCount") val featuredCount: Int = 0,
    @SerializedName("moviesCount") val moviesCount: Int = 0,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("featured") val featured: List<MovieItem> = emptyList(),
    @SerializedName("movies") val movies: List<MovieItem> = emptyList()
)

data class MovieItem(
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    @SerializedName("image") val image: String,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("type") val type: String? = "normal",
    @SerializedName("extractUrl") val extractUrl: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("quality") val quality: String? = null,
    @SerializedName("languages") val languages: List<String>? = null,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("runtime") val runtime: String? = null,
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("postId") val postId: Int? = null,
    @SerializedName("slug") val slug: String? = null
)

data class MovieSearchResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("movies") val movies: List<MovieItem> = emptyList()
)

data class MovieDetailResponse(
    @SerializedName("status") val status: String? = null,
    @SerializedName("post_id") val postId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("tagline") val tagline: String? = null,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("tmdbRating") val tmdbRating: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("logo") val logo: String? = null,
    @SerializedName("runtime") val runtime: String? = null,
    @SerializedName("releaseDate") val releaseDate: String? = null,
    @SerializedName("trailer") val trailer: String? = null,
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("gallery") val gallery: List<String> = emptyList(),
    @SerializedName("streams") val streams: List<StreamItem> = emptyList(),
    @SerializedName("streamsCount") val streamsCount: Int = 0,
    val isSeries: Boolean = false,
    val seasonsCount: Int = 1,
    val episodes: List<EpisodeItem> = emptyList(),
    val similarMovies: List<MovieItem> = emptyList()
)

data class EpisodeItem(
    val id: Int,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val title: String,
    val overview: String,
    val image: String,
    val runtime: String = "45m",
    val slug: String = ""
)

data class SubtitleTrackData(
    @SerializedName("url") val url: String,
    @SerializedName("label") val label: String,
    @SerializedName("language") val language: String = "es",
    @SerializedName("isDefault") val isDefault: Boolean = false
)

data class StreamItem(
    @SerializedName("type") val type: String? = null,
    @SerializedName("url") val url: String,
    @SerializedName("source") val source: String? = null,
    @SerializedName("foundVia") val foundVia: String? = null,
    @SerializedName("subtitle") val subtitle: Int? = null,
    @SerializedName("subtitles") val subtitleTracks: List<SubtitleTrackData>? = emptyList()
) {
    val safeSubtitles: List<SubtitleTrackData> get() = subtitleTracks ?: emptyList()
}

data class WatchHistoryItem(
    val movieUrl: String,
    val title: String,
    val image: String,
    val rating: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val playbackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isSeries: Boolean = false,
    val postId: Int? = null,
    val slug: String? = null
)
