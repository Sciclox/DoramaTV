package com.tvpro.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tvpro.core.data.model.MovieItem
import com.tvpro.core.data.model.WatchHistoryItem

object WatchHistoryManager {
    private const val PREF_NAME = "tvpro_preferences"
    private const val KEY_HISTORY = "watch_history"
    private const val KEY_MY_LIST = "my_list"
    private val gson = Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveToHistory(
        context: Context,
        movie: MovieItem,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        isSeries: Boolean = false,
        postId: Int? = null,
        slug: String? = null
    ) {
        val history = getHistory(context).toMutableList()
        val existing = history.find { it.movieUrl == movie.url }
        val currentPos = if (positionMs > 0) positionMs else (existing?.playbackPositionMs ?: 0L)
        val currentDur = if (durationMs > 0) durationMs else (existing?.durationMs ?: 0L)

        val finalIsSeries = isSeries || (existing?.isSeries ?: false)
        val finalSeason = if (finalIsSeries) (seasonNumber ?: existing?.seasonNumber) else null
        val finalEpisode = if (finalIsSeries) (episodeNumber ?: existing?.episodeNumber) else null
        val finalPostId = postId ?: movie.postId ?: existing?.postId
        val finalSlug = slug ?: movie.slug ?: existing?.slug

        val cleanTitle = movie.title.replace(Regex("\\s*-\\s*T\\d+[:E\\d+]*.*", RegexOption.IGNORE_CASE), "").trim()
        history.removeAll { it.movieUrl == movie.url }
        history.add(
            0, WatchHistoryItem(
                movieUrl = movie.url,
                title = if (cleanTitle.isNotBlank()) cleanTitle else movie.title,
                image = movie.image,
                rating = movie.rating,
                playbackPositionMs = currentPos,
                durationMs = currentDur,
                seasonNumber = finalSeason,
                episodeNumber = finalEpisode,
                isSeries = finalIsSeries,
                postId = finalPostId,
                slug = finalSlug
            )
        )
        val trimmed = history.take(20)
        getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(trimmed)).apply()
    }

    fun updatePosition(
        context: Context,
        movieUrl: String,
        positionMs: Long,
        durationMs: Long,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        isSeries: Boolean? = null
    ) {
        if (positionMs <= 0) return
        val history = getHistory(context).toMutableList()
        val index = history.indexOfFirst { it.movieUrl == movieUrl }
        if (index != -1) {
            val item = history[index]
            val finalIsSeries = isSeries ?: item.isSeries
            history[index] = item.copy(
                playbackPositionMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else item.durationMs,
                seasonNumber = if (finalIsSeries) (seasonNumber ?: item.seasonNumber) else null,
                episodeNumber = if (finalIsSeries) (episodeNumber ?: item.episodeNumber) else null,
                isSeries = finalIsSeries,
                timestamp = System.currentTimeMillis()
            )
            getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
        }
    }

    fun resetMoviePosition(context: Context, movieUrl: String) {
        val history = getHistory(context).toMutableList()
        val index = history.indexOfFirst { it.movieUrl == movieUrl }
        if (index != -1) {
            val item = history[index]
            history[index] = item.copy(
                playbackPositionMs = 0L,
                timestamp = System.currentTimeMillis()
            )
            getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
        }
    }

    fun getMovieHistory(context: Context, movieUrl: String): WatchHistoryItem? {
        val item = getHistory(context).firstOrNull { it.movieUrl == movieUrl } ?: return null
        val cleanTitle = item.title.replace(Regex("\\s*-\\s*T\\d+[:E\\d+]*.*", RegexOption.IGNORE_CASE), "").trim()
        return if (cleanTitle.isNotBlank() && cleanTitle != item.title) item.copy(title = cleanTitle) else item
    }

    fun getHistory(context: Context): List<WatchHistoryItem> {
        val json = getPrefs(context).getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
            val list: List<WatchHistoryItem> = gson.fromJson(json, type) ?: emptyList()
            list.map { item ->
                val cleanTitle = item.title.replace(Regex("\\s*-\\s*T\\d+[:E\\d+]*.*", RegexOption.IGNORE_CASE), "").trim()
                if (cleanTitle.isNotBlank() && cleanTitle != item.title) item.copy(title = cleanTitle) else item
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply()
    }

    fun removeFromHistory(context: Context, movieUrl: String) {
        val history = getHistory(context).toMutableList()
        history.removeAll { it.movieUrl == movieUrl }
        getPrefs(context).edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
    }

    fun toggleMyList(context: Context, movie: MovieItem): Boolean {
        val myList = getMyList(context).toMutableList()
        val existing = myList.find { it.url == movie.url }
        val isAdded: Boolean
        if (existing != null) {
            myList.remove(existing)
            isAdded = false
        } else {
            myList.add(0, movie)
            isAdded = true
        }
        getPrefs(context).edit().putString(KEY_MY_LIST, gson.toJson(myList)).apply()
        return isAdded
    }

    fun isInMyList(context: Context, movieUrl: String): Boolean {
        return getMyList(context).any { it.url == movieUrl }
    }

    fun removeFromMyList(context: Context, movieUrl: String) {
        val myList = getMyList(context).toMutableList()
        myList.removeAll { it.url == movieUrl }
        getPrefs(context).edit().putString(KEY_MY_LIST, gson.toJson(myList)).apply()
    }

    fun clearMyList(context: Context) {
        getPrefs(context).edit().remove(KEY_MY_LIST).apply()
    }

    fun getMyList(context: Context): List<MovieItem> {
        val json = getPrefs(context).getString(KEY_MY_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<MovieItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
