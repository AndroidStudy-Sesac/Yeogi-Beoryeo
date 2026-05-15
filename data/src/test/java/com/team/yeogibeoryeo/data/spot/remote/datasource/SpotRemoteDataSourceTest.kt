package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotHeaderDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemsDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private class FakeSpotApiService(
        private val response: SpotResponseDto,
    ) : SpotApiService {

        var requestedServiceKey: String? = null
        var requestedPageNo: Int? = null
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
            requestedNumOfRows = numOfRows
            requestedAddr = addr
            requestedLatitude = latitude
            requestedLongitude = longitude
            requestedRadius = radius
            requestedType = type

            return response
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
                            ),
                        ),
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
    }
}