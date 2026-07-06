package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

class SpotRemoteDataSource @Inject constructor(
    private val apiService: SpotApiService,
) {
    suspend fun searchByKeyword(
        serviceKey: String,
        keyword: String,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): List<SpotItemDto> {
        val firstPage = fetchKeywordPage(
            serviceKey = serviceKey,
            keyword = keyword,
            pageNo = pageNo,
            numOfRows = numOfRows,
        )
        val effectiveNumOfRows = firstPage.numOfRows ?: numOfRows
        val currentPageNo = firstPage.pageNo ?: pageNo
        val totalCount = firstPage.totalCount
        val totalPages = if (totalCount == null || effectiveNumOfRows <= 0) {
            currentPageNo
        } else {
            ((totalCount + effectiveNumOfRows - 1) / effectiveNumOfRows)
                .coerceAtLeast(currentPageNo)
        }
        val lastPage = totalPages.coerceAtMost(currentPageNo + MAX_ADDR_SEARCH_PAGE_COUNT - 1)
        val mergedItems = firstPage.items.toMutableList()

        for (nextPageNo in (currentPageNo + 1)..lastPage) {
            val nextPage = try {
                fetchKeywordPage(
                    serviceKey = serviceKey,
                    keyword = keyword,
                    pageNo = nextPageNo,
                    numOfRows = numOfRows,
                )
            } catch (exception: Throwable) {
                if (exception is CancellationException) throw exception

                break
            }

            mergedItems += nextPage.items
        }

        return mergedItems.distinctBy { item ->
            listOf(
                item.spotNm.orEmpty().trim(),
                item.addrBase.orEmpty().trim(),
                item.addrDtl.orEmpty().trim(),
            )
        }
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

    private suspend fun fetchKeywordPage(
        serviceKey: String,
        keyword: String,
        pageNo: Int,
        numOfRows: Int,
    ): SpotPageResult {
        val response = apiService.getSpots(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            addr = keyword,
        )

        return response.toSpotPageResult()
    }

    private fun SpotResponseDto.toSpotPageResult(): SpotPageResult {
        return SpotPageResult(
            items = toSpotItemsOrEmpty(),
            numOfRows = response.body.numOfRows.toIntOrNull(),
            pageNo = response.body.pageNo.toIntOrNull(),
            totalCount = response.body.totalCount.toIntOrNull(),
        )
    }

    private fun SpotResponseDto.toSpotItemsOrEmpty(): List<SpotItemDto> {
        val resultCode = response.header.resultCode

        if (resultCode == RESULT_CODE_NO_DATA) {
            return emptyList()
        }

        return response.body.items?.item.orEmpty()
    }

    private fun kotlinx.serialization.json.JsonElement?.toIntOrNull(): Int? {
        return (this as? JsonPrimitive)
            ?.content
            ?.toIntOrNull()
    }

    private data class SpotPageResult(
        val items: List<SpotItemDto>,
        val numOfRows: Int?,
        val pageNo: Int?,
        val totalCount: Int?,
    )

    private companion object {
        const val RESULT_CODE_NO_DATA = "03"
        const val MAX_ADDR_SEARCH_PAGE_COUNT = 5
    }
}
