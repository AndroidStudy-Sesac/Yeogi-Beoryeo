package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFailureReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupException
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRegionalDisposalGuideUseCaseTest {

    @Test
    fun `repository network failure returns network failure`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.failure(
                RegionalGuideLookupException(
                    reason = RegionalGuideFailureReason.NETWORK
                )
            )
        )

        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "중구"
            )
        )

        assertEquals(
            RegionalGuideFailureReason.NETWORK,
            (result as RegionalGuideLookupResult.Failure).reason
        )
    }

    @Test
    fun `repository api failure returns api failure`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.failure(
                RegionalGuideLookupException(
                    reason = RegionalGuideFailureReason.API
                )
            )
        )

        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "중구"
            )
        )

        assertEquals(
            RegionalGuideFailureReason.API,
            (result as RegionalGuideLookupResult.Failure).reason
        )
    }

    @Test
    fun `unknown repository failure returns unknown failure`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.failure(IllegalStateException())
        )

        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "중구"
            )
        )

        assertEquals(
            RegionalGuideFailureReason.UNKNOWN,
            (result as RegionalGuideLookupResult.Failure).reason
        )
    }

    @Test
    fun `candidate list success returns selected guide`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.success(
                listOf(
                    RegionalDisposalGuide(
                        region = Region(
                            sido = "서울특별시",
                            sigungu = "중구"
                        ),
                        targetRegionName = "서울시 중구",
                        schedules = emptyList()
                    )
                )
            )
        )

        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "중구"
            )
        )

        assertTrue(result is RegionalGuideLookupResult.Success)
    }

    private fun createUseCase(
        repositoryResult: Result<List<RegionalDisposalGuide>>
    ): GetRegionalDisposalGuideUseCase {
        return GetRegionalDisposalGuideUseCase(
            repository = FakeRegionalDisposalGuideRepository(repositoryResult),
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase()
        )
    }

    private class FakeRegionalDisposalGuideRepository(
        private val result: Result<List<RegionalDisposalGuide>>
    ) : RegionalDisposalGuideRepository {

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery
        ): Result<List<RegionalDisposalGuide>> = result
    }
}
