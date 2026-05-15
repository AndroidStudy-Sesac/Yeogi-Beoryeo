package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import javax.inject.Inject

class SpotRemoteDataSource @Inject constructor(
    private val apiService: SpotApiService,
) {
    suspend fun searchByKeyword(
        serviceKey: String,
        keyword: String,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): List<SpotItemDto> {
        val response = apiService.getSpots(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            addr = keyword,
        )

        return response.toSpotItemsOrEmpty()
    }

    suspend fun searchByLocation(
        serviceKey: String,
        latitude: Double,
        longitude: Double,
        radiusMeter: Int,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): List<SpotItemDto> {
        val response = apiService.getSpots(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            addr = " ",
            latitude = latitude,
            longitude = longitude,
            radius = radiusMeter,
        )

        return response.toSpotItemsOrEmpty()
    }

    private fun com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto.toSpotItemsOrEmpty(): List<SpotItemDto> {
        val resultCode = response.header.resultCode

        if (resultCode == RESULT_CODE_NO_DATA) {
            return emptyList()
        }

        return response.body.items?.item.orEmpty()
    }

    private companion object {
        const val RESULT_CODE_NO_DATA = "03"
    }
}