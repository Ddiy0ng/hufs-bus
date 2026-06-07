package com.example.hufs_bus.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Android emulator에서 로컬 Spring 서버(8080)에 붙는 기본 주소입니다.
    // 실제 배포 서버가 생기면 이 값만 서버 URL로 바꾸면 됩니다.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    @Volatile
    private var accessToken: String? = null

    fun setAccessToken(token: String?) {
        accessToken = token
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = accessToken
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: BusApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BusApiService::class.java)
}
