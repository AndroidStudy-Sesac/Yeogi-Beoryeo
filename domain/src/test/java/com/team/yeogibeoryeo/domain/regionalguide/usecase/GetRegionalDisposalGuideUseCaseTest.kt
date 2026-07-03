package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.usecase.FindAdminDongCandidatesForLegalDongUseCase
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

    @Test
    fun `legal admin mapped candidates are used when selecting regional guide candidate`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.success(
                listOf(
                    RegionalDisposalGuide(
                        region = Region(
                            sido = "서울특별시",
                            sigungu = "노원구"
                        ),
                        managementZoneName = "하계1동",
                        targetRegionName = "하계1동",
                        schedules = emptyList()
                    ),
                    RegionalDisposalGuide(
                        region = Region(
                            sido = "서울특별시",
                            sigungu = "노원구"
                        ),
                        managementZoneName = "하계2동",
                        targetRegionName = "하계2동",
                        schedules = emptyList()
                    )
                )
            ),
            adminDongCandidates = listOf(
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동"),
                Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계2동")
            )
        )

        val result = useCase(
            Region(
                sido = "서울특별시",
                sigungu = "노원구",
                eupmyeondong = "하계동"
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("하계1동", candidates[0].managementZoneName)
        assertEquals("하계2동", candidates[1].managementZoneName)
    }

    private fun createUseCase(
        repositoryResult: Result<List<RegionalDisposalGuide>>,
        adminDongCandidates: List<Region> = emptyList()
    ): GetRegionalDisposalGuideUseCase {
        return GetRegionalDisposalGuideUseCase(
            repository = FakeRegionalDisposalGuideRepository(repositoryResult),
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase(),
            findAdminDongCandidatesForLegalDongUseCase = FindAdminDongCandidatesForLegalDongUseCase(
                FakeRegionOptionsRepository(adminDongCandidates)
            )
        )
    }

    private class FakeRegionalDisposalGuideRepository(
        private val result: Result<List<RegionalDisposalGuide>>
    ) : RegionalDisposalGuideRepository {

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery
        ): Result<List<RegionalDisposalGuide>> = result
    }

    private class FakeRegionOptionsRepository(
        private val adminDongCandidates: List<Region>
    ) : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String
        ): List<String> = emptyList()

        override suspend fun findRegionsByEupmyeondongKeyword(
            keyword: String
        ): List<Region> = emptyList()

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> = emptyList()

        override suspend fun normalizeRegionForRegionalGuide(
            region: Region
        ): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(
            region: Region
        ): List<Region> = adminDongCandidates
    }
}
