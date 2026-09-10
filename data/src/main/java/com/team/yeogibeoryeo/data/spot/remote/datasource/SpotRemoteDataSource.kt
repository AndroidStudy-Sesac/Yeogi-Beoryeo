package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalHttpStatusClass
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

class SpotRemoteDataSource @Inject constructor(
    private val apiService: SpotApiService,
    private val nonFatalErrorReporter: NonFatalErrorReporter,
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
        val firstPage = fetchFirstPage {
            fetchKeywordPage(
                serviceKey = serviceKey,
                keyword = keyword,
                pageNo = pageNo,
                numOfRows = numOfRows,
            )
        }
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

                nonFatalErrorReporter.reportCollectionSpotFailure(
                    error = exception,
                    isPartialResult = true,
                )
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
        val firstPage = fetchFirstPage {
            fetchLocationPage(
                serviceKey = serviceKey,
                pageNo = pageNo,
                numOfRows = numOfRows,
                latitude = latitude,
                longitude = longitude,
                radiusMeter = radiusMeter,
            )
        }
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

                nonFatalErrorReporter.reportCollectionSpotFailure(
                    error = exception,
                    isPartialResult = true,
                )
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

        return response.bodyOrThrow().toSpotPageResult()
    }

    private suspend fun fetchFirstPage(
        fetch: suspend () -> SpotPageResult,
    ): SpotPageResult = try {
        fetch()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        nonFatalErrorReporter.reportCollectionSpotFailure(
            error = exception,
            isPartialResult = false,
        )
        throw exception
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

        return response.bodyOrThrow().toSpotPageResult()
    }

    private fun Response<SpotResponseDto>.bodyOrThrow(): SpotResponseDto {
        if (!isSuccessful) throw HttpException(this)

        return body() ?: throw SerializationException(
            "수거 장소 API 응답 본문이 없습니다 (HTTP ${code()})",
        )
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

        return when (resultCode) {
            RESULT_CODE_SUCCESS -> response.body.items?.item.orEmpty()
            RESULT_CODE_NO_DATA -> emptyList()
            else -> throw SpotApiResponseException(
                resultCode = resultCode,
                resultMessage = response.header.resultMsg,
            )
        }
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
        const val RESULT_CODE_SUCCESS = "00"
        const val RESULT_CODE_NO_DATA = "03"
        const val UNKNOWN_TOTAL_COUNT = "unknown"
        const val LOCATION_SEARCH_ADDR_QUERY = " "
        const val LOCATION_MAX_PAGE_COUNT = 2
        const val LOCATION_MAX_RESULT_COUNT = 120
    }
}

private class SpotApiResponseException(
    resultCode: String,
    resultMessage: String,
) : IllegalStateException("수거 장소 API 오류($resultCode): $resultMessage")

private fun NonFatalErrorReporter.reportCollectionSpotFailure(
    error: Throwable,
    isPartialResult: Boolean,
) {
    val context = error.toCollectionSpotNonFatalErrorContext(isPartialResult) ?: return
    report(error, context)
}

private fun Throwable.toCollectionSpotNonFatalErrorContext(
    isPartialResult: Boolean,
): NonFatalErrorContext? {
    val failure = when (this) {
        is SocketTimeoutException -> CollectionSpotFailureContext(
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.TIMEOUT,
        )
        is HttpException -> CollectionSpotFailureContext(
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.HTTP,
            httpStatusClass = code().toNonFatalHttpStatusClass(),
        )
        is SpotApiResponseException -> CollectionSpotFailureContext(
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.HTTP,
        )
        is SerializationException -> CollectionSpotFailureContext(
            stage = NonFatalStage.RESPONSE_PARSING,
            category = NonFatalCategory.PARSING,
        )
        is IOException -> CollectionSpotFailureContext(
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.NETWORK,
        )
        else -> return null
    }

    return NonFatalErrorContext(
        api = NonFatalApi.COLLECTION_SPOT,
        stage = failure.stage,
        category = failure.category,
        httpStatusClass = failure.httpStatusClass,
        isPartialResult = isPartialResult,
    )
}

private data class CollectionSpotFailureContext(
    val stage: NonFatalStage,
    val category: NonFatalCategory,
    val httpStatusClass: NonFatalHttpStatusClass = NonFatalHttpStatusClass.NOT_AVAILABLE,
)

private fun Int.toNonFatalHttpStatusClass(): NonFatalHttpStatusClass =
    when (this) {
        in 100..199 -> NonFatalHttpStatusClass.INFORMATIONAL
        in 200..299 -> NonFatalHttpStatusClass.SUCCESS
        in 300..399 -> NonFatalHttpStatusClass.REDIRECTION
        in 400..499 -> NonFatalHttpStatusClass.CLIENT_ERROR
        in 500..599 -> NonFatalHttpStatusClass.SERVER_ERROR
        else -> NonFatalHttpStatusClass.NOT_AVAILABLE
    }

private fun Long.elapsedMs(): Long =
    (System.nanoTime() - this) / NANOS_PER_MILLISECOND

private const val NANOS_PER_MILLISECOND = 1_000_000L

data class SpotKeywordSearchResult(
    val items: List<SpotItemDto>,
    val isPartial: Boolean,
)
