package com.team.yeogibeoryeo.data.spot.remote

import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotApiService {

    @GET("getSpot")
    suspend fun getSpots(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("addr") addr: String,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("_type") type: String = "json",
    ): SpotResponseDto
}