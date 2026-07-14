package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetRegionalGuideEupmyeondongOptionsUseCaseTest {

    @Test
    fun `동지역 후보만 있으면 동 선택지만 반환한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("아포읍", "봉산면", "율곡동", "평화남산동"),
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "동지역")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(listOf("율곡동", "평화남산동"), result)
    }

    @Test
    fun `동지역 후보만 있고 읍면 선택지만 있으면 빈 선택지를 반환한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("아포읍", "봉산면"),
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "동지역")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `읍면지역 후보만 있으면 읍과 면 선택지만 반환한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("아포읍", "봉산면", "율곡동", "평화남산동"),
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "김천시 읍면 지역")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(listOf("아포읍", "봉산면"), result)
    }

    @Test
    fun `동지역과 읍면지역 후보가 모두 있으면 읍면동 선택지를 유지한다`() = runBlocking {
        val options = listOf("아포읍", "봉산면", "율곡동", "평화남산동")
        val useCase = createUseCase(
            options = options,
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "동지역"),
                regionalDisposalGuide(targetRegionName = "읍면지역")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(options, result)
    }

    @Test
    fun `권역 후보와 개별 후보가 함께 있으면 개별 후보 선택지도 유지한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("아포읍", "봉산면", "율곡동", "평화남산동"),
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "동지역"),
                regionalDisposalGuide(targetRegionName = "아포읍")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(listOf("아포읍", "율곡동", "평화남산동"), result)
    }

    @Test
    fun `권역 후보가 없으면 전체 선택지를 유지한다`() = runBlocking {
        val options = listOf("아포읍", "봉산면", "율곡동", "평화남산동")
        val useCase = createUseCase(
            options = options,
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "율곡동")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(options, result)
    }

    @Test
    fun `중동지역처럼 단어 일부가 겹치는 후보는 권역 표현으로 보지 않는다`() = runBlocking {
        val options = listOf("중동", "남산동", "아포읍")
        val useCase = createUseCase(
            options = options,
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "중동지역")
            )
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(options, result)
    }

    @Test
    fun `후보 조회에 실패하면 전체 선택지를 유지한다`() = runBlocking {
        val options = listOf("아포읍", "봉산면", "율곡동", "평화남산동")
        val useCase = createUseCase(
            options = options,
            candidatesResult = Result.failure(IllegalStateException("network error"))
        )

        val result = useCase(
            sido = "경상북도",
            sigungu = "김천시"
        )

        assertEquals(options, result)
    }

    private fun createUseCase(
        options: List<String>,
        candidates: List<RegionalDisposalGuide> = emptyList(),
        candidatesResult: Result<List<RegionalDisposalGuide>> = Result.success(candidates),
    ): GetRegionalGuideEupmyeondongOptionsUseCase {
        val regionOptionsRepository = FakeRegionOptionsRepository(options)

        return GetRegionalGuideEupmyeondongOptionsUseCase(
            getEupmyeondongOptionsUseCase = GetEupmyeondongOptionsUseCase(regionOptionsRepository),
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            repository = FakeRegionalDisposalGuideRepository(candidatesResult),
        )
    }

    private fun regionalDisposalGuide(
        managementZoneName: String? = null,
        targetRegionName: String? = null,
    ): RegionalDisposalGuide {
        return RegionalDisposalGuide(
            region = Region(sido = "경상북도", sigungu = "김천시"),
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
            schedules = emptyList(),
        )
    }

    private class FakeRegionOptionsRepository(
        private val options: List<String>,
    ) : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String,
        ): List<String> = options

        override suspend fun findRegionsByEupmyeondongKeyword(keyword: String): List<Region> =
            emptyList()

        override suspend fun findRegionsBySigunguKeyword(keyword: String): List<Region> =
            emptyList()

        override suspend fun normalizeRegionForRegionalGuide(region: Region): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(region: Region): List<Region> =
            emptyList()
    }

    private class FakeRegionalDisposalGuideRepository(
        private val candidatesResult: Result<List<RegionalDisposalGuide>>,
    ) : RegionalDisposalGuideRepository {
        override suspend fun getRegionalDisposalGuideCandidates(
            query: RegionalGuideQuery,
        ): Result<List<RegionalDisposalGuide>> = candidatesResult
    }
}
