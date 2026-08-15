package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideBodyDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemsDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideResponseDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideRootDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RegionalGuideRemoteDataSourceUnitTest {

    @Test
    fun `전체 건수가 페이지 크기 이하면 첫 페이지만 조회한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 1,
                items = listOf(regionalGuideItem("1페이지")),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(listOf("1페이지"), result.getOrThrow().items.map { item -> item.managementZoneName })
    }

    @Test
    fun `전체 건수가 페이지 크기를 초과하면 필요한 추가 페이지를 모두 조회해 병합한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    regionalGuideItem("1페이지-1"),
                    regionalGuideItem("1페이지-2"),
                ),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = listOf(regionalGuideItem("2페이지-1")),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지-1", "1페이지-2", "2페이지-1"),
            result.getOrThrow().items.map { item -> item.managementZoneName },
        )
    }

    @Test
    fun `전체 건수가 두 페이지를 초과하면 마지막 페이지까지 조회한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 201,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 100,
                    totalCount = 201,
                    items = listOf(regionalGuideItem("2페이지")),
                ),
                3 to regionalGuideResponse(
                    pageNo = 3,
                    numOfRows = 100,
                    totalCount = 201,
                    items = listOf(regionalGuideItem("3페이지")),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2, 3), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지", "2페이지", "3페이지"),
            result.getOrThrow().items.map { item -> item.managementZoneName },
        )
    }

    @Test
    fun `응답 페이지 번호가 요청 페이지 번호와 달라도 요청 페이지 번호 기준으로 다음 페이지를 조회한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 3,
                numOfRows = 100,
                totalCount = 201,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 100,
                    totalCount = 201,
                    items = listOf(regionalGuideItem("2페이지")),
                ),
                3 to regionalGuideResponse(
                    pageNo = 3,
                    numOfRows = 100,
                    totalCount = 201,
                    items = listOf(regionalGuideItem("3페이지")),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2, 3), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지", "2페이지", "3페이지"),
            result.getOrThrow().items.map { item -> item.managementZoneName },
        )
    }

    @Test
    fun `응답 페이지 크기가 요청 페이지 크기보다 크면 요청 페이지 크기 기준으로 추가 페이지를 조회한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 150,
                totalCount = 150,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 150,
                    totalCount = 150,
                    items = listOf(regionalGuideItem("2페이지")),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(
            listOf("1페이지", "2페이지"),
            result.getOrThrow().items.map { item -> item.managementZoneName },
        )
    }

    @Test
    fun `빈 응답이면 빈 리스트를 반환하고 추가 페이지를 조회하지 않는다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 0,
                items = emptyList(),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(emptyList<RegionalGuideItemDto>(), result.getOrThrow().items)
    }

    @Test
    fun `성공 응답에 본문이 없으면 API 실패로 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = Response.success(
                RegionalGuideRootDto(
                    response = RegionalGuideResponseDto(body = null),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)
        val exception = result.exceptionOrNull() as RegionalGuideLookupException

        assertEquals(RegionalGuideFailureReason.API, exception.reason)
    }

    @Test
    fun `전체 건수가 없으면 첫 페이지만 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = null,
                items = listOf(regionalGuideItem("1페이지")),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(listOf("1페이지"), result.getOrThrow().items.map { item -> item.managementZoneName })
    }

    @Test
    fun `페이지 크기가 없으면 요청한 기본 페이지 크기 기준으로 추가 페이지를 조회한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = null,
                totalCount = 101,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = null,
                    totalCount = 101,
                    items = listOf(regionalGuideItem("2페이지")),
                ),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(listOf("1페이지", "2페이지"), result.getOrThrow().items.map { item -> item.managementZoneName })
    }

    @Test
    fun `페이지 크기가 비정상 값이면 첫 페이지만 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 0,
                totalCount = 101,
                items = listOf(regionalGuideItem("1페이지")),
            ),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(listOf("1페이지"), result.getOrThrow().items.map { item -> item.managementZoneName })
    }

    @Test
    fun `추가 페이지 조회 실패 시 네트워크 실패로 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            failurePages = setOf(2),
        )
        val dataSource = RegionalGuideRemoteDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME).getOrThrow()

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.NETWORK, result.partialReason)
        assertEquals(listOf("1페이지"), result.items.map { item -> item.managementZoneName })
    }

    @Test
    fun `첫 페이지 실패 시 실패 결과를 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 1,
                items = emptyList(),
            ),
            failurePages = setOf(1),
        )
        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isFailure)
        assertEquals(listOf(1), apiService.requestedPageNos)
    }

    @Test
    fun `첫 페이지 시간이 초과되면 실패 결과를 반환한다`() = runTest {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 1,
                items = listOf(regionalGuideItem("첫 페이지")),
            ),
            delayByPage = mapOf(1 to 2_100L),
        )

        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isFailure)
        assertEquals(listOf(1), apiService.requestedPageNos)
    }

    @Test
    fun `페이지별 시간은 지키지만 전체 조회 시간이 초과되면 부분 결과를 반환한다`() = runTest {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 401,
                items = listOf(regionalGuideItem("1페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 100,
                    totalCount = 401,
                    items = listOf(regionalGuideItem("2페이지")),
                ),
                3 to regionalGuideResponse(
                    pageNo = 3,
                    numOfRows = 100,
                    totalCount = 401,
                    items = listOf(regionalGuideItem("3페이지")),
                ),
                4 to regionalGuideResponse(
                    pageNo = 4,
                    numOfRows = 100,
                    totalCount = 401,
                    items = listOf(regionalGuideItem("4페이지")),
                ),
            ),
            delayByPage = mapOf(
                1 to 1_500L,
                2 to 1_500L,
                3 to 1_500L,
                4 to 1_500L,
            ),
        )

        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(listOf(1, 2, 3, 4), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.TIMEOUT, result.partialReason)
        assertEquals(listOf("1페이지", "2페이지", "3페이지"), result.items.map { item -> item.managementZoneName })
    }

    @Test
    fun `전체 건수가 상한을 넘으면 최대 다섯 페이지만 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 501,
                items = listOf(regionalGuideItem("페이지")),
            ),
        )
        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(listOf(1, 2, 3, 4, 5), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.PAGE_LIMIT, result.partialReason)
        assertEquals(5, result.items.size)
    }

    @Test
    fun `수신 건수가 전체 건수보다 부족하고 빈 페이지면 부분 결과를 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(regionalGuideItem("첫 페이지")),
            ),
            responsesByPage = mapOf(
                2 to regionalGuideResponse(
                    pageNo = 2,
                    numOfRows = 2,
                    totalCount = 3,
                    items = emptyList(),
                ),
            ),
        )
        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE, result.partialReason)
        assertEquals(listOf("첫 페이지"), result.items.map { item -> item.managementZoneName })
    }

    @Test
    fun `후속 페이지 시간이 초과되면 앞 페이지를 부분 결과로 반환한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(regionalGuideItem("첫 페이지")),
            ),
            delayByPage = mapOf(2 to 2_100L),
        )
        val result = RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(RegionalGuidePartialResultReason.TIMEOUT, result.partialReason)
        assertEquals(listOf("첫 페이지"), result.items.map { item -> item.managementZoneName })
    }

    @Test
    fun `후속 페이지의 취소는 결과로 변환하지 않고 전파한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(regionalGuideItem("첫 페이지")),
            ),
            cancellationPages = setOf(2),
        )

        try {
            RegionalGuideRemoteDataSource(apiService, FakePublicDataKeyProvider)
                .fetchRegionalGuides(SIGUNGU_NAME)
            fail("취소 예외가 전파되어야 합니다")
        } catch (_: CancellationException) {
            assertEquals(listOf(1, 2), apiService.requestedPageNos)
        }
    }

    private class FakeRegionalGuideApiService(
        private val response: Response<RegionalGuideRootDto>,
        private val responsesByPage: Map<Int, Response<RegionalGuideRootDto>> = emptyMap(),
        private val failurePages: Set<Int> = emptySet(),
        private val delayByPage: Map<Int, Long> = emptyMap(),
        private val cancellationPages: Set<Int> = emptySet(),
    ) : RegionalGuideApiService {

        val requestedPageNos = mutableListOf<Int>()

        override suspend fun getRegionalGuides(
            serviceKey: String,
            pageNo: Int,
            numOfRows: Int,
            returnType: String,
            sigunguName: String,
        ): Response<RegionalGuideRootDto> {
            requestedPageNos += pageNo

            delayByPage[pageNo]?.let { delayMillis -> delay(delayMillis) }

            if (pageNo in cancellationPages) {
                throw CancellationException("page cancelled")
            }

            if (pageNo in failurePages) {
                throw IOException("page failed")
            }

            return responsesByPage[pageNo] ?: response
        }
    }

    private companion object {
        const val SIGUNGU_NAME = "수원시"

        val FakePublicDataKeyProvider = object : AppKeyProvider {
            override val publicDataServiceKey: String = "test-service-key"
            override val naverClientId: String = "naver-client-id"
        }

        fun regionalGuideResponse(
            pageNo: Int?,
            numOfRows: Int?,
            totalCount: Int?,
            items: List<RegionalGuideItemDto>,
        ): Response<RegionalGuideRootDto> {
            return Response.success(
                RegionalGuideRootDto(
                    response = RegionalGuideResponseDto(
                        body = RegionalGuideBodyDto(
                            pageNo = pageNo,
                            numOfRows = numOfRows,
                            totalCount = totalCount,
                            items = RegionalGuideItemsDto(item = items),
                        ),
                    ),
                ),
            )
        }

        fun regionalGuideItem(
            managementZoneName: String,
        ): RegionalGuideItemDto {
            return RegionalGuideItemDto(
                sidoName = "경기도",
                sigunguName = SIGUNGU_NAME,
                managementZoneName = managementZoneName,
                dongName = "정자동",
            )
        }
    }
}
