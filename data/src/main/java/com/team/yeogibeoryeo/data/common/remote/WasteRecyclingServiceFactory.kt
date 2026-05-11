package com.team.yeogibeoryeo.data.common.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

object WasteRecyclingServiceFactory {
    private const val BASE_URL = "https://apis.data.go.kr/1482000/WasteRecyclingService/"

    fun createItemApiService(
        json: Json = Json { ignoreUnknownKeys = true },
    ): ItemApiService = createRetrofit(json).create(ItemApiService::class.java)

    private fun createRetrofit(json: Json): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
