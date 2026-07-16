package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
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

        assertEquals(listOf("봉산면", "아포읍"), result)
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

        assertEquals(listOf("봉산면", "아포읍", "율곡동", "평화남산동"), result)
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
    fun `대상지역명 법정동 별칭은 읍면동 선택지에 추가하지 않는다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("노은1동", "노은2동", "노은3동"),
            candidates = listOf(
                regionalDisposalGuide(
                    managementZoneName = "노은2동",
                    targetRegionName = "반석동 일부지역"
                ),
                regionalDisposalGuide(
                    managementZoneName = "노은3동",
                    targetRegionName = "노은동"
                ),
                regionalDisposalGuide(
                    managementZoneName = "노은3동",
                    targetRegionName = "대동"
                ),
            )
        )

        val result = useCase(
            sido = "대전광역시",
            sigungu = "유성구"
        )

        assertEquals(listOf("노은1동", "노은2동", "노은3동"), result)
    }

    @Test
    fun `대상지역명 별칭이 여러 개 있어도 기존 선택지만 자연 정렬한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("노은10동", "노은2동"),
            candidates = listOf(
                regionalDisposalGuide(targetRegionName = "하기동 일부지역"),
                regionalDisposalGuide(targetRegionName = "갑동 일부지역")
            )
        )

        val result = useCase(
            sido = "대전광역시",
            sigungu = "유성구"
        )

        assertEquals(listOf("노은2동", "노은10동"), result)
    }

    @Test
    fun `출장소가 배출정보 후보에 없으면 읍면동 선택지에서 제외한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("구지면", "논공읍", "논공읍공단출장소", "다사읍", "다사읍서재출장소", "옥포읍"),
            candidates = listOf(
                regionalDisposalGuide(
                    managementZoneName = "구지면",
                    targetRegionName = "고봉리+예현리+평촌리+가천리"
                ),
                regionalDisposalGuide(
                    managementZoneName = "논공읍",
                    targetRegionName = "금포리"
                ),
                regionalDisposalGuide(
                    managementZoneName = "다사읍",
                    targetRegionName = "서재리"
                ),
                regionalDisposalGuide(
                    managementZoneName = "옥포읍",
                    targetRegionName = "강림1.2리"
                )
            )
        )

        val result = useCase(
            sido = "대구광역시",
            sigungu = "달성군"
        )

        assertEquals(listOf("구지면", "논공읍", "다사읍", "옥포읍"), result)
    }

    @Test
    fun `출장소가 배출정보 후보에 정확히 있으면 읍면동 선택지에 유지한다`() = runBlocking {
        val useCase = createUseCase(
            options = listOf("논공읍", "논공읍공단출장소"),
            candidates = listOf(
                regionalDisposalGuide(managementZoneName = "논공읍공단출장소")
            )
        )

        val result = useCase(
            sido = "대구광역시",
            sigungu = "달성군"
        )

        assertEquals(listOf("논공읍", "논공읍공단출장소"), result)
    }

    @Test
    fun `권역 후보가 없으면 전체 선택지를 자연 정렬해 유지한다`() = runBlocking {
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

        assertEquals(listOf("봉산면", "아포읍", "율곡동", "평화남산동"), result)
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

        assertEquals(listOf("남산동", "아포읍", "중동"), result)
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

    @Test
    fun `후보 조회에 실패해도 제공 가능 읍면동 선택지만 유지한다`() = runBlocking {
        val result = createUseCase(
            options = listOf("아포읍", "봉산면", "율곡동"),
            regionalGuideOptions = listOf("아포읍", "율곡동"),
            candidatesResult = Result.failure(IllegalStateException("network error")),
        )("경상북도", "김천시")

        assertEquals(listOf("아포읍", "율곡동"), result)
    }

    private fun createUseCase(
        options: List<String>,
        regionalGuideOptions: List<String> = options,
        candidates: List<RegionalDisposalGuide> = emptyList(),
        candidatesResult: Result<List<RegionalDisposalGuide>> = Result.success(candidates),
    ): GetRegionalGuideEupmyeondongOptionsUseCase {
        val regionOptionsRepository = FakeRegionOptionsRepository(
            options = options,
            regionalGuideOptions = regionalGuideOptions,
        )

        return GetRegionalGuideEupmyeondongOptionsUseCase(
            regionOptionsRepository = regionOptionsRepository,
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
        private val regionalGuideOptions: List<String>,
    ) : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String,
        ): List<String> = options

        override suspend fun getRegionalGuideEupmyeondongOptions(
            sido: String,
            sigungu: String,
        ): List<String> = regionalGuideOptions

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
