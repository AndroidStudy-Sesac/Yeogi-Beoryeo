package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import com.team.yeogibeoryeo.presentation.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideErrorViewModelTest {
    @get:Rule
    val mainDispatcherRule = RegionalGuideMainDispatcherRule()

    @Test
    fun `네트워크 실패는 기존 네트워크 오류 안내를 유지한다`() = runTest {
        assertErrorMessage(
            reason = RegionalGuideFailureReason.NETWORK,
            errorType = RegionalGuideErrorType.NETWORK,
            messageResId = R.string.regional_guide_error_network_message,
        )
    }

    @Test
    fun `서버 응답 실패는 기존 서버 오류 안내를 유지한다`() = runTest {
        assertErrorMessage(
            reason = RegionalGuideFailureReason.API,
            errorType = RegionalGuideErrorType.API,
            messageResId = R.string.regional_guide_error_api_message,
        )
    }

    @Test
    fun `알 수 없는 실패는 기존 일반 오류 안내를 유지한다`() = runTest {
        assertErrorMessage(
            reason = RegionalGuideFailureReason.UNKNOWN,
            errorType = RegionalGuideErrorType.UNKNOWN,
            messageResId = R.string.regional_guide_error_unknown_message,
        )
    }

    @Test
    fun `오류 안내 후 재시도 성공은 같은 조회 조건의 상세 화면으로 복구한다`() = runTest {
        val repository = RecoverableRegionalGuideRepository()
        val viewModel = createErrorViewModel(repository)
        advanceUntilIdle()

        viewModel.loadByAddress("서울특별시 중구")
        advanceUntilIdle()
        assertEquals(
            RegionalGuideErrorType.NETWORK,
            (viewModel.uiState.value as RegionalGuideUiState.Error).errorType,
        )

        repository.result = Result.success(listOf(sampleGuide("서울특별시", "중구", "서울시 중구")))
        viewModel.retryLastRequest()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Success
        assertEquals("서울특별시 중구", state.guide.regionName)
        assertEquals(2, repository.queries.size)
        assertEquals(repository.queries.first(), repository.queries.last())
    }

    private fun TestScope.assertErrorMessage(
        reason: RegionalGuideFailureReason,
        errorType: RegionalGuideErrorType,
        messageResId: Int,
    ) {
        val repository = FakeRegionalDisposalGuideRepository(
            failure = RegionalGuideLookupException(reason),
        )
        val viewModel = createErrorViewModel(repository)
        advanceUntilIdle()
        viewModel.loadByAddress("서울특별시 중구")
        advanceUntilIdle()

        val state = viewModel.uiState.value as RegionalGuideUiState.Error
        assertEquals(errorType, state.errorType)
        assertEquals(messageResId, state.errorType.messageResId)
        assertTrue(repository.queries.isNotEmpty())
    }

    private fun createErrorViewModel(repository: RegionalDisposalGuideRepository) = createViewModel(
        regionRepository = FakeRegionRepository(
            extractedRegion = Region(sido = "서울특별시", sigungu = "중구"),
        ),
        regionalGuideRepository = repository,
    )
}

private class RecoverableRegionalGuideRepository : RegionalDisposalGuideRepository {
    val queries = mutableListOf<RegionalGuideQuery>()
    var result: Result<List<RegionalDisposalGuide>> = Result.failure(
        RegionalGuideLookupException(RegionalGuideFailureReason.NETWORK),
    )

    override suspend fun getRegionalDisposalGuideCandidates(
        query: RegionalGuideQuery,
    ): Result<List<RegionalDisposalGuide>> {
        queries += query
        return result
    }
}
