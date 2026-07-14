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

    private fun createUseCase(
        resolvedRegion: Region?,
        keywordRegions: List<Region> = emptyList(),
    ): ResolveRegionalGuideRegionFromKeywordUseCase =
        ResolveRegionalGuideRegionFromKeywordUseCase(
            ResolveRegionFromKeywordUseCase(
                repository = FakeRegionRepository(resolvedRegion),
                regionOptionsRepository = FakeRegionOptionsRepository(keywordRegions),
            )
        )

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
        private val regions: List<Region>
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
