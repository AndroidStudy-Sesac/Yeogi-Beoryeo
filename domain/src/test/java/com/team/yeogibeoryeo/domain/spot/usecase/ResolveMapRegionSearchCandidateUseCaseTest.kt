package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidateResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveMapRegionSearchCandidateUseCaseTest {

    private val repository = FakeRegionOptionsRepository(
        eupmyeondongCandidates = mapOf(
            "명동" to listOf(
                Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                Region(sido = "충청북도", sigungu = "제천시", eupmyeondong = "명동"),
                Region(sido = "경상남도", sigungu = "창원시 진해구", eupmyeondong = "명동"),
            ),
            "금호동" to listOf(
                Region(sido = "서울특별시", sigungu = "성동구", eupmyeondong = "금호동"),
                Region(sido = "광주광역시", sigungu = "서구", eupmyeondong = "금호동"),
                Region(sido = "전라남도", sigungu = "광양시", eupmyeondong = "금호동"),
            ),
            "운남동" to listOf(
                Region(sido = "광주광역시", sigungu = "광산구", eupmyeondong = "운남동"),
            ),
            "노안면" to listOf(
                Region(sido = "전라남도", sigungu = "나주시", eupmyeondong = "노안면"),
            ),
            "농성동" to listOf(
                Region(sido = "전남광주통합특별시", sigungu = "서구", eupmyeondong = "농성동"),
            ),
            "왕곡면" to listOf(
                Region(sido = "전남광주통합특별시", sigungu = "나주시", eupmyeondong = "왕곡면"),
            ),
            "문래동" to listOf(
                Region(sido = "서울특별시", sigungu = "영등포구", eupmyeondong = "문래동"),
            ),
        ),
        legalDongKeywords = mapOf(
            "서울특별시|중구|명동|명동" to listOf("명동1가", "명동2가"),
        ),
    )
    private val useCase = ResolveMapRegionSearchCandidateUseCase(
        regionOptionsRepository = repository,
        normalizeKeywordUseCase = NormalizeCollectionSpotSearchKeywordUseCase(),
    )

    @Test
    fun `동 단독 검색어에 여러 지역 후보가 있으면 후보 선택 결과를 반환한다`() =
        runSuspendTest {
            val result = useCase("명동")

            assertTrue(result is MapRegionSearchCandidateResult.NeedSelection)
            result as MapRegionSearchCandidateResult.NeedSelection
            assertEquals("명동", result.searchKeyword)
            assertEquals(
                listOf(
                    "서울특별시 중구 명동",
                    "충청북도 제천시 명동",
                    "경상남도 창원시 진해구 명동",
                ),
                result.candidates.map { candidate -> candidate.displayName },
            )
        }

    @Test
    fun `시도 시군구 동 입력은 후보를 좁혀 바로 검색 결과를 반환한다`() =
        runSuspendTest {
            val result = useCase("서울 중구 명동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("명동", result.searchKeyword)
            assertEquals("서울특별시 중구 명동", result.selectedCandidate?.displayName)
            assertEquals(listOf("명동", "명동1가", "명동2가"), result.selectedCandidate?.searchKeywords)
        }

    @Test
    fun `구 동 입력은 후보를 좁혀 바로 검색 결과를 반환한다`() =
        runSuspendTest {
            val result = useCase("성동구 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("서울특별시 성동구 금호동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `후보가 1개이면 선택 UI 없이 바로 검색 결과를 반환한다`() =
        runSuspendTest {
            val result = useCase("문래동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("문래동", result.searchKeyword)
            assertEquals("서울특별시 영등포구 문래동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `후보가 없으면 기존 검색 흐름을 유지한다`() =
        runSuspendTest {
            val result = useCase("없는동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("없는동", result.searchKeyword)
            assertEquals(null, result.selectedCandidate)
        }

    @Test
    fun `지역 범위가 있지만 후보 데이터가 없으면 입력 범위로 후보를 보강한다`() =
        runSuspendTest {
            val result = useCase("대구 북구 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("대구광역시 북구 금호동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전남광주통합특별시 광주 5개 구 입력은 광주 후보로 좁혀 검색한다`() =
        runSuspendTest {
            val result = useCase("전남광주통합특별시 서구 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("광주광역시 서구 금호동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전남광주통합특별시 전남 시군 입력은 전남 후보로 좁혀 검색한다`() =
        runSuspendTest {
            val result = useCase("전남광주통합특별시 광양시 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("전라남도 광양시 금호동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전남광주통합특별시 광주 5개 구 입력은 현재 광주광역시 후보와 매칭된다`() =
        runSuspendTest {
            val result = useCase("전남광주통합특별시 광산구 운남동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("운남동", result.searchKeyword)
            assertEquals("광주광역시 광산구 운남동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전남광주통합특별시 전남 시군 입력은 현재 전라남도 후보와 매칭된다`() =
        runSuspendTest {
            val result = useCase("전남광주통합특별시 나주시 노안면")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("노안면", result.searchKeyword)
            assertEquals("전라남도 나주시 노안면", result.selectedCandidate?.displayName)
        }

    @Test
    fun `광주광역시 입력은 전남광주통합특별시 광주 5개 구 후보와 매칭된다`() =
        runSuspendTest {
            val result = useCase("광주광역시 서구 농성동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("농성동", result.searchKeyword)
            assertEquals("전남광주통합특별시 서구 농성동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전라남도 입력은 전남광주통합특별시 전남 시군 후보와 매칭된다`() =
        runSuspendTest {
            val result = useCase("전라남도 나주시 왕곡면")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("왕곡면", result.searchKeyword)
            assertEquals("전남광주통합특별시 나주시 왕곡면", result.selectedCandidate?.displayName)
        }

    @Test
    fun `광주 축약명 입력은 광주광역시 후보로 좁혀 검색한다`() =
        runSuspendTest {
            val result = useCase("광주 서구 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("광주광역시 서구 금호동", result.selectedCandidate?.displayName)
        }

    @Test
    fun `전남 축약명 입력은 전라남도 후보로 좁혀 검색한다`() =
        runSuspendTest {
            val result = useCase("전남 광양시 금호동")

            assertTrue(result is MapRegionSearchCandidateResult.ReadyToSearch)
            result as MapRegionSearchCandidateResult.ReadyToSearch
            assertEquals("금호동", result.searchKeyword)
            assertEquals("전라남도 광양시 금호동", result.selectedCandidate?.displayName)
        }

    private fun runSuspendTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }

    private class FakeRegionOptionsRepository(
        private val eupmyeondongCandidates: Map<String, List<Region>> = emptyMap(),
        private val legalDongKeywords: Map<String, List<String>> = emptyMap(),
    ) : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String,
        ): List<String> = emptyList()

        override suspend fun findRegionsByEupmyeondongKeyword(keyword: String): List<Region> {
            return eupmyeondongCandidates[keyword].orEmpty()
        }

        override suspend fun findLegalDongKeywordsByRegion(
            region: Region,
            keyword: String,
        ): List<String> {
            return legalDongKeywords[
                listOf(
                    region.sido.orEmpty(),
                    region.sigungu.orEmpty(),
                    region.eupmyeondong.orEmpty(),
                    keyword,
                ).joinToString("|")
            ].orEmpty()
        }

        override suspend fun findRegionsBySigunguKeyword(keyword: String): List<Region> = emptyList()

        override suspend fun normalizeRegionForRegionalGuide(region: Region): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(region: Region): List<Region> = emptyList()
    }
}
