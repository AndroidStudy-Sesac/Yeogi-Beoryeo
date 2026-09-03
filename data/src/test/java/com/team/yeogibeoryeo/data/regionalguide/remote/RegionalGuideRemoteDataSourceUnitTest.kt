package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideBodyDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemsDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideResponseDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideRootDto
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalHttpStatusClass
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RegionalGuideRemoteDataSourceUnitTest {

    private val reporter = RecordingNonFatalErrorReporter()

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
        val dataSource = createDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(listOf("1페이지"), result.getOrThrow().items.map { item -> item.managementZoneName })
        assertTrue(reporter.contexts.isEmpty())
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isSuccess)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(emptyList<RegionalGuideItemDto>(), result.getOrThrow().items)
        assertTrue(reporter.contexts.isEmpty())
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
        val dataSource = createDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME)
        val exception = result.exceptionOrNull() as RegionalGuideLookupException

        assertEquals(RegionalGuideFailureReason.API, exception.reason)
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.RESPONSE_PARSING,
                    category = NonFatalCategory.PARSING,
                    httpStatusClass = NonFatalHttpStatusClass.SUCCESS,
                ),
            ),
            reporter.contexts,
        )
    }

    @Test
    fun `서버 오류 응답은 상태 코드 범주와 함께 한 번 기록한다`() = runBlocking {
        val apiService = FakeRegionalGuideApiService(
            response = Response.error(503, byteArrayOf().toResponseBody()),
        )

        val result = createDataSource(apiService).fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isFailure)
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.HTTP,
                    httpStatusClass = NonFatalHttpStatusClass.SERVER_ERROR,
                ),
            ),
            reporter.contexts,
        )
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
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
        val dataSource = createDataSource(
            apiService = apiService,
            keyProvider = FakePublicDataKeyProvider,
        )

        val result = dataSource.fetchRegionalGuides(SIGUNGU_NAME).getOrThrow()

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.NETWORK, result.partialReason)
        assertEquals(listOf("1페이지"), result.items.map { item -> item.managementZoneName })
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.NETWORK,
                    isPartialResult = true,
                ),
            ),
            reporter.contexts,
        )
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
        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isFailure)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.NETWORK,
                ),
            ),
            reporter.contexts,
        )
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

        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)

        assertTrue(result.isFailure)
        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.TIMEOUT,
                ),
            ),
            reporter.contexts,
        )
    }

    @Test
    fun `호출자 시간 초과 취소는 결과로 변환하지 않고 전파한다`() = runTest {
        val apiService = FakeRegionalGuideApiService(
            response = regionalGuideResponse(
                pageNo = 1,
                numOfRows = 100,
                totalCount = 1,
                items = listOf(regionalGuideItem("첫 페이지")),
            ),
            delayByPage = mapOf(1 to 100L),
        )

        try {
            withTimeout(50L) {
                createDataSource(apiService, FakePublicDataKeyProvider)
                    .fetchRegionalGuides(SIGUNGU_NAME)
            }
            fail("호출자 시간 초과 취소가 전파되어야 합니다")
        } catch (_: TimeoutCancellationException) {
            assertEquals(listOf(1), apiService.requestedPageNos)
            assertTrue(reporter.contexts.isEmpty())
        }
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

        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(listOf(1, 2, 3, 4), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.TIMEOUT, result.partialReason)
        assertEquals(listOf("1페이지", "2페이지", "3페이지"), result.items.map { item -> item.managementZoneName })
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.TIMEOUT,
                    isPartialResult = true,
                ),
            ),
            reporter.contexts,
        )
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
        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(listOf(1, 2, 3, 4, 5), apiService.requestedPageNos)
        assertEquals(RegionalGuidePartialResultReason.PAGE_LIMIT, result.partialReason)
        assertEquals(5, result.items.size)
        assertTrue(reporter.contexts.isEmpty())
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
        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(RegionalGuidePartialResultReason.INCONSISTENT_RESPONSE, result.partialReason)
        assertEquals(listOf("첫 페이지"), result.items.map { item -> item.managementZoneName })
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.RESPONSE_PARSING,
                    category = NonFatalCategory.PARSING,
                    isPartialResult = true,
                ),
            ),
            reporter.contexts,
        )
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
        val result = createDataSource(apiService, FakePublicDataKeyProvider)
            .fetchRegionalGuides(SIGUNGU_NAME)
            .getOrThrow()

        assertEquals(RegionalGuidePartialResultReason.TIMEOUT, result.partialReason)
        assertEquals(listOf("첫 페이지"), result.items.map { item -> item.managementZoneName })
        assertEquals(
            listOf(
                regionalGuideErrorContext(
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.TIMEOUT,
                    isPartialResult = true,
                ),
            ),
            reporter.contexts,
        )
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
            createDataSource(apiService, FakePublicDataKeyProvider)
                .fetchRegionalGuides(SIGUNGU_NAME)
            fail("취소 예외가 전파되어야 합니다")
        } catch (_: CancellationException) {
            assertEquals(listOf(1, 2), apiService.requestedPageNos)
            assertTrue(reporter.contexts.isEmpty())
        }
    }

    private fun createDataSource(
        apiService: RegionalGuideApiService,
        keyProvider: AppKeyProvider = FakePublicDataKeyProvider,
    ): RegionalGuideRemoteDataSource = RegionalGuideRemoteDataSource(
        apiService = apiService,
        keyProvider = keyProvider,
        nonFatalErrorReporter = reporter,
    )

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

private class RecordingNonFatalErrorReporter : NonFatalErrorReporter {
    val contexts = mutableListOf<NonFatalErrorContext>()

    override fun report(error: Throwable, context: NonFatalErrorContext) {
        contexts += context
    }
}

private fun regionalGuideErrorContext(
    stage: NonFatalStage,
    category: NonFatalCategory,
    httpStatusClass: NonFatalHttpStatusClass = NonFatalHttpStatusClass.NOT_AVAILABLE,
    isPartialResult: Boolean = false,
): NonFatalErrorContext = NonFatalErrorContext(
    api = NonFatalApi.REGIONAL_GUIDE,
    stage = stage,
    category = category,
    httpStatusClass = httpStatusClass,
    isPartialResult = isPartialResult,
)
