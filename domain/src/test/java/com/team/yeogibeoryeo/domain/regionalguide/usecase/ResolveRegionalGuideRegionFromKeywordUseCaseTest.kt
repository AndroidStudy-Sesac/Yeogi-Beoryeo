package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordResult
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveRegionalGuideRegionFromKeywordUseCaseTest {

    @Test
    fun `시도 없는 기존 인천 구명 검색은 기존 동일 구명과 최신 인천 후보를 함께 안내한다`() =
        runBlocking {
            val useCase = createUseCase(
                resolvedRegion = Region(sigungu = "중구"),
                keywordRegions = listOf(
                    Region(sido = "서울특별시", sigungu = "중구"),
                    Region(sido = "대구광역시", sigungu = "중구")
                )
            )

            val result = useCase("중구")

            val candidates = (result as ResolveRegionFromKeywordResult.Ambiguous).candidates

            assertEquals(
                listOf(
                    Region(sido = "대구광역시", sigungu = "중구"),
                    Region(sido = "서울특별시", sigungu = "중구"),
                    Region(sido = "인천광역시", sigungu = "영종구"),
                    Region(sido = "인천광역시", sigungu = "제물포구")
                ),
                candidates
            )
        }

    @Test
    fun `인천광역시 중구 검색은 최신 영종구와 제물포구 후보로 안내한다`() = runBlocking {
        val useCase = createUseCase(
            resolvedRegion = Region(sido = "인천광역시", sigungu = "중구")
        )

        val result = useCase("인천광역시 중구")

        val candidates = (result as ResolveRegionFromKeywordResult.Ambiguous).candidates

        assertEquals(
            listOf(
                Region(sido = "인천광역시", sigungu = "영종구"),
                Region(sido = "인천광역시", sigungu = "제물포구")
            ),
            candidates
        )
    }

    @Test
    fun `인천광역시 동구 검색은 최신 제물포구로 보완한다`() = runBlocking {
        val useCase = createUseCase(
            resolvedRegion = Region(sido = "인천광역시", sigungu = "동구")
        )

        val result = useCase("인천광역시 동구")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(sido = "인천광역시", sigungu = "제물포구")
            ),
            result
        )
    }

    @Test
    fun `안양8동 검색은 최신 명학동으로 보완하고 만안구를 유지한다`() = runBlocking {
        val useCase = createUseCase(
            resolvedRegion = Region(
                sido = "경기도",
                sigungu = "안양시 만안구",
                eupmyeondong = "안양8동"
            )
        )

        val result = useCase("경기도 안양시 만안구 안양8동")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(
                    sido = "경기도",
                    sigungu = "안양시 만안구",
                    eupmyeondong = "명학동"
                )
            ),
            result
        )
    }

    @Test
    fun `안양9동 검색은 최신 병목안동으로 보완하고 만안구를 유지한다`() = runBlocking {
        val useCase = createUseCase(
            resolvedRegion = Region(
                sido = "경기도",
                sigungu = "안양시 만안구",
                eupmyeondong = "안양9동"
            )
        )

        val result = useCase("경기도 안양시 만안구 안양9동")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(
                    sido = "경기도",
                    sigungu = "안양시 만안구",
                    eupmyeondong = "병목안동"
                )
            ),
            result
        )
    }

    @Test
    fun `문자 점 묶음 행정동 검색 결과는 지역 가이드용 붙여쓴 표시명으로 변환한다`() = runBlocking {
        val useCase = createUseCase(
            resolvedRegion = Region(
                sido = "대구광역시",
                sigungu = "동구",
                eupmyeondong = "불로.봉무동",
            )
        )

        val result = useCase("불로봉무동")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(
                    sido = "대구광역시",
                    sigungu = "동구",
                    eupmyeondong = "불로봉무동",
                )
            ),
            result
        )
    }

    @Test
    fun `지역 가이드는 번호 행정동 별칭이 있는 범위에서 법정동 후보를 대체한다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = null,
            keywordRegions = listOf(
                Region(sido = "대전광역시", sigungu = "서구", eupmyeondong = "괴정동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정동")
            ),
            regionalGuideKeywordRegions = listOf(
                Region(sido = "대전광역시", sigungu = "서구", eupmyeondong = "괴정동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동")
            )
        )("괴정동")

        assertEquals(
            ResolveRegionFromKeywordResult.Ambiguous(
                listOf(
                    Region(sido = "대전광역시", sigungu = "서구", eupmyeondong = "괴정동"),
                    Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                    Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동")
                )
            ),
            result
        )
    }

    @Test
    fun `전체 주소의 법정동은 해석된 읍면동으로 번호 행정동 후보를 찾는다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = Region(
                sido = "대전광역시",
                sigungu = "유성구",
                eupmyeondong = "노은동",
            ),
            regionalGuideKeywordRegions = listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은1동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동"),
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은3동"),
                Region(sido = "부산광역시", sigungu = "북구", eupmyeondong = "노은1동"),
            ),
            regionalGuideKeyword = "노은동",
        )("대전광역시 유성구 노은동")

        assertEquals(
            ResolveRegionFromKeywordResult.Ambiguous(
                listOf(
                    Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은1동"),
                    Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은2동"),
                    Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "노은3동"),
                )
            ),
            result,
        )
    }

    @Test
    fun `전체 주소의 괴정동은 같은 시군구의 번호 행정동 후보만 찾는다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = Region(
                sido = "부산광역시",
                sigungu = "사하구",
                eupmyeondong = "괴정동",
            ),
            regionalGuideKeywordRegions = listOf(
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동"),
                Region(sido = "대전광역시", sigungu = "서구", eupmyeondong = "괴정동"),
            ),
            regionalGuideKeyword = "괴정동",
        )("부산광역시 사하구 괴정동")

        assertEquals(
            ResolveRegionFromKeywordResult.Ambiguous(
                listOf(
                    Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제1동"),
                    Region(sido = "부산광역시", sigungu = "사하구", eupmyeondong = "괴정제2동"),
                )
            ),
            result,
        )
    }

    @Test
    fun `광주광역시 전체 주소 법정동은 통합 시도 번호 행정동 후보를 찾는다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = Region(
                sido = "광주광역시",
                sigungu = "동구",
                eupmyeondong = "충장동",
            ),
            regionalGuideKeywordRegions = listOf(
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "동구",
                    eupmyeondong = "충장1동",
                ),
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "나주시",
                    eupmyeondong = "충장2동",
                ),
            ),
            regionalGuideKeyword = "충장동",
        )("광주광역시 동구 충장동")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "동구",
                    eupmyeondong = "충장1동",
                )
            ),
            result,
        )
    }

    @Test
    fun `전라남도 전체 주소 법정동은 통합 시도 번호 행정동 후보를 찾는다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = Region(
                sido = "전라남도",
                sigungu = "나주시",
                eupmyeondong = "빛가람동",
            ),
            regionalGuideKeywordRegions = listOf(
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "나주시",
                    eupmyeondong = "빛가람1동",
                ),
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "동구",
                    eupmyeondong = "빛가람2동",
                ),
            ),
            regionalGuideKeyword = "빛가람동",
        )("전라남도 나주시 빛가람동")

        assertEquals(
            ResolveRegionFromKeywordResult.Resolved(
                Region(
                    sido = "전남광주통합특별시",
                    sigungu = "나주시",
                    eupmyeondong = "빛가람1동",
                )
            ),
            result,
        )
    }

    @Test
    fun `제공 정보가 없는 일반 읍면동 후보는 지역 가이드 검색에서 제외한다`() = runBlocking {
        val result = createUseCase(
            resolvedRegion = null,
            keywordRegions = listOf(
                Region(sido = "대전광역시", sigungu = "유성구", eupmyeondong = "대동"),
            ),
            availableRegionalGuideRegions = emptyList(),
        )("대동")

        assertEquals(ResolveRegionFromKeywordResult.NotFound, result)
    }

    private fun createUseCase(
        resolvedRegion: Region?,
        keywordRegions: List<Region> = emptyList(),
        regionalGuideKeywordRegions: List<Region> = emptyList(),
        regionalGuideKeyword: String? = null,
        availableRegionalGuideRegions: List<Region>? = null,
    ): ResolveRegionalGuideRegionFromKeywordUseCase {
        val regionOptionsRepository = FakeRegionOptionsRepository(
            regions = keywordRegions,
            regionalGuideRegions = regionalGuideKeywordRegions,
            regionalGuideKeyword = regionalGuideKeyword,
            availableRegionalGuideRegions = availableRegionalGuideRegions,
        )

        return ResolveRegionalGuideRegionFromKeywordUseCase(
            ResolveRegionFromKeywordUseCase(
                repository = FakeRegionRepository(resolvedRegion),
                regionOptionsRepository = regionOptionsRepository,
            ),
            regionOptionsRepository = regionOptionsRepository,
        )
    }

    private class FakeRegionRepository(
        private val resolvedRegion: Region?
    ) : RegionRepository {

        override fun extractRegionFromAddress(address: String): Region? = resolvedRegion

        override suspend fun resolveRegionFromKeyword(keyword: String): Region? = resolvedRegion

        override suspend fun resolveRegionFromCoordinate(
            latitude: Double,
            longitude: Double
        ): Region? = null
    }

    private class FakeRegionOptionsRepository(
        private val regions: List<Region>,
        private val regionalGuideRegions: List<Region>,
        private val regionalGuideKeyword: String?,
        private val availableRegionalGuideRegions: List<Region>?,
    ) : RegionOptionsRepository {

        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String
        ): List<String> = emptyList()

        override suspend fun findRegionsByEupmyeondongKeyword(
            keyword: String
        ): List<Region> = regions.filter { region -> region.eupmyeondong == keyword }

        override suspend fun findRegionalGuideRegionsByEupmyeondongKeyword(
            keyword: String
        ): List<Region> = regionalGuideRegions.takeIf {
            regionalGuideKeyword == null || keyword == regionalGuideKeyword
        }.orEmpty()

        override suspend fun filterAvailableRegionalGuideRegions(
            regions: List<Region>
        ): List<Region> = availableRegionalGuideRegions?.let { availableRegions ->
            regions.filter { region -> region in availableRegions }
        } ?: regions

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> =
            regions.filter { region -> region.sigungu == keyword }

        override suspend fun normalizeRegionForRegionalGuide(
            region: Region
        ): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(
            region: Region
        ): List<Region> = emptyList()
    }
}
