package com.team.yeogibeoryeo.data.spot.remote.datasource

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.team.yeogibeoryeo.data.spot.remote.SpotApiService
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotHeaderDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemsDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseBodyDto
import com.team.yeogibeoryeo.data.spot.remote.dto.SpotResponseDto
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalHttpStatusClass
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException

class SpotRemoteDataSourceTest {

    @Test
    fun `검색어 기반 조회 시 keyword가 addr 파라미터로 전달된다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

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
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `현재 위치 기반 조회 시 공백 addr와 좌표 반경이 전달된다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
        )
        val dataSource = createDataSource(apiService)

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
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals(1, result.size)
        assertEquals("폐건전지 수거함", result.first().spotNm)
        assertEquals("서울특별시 영등포구 문래동", result.first().addrBase)
        assertEquals("주민센터 앞", result.first().addrDtl)
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `NODATA_ERROR 응답이면 빈 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNoDataResponse(),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "약수동",
        )

        assertEquals(emptyList<SpotItemDto>(), result)
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `정상과 데이터 없음 외 응답 코드는 예외를 전달한다`() {
        val apiService = FakeSpotApiService(
            response = createErrorResponse(),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.searchByKeyword(
                    serviceKey = TEST_SERVICE_KEY,
                    keyword = "문래동",
                )
            }
        }

        assertEquals("수거 장소 API 오류(30): SERVICE_KEY_IS_NOT_REGISTERED_ERROR", exception.message)
        assertEquals(
            listOf(RecordedError(exception, REMOTE_HTTP_CONTEXT)),
            reporter.errors,
        )
    }

    @Test
    fun `검색어 첫 페이지의 실제 실패 원인을 구분해 요청당 한 번 기록한다`() {
        val cases = listOf(
            IOException("private url") to REMOTE_NETWORK_CONTEXT,
            SocketTimeoutException("private url") to REMOTE_TIMEOUT_CONTEXT,
            SerializationException("private response") to RESPONSE_PARSING_CONTEXT,
            HttpException(
                Response.error<SpotResponseDto>(503, "private response".toResponseBody()),
            ) to REMOTE_SERVER_HTTP_CONTEXT,
        )

        cases.forEach { (failure, expectedContext) ->
            val reporter = RecordingNonFatalErrorReporter()
            val dataSource = createDataSource(
                apiService = FakeSpotApiService(
                    response = createNormalResponse(),
                    failuresByPage = mapOf(1 to failure),
                ),
                reporter = reporter,
            )

            val thrown = runCatching {
                runBlocking {
                    dataSource.searchByKeyword(
                        serviceKey = TEST_SERVICE_KEY,
                        keyword = "문래동",
                    )
                }
            }.exceptionOrNull()

            assertSame(failure, thrown)
            assertEquals(
                listOf(RecordedError(failure, expectedContext)),
                reporter.errors,
            )
        }
    }

    @Test
    fun `Retrofit이 204를 반환한 첫 페이지는 파싱 실패로 한 번 기록하고 예외를 전달한다`() {
        val apiService = createRetrofitApiService { HttpFixtureResponse(code = 204) }
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val thrown = assertThrows(SerializationException::class.java) {
            runBlocking {
                dataSource.searchByKeyword(
                    serviceKey = TEST_SERVICE_KEY,
                    keyword = "문래동",
                )
            }
        }

        assertEquals("수거 장소 API 응답 본문이 없습니다 (HTTP 204)", thrown.message)
        assertEquals(
            listOf(RecordedError(thrown, RESPONSE_PARSING_CONTEXT)),
            reporter.errors,
        )
    }

    @Test
    fun `Retrofit이 205를 반환한 추가 페이지는 파싱 실패로 한 번 기록하고 부분 결과를 반환한다`() = runBlocking {
        val requestedPageNos = mutableListOf<Int>()
        val apiService = createRetrofitApiService { pageNo ->
            requestedPageNos += pageNo
            when (pageNo) {
                1 -> HttpFixtureResponse(code = 200, body = FIRST_PAGE_RESPONSE_JSON)
                2 -> HttpFixtureResponse(code = 205)
                else -> error("예상하지 않은 페이지 요청: $pageNo")
            }
        }
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByKeywordResult(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
            numOfRows = 1,
        )

        assertEquals(listOf(1, 2), requestedPageNos)
        assertEquals(listOf("1페이지 수거함"), result.items.map { item -> item.spotNm })
        assertTrue(result.isPartial)
        val recordedError = reporter.errors.single()
        assertTrue(recordedError.error is SerializationException)
        assertEquals("수거 장소 API 응답 본문이 없습니다 (HTTP 205)", recordedError.error.message)
        assertEquals(RESPONSE_PARTIAL_PARSING_CONTEXT, recordedError.context)
    }

    @Test
    fun `items가 null이면 빈 리스트를 반환한다`() = runBlocking {
        val apiService = FakeSpotApiService(
            response = createNormalResponseWithNullItems(),
        )
        val dataSource = createDataSource(apiService)

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
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByKeyword(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
        )

        assertEquals(listOf(1), apiService.requestedPageNos)
        assertEquals(1, result.size)
        assertTrue(reporter.errors.isEmpty())
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
        val dataSource = createDataSource(apiService)

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
        val dataSource = createDataSource(apiService)

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
        val dataSource = createDataSource(apiService)

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
        val failure = IOException("private url")
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                ),
            ),
            failuresByPage = mapOf(2 to failure),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByKeywordResult(
            serviceKey = TEST_SERVICE_KEY,
            keyword = "문래동",
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(listOf("1페이지 수거함"), result.items.map { item -> item.spotNm })
        assertEquals(true, result.isPartial)
        assertEquals(
            listOf(RecordedError(failure, REMOTE_PARTIAL_NETWORK_CONTEXT)),
            reporter.errors,
        )
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
        val dataSource = createDataSource(apiService)

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
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

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
        assertTrue(reporter.errors.isEmpty())
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
        val dataSource = createDataSource(apiService)

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
        val dataSource = createDataSource(apiService)

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
        val failure = SocketTimeoutException("private coordinates")
        val apiService = FakeSpotApiService(
            response = createResponse(
                pageNo = 1,
                numOfRows = 2,
                totalCount = 3,
                items = listOf(
                    spotItem("1페이지 수거함", "서울특별시 영등포구 문래동", "주민센터 앞"),
                ),
            ),
            failuresByPage = mapOf(2 to failure),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val result = dataSource.searchByLocation(
            serviceKey = TEST_SERVICE_KEY,
            latitude = 37.5182396969791,
            longitude = 126.895880210522,
            radiusMeter = 500,
            numOfRows = 2,
        )

        assertEquals(listOf(1, 2), apiService.requestedPageNos)
        assertEquals(listOf("1페이지 수거함"), result.map { item -> item.spotNm })
        assertEquals(
            listOf(RecordedError(failure, REMOTE_PARTIAL_TIMEOUT_CONTEXT)),
            reporter.errors,
        )
    }

    @Test
    fun `좌표 기반 첫 페이지 실패 시 예외를 전달한다`() {
        val failure = IOException("private coordinates")
        val apiService = FakeSpotApiService(
            response = createNormalResponse(),
            failuresByPage = mapOf(1 to failure),
        )
        val reporter = RecordingNonFatalErrorReporter()
        val dataSource = createDataSource(apiService, reporter)

        val thrown = assertThrows(IOException::class.java) {
            runBlocking {
                dataSource.searchByLocation(
                    serviceKey = TEST_SERVICE_KEY,
                    latitude = 37.5182396969791,
                    longitude = 126.895880210522,
                    radiusMeter = 500,
                )
            }
        }

        assertSame(failure, thrown)
        assertEquals(
            listOf(RecordedError(failure, REMOTE_NETWORK_CONTEXT)),
            reporter.errors,
        )
    }

    @Test
    fun `caller cancellation과 fatal Error는 기록하지 않고 원래 오류를 전파한다`() {
        listOf(CancellationException("cancelled"), LinkageError("fatal")).forEach { failure ->
            val reporter = RecordingNonFatalErrorReporter()
            val dataSource = createDataSource(
                apiService = FakeSpotApiService(
                    response = createNormalResponse(),
                    failuresByPage = mapOf(1 to failure),
                ),
                reporter = reporter,
            )

            val thrown = runCatching {
                runBlocking {
                    dataSource.searchByLocation(
                        serviceKey = TEST_SERVICE_KEY,
                        latitude = 37.5182396969791,
                        longitude = 126.895880210522,
                        radiusMeter = 500,
                    )
                }
            }.exceptionOrNull()

            assertSame(failure, thrown)
            assertTrue(reporter.errors.isEmpty())
        }
    }

    private class FakeSpotApiService(
        private val response: SpotResponseDto,
        private val responsesByPage: Map<Int, SpotResponseDto> = emptyMap(),
        private val failurePages: Set<Int> = emptySet(),
        private val failuresByPage: Map<Int, Throwable> = emptyMap(),
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
        ): Response<SpotResponseDto> {
            requestedServiceKey = serviceKey
            requestedPageNo = pageNo
            requestedPageNos += pageNo
            requestedNumOfRows = numOfRows
            requestedAddr = addr
            requestedLatitude = latitude
            requestedLongitude = longitude
            requestedRadius = radius
            requestedType = type

            failuresByPage[pageNo]?.let { failure -> throw failure }
            if (pageNo in failurePages) {
                error("page failed")
            }

            return Response.success(responsesByPage[pageNo] ?: response)
        }
    }

    private fun createDataSource(
        apiService: SpotApiService,
        reporter: NonFatalErrorReporter = RecordingNonFatalErrorReporter(),
    ): SpotRemoteDataSource = SpotRemoteDataSource(
        apiService = apiService,
        nonFatalErrorReporter = reporter,
    )

    private fun createRetrofitApiService(
        responseForPage: (Int) -> HttpFixtureResponse,
    ): SpotApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val pageNo = checkNotNull(request.url.queryParameter("pageNo")).toInt()
                val fixture = responseForPage(pageNo)
                okhttp3.Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(fixture.code)
                    .message(fixture.code.toString())
                    .body(fixture.body.toResponseBody(JSON_MEDIA_TYPE))
                    .build()
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(Json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(SpotApiService::class.java)
    }

    private data class HttpFixtureResponse(
        val code: Int,
        val body: String = "",
    )

    private class RecordingNonFatalErrorReporter : NonFatalErrorReporter {
        val errors = mutableListOf<RecordedError>()

        override fun report(error: Throwable, context: NonFatalErrorContext) {
            errors += RecordedError(error, context)
        }
    }

    private data class RecordedError(
        val error: Throwable,
        val context: NonFatalErrorContext,
    )

    private companion object {
        const val TEST_SERVICE_KEY = "test-service-key"
        val REMOTE_NETWORK_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.NETWORK,
        )
        val REMOTE_TIMEOUT_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.TIMEOUT,
        )
        val REMOTE_HTTP_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.HTTP,
        )
        val REMOTE_SERVER_HTTP_CONTEXT = REMOTE_HTTP_CONTEXT.copy(
            httpStatusClass = NonFatalHttpStatusClass.SERVER_ERROR,
        )
        val RESPONSE_PARSING_CONTEXT = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.RESPONSE_PARSING,
            category = NonFatalCategory.PARSING,
        )
        val RESPONSE_PARTIAL_PARSING_CONTEXT = RESPONSE_PARSING_CONTEXT.copy(
            isPartialResult = true,
        )
        val REMOTE_PARTIAL_NETWORK_CONTEXT = REMOTE_NETWORK_CONTEXT.copy(
            isPartialResult = true,
        )
        val REMOTE_PARTIAL_TIMEOUT_CONTEXT = REMOTE_TIMEOUT_CONTEXT.copy(
            isPartialResult = true,
        )
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
        val FIRST_PAGE_RESPONSE_JSON =
            """
            {
              "response": {
                "header": {
                  "resultCode": "00",
                  "resultMsg": "NORMAL SERVICE"
                },
                "body": {
                  "items": {
                    "item": [
                      {
                        "spotNm": "1페이지 수거함",
                        "addrBase": "서울특별시 영등포구 문래동",
                        "addrDtl": "주민센터 앞"
                      }
                    ]
                  },
                  "numOfRows": 1,
                  "pageNo": 1,
                  "totalCount": 2
                }
              }
            }
            """.trimIndent()

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
