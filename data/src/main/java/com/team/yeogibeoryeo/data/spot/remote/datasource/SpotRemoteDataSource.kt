package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive

class SpotRemoteDataSource @Inject constructor(
    private val apiService: SpotApiService,
    private val mapSearchTimingLogger: MapSearchTimingLogger = MapSearchTimingLogger.NoOp,
) {
    suspend fun searchByKeyword(
        serviceKey: String,
        keyword: String,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): List<SpotItemDto> {
        return searchByKeywordResult(
            serviceKey = serviceKey,
            keyword = keyword,
            pageNo = pageNo,
            numOfRows = numOfRows,
        ).items
    }

    suspend fun searchByKeywordResult(
        serviceKey: String,
        keyword: String,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): SpotKeywordSearchResult {
        val searchStartedAtNanos = System.nanoTime()
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
        val lastPage = totalPages
        val mergedItems = firstPage.items.toMutableList()
        var isPartial = false
        var fetchedPageCount = 1

        mapSearchTimingLogger.log(
            "getSpot addr first page finished page=$currentPageNo " +
                "count=${firstPage.items.size} totalCount=${totalCount ?: UNKNOWN_TOTAL_COUNT} " +
                "elapsedMs=${searchStartedAtNanos.elapsedMs()}",
        )

        for (nextPageNo in (currentPageNo + 1)..lastPage) {
            val nextPage = try {
                fetchKeywordPage(
                    serviceKey = serviceKey,
                    keyword = keyword,
                    pageNo = nextPageNo,
                    numOfRows = numOfRows,
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception

                isPartial = true
                break
            }

            mergedItems += nextPage.items
            fetchedPageCount += 1
        }

        mapSearchTimingLogger.log(
            "getSpot addr all pages finished pages=$fetchedPageCount " +
                "rawCount=${mergedItems.size} elapsedMs=${searchStartedAtNanos.elapsedMs()} " +
                "partial=$isPartial",
        )

        val mergeStartedAtNanos = System.nanoTime()
        val dedupedItems = mergedItems.distinctBy { item -> item.toDedupKey() }
        mapSearchTimingLogger.log(
            "merge/dedup finished before=${mergedItems.size} after=${dedupedItems.size} " +
                "elapsedMs=${mergeStartedAtNanos.elapsedMs()}",
        )

        return SpotKeywordSearchResult(
            items = dedupedItems,
            isPartial = isPartial,
        )
    }

    suspend fun searchByLocation(
        serviceKey: String,
        latitude: Double,
        longitude: Double,
        radiusMeter: Int,
        pageNo: Int = 1,
        numOfRows: Int = 100,
    ): List<SpotItemDto> {
        val firstPage = fetchLocationPage(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            latitude = latitude,
            longitude = longitude,
            radiusMeter = radiusMeter,
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
        val lastPage = minOf(
            totalPages,
            currentPageNo + LOCATION_MAX_PAGE_COUNT - 1,
        )
        val mergedItems = firstPage.items.toMutableList()

        for (nextPageNo in (currentPageNo + 1)..lastPage) {
            val nextPage = try {
                fetchLocationPage(
                    serviceKey = serviceKey,
                    pageNo = nextPageNo,
                    numOfRows = numOfRows,
                    latitude = latitude,
                    longitude = longitude,
                    radiusMeter = radiusMeter,
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception

                break
            }

            mergedItems += nextPage.items
        }

        return mergedItems
            .distinctBy { item -> item.toDedupKey() }
            .take(LOCATION_MAX_RESULT_COUNT)
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

    private suspend fun fetchLocationPage(
        serviceKey: String,
        pageNo: Int,
        numOfRows: Int,
        latitude: Double,
        longitude: Double,
        radiusMeter: Int,
    ): SpotPageResult {
        val response = apiService.getSpots(
            serviceKey = serviceKey,
            pageNo = pageNo,
            numOfRows = numOfRows,
            addr = LOCATION_SEARCH_ADDR_QUERY,
            latitude = latitude,
            longitude = longitude,
            radius = radiusMeter,
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

    private fun SpotItemDto.toDedupKey(): List<String> {
        return listOf(
            spotNm.orEmpty().trim(),
            addrBase.orEmpty().trim(),
            addrDtl.orEmpty().trim(),
        )
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
        const val UNKNOWN_TOTAL_COUNT = "unknown"
        const val LOCATION_SEARCH_ADDR_QUERY = " "
        const val LOCATION_MAX_PAGE_COUNT = 2
        const val LOCATION_MAX_RESULT_COUNT = 120
    }
}

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L

data class SpotKeywordSearchResult(
    val items: List<SpotItemDto>,
    val isPartial: Boolean,
)
