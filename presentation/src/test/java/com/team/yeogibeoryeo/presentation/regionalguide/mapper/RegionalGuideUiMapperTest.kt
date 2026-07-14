package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideUiMapperTest {

    @Test
    fun `관리구역명과 대상지역명이 없음이어도 UI 모델에는 원본 값을 유지한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "경기도", sigungu = "성남시"),
            managementZoneName = "없음",
            targetRegionName = "없음",
            schedules = emptyList()
        )

        val uiModel = guide.toUiModel()

        assertEquals("없음", uiModel.managementZoneName)
        assertEquals("없음", uiModel.targetRegionName)
        assertEquals("경기도 성남시", uiModel.regionName)
        assertEquals("없음", guide.managementZoneName)
        assertEquals("없음", guide.targetRegionName)
    }

    @Test
    fun `관리구역명과 대상지역명이 공백이어도 UI 모델에는 원본 값을 유지한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "경기도", sigungu = "성남시"),
            managementZoneName = " ",
            targetRegionName = "",
            schedules = emptyList()
        )

        val uiModel = guide.toUiModel()

        assertEquals(" ", uiModel.managementZoneName)
        assertEquals("", uiModel.targetRegionName)
        assertEquals("경기도 성남시", uiModel.regionName)
    }

    @Test
    fun `의미 있는 관리구역명과 대상지역명은 UI 모델에 유지한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "대전광역시", sigungu = "유성구"),
            managementZoneName = "노은2동",
            targetRegionName = "반석동 일부지역",
            schedules = emptyList()
        )

        val uiModel = guide.toUiModel()

        assertEquals("노은2동", uiModel.managementZoneName)
        assertEquals("반석동 일부지역", uiModel.targetRegionName)
    }

    @Test
    fun `문자 점 묶음 행정동은 붙여쓴 대상지역명으로 지역명을 표시한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(
                sido = "대구광역시",
                sigungu = "동구",
                eupmyeondong = "불로.봉무동"
            ),
            managementZoneName = "2권역",
            targetRegionName = "불로봉무동",
            schedules = emptyList()
        )

        val uiModel = guide.toUiModel()

        assertEquals("대구광역시 동구 불로봉무동", uiModel.regionName)
        assertEquals("불로봉무동", uiModel.targetRegionName)
    }

    @Test
    fun `문자 점 묶음 행정동이 묶음 대상지역에 포함되면 매칭된 토큰으로 지역명을 표시한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(
                sido = "충청북도",
                sigungu = "충주시",
                eupmyeondong = "성내.충인동"
            ),
            managementZoneName = "3권역",
            targetRegionName = "성내충인동+교현안림동+교현2동",
            schedules = emptyList()
        )

        val uiModel = guide.toUiModel()

        assertEquals("충청북도 충주시 성내충인동", uiModel.regionName)
        assertEquals("성내충인동+교현안림동+교현2동", uiModel.targetRegionName)
    }
}
