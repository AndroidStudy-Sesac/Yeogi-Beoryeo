package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideFetchResult
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuidePartialResultReason
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class FakeRegionalGuideDataSource : RegionalGuideDataSource {
    var mockResult: Result<RegionalGuideFetchResult> = Result.success(RegionalGuideFetchResult(emptyList()))
    var delayMillis: Long = 0L
    var throwable: Throwable? = null
    var calledSigunguName: String? = null
    val calledSigunguNames = mutableListOf<String>()
    val fetchStartedSignals = mutableMapOf<String, CompletableDeferred<Unit>>()
    val responseGates = mutableMapOf<String, CompletableDeferred<Unit>>()
    var beforeResult: (() -> Unit)? = null

    override suspend fun fetchRegionalGuides(sigunguName: String): Result<RegionalGuideFetchResult> {
        calledSigunguName = sigunguName
        calledSigunguNames += sigunguName
        fetchStartedSignals[sigunguName]?.complete(Unit)
        responseGates[sigunguName]?.await()
        if (delayMillis > 0) delay(delayMillis)
        throwable?.let { throwable -> throw throwable }
        beforeResult?.invoke()
        return mockResult
    }
}

class RegionalDisposalGuideRepositoryImplTest {

    private lateinit var fakeDataSource: FakeRegionalGuideDataSource
    private lateinit var fetchScope: CoroutineScope
    private lateinit var repository: RegionalDisposalGuideRepositoryImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeRegionalGuideDataSource()
        fetchScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        repository = RegionalDisposalGuideRepositoryImpl(fakeDataSource, fetchScope)
    }

    @After
    fun tearDown() {
        fetchScope.cancel()
    }

    @Test
    fun `조회 키로 원격 데이터를 호출하고 후보 목록을 반환한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            RegionalGuideFetchResult(listOf(
                RegionalGuideItemDto(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    dongName = "서울시 중구"
                ),
                RegionalGuideItemDto(
                    sidoName = "대구광역시",
                    sigunguName = "중구",
                    dongName = "대봉2동"
                )
            ))
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guides = result.getOrThrow()

        assertEquals("중구", fakeDataSource.calledSigunguName)
        assertEquals(2, guides.size)
        assertEquals("서울특별시", guides[0].region.sido)
        assertEquals("대구광역시", guides[1].region.sido)
    }

    @Test
    fun `저장소는 후보를 선택하지 않고 전체 후보를 도메인으로 변환한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            RegionalGuideFetchResult(listOf(
                RegionalGuideItemDto(
                    sidoName = "인천광역시",
                    sigunguName = "중구",
                    dongName = "신흥동+율목동+영종동"
                ),
                RegionalGuideItemDto(
                    sidoName = "인천광역시",
                    sigunguName = "중구",
                    dongName = "신포동+연안동+도원동"
                )
            ))
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = "신흥동"
                ),
                sigunguQuery = "중구"
            )
        )

        val guides = result.getOrThrow()

        assertEquals(2, guides.size)
        assertEquals("신흥동", guides[0].region.eupmyeondong)
        assertEquals("신흥동+율목동+영종동", guides[0].targetRegionName)
        assertEquals("신포동+연안동+도원동", guides[1].targetRegionName)
    }

    @Test
    fun `저장소는 에이피아이 시도명 원본값을 정규화하지 않는다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            RegionalGuideFetchResult(listOf(
                RegionalGuideItemDto(
                    sidoName = "전남광주통합특별시",
                    sigunguName = "광산구",
                    dongName = "광산구 전체"
                )
            ))
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "광주광역시", sigungu = "광산구"),
                sigunguQuery = "광산구"
            )
        )

        val guide = result.getOrThrow().single()

        assertEquals("전남광주통합특별시", guide.region.sido)
        assertEquals("광산구", guide.region.sigungu)
        assertEquals("광산구 전체", guide.targetRegionName)
    }

    @Test
    fun `원격 데이터 소스 실패는 실패 결과로 유지한다`() = runBlocking {
        fakeDataSource.mockResult = Result.failure(IllegalStateException("network error"))

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `같은 조회 키 후보 목록은 원격 데이터를 다시 호출하지 않고 캐시를 사용한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            RegionalGuideFetchResult(listOf(
                RegionalGuideItemDto(
                    sidoName = "경상북도",
                    sigunguName = "김천시",
                    dongName = "동지역"
                )
            ))
        )
        val query = RegionalGuideQuery(
            displayRegion = Region(sido = "경상북도", sigungu = "김천시"),
            sigunguQuery = "김천시"
        )

        val firstResult = repository.getRegionalDisposalGuideCandidates(query)
        val secondResult = repository.getRegionalDisposalGuideCandidates(query)

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        assertEquals(listOf("김천시"), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `부분 결과는 캐시하지 않아 다음 조회에서 원격 데이터를 다시 호출한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            RegionalGuideFetchResult(
                items = emptyList(),
                partialReason = RegionalGuidePartialResultReason.NETWORK,
            ),
        )
        val query = regionalGuideQuery(sigunguQuery = "김천시")

        repository.getRegionalDisposalGuideCandidates(query)
        repository.getRegionalDisposalGuideCandidates(query)

        assertEquals(listOf("김천시", "김천시"), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `동시에 같은 조회 키를 요청해도 원격 데이터는 한 번만 호출한다`() = runBlocking {
        fakeDataSource.delayMillis = 50
        fakeDataSource.mockResult = Result.success(RegionalGuideFetchResult(emptyList()))
        val query = regionalGuideQuery(sigunguQuery = "김천시")

        val results = coroutineScope {
            listOf(
                async { repository.getRegionalDisposalGuideCandidates(query) },
                async { repository.getRegionalDisposalGuideCandidates(query) },
            ).map { deferred -> deferred.await() }
        }

        assertTrue(results.all { result -> result.isSuccess })
        assertEquals(listOf("김천시"), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `동일한 실패를 공유하는 여러 요청은 원격 실패를 한 번만 기록한다`() = runBlocking {
        val reporter = RepositoryRecordingNonFatalErrorReporter()
        val failure = IOException()
        val sigunguQuery = "김천시"
        fakeDataSource.fetchStartedSignals[sigunguQuery] = CompletableDeferred()
        fakeDataSource.responseGates[sigunguQuery] = CompletableDeferred()
        fakeDataSource.mockResult = Result.failure(failure)
        fakeDataSource.beforeResult = {
            reporter.report(
                error = failure,
                context = NonFatalErrorContext(
                    api = NonFatalApi.REGIONAL_GUIDE,
                    stage = NonFatalStage.REMOTE_REQUEST,
                    category = NonFatalCategory.NETWORK,
                ),
            )
        }
        val query = regionalGuideQuery(sigunguQuery)

        val results = coroutineScope {
            val firstRequest = async(start = UNDISPATCHED) {
                repository.getRegionalDisposalGuideCandidates(query)
            }
            fakeDataSource.fetchStartedSignals.getValue(sigunguQuery).await()
            val waitingRequest = async(start = UNDISPATCHED) {
                repository.getRegionalDisposalGuideCandidates(query)
            }
            fakeDataSource.responseGates.getValue(sigunguQuery).complete(Unit)

            listOf(firstRequest.await(), waitingRequest.await())
        }

        assertTrue(results.all { result -> result.isFailure })
        assertEquals(listOf(sigunguQuery), fakeDataSource.calledSigunguNames)
        assertEquals(1, reporter.contexts.size)
    }

    @Test
    fun `서로 다른 조회 키는 진행 중인 요청과 관계없이 원격 데이터를 동시에 호출한다`() = runBlocking {
        val firstQuery = "김천시"
        val secondQuery = "구미시"
        fakeDataSource.fetchStartedSignals[firstQuery] = CompletableDeferred()
        fakeDataSource.fetchStartedSignals[secondQuery] = CompletableDeferred()
        fakeDataSource.responseGates[firstQuery] = CompletableDeferred()
        fakeDataSource.responseGates[secondQuery] = CompletableDeferred()

        coroutineScope {
            val firstRequest = async {
                repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(firstQuery))
            }
            fakeDataSource.fetchStartedSignals.getValue(firstQuery).await()

            val secondRequest = async {
                repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(secondQuery))
            }
            withTimeout(1_000) {
                fakeDataSource.fetchStartedSignals.getValue(secondQuery).await()
            }

            fakeDataSource.responseGates.getValue(firstQuery).complete(Unit)
            fakeDataSource.responseGates.getValue(secondQuery).complete(Unit)

            assertTrue(firstRequest.await().isSuccess)
            assertTrue(secondRequest.await().isSuccess)
        }

        assertEquals(listOf(firstQuery, secondQuery), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `실패한 조회 키는 진행 중 요청에서 제거해 다음 요청을 새로 시작한다`() = runBlocking {
        val query = regionalGuideQuery(sigunguQuery = "김천시")
        fakeDataSource.mockResult = Result.failure(IllegalStateException("network error"))

        assertTrue(repository.getRegionalDisposalGuideCandidates(query).isFailure)

        fakeDataSource.mockResult = Result.success(RegionalGuideFetchResult(emptyList()))

        assertTrue(repository.getRegionalDisposalGuideCandidates(query).isSuccess)
        assertEquals(listOf("김천시", "김천시"), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `첫 조회가 취소돼도 같은 조회 키를 기다리던 요청은 성공한다`() = runBlocking {
        val sigunguQuery = "김천시"
        fakeDataSource.fetchStartedSignals[sigunguQuery] = CompletableDeferred()
        fakeDataSource.responseGates[sigunguQuery] = CompletableDeferred()

        val firstRequest = launch(start = UNDISPATCHED) {
            repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery))
        }
        fakeDataSource.fetchStartedSignals.getValue(sigunguQuery).await()

        val waitingRequest = async(start = UNDISPATCHED) {
            repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery))
        }
        firstRequest.cancelAndJoin()
        fakeDataSource.responseGates.getValue(sigunguQuery).complete(Unit)

        assertTrue(waitingRequest.await().isSuccess)
        assertEquals(listOf(sigunguQuery), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `취소된 원격 조회 키는 진행 중 요청에서 제거해 다음 요청을 새로 시작한다`() = runBlocking {
        val sigunguQuery = "김천시"
        fakeDataSource.throwable = CancellationException()

        val cancellation = runCatching {
            repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery))
        }.exceptionOrNull()

        fakeDataSource.throwable = null
        fakeDataSource.mockResult = Result.success(RegionalGuideFetchResult(emptyList()))

        assertTrue(cancellation is CancellationException)
        assertTrue(repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery)).isSuccess)
        assertEquals(listOf(sigunguQuery, sigunguQuery), fakeDataSource.calledSigunguNames)
    }

    @Test
    fun `다른 조회 키를 요청하면 최근 캐시를 교체한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(RegionalGuideFetchResult(emptyList()))

        repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery = "김천시"))
        repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery = "구미시"))
        repository.getRegionalDisposalGuideCandidates(regionalGuideQuery(sigunguQuery = "김천시"))

        assertEquals(
            listOf("김천시", "구미시", "김천시"),
            fakeDataSource.calledSigunguNames
        )
    }

    private fun regionalGuideQuery(sigunguQuery: String): RegionalGuideQuery =
        RegionalGuideQuery(
            displayRegion = Region(sido = "경상북도", sigungu = sigunguQuery),
            sigunguQuery = sigunguQuery,
        )
}

private class RepositoryRecordingNonFatalErrorReporter : NonFatalErrorReporter {
    val contexts = mutableListOf<NonFatalErrorContext>()

    override fun report(error: Throwable, context: NonFatalErrorContext) {
        contexts += context
    }
}
