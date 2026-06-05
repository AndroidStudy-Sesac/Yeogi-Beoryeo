package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveRegionFromKeywordUseCaseTest {

    @Test
    fun `읍면동 단독 검색어가 유일하면 상위 지역을 보완한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = Region(eupmyeondong = "전의면")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "세종특별자치시",
                        eupmyeondong = "전의면"
                    )
                )
            )
        )

        val result = useCase("전의면")

        val region = (result as ResolveRegionFromKeywordResult.Resolved).region

        assertEquals("세종특별자치시", region.sido)
        assertEquals(null, region.sigungu)
        assertEquals("전의면", region.eupmyeondong)
    }

    @Test
    fun `접미사 없는 읍면동 검색어가 유일하면 상위 지역을 보완한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = null
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "언양읍"
                    )
                )
            )
        )

        val result = useCase("언양")

        val region = (result as ResolveRegionFromKeywordResult.Resolved).region

        assertEquals("울산광역시", region.sido)
        assertEquals("울주군", region.sigungu)
        assertEquals("언양읍", region.eupmyeondong)
    }

    @Test
    fun `읍면동 단독 검색어 후보가 여러 개면 임의로 보완하지 않는다`() = runBlocking {
        val parsedRegion = Region(eupmyeondong = "중앙동")
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = parsedRegion
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "성남시",
                        eupmyeondong = "중앙동"
                    ),
                    Region(
                        sido = "경상남도",
                        sigungu = "창원시",
                        eupmyeondong = "중앙동"
                    )
                )
            )
        )

        val result = useCase("중앙동")

        assertTrue(result is ResolveRegionFromKeywordResult.Ambiguous)
        assertEquals(2, (result as ResolveRegionFromKeywordResult.Ambiguous).candidates.size)
    }

    @Test
    fun `접미사 없는 읍면동 검색어 후보가 여러 개면 임의로 보완하지 않는다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = null
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "충청남도",
                        sigungu = "아산시",
                        eupmyeondong = "온양1동"
                    )
                )
            )
        )

        val result = useCase("온양")

        assertTrue(result is ResolveRegionFromKeywordResult.Ambiguous)
        assertEquals(2, (result as ResolveRegionFromKeywordResult.Ambiguous).candidates.size)
    }

    @Test
    fun `검색어가 시군구와 읍면동 후보에 모두 포함되면 함께 후보로 반환한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = null
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구",
                        eupmyeondong = "파장동"
                    ),
                    Region(
                        sido = "부산광역시",
                        sigungu = "기장군",
                        eupmyeondong = "장안읍"
                    )
                )
            )
        )

        val result = useCase("장안")

        val candidates = (result as ResolveRegionFromKeywordResult.Ambiguous).candidates

        assertEquals(2, candidates.size)
        assertEquals(
            Region(
                sido = "경기도",
                sigungu = "수원시 장안구",
                eupmyeondong = null
            ),
            candidates[0]
        )
        assertEquals(
            Region(
                sido = "부산광역시",
                sigungu = "기장군",
                eupmyeondong = "장안읍"
            ),
            candidates[1]
        )
    }

    @Test
    fun `검색어로 해석할 지역이 없으면 NotFound를 반환한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = null
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = emptyList()
            )
        )

        val result = useCase("없는지역")

        assertEquals(ResolveRegionFromKeywordResult.NotFound, result)
    }

    @Test
    fun `시군구가 이미 있으면 지역 옵션 후보로 덮어쓰지 않는다`() = runBlocking {
        val parsedRegion = Region(sigungu = "수원시")
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = parsedRegion
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구"
                    ),
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 영통구"
                    )
                )
            )
        )

        val result = useCase("수원시")

        assertEquals(ResolveRegionFromKeywordResult.Resolved(parsedRegion), result)
    }

    @Test
    fun `행정구 단독 검색어가 유일하면 상위 지역을 보완한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "장안구")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구",
                        eupmyeondong = "파장동"
                    ),
                    Region(
                        sido = "경기도",
                        sigungu = "수원시 장안구",
                        eupmyeondong = "정자1동"
                    )
                )
            )
        )

        val result = useCase("장안구")

        val region = (result as ResolveRegionFromKeywordResult.Resolved).region

        assertEquals("경기도", region.sido)
        assertEquals("수원시 장안구", region.sigungu)
        assertEquals(null, region.eupmyeondong)
    }

    @Test
    fun `시도 없는 동일 시군구 후보가 여러 개면 임의로 보완하지 않는다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = Region(sigungu = "중구")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "서울특별시",
                        sigungu = "중구"
                    ),
                    Region(
                        sido = "대구광역시",
                        sigungu = "중구"
                    )
                )
            )
        )

        val result = useCase("중구")

        assertTrue(result is ResolveRegionFromKeywordResult.Ambiguous)
        assertEquals(2, (result as ResolveRegionFromKeywordResult.Ambiguous).candidates.size)
    }

    @Test
    fun `접미사 없는 시군구 검색어가 유일하면 상위 지역을 보완하고 읍면동은 비워둔다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = null
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "언양읍"
                    )
                )
            )
        )

        val result = useCase("울주")

        val region = (result as ResolveRegionFromKeywordResult.Resolved).region

        assertEquals("울산광역시", region.sido)
        assertEquals("울주군", region.sigungu)
        assertEquals(null, region.eupmyeondong)
    }

    @Test
    fun `부분 지역으로 파싱됐더라도 시군구 후보가 유일하면 시군구로 보완한다`() = runBlocking {
        val useCase = ResolveRegionFromKeywordUseCase(
            repository = FakeRegionRepository(
                resolvedRegion = Region(eupmyeondong = "울주")
            ),
            regionOptionsRepository = FakeRegionOptionsRepository(
                regions = listOf(
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "온양읍"
                    ),
                    Region(
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = "언양읍"
                    )
                )
            )
        )

        val result = useCase("울주")

        val region = (result as ResolveRegionFromKeywordResult.Resolved).region

        assertEquals("울산광역시", region.sido)
        assertEquals("울주군", region.sigungu)
        assertEquals(null, region.eupmyeondong)
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
        ): List<Region> {
            val exactMatches = regions.filter { region -> region.eupmyeondong == keyword }

            return exactMatches.ifEmpty {
                regions.filter { region ->
                    region.eupmyeondong?.startsWith(keyword) == true
                }
            }
        }

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> {
            val exactMatches = regions.filter { region -> region.sigungu == keyword }

            val prefixMatches = exactMatches.ifEmpty {
                regions.filter { region ->
                    region.sigungu?.startsWith(keyword) == true
                }
            }

            return prefixMatches.ifEmpty {
                regions.filter { region ->
                    region.sigungu?.contains(keyword) == true
                }
            }.map { region ->
                region.copy(eupmyeondong = null)
            }.distinctBy { region ->
                listOf(
                    region.sido.orEmpty(),
                    region.sigungu.orEmpty()
                )
            }
                .map { region ->
                    region.copy(eupmyeondong = null)
                }
                .distinctBy { region ->
                    listOf(
                        region.sido.orEmpty(),
                        region.sigungu.orEmpty()
                    )
                }
        }
    }
}
