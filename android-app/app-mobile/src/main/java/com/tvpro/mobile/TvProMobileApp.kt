package com.tvpro.mobile

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.tvpro.core.data.api.ApiClient
import javax.net.ssl.HttpsURLConnection

class TvProMobileApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(ApiClient.sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache_mobile"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
