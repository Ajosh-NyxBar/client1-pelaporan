package com.laporan.ops.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * KONFIGURASI BASE URL
     * - Production server (deployed): http://103.247.10.60/api/
     * - Local development:
     *   - Emulator AVD  : http://10.0.2.2:3000/api/
     *   - Perangkat fisik: http://<IP-LAN>:3000/api/  (mis. http://192.168.1.5:3000/api/)
     */
    const val BASE_URL = "http://103.247.10.60/api/"

    /** Base URL untuk static files (foto upload) — tanpa /api/ */
    val PHOTO_BASE_URL: String get() = BASE_URL.removeSuffix("api/")

    private var authToken: String? = null

    fun setToken(token: String?) {
        authToken = token
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .apply { authToken?.let { header("Authorization", "Bearer $it") } }
                    .build()
                chain.proceed(req)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
