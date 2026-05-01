package com.moon.yeogi_beoryeo.spike

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotApiService {

    @GET("1482000/WasteRecyclingService/getSpot")
    suspend fun getSpotLocations(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 10,
        @Query("addr") addr: String
    ): SpotBasicResponse

    @GET("1741000/household_waste_info/info")
    suspend fun getSpotDetails(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("returnType") returnType: String = "json",
        @Query("cond[SGG_NM::LIKE]") sggName: String
    ): SpotDetailResponse

    companion object {
        private const val BASE_URL = "https://apis.data.go.kr/"

        fun create(): SpotApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpotApiService::class.java)
        }
    }
}