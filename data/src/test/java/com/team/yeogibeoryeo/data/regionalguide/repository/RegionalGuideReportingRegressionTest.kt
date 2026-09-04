package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideApiService
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideRemoteDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideBodyDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemsDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideResponseDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideRootDto
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideReportingRegressionTest {
    private val query = RegionalGuideQuery(
        displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
        sigunguQuery = "중구",
    )

    @Test
    fun `공유 요청 실패는 한 번 기록하고 정리되어 다음 성공 결과를 캐시한다`() = runTest {
        val fixture = ReportingFixture(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        fixture.api.respond = {
            gate.await()
            throw IOException()
        }
        val first = async { fixture.repository.getRegionalDisposalGuideCandidates(query) }
        val second = async { fixture.repository.getRegionalDisposalGuideCandidates(query) }
        runCurrent()
        assertEquals(listOf(1), fixture.api.pages)
        gate.complete(Unit)

        listOf(first.await(), second.await()).forEach { result ->
            assertEquals(
                RegionalGuideFailureReason.NETWORK,
                (result.exceptionOrNull() as RegionalGuideLookupException).reason,
            )
        }
        assertEquals(listOf(networkContext(isPartial = false)), fixture.contexts)

        fixture.api.respond = { response("복구 결과") }
        val recovered = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        val cached = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        assertEquals(listOf("복구 결과"), recovered.map { it.managementZoneName })
        assertEquals(recovered, cached)
        assertEquals(listOf(1, 1), fixture.api.pages)
        assertEquals(1, fixture.contexts.size)
    }

    @Test
    fun `후속 페이지 실패 기록 후 부분 결과는 반환하되 캐시하지 않고 다음 조회를 재실행한다`() = runTest {
        val fixture = ReportingFixture(backgroundScope)
        fixture.api.respond = { page ->
            if (page == 1) response("부분 결과", totalCount = 2) else throw IOException()
        }

        val partial = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        assertEquals(listOf("부분 결과"), partial.map { it.managementZoneName })
        assertEquals(listOf(networkContext(isPartial = true)), fixture.contexts)

        fixture.api.respond = { response("완전한 결과") }
        val complete = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        val cached = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        assertEquals(listOf("완전한 결과"), complete.map { it.managementZoneName })
        assertEquals(complete, cached)
        assertEquals(listOf(1, 2, 1), fixture.api.pages)
        assertEquals(1, fixture.contexts.size)
    }

    @Test
    fun `호출자 취소는 기록하지 않고 독립 원격 요청의 성공 결과를 캐시한다`() = runTest {
        val fixture = ReportingFixture(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        fixture.api.respond = {
            gate.await()
            response("완료 결과")
        }
        val caller = async { fixture.repository.getRegionalDisposalGuideCandidates(query) }
        runCurrent()
        caller.cancelAndJoin()
        assertTrue(caller.isCancelled)
        gate.complete(Unit)
        runCurrent()

        val cached = fixture.repository.getRegionalDisposalGuideCandidates(query).getOrThrow()
        assertEquals(listOf("완료 결과"), cached.map { it.managementZoneName })
        assertEquals(listOf(1), fixture.api.pages)
        assertTrue(fixture.contexts.isEmpty())
    }
}

private class ReportingFixture(scope: CoroutineScope) {
    val api = ControlledRegionalGuideApi()
    val contexts = mutableListOf<NonFatalErrorContext>()
    val repository = RegionalDisposalGuideRepositoryImpl(
        remoteDataSource = RegionalGuideRemoteDataSource(
            apiService = api,
            keyProvider = object : AppKeyProvider {
                override val publicDataServiceKey = "test-key"
                override val naverClientId = "test-client"
            },
            nonFatalErrorReporter = object : NonFatalErrorReporter {
                override fun report(error: Throwable, context: NonFatalErrorContext) {
                    contexts += context
                }
            },
        ),
        fetchScope = scope,
    )
}

private class ControlledRegionalGuideApi : RegionalGuideApiService {
    val pages = mutableListOf<Int>()
    var respond: suspend (Int) -> Response<RegionalGuideRootDto> = { response("기본 결과") }

    override suspend fun getRegionalGuides(
        serviceKey: String,
        pageNo: Int,
        numOfRows: Int,
        returnType: String,
        sigunguName: String,
    ): Response<RegionalGuideRootDto> {
        pages += pageNo
        return respond(pageNo)
    }
}

private fun response(zoneName: String, totalCount: Int = 1): Response<RegionalGuideRootDto> =
    Response.success(
        RegionalGuideRootDto(
            response = RegionalGuideResponseDto(
                body = RegionalGuideBodyDto(
                    pageNo = 1,
                    numOfRows = 1,
                    totalCount = totalCount,
                    items = RegionalGuideItemsDto(
                        item = listOf(
                            RegionalGuideItemDto(
                                sidoName = "서울특별시",
                                sigunguName = "중구",
                                managementZoneName = zoneName,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

private fun networkContext(isPartial: Boolean) = NonFatalErrorContext(
    api = NonFatalApi.REGIONAL_GUIDE,
    stage = NonFatalStage.REMOTE_REQUEST,
    category = NonFatalCategory.NETWORK,
    isPartialResult = isPartial,
)
