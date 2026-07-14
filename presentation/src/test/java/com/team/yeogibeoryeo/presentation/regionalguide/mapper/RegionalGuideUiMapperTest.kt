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
}
