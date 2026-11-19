package com.tvpro.core.data.api

import com.tvpro.core.BuildConfig
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        val currentUserAgent = originalRequest.header("User-Agent")
        if (currentUserAgent == null || currentUserAgent.contains("TVPro") || currentUserAgent.contains("ExoPlayer") || currentUserAgent.contains("media3")) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
        }

        val host = originalRequest.url.host.lowercase()
        if (originalRequest.header("Referer") == null) {
            val referer = when {
                host.contains("tudorama") || host.contains("minochinos") -> "https://tudorama.com/"
                host.contains("doramasflix") || host.contains("seriesapi") -> "https://doramasflix.in/"
                host.contains("ok.ru") || host.contains("okcdn.ru") || host.contains("odnoklassniki") || host.contains("vkuser.net") || host.contains("mycdn.me") -> "https://ok.ru/"
                host.contains("voe") || host.contains("megankid") || host.contains("cloudwindow") || host.contains("delivery") || host.contains("goofy-banana") || host.contains("susanexpectcare") || host.contains("arianathankspecial") -> "https://voe.sx/"
                host.contains("filemoon") || host.contains("bysesukior") || host.contains("bysefujedu") -> "https://filemoon.sx/"
                host.contains("abyss") -> "https://abyssplayer.com/"
                host.contains("vimeos") || host.contains("vcdn") || host.contains("storage") -> "https://vimeos.net/"
                host.contains("goodstream") -> "https://goodstream.one/"
                else -> null
            }
            if (referer != null) {
                builder.header("Referer", referer)
            }
        }
        if (originalRequest.header("Origin") == null) {
            val origin = when {
                host.contains("tudorama") || host.contains("minochinos") -> "https://tudorama.com"
                host.contains("doramasflix") || host.contains("seriesapi") -> "https://doramasflix.in"
                host.contains("ok.ru") || host.contains("okcdn.ru") || host.contains("odnoklassniki") || host.contains("vkuser.net") || host.contains("mycdn.me") -> "https://ok.ru"
                host.contains("voe") || host.contains("megankid") || host.contains("cloudwindow") || host.contains("delivery") || host.contains("goofy-banana") || host.contains("susanexpectcare") || host.contains("arianathankspecial") -> "https://voe.sx"
                host.contains("filemoon") || host.contains("bysesukior") || host.contains("bysefujedu") -> "https://filemoon.sx"
                host.contains("abyss") -> "https://abyssplayer.com"
                host.contains("vimeos") || host.contains("vcdn") || host.contains("storage") -> "https://vimeos.net"
                host.contains("goodstream") -> "https://goodstream.one"
                else -> null
            }
            if (origin != null) {
                builder.header("Origin", origin)
            }
        }

        chain.proceed(builder.build())
    }

    val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES))
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val streamingOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES))
            .addInterceptor(userAgentInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    val healthCheckOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(10, 2, TimeUnit.MINUTES))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    val apiService: TvProApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TvProApiService::class.java)
    }
}
