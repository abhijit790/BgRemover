package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RemoveBgApiClient {
    private const val BASE_URL = "https://api.remove.bg/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: RemoveBgService = retrofit.create(RemoveBgService::class.java)

    fun parseError(errorJson: String?): String {
        if (errorJson.isNullOrBlank()) return "Unknown server error occurred."
        return try {
            val adapter = moshi.adapter(ErrorResponse::class.java)
            val response = adapter.fromJson(errorJson)
            response?.errors?.firstOrNull()?.title ?: "Unknown API response"
        } catch (e: Exception) {
            "Error decoding API response: ${e.localizedMessage}"
        }
    }
}

data class ErrorResponse(
    val errors: List<ErrorDetail>?
)

data class ErrorDetail(
    val title: String?
)
