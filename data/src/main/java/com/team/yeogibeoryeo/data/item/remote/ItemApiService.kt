package com.team.yeogibeoryeo.data.item.remote

import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ItemApiService {
    @GET("getItem")
    suspend fun getItem(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("itemNm") itemNm: String,
    ): ItemGuideResponseDto
}
