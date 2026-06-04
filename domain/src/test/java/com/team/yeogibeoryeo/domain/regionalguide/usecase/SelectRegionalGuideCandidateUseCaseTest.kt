package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectRegionalGuideCandidateUseCaseTest {

    private val useCase = SelectRegionalGuideCandidateUseCase()

    @Test
    fun `후보가 없으면 NotFound를 반환한다`() {
        val result = useCase(
            candidates = emptyList(),
            query = query(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertEquals(RegionalGuideLookupResult.NotFound, result)
    }

    @Test
    fun `동일 시군구명이 여러 시도에 있으면 선택한 시도 기준으로 후보를 고른다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "대구광역시", sigungu = "중구", targetRegionName = "대봉2동"),
                guide(sido = "서울특별시", sigungu = "중구", targetRegionName = "서울시 중구")
            ),
            query = query(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("서울특별시", guide.region.sido)
        assertEquals("중구", guide.region.sigungu)
        assertEquals("서울시 중구", guide.targetRegionName)
    }

    @Test
    fun `선택한 시도와 일치하는 후보가 없으면 CandidateNotFound를 반환한다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "대구광역시", sigungu = "중구", targetRegionName = "대봉2동")
            ),
            query = query(
                displayRegion = Region(sido = "서울특별시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `대상지역 설명에 선택 읍면동이 포함되면 해당 후보를 선택하고 Region 읍면동은 선택값으로 유지한다`() {
        val result = useCase(
            candidates = listOf(
                guide(
                    sido = "인천광역시",
                    sigungu = "중구",
                    targetRegionName = "신흥동+율목동+영종동+영종1동+영종2동+용유동"
                ),
                guide(
                    sido = "인천광역시",
                    sigungu = "중구",
                    targetRegionName = "신포동+연안동+도원동"
                )
            ),
            query = query(
                displayRegion = Region(
                    sido = "인천광역시",
                    sigungu = "중구",
                    eupmyeondong = "신흥동"
                ),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신흥동", guide.region.eupmyeondong)
        assertEquals(
            "신흥동+율목동+영종동+영종1동+영종2동+용유동",
            guide.targetRegionName
        )
    }

    @Test
    fun `세종특별자치시 동지역에 포함된 동은 동지역 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "세종특별자치시", sigungu = null, targetRegionName = "전의면, 전동면, 소정면"),
                guide(sido = "세종특별자치시", sigungu = null, targetRegionName = "동지역")
            ),
            query = query(
                displayRegion = Region(
                    sido = "세종특별자치시",
                    eupmyeondong = "한솔동"
                ),
                sigunguQuery = "없음"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("한솔동", guide.region.eupmyeondong)
        assertEquals("동지역", guide.targetRegionName)
    }

    @Test
    fun `전체 적용 대상지역은 읍면동이 없을 때 우선 선택된다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "경기도", sigungu = "수원시", targetRegionName = "수원시 전체"),
                guide(sido = "경기도", sigungu = "수원시", targetRegionName = "일부 권역")
            ),
            query = query(
                displayRegion = Region(sido = "경기도", sigungu = "수원시"),
                sigunguQuery = "수원시"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("수원시 전체", guide.targetRegionName)
    }

    @Test
    fun `조회 key와 일치하는 시군구 후보가 없으면 CandidateNotFound를 반환한다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "경기도", sigungu = "수원시 장안구", targetRegionName = "수원시 전체")
            ),
            query = query(
                displayRegion = Region(sido = "경기도", sigungu = "수원시 장안구"),
                sigunguQuery = "수원시"
            )
        )

        assertTrue(result is RegionalGuideLookupResult.CandidateNotFound)
    }

    @Test
    fun `읍면동 없이 복수 권역 후보만 있으면 임의 선택하지 않는다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동"),
                guide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신포동+연안동")
            ),
            query = query(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        assertEquals(RegionalGuideLookupResult.CandidateNotFound, result)
    }

    @Test
    fun `읍면동 없이 단일 권역 후보만 있으면 해당 후보를 선택한다`() {
        val result = useCase(
            candidates = listOf(
                guide(sido = "인천광역시", sigungu = "중구", targetRegionName = "신흥동+율목동")
            ),
            query = query(
                displayRegion = Region(sido = "인천광역시", sigungu = "중구"),
                sigunguQuery = "중구"
            )
        )

        val guide = (result as RegionalGuideLookupResult.Success).guide

        assertEquals("신흥동+율목동", guide.targetRegionName)
    }

    private fun query(
        displayRegion: Region,
        sigunguQuery: String
    ): RegionalGuideQuery =
        RegionalGuideQuery(
            displayRegion = displayRegion,
            sigunguQuery = sigunguQuery
        )

    private fun guide(
        sido: String?,
        sigungu: String?,
        targetRegionName: String?
    ): RegionalDisposalGuide =
        RegionalDisposalGuide(
            region = Region(
                sido = sido,
                sigungu = sigungu
            ),
            targetRegionName = targetRegionName,
            schedules = emptyList()
        )
}
