package com.tvpro.mobile.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SearchHistoryManager {
    private const val PREF_NAME = "tvpro_search_prefs"
    private const val KEY_SEARCH_HISTORY = "recent_queries"
    private val gson = Gson()

    fun getRecentQueries(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addQuery(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val current = getRecentQueries(context).toMutableList()
        current.removeAll { it.equals(trimmed, ignoreCase = true) }
        current.add(0, trimmed)
        val capped = current.take(10)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_HISTORY, gson.toJson(capped))
            .apply()
    }

    fun removeQuery(context: Context, query: String) {
        val current = getRecentQueries(context).toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SEARCH_HISTORY, gson.toJson(current))
            .apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SEARCH_HISTORY)
            .apply()
    }
}
