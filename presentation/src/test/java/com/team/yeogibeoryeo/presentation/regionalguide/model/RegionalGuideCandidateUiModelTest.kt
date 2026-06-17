package com.team.yeogibeoryeo.presentation.regionalguide.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalGuideCandidateUiModelTest {

    @Test
    fun `관리구역명과 대상지역명이 다르면 두 값을 함께 표시한다`() {
        val candidate = candidate(
            managementZoneName = "노은2동",
            targetRegionName = "반석동 일부지역"
        )

        assertEquals("노은2동 / 반석동 일부지역", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 같으면 한 번만 표시한다`() {
        val candidate = candidate(
            managementZoneName = "문전수거 지역",
            targetRegionName = "문전수거 지역"
        )

        assertEquals("문전수거 지역", candidate.displayText)
    }

    @Test
    fun `관리구역명만 있으면 관리구역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = "온천1동",
            targetRegionName = null
        )

        assertEquals("온천1동", candidate.displayText)
    }

    @Test
    fun `대상지역명만 있으면 대상지역명을 표시한다`() {
        val candidate = candidate(
            managementZoneName = null,
            targetRegionName = "반석동 일부지역"
        )

        assertEquals("반석동 일부지역", candidate.displayText)
    }

    @Test
    fun `관리구역명과 대상지역명이 모두 비어 있으면 지역명을 표시한다`() {
        val candidate = candidate(
            regionName = "대전광역시 유성구",
            managementZoneName = " ",
            targetRegionName = ""
        )

        assertEquals("대전광역시 유성구", candidate.displayText)
    }

    private fun candidate(
        regionName: String = "대전광역시 유성구",
        managementZoneName: String?,
        targetRegionName: String?
    ): RegionalGuideCandidateUiModel =
        RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = regionName,
                managementZoneName = managementZoneName,
                targetRegionName = targetRegionName,
                disposalPlaceType = null,
                disposalPlaceDescription = null,
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null
            ),
            sido = "대전광역시",
            sigungu = "유성구",
            eupmyeondong = null
        )
}
