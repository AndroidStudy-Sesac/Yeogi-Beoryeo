package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotHeaderDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemsDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SpotRemoteDataSourceTest {

    @Test
    fun `검색어 기반 조회 시 keyword가 addr 파라미터로 전달된다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals("문래동", apiService.requestedAddr)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertNull(apiService.requestedLatitude)
        assertNull(apiService.requestedLongitude)
        assertNull(apiService.requestedRadius)
        assertEquals(1, result.size)
        assertEquals("폐건전지 수거함", result.first().spotNm)
    }

    @Test
    fun `현재 위치 기반 조회 시 공백 addr와 좌표 반경이 전달된다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
        )

        assertEquals(" ", apiService.requestedAddr)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(37.5182396969791, apiService.requestedLatitude)
        assertEquals(126.895880210522, apiService.requestedLongitude)
        assertEquals(500, apiService.requestedRadius)
        assertEquals(1, result.size)
    }

    @Test
    fun `정상 응답이면 item 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals(1, result.size)
        assertEquals("폐건전지 수거함", result.first().spotNm)
        assertEquals("서울특별시 영등포구 문래동", result.first().addrBase)
        assertEquals("주민센터 앞", result.first().addrDtl)
    }

    @Test
    fun `NODATA_ERROR 응답이면 빈 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNoDataResponse(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "약수동",
        )

        assertEquals(emptyList<SpotItemDto>(), result)
    }

    @Test
    fun `정상과 데이터 없음 외 응답 코드는 예외를 전달한다`() {
        val apiService = FakeSpotApiService(
            response = createErrorResponse(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.searchByKeyword(
                    serviceKey = TEST_SERVICE_KEY,
                    keyword = "문래동",
                )
            }
        }

        assertEquals("수거 장소 API 오류(30): SERVICE_KEY_IS_NOT_REGISTERED_ERROR", exception.message)
    }

    @Test
    fun `items가 null이면 빈 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponseWithNullItems(),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals(emptyList<SpotItemDto>(), result)
    }

    @Test
    fun `검색어 기반 조회 시 totalCount가 numOfRows 이하면 첫 페이지만 조회한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 1,
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(1, result.size)
    }

    @Test
    fun `검색어 기반 조회 시 totalCount가 numOfRows를 초과하면 추가 페이지를 병합한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                    spotItem("1페이지 재활용센터", "서울특별시 영등포구 문래동", "학교 앞"),
                ),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = listOf(
                        spotItem("2페이지 수거함", "서울특별시 영등포구 문래동", "공원 앞"),
                    ),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지 수거함", "1페이지 재활용센터", "2페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `검색어 기반 여러 페이지 결과는 기존 순서를 유지하며 중복을 제거한다`() = runBlocking {
        val duplicateItem = spotItem("중복 수거함", "서울특별시 영등포구 문래동", "주민센터 앞")
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    duplicateItem,
                    spotItem("1페이지 재활용센터", "서울특별시 영등포구 문래동", "학교 앞"),
                ),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = listOf(
                        duplicateItem,
                        spotItem("2페이지 수거함", "서울특별시 영등포구 문래동", "공원 앞"),
                    ),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("중복 수거함", "1페이지 재활용센터", "2페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `검색어 기반 조회는 5페이지를 초과해도 전체 페이지를 조회한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 12,
                items = listOf(spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "1")),
            ),
            responsesByPage = (2..6).associateWith { pageNo ->
                createResponse(
                    pageNo = pageNo,
                    numOfRows = 2,
                    totalCount = 12,
                    items = listOf(
                        spotItem("${pageNo}페이지 수거함", "서울특별시 영등포구 문래동", pageNo.toString()),
                    ),
                )
            },
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "상동",
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2, 3, 4, 5, 6), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지 수거함", "2페이지 수거함", "3페이지 수거함", "4페이지 수거함", "5페이지 수거함", "6페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `검색어 기반 추가 페이지 실패 시 조회된 페이지 결과를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                ),
            ),
            failurePages = setOf(2),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByKeywordResult(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(listOf("1페이지 수거함"), result.items.map { item -> item.spotNm })
        assertEquals(true, result.isPartial)
    }

    @Test
    fun `좌표 기반 조회 시 totalCount가 numOfRows를 초과하면 2페이지까지 병합한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                    spotItem("1페이지 재활용센터", "서울특별시 영등포구 문래동", "학교 앞"),
                ),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = listOf(
                        spotItem("2페이지 수거함", "서울특별시 영등포구 문래동", "공원 앞"),
                    ),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지 수거함", "1페이지 재활용센터", "2페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `좌표 기반 조회는 totalCount가 커도 최대 2페이지만 조회한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 1,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                ),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 1,
                    totalCount = 3,
                    items = listOf(
                        spotItem("2페이지 수거함", "서울특별시 영등포구 문래동", "공원 앞"),
                    ),
                ),
                3 to createResponse(
                    pageNo = 3,
                    numOfRows = 1,
                    totalCount = 3,
                    items = listOf(
                        spotItem("3페이지 수거함", "서울특별시 영등포구 문래동", "학교 앞"),
                    ),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
            numOfRows = 1,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지 수거함", "2페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `좌표 기반 여러 페이지 결과는 기존 순서를 유지하며 중복을 제거한다`() = runBlocking {
        val duplicateItem = spotItem("중복 수거함", "서울특별시 영등포구 문래동", "주민센터 앞")
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    duplicateItem,
                    spotItem("1페이지 재활용센터", "서울특별시 영등포구 문래동", "학교 앞"),
                ),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = listOf(
                        duplicateItem,
                        spotItem("2페이지 수거함", "서울특별시 영등포구 문래동", "공원 앞"),
                    ),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("중복 수거함", "1페이지 재활용센터", "2페이지 수거함"),
            result.map { item -> item.spotNm },
        )
    }

    @Test
    fun `좌표 기반 조회는 병합 결과를 최대 120개로 제한한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 150,
                items = numberedSpotItems(range = 1..100),
            ),
            responsesByPage = mapOf(
                2 to createResponse(
                    pageNo = 2,
                    numOfRows = 100,
                    totalCount = 150,
                    items = numberedSpotItems(range = 101..150),
                ),
            ),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(120, result.size)
        assertEquals("수거함 1", result.first().spotNm)
        assertEquals("수거함 120", result.last().spotNm)
    }

    @Test
    fun `좌표 기반 추가 페이지 실패 시 첫 페이지 결과를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                ),
            ),
            failurePages = setOf(2),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(listOf("1페이지 수거함"), result.map { item -> item.spotNm })
    }

    @Test
    fun `좌표 기반 첫 페이지 실패 시 예외를 전달한다`() {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
            failurePages = setOf(1),
        )
        val dataSource = SpotRemoteDataSource(apiService)

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.searchByLocation(
                    serviceKey = TEST_SERVICE_KEY,
                    latitude = 37.5182396969791,
                    longitude = 126.895880210522,
                    radiusMeter = 500,
                )
            }
        }
    }

    private class FakeSpotApiService(
        private val response: SpotResponseDto,
        private val responsesByPage: Map<Int, SpotResponseDto> = emptyMap(),
        private val failurePages: Set<Int> = emptySet(),
    ) : SpotApiService {

        var requestedServiceKey: String? = null
        var requestedPageNo: Int? = null
        val requestedPageNos = mutableListOf<Int>()
        var requestedNumOfRows: Int? = null
        var requestedAddr: String? = null
        var requestedLatitude: Double? = null
        var requestedLongitude: Double? = null
        var requestedRadius: Int? = null
        var requestedType: String? = null

        override suspend fun getSpots(
            serviceKey: String,
            pageNo: Int,
            numOfRows: Int,
            addr: String,
            latitude: Double?,
            longitude: Double?,
            radius: Int?,
            type: String,
        ): SpotResponseDto {
            requestedServiceKey = serviceKey
            requestedPageNo = pageNo
            requestedPageNos += pageNo
            requestedNumOfRows = numOfRows
            requestedAddr = addr
            requestedLatitude = latitude
            requestedLongitude = longitude
            requestedRadius = radius
            requestedType = type

            if (pageNo in failurePages) {
                error("page failed")
            }

            return responsesByPage[pageNo] ?: response
        }
    }

    private companion object {
        const val TEST_SERVICE_KEY = "test-service-key"

        fun createNormalResponse(
            pageNo: Int? = null,
            numOfRows: Int? = null,
            totalCount: Int? = null,
        ): SpotResponseDto {
            return createResponse(
                pageNo = pageNo,
                numOfRows = numOfRows,
                totalCount = totalCount,
                items = listOf(
                    spotItem(
                        name = "폐건전지 수거함",
                        address = "서울특별시 영등포구 문래동",
                        detailAddress = "주민센터 앞",
                    ),
                ),
            )
        }

        fun createNoDataResponse(): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "03",
                        resultMsg = "NODATA_ERROR",
                    ),
                    body = SpotBodyDto(
                        items = null,
                        numOfRows = null,
                        pageNo = null,
                        totalCount = null,
                    ),
                ),
            )
        }

        fun createErrorResponse(): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "30",
                        resultMsg = "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                    ),
                    body = SpotBodyDto(
                        items = null,
                    ),
                ),
            )
        }

        fun createNormalResponseWithNullItems(): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "00",
                        resultMsg = "NORMAL SERVICE",
                    ),
                    body = SpotBodyDto(
                        items = null,
                    ),
                ),
            )
        }

        fun createResponse(
            pageNo: Int?,
            numOfRows: Int?,
            totalCount: Int?,
            items: List<SpotItemDto>,
        ): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "00",
                        resultMsg = "NORMAL SERVICE",
                    ),
                    body = SpotBodyDto(
                        items = SpotItemsDto(item = items),
                        numOfRows = numOfRows?.let(::JsonPrimitive),
                        pageNo = pageNo?.let(::JsonPrimitive),
                        totalCount = totalCount?.let(::JsonPrimitive),
                    ),
                ),
            )
        }

        fun spotItem(
            name: String,
            address: String,
            detailAddress: String,
        ): SpotItemDto {
            return SpotItemDto(
                spotNm = name,
                addrBase = address,
                addrDtl = detailAddress,
            )
        }

        fun numberedSpotItems(range: IntRange): List<SpotItemDto> {
            return range.map { number ->
                spotItem(
                    name = "수거함 $number",
                    address = "서울특별시 영등포구 문래동 $number",
                    detailAddress = "상세 위치 $number",
                )
            }
        }
    }
}
