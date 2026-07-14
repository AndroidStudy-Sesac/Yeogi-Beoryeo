package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
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
    fun `저장소 네트워크 실패는 네트워크 실패로 반환한다`() = runBlocking {
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
    fun `저장소 에이피아이 실패는 에이피아이 실패로 반환한다`() = runBlocking {
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
    fun `알 수 없는 저장소 실패는 알 수 없는 실패로 반환한다`() = runBlocking {
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
    fun `후보 목록 조회 성공 시 선택된 가이드를 반환한다`() = runBlocking {
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
    fun `지역 가이드 후보 선택 시 법정동 행정동 매핑 후보를 사용한다`() = runBlocking {
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

    @Test
    fun `과거 인천 중구 즐겨찾기 키는 최신 복수 구 후보를 임의 선택하지 않는다`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.success(emptyList()),
            repositoryResultsBySigungu = mapOf(
                "영종구" to Result.success(
                    listOf(
                        RegionalDisposalGuide(
                            region = Region(sido = "인천광역시", sigungu = "영종구"),
                            targetRegionName = "영종구 전체",
                            schedules = emptyList()
                        )
                    )
                ),
                "제물포구" to Result.success(
                    listOf(
                        RegionalDisposalGuide(
                            region = Region(sido = "인천광역시", sigungu = "제물포구"),
                            targetRegionName = "제물포구 전체",
                            schedules = emptyList()
                        )
                    )
                )
            )
        )

        val result = useCase(
            region = Region(sido = "인천광역시", sigungu = "중구"),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "중구",
                eupmyeondong = null,
                targetRegionName = "중구 전체",
                managementZoneName = null,
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(2, candidates.size)
        assertEquals("영종구", candidates[0].region.sigungu)
        assertEquals("제물포구", candidates[1].region.sigungu)
    }

    @Test
    fun `과거 인천 서구 즐겨찾기 키는 최신 후보가 하나만 조회돼도 상세로 확정하지 않는다`() = runBlocking {
        val useCase = createUseCase(
            repositoryResult = Result.success(emptyList()),
            repositoryResultsBySigungu = mapOf(
                "서해구" to Result.success(
                    listOf(
                        RegionalDisposalGuide(
                            region = Region(sido = "인천광역시", sigungu = "서해구"),
                            targetRegionName = "서해구 전체",
                            schedules = emptyList()
                        )
                    )
                ),
                "검단구" to Result.success(emptyList())
            )
        )

        val result = useCase(
            region = Region(sido = "인천광역시", sigungu = "서구"),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "인천광역시",
                sigungu = "서구",
                eupmyeondong = null,
                targetRegionName = "서구 전체",
                managementZoneName = null,
            )
        )

        val candidates = (result as RegionalGuideLookupResult.Candidates).guides

        assertEquals(1, candidates.size)
        assertEquals("서해구", candidates.single().region.sigungu)
    }

    @Test
    fun `과거 안양8동 즐겨찾기 키는 최신 명학동 후보로 복원한다`() = runBlocking {
        val repository = FakeRegionalDisposalGuideRepository(
            result = Result.success(emptyList()),
            resultsBySigungu = mapOf(
                "안양시" to Result.success(
                    listOf(
                        RegionalDisposalGuide(
                            region = Region(
                                sido = "경기도",
                                sigungu = "안양시 만안구",
                                eupmyeondong = "명학동"
                            ),
                            targetRegionName = "명학동",
                            managementZoneName = "명학동",
                            schedules = emptyList()
                        )
                    )
                )
            )
        )
        val useCase = createUseCase(repository = repository)

        val result = useCase(
            region = Region(
                sido = "경기도",
                sigungu = "안양시 만안구",
                eupmyeondong = "안양8동"
            ),
            favoriteKey = RegionalGuideFavoriteKey(
                sido = "경기도",
                sigungu = "안양시 만안구",
                eupmyeondong = "안양8동",
                targetRegionName = "안양8동",
                managementZoneName = "안양8동",
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("안양시", repository.requestedSigungu.single())
        assertEquals("안양시 만안구", guide.region.sigungu)
        assertEquals("명학동", guide.region.eupmyeondong)
        assertEquals("명학동", guide.targetRegionName)
    }

    private fun createUseCase(
        repositoryResult: Result<List<RegionalDisposalGuide>>,
        repositoryResultsBySigungu: Map<String, Result<List<RegionalDisposalGuide>>> = emptyMap(),
        adminDongCandidates: List<Region> = emptyList()
    ): GetRegionalDisposalGuideUseCase {
        return createUseCase(
            repository = FakeRegionalDisposalGuideRepository(
                result = repositoryResult,
                resultsBySigungu = repositoryResultsBySigungu,
            ),
            adminDongCandidates = adminDongCandidates,
        )
    }

    private fun createUseCase(
        repository: FakeRegionalDisposalGuideRepository,
        adminDongCandidates: List<Region> = emptyList()
    ): GetRegionalDisposalGuideUseCase {
        return GetRegionalDisposalGuideUseCase(
            repository = repository,
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase(),
            findAdminDongCandidatesForLegalDongUseCase = FindAdminDongCandidatesForLegalDongUseCase(
                FakeRegionOptionsRepository(adminDongCandidates)
            )
        )
    }

    private class FakeRegionalDisposalGuideRepository(
        private val result: Result<List<RegionalDisposalGuide>>,
        private val resultsBySigungu: Map<String, Result<List<RegionalDisposalGuide>>> = emptyMap(),
    ) : RegionalDisposalGuideRepository {
        val requestedSigungu = mutableListOf<String?>()

        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery
        ): Result<List<RegionalDisposalGuide>> {
            requestedSigungu += query.sigunguQuery
            return resultsBySigungu[query.sigunguQuery] ?: result
        }
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
