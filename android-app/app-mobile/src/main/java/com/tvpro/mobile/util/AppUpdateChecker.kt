package com.tvpro.mobile.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tvpro.core.data.api.ApiClient
import com.tvpro.mobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class UpdateInfo(
    @SerializedName("version_code") val versionCode: Int = 1,
    @SerializedName("version_name") val versionName: String = "1.0.0",
    @SerializedName("release_notes") val releaseNotes: String = "",
    @SerializedName("download_url") val downloadUrl: String = "",
    @SerializedName("force_update") val forceUpdate: Boolean = false
)

object AppUpdateChecker {
    private const val UPDATE_CHECK_URL = "https://raw.githubusercontent.com/usuario/tvpro-releases/main/version.json"
    private val gson = Gson()

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_CHECK_URL)
                .build()
            val response = ApiClient.okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return@withContext null
                val remoteInfo = gson.fromJson(json, UpdateInfo::class.java)
                if (remoteInfo != null && remoteInfo.versionCode > BuildConfig.VERSION_CODE) {
                    return@withContext remoteInfo
                }
            }
        } catch (_: Exception) {
            // Silently ignore if offline or check fails
        }
        null
    }

    fun openDownloadPage(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
