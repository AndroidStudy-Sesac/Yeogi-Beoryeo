package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.spot.mapper.SpotMapper
import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.datasource.SpotRemoteDataSource
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotHeaderDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemsDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSpotRepositoryImplTest {

    @Test
    fun `검색어 기반 raw 조회 결과를 CollectionSpot으로 변환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchRawByKeyword(
            keyword = "문래동",
        ).spots

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals("문래동", apiService.requestedAddr)
        assertEquals(2, result.size)

        val firstSpot = result.first()

        assertEquals("폐건전지 수거함", firstSpot.name)
        assertEquals("서울특별시 영등포구 문래동", firstSpot.address)
        assertEquals("주민센터 앞", firstSpot.detailLocation)
        assertEquals(CollectionSpotType.BATTERY_BIN, firstSpot.type)
        assertNull(firstSpot.coordinate)
    }

    @Test
    fun `검색어 기반 조회 시 선택된 타입에 해당하는 장소만 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchRawByKeyword(
            keyword = "문래동",
            types = setOf(CollectionSpotType.RECYCLING_CENTER),
        ).spots

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals(1, result.size)
        assertEquals("재활용센터", result.first().name)
        assertEquals(CollectionSpotType.RECYCLING_CENTER, result.first().type)
    }

    @Test
    fun `좌표 없는 검색어 기반 조회는 geocoding하지 않고 결과를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(
            apiService = apiService,
        )

        val result = repository.searchRawByKeyword(
            keyword = "문래동",
        )

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals("문래동", apiService.requestedAddr)
        assertEquals(2, result.spots.size)
        assertNull(result.spots.first().coordinate)
    }

    @Test
    fun `검색어 기반 조회 시 여러 페이지 결과를 변환하고 반환한다`() = runBlocking {
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
        val repository = createRepository(apiService)

        val result = repository.searchRawByKeyword(
            keyword = "문래동",
        ).spots

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지 수거함", "1페이지 재활용센터", "2페이지 수거함"),
            result.map { spot -> spot.name },
        )
    }

    @Test
    fun `현재 위치 기반 조회 시 공백 addr와 좌표 반경을 전달하고 결과를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchRawByLocation(
            coordinate = Coordinate(
                latitude = 37.5182396969791,
                longitude = 126.895880210522,
            ),
            radiusMeter = 500,
        )

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals(" ", apiService.requestedAddr)
        assertEquals(37.5182396969791, apiService.requestedLatitude)
        assertEquals(126.895880210522, apiService.requestedLongitude)
        assertEquals(500, apiService.requestedRadius)
        assertEquals(2, result.size)
    }

    @Test
    fun `NODATA_ERROR 응답이면 빈 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNoDataResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchRawByKeyword(
            keyword = "약수동",
        ).spots

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals(emptyList<Any>(), result)
    }

    private fun createRepository(
        apiService: FakeSpotApiService,
    ): CollectionSpotRepositoryImpl {
        return CollectionSpotRepositoryImpl(
            remoteDataSource = SpotRemoteDataSource(apiService),
            spotMapper = SpotMapper(),
            publicDataKeyProvider = FakePublicDataKeyProvider(),
        )
    }

    private class FakePublicDataKeyProvider : AppKeyProvider {
        override val publicDataServiceKey: String = TEST_SERVICE_KEY
        override val naverClientId: String = "naver-client-id"
    }

    private class FakeSpotApiService(
        private val response: SpotResponseDto,
        private val responsesByPage: Map<Int, SpotResponseDto> = emptyMap(),
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

            return responsesByPage[pageNo] ?: response
        }
    }

    private companion object {
        const val TEST_SERVICE_KEY = "test-service-key"

        fun createNormalResponse(): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "00",
                        resultMsg = "NORMAL SERVICE",
                    ),
                    body = SpotBodyDto(
                        items = SpotItemsDto(
                            item = listOf(
                                SpotItemDto(
                                    spotNm = "폐건전지 수거함",
                                    addrBase = "서울특별시 영등포구 문래동",
                                    addrDtl = "주민센터 앞",
                                ),
                                SpotItemDto(
                                    spotNm = "재활용센터",
                                    addrBase = "서울특별시 구로구 구로동",
                                    addrDtl = "센터 입구",
                                ),
                            ),
                        ),
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

        fun createNoDataResponse(): SpotResponseDto {
            return SpotResponseDto(
                response = SpotResponseBodyDto(
                    header = SpotHeaderDto(
                        resultCode = "03",
                        resultMsg = "NODATA_ERROR",
                    ),
                    body = SpotBodyDto(
                        items = null,
                    ),
                ),
            )
        }
    }
}
