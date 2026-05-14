package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 모킹 프레임워크를 대체하는 Fake 데이터 소스.ㅇ
 */
class FakeRegionalGuideDataSource : RegionalGuideDataSource {
    var mockResult: Result<List<RegionalGuideItemDto>> = Result.success(emptyList())
    var calledSigunguName: String? = null

    override suspend fun fetchRegionalGuides(sigunguName: String): Result<List<RegionalGuideItemDto>> {
        calledSigunguName = sigunguName
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
    fun `세종시 지역 객체 전달 시 시군구가 없어도 세종 키워드로 API를 호출한다`() = runBlocking {
        val sejongRegion = Region(sido = "세종특별자치시", sigungu = "", eupmyeondong = "새롬동")
        val mockData = listOf(RegionalGuideItemDto(sidoName = "세종특별자치시", dongName = "새롬동"))
        fakeDataSource.mockResult = Result.success(mockData)

        val result = repository.getRegionalDisposalGuide(sejongRegion)

        assertEquals("세종", fakeDataSource.calledSigunguName)
        assertEquals("새롬동", result?.region?.eupmyeondong)
    }

    @Test
    fun `동일한 동명칭 포함 시 정확히 일치하는 데이터를 최우선으로 선택한다`() = runBlocking {
        val region = Region(sido = "서울특별시", sigungu = "종로구", eupmyeondong = "인사동")
        val mockData = listOf(
            RegionalGuideItemDto(dongName = "인사동1가"),
            RegionalGuideItemDto(dongName = "인사동"),
            RegionalGuideItemDto(dongName = "인사동2가")
        )
        fakeDataSource.mockResult = Result.success(mockData)

        val result = repository.getRegionalDisposalGuide(region)

        assertEquals("종로구", fakeDataSource.calledSigunguName)
        assertEquals("인사동", result?.region?.eupmyeondong)
    }
}