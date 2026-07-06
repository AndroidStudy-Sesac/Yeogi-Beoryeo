package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSpotRepositoryImplTest {

    @Test
    fun `검색어 기반 조회 결과를 CollectionSpot으로 변환하고 좌표를 설정한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchByKeyword(
            keyword = "문래동",
        )

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals("문래동", apiService.requestedAddr)
        assertEquals(2, result.size)

        val firstSpot = result.first()

        assertEquals("폐건전지 수거함", firstSpot.name)
        assertEquals("서울특별시 영등포구 문래동", firstSpot.address)
        assertEquals("주민센터 앞", firstSpot.detailLocation)
        assertEquals(CollectionSpotType.BATTERY_BIN, firstSpot.type)
        assertNotNull(firstSpot.coordinate)
        assertEquals(37.5, firstSpot.coordinate?.latitude)
        assertEquals(126.9, firstSpot.coordinate?.longitude)
    }

    @Test
    fun `검색어 기반 조회 시 선택된 타입에 해당하는 장소만 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val repository = createRepository(apiService)

        val result = repository.searchByKeyword(
            keyword = "문래동",
            types = setOf(CollectionSpotType.RECYCLING_CENTER),
        )

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals(1, result.size)
        assertEquals("재활용센터", result.first().name)
        assertEquals(CollectionSpotType.RECYCLING_CENTER, result.first().type)
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

        val result = repository.searchByKeyword(
            keyword = "문래동",
        )

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

        val result = repository.searchByLocation(
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

        val result = repository.searchByKeyword(
            keyword = "약수동",
        )

        assertEquals(TEST_SERVICE_KEY, apiService.requestedServiceKey)
        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `같은 주소는 한 번만 geocoding하고 기존 순서를 유지한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createDuplicateAddressResponse(),
        )
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 영등포구 문래동" to Coordinate(
                    latitude = 37.5,
                    longitude = 126.9,
                ),
                "서울특별시 구로구 구로동" to Coordinate(
                    latitude = 37.4,
                    longitude = 126.8,
                ),
            ),
        )
        val repository = createRepository(
            apiService = apiService,
            spotGeocoder = spotGeocoder,
        )

        val result = repository.searchByKeyword(
            keyword = "문래동",
        )

        assertEquals(
            listOf(
                "폐건전지 수거함",
                "폐건전지 수거함 2",
                "재활용센터",
            ),
            result.map { it.name },
        )
        assertEquals(
            listOf(
                "서울특별시 영등포구 문래동",
                "서울특별시 구로구 구로동",
            ),
            spotGeocoder.requestedAddresses,
        )
        assertEquals(result[0].coordinate, result[1].coordinate)
        assertEquals(37.4, result[2].coordinate?.latitude)
    }

    @Test
    fun `괄호가 포함된 도로명 주소는 괄호를 제거한 주소로 geocoding한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createParenthesizedRoadAddressResponse(),
        )
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 성동구 행당로 35" to Coordinate(
                    latitude = 37.5570,
                    longitude = 127.0332,
                ),
            ),
        )
        val repository = createRepository(
            apiService = apiService,
            spotGeocoder = spotGeocoder,
        )

        val result = repository.searchByKeyword(
            keyword = "금호동",
        )

        assertEquals(
            listOf("서울특별시 성동구 행당로 35"),
            spotGeocoder.requestedAddresses,
        )
        assertEquals(37.5570, result.first().coordinate?.latitude)
        assertEquals(127.0332, result.first().coordinate?.longitude)
    }

    @Test
    fun `공백 주소는 geocoding하지 않고 좌표 없이 유지한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createBlankAddressResponse(),
        )
        val spotGeocoder = FakeSpotGeocoder()
        val repository = createRepository(
            apiService = apiService,
            spotGeocoder = spotGeocoder,
        )

        val result = repository.searchByKeyword(
            keyword = "문래동",
        )

        assertEquals(1, result.size)
        assertNull(result.first().coordinate)
        assertEquals(emptyList<String>(), spotGeocoder.requestedAddresses)
    }

    @Test
    fun `일부 geocoding 실패가 전체 검색 결과를 실패시키지 않는다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 구로구 구로동" to Coordinate(
                    latitude = 37.4,
                    longitude = 126.8,
                ),
            ),
            failureAddresses = setOf("서울특별시 영등포구 문래동"),
        )
        val repository = createRepository(
            apiService = apiService,
            spotGeocoder = spotGeocoder,
        )

        val result = repository.searchByKeyword(
            keyword = "문래동",
        )

        assertEquals(2, result.size)
        assertEquals("폐건전지 수거함", result[0].name)
        assertNull(result[0].coordinate)
        assertEquals("재활용센터", result[1].name)
        assertEquals(37.4, result[1].coordinate?.latitude)
    }

    private fun createRepository(
        apiService: FakeSpotApiService,
        spotGeocoder: SpotGeocoder = FakeSpotGeocoder(),
    ): CollectionSpotRepositoryImpl {
        return CollectionSpotRepositoryImpl(
            remoteDataSource = SpotRemoteDataSource(apiService),
            spotMapper = SpotMapper(),
            spotGeocoder = spotGeocoder,
            publicDataKeyProvider = FakePublicDataKeyProvider(),
        )
    }

    private class FakePublicDataKeyProvider : AppKeyProvider {
        override val publicDataServiceKey: String = TEST_SERVICE_KEY
        override val naverClientId: String = "naver-client-id"
    }

    private class FakeSpotGeocoder(
        private val coordinatesByAddress: Map<String, Coordinate> = emptyMap(),
        private val failureAddresses: Set<String> = emptySet(),
    ) : SpotGeocoder {

        val requestedAddresses = mutableListOf<String>()

        override suspend fun geocode(address: String): Coordinate? {
            requestedAddresses += address

            if (address in failureAddresses) {
                error("geocoding failed")
            }

            return coordinatesByAddress[address] ?: Coordinate(
                latitude = 37.5,
                longitude = 126.9,
            )
        }
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

        fun createDuplicateAddressResponse(): SpotResponseDto {
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
                                    spotNm = "폐건전지 수거함 2",
                                    addrBase = " 서울특별시 영등포구 문래동 ",
                                    addrDtl = "학교 앞",
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

        fun createParenthesizedRoadAddressResponse(): SpotResponseDto {
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
                                    spotNm = "종량제봉투 판매소",
                                    addrBase = "서울특별시 성동구 행당로 35 (금호동1가, 금북빌딩)",
                                    addrDtl = "103호, 105호",
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        fun createBlankAddressResponse(): SpotResponseDto {
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
                                    spotNm = "주소 없는 수거함",
                                    addrBase = " ",
                                    addrDtl = "상세 위치",
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
