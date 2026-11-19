package com.example.peak.data.remote.retrofit

import com.example.peak.BuildConfig
import com.example.peak.data.remote.api.TmdbApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object to provide the Retrofit API instance.
 * Moved to data.remote.retrofit package.
 */
object RetrofitInstance {
    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            when (original.url.host) {
                "api.themoviedb.org" -> {
                    val url = original.url.newBuilder()
                        .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                        .build()
                    val request = original.newBuilder().url(url).build()
                    chain.proceed(request)
                }
                "api.trakt.tv" -> {
                    val clientId = BuildConfig.TRAKT_CLIENT_ID
                    if (clientId.isNotBlank()) {
                        val request = original.newBuilder()
                            .header("trakt-api-key", clientId)
                            .build()
                        chain.proceed(request)
                    } else {
                        chain.proceed(original)
                    }
                }
                else -> {
                    chain.proceed(original)
                }
            }
        }
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun getOkHttpClient(): OkHttpClient = client

    val api: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    val traktApi: com.example.peak.data.remote.api.TraktApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.example.peak.data.remote.api.TraktApi::class.java)
    }
}
