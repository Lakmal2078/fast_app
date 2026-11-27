package com.example.fastcash.data.remote

import com.example.fastcash.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    fun authenticated(accessKey: String): FastCashApiService {
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder()
                    .header("Authorization", "Bearer $accessKey").build())
            }.build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(FastCashApiService::class.java)
    }
}
