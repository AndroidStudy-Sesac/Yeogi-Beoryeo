package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRegionalGuideDataSource : RegionalGuideDataSource {
    var mockResult: Result<List<RegionalGuideItemDto>> = Result.success(emptyList())
    var calledSigunguName: String? = null
    val calledSigunguNames = mutableListOf<String>()

    override suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>> {
        calledSigunguName = sigunguName
        calledSigunguNames += sigunguName
        return mockResult
    }
}

class RegionalDisposalGuideRepositoryImplTest {

    private lateinit var fakeDataSource: FakeRegionalGuideDataSource
    private lateinit var repository: RegionalDisposalGuideRepositoryImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeRegionalGuideDataSource()
        repository = RegionalDisposalGuideRepositoryImpl(fakeDataSource)
    }

    @Test
    fun `조회 키로 원격 데이터를 호출하고 후보 목록을 반환한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            listOf(
                RegionalGuideItemDto(
                    sidoName = "서울특별시",
                    sigunguName = "중구",
                    dongName = "서울시 중구"
                ),
                RegionalGuideItemDto(
                    sidoName = "대구광역시",
                    sigunguName = "중구",
                    dongName = "대봉2동"
                )
            )
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guides = result.getOrThrow()

        assertEquals("중구", fakeDataSource.calledSigunguName)
        assertEquals(2, guides.size)
        assertEquals("서울특별시", guides[0].region.sido)
        assertEquals("대구광역시", guides[1].region.sido)
    }

    @Test
    fun `저장소는 후보를 선택하지 않고 전체 후보를 도메인으로 변환한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            listOf(
                RegionalGuideItemDto(
                    sidoName = "인천광역시",
                    sigunguName = "중구",
                    dongName = "신흥동+율목동+영종동"
                ),
                RegionalGuideItemDto(
                    sidoName = "인천광역시",
                    sigunguName = "중구",
                    dongName = "신포동+연안동+도원동"
                )
            )
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = "신흥동"
                ),
                sigunguQuery = "중구"
            )
        )

        val guides = result.getOrThrow()

        assertEquals(2, guides.size)
        assertEquals("신흥동", guides[0].region.eupmyeondong)
        assertEquals("신흥동+율목동+영종동", guides[0].targetRegionName)
        assertEquals("신포동+연안동+도원동", guides[1].targetRegionName)
    }

    @Test
    fun `저장소는 에이피아이 시도명 원본값을 정규화하지 않는다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            listOf(
                RegionalGuideItemDto(
                    sidoName = "전남광주통합특별시",
                    sigunguName = "광산구",
                    dongName = "광산구 전체"
                )
            )
        )

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "광주광역시", sigungu = "광산구"),
                sigunguQuery = "광산구"
            )
        )

        val guide = result.getOrThrow().single()

        assertEquals("전남광주통합특별시", guide.region.sido)
        assertEquals("광산구", guide.region.sigungu)
        assertEquals("광산구 전체", guide.targetRegionName)
    }

    @Test
    fun `원격 데이터 소스 실패는 실패 결과로 유지한다`() = runBlocking {
        fakeDataSource.mockResult = Result.failure(IllegalStateException("network error"))

        val result = repository.getRegionalDisposalGuideCandidates(
            RegionalGuideQuery(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `같은 조회 키 후보 목록은 원격 데이터를 다시 호출하지 않고 캐시를 사용한다`() = runBlocking {
        fakeDataSource.mockResult = Result.success(
            listOf(
                RegionalGuideItemDto(
                    sidoName = "경상북도",
                    sigunguName = "김천시",
                    dongName = "동지역"
                )
            )
        )
        val query = RegionalGuideQuery(
            displayRegion = Region(sido = "경상북도", sigungu = "김천시"),
            sigunguQuery = "김천시"
        )

        val firstResult = repository.getRegionalDisposalGuideCandidates(query)
        val secondResult = repository.getRegionalDisposalGuideCandidates(query)

        assertTrue(firstResult.isSuccess)
        assertTrue(secondResult.isSuccess)
        assertEquals(listOf("김천시"), fakeDataSource.calledSigunguNames)
    }
}
