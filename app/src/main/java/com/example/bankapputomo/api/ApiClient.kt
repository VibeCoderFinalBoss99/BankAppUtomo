package com.example.bankapputomo.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val AUTO_ASSIGNED_BASE_URL = "http://10.0.2.2:8181/bankAppUtomo/"

    fun getBaseUrl(context: Context): String {
        return AUTO_ASSIGNED_BASE_URL
    }

    fun getInstance(context: Context): ApiService {
        val baseUrl = getBaseUrl(context)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
