package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `대형폐기물 요일이 없으면 UI 일정 요일을 비워 행을 숨길 수 있게 한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = listOf(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.LARGE_ITEM,
                    disposalPlace = "대형폐기물 지정 장소",
                )
            )
        )

        val schedule = guide.toUiModel().schedules.single()

        assertEquals("대형폐기물", schedule.wasteTypeName)
        assertNull(schedule.disposalDays)
        assertNull(schedule.disposalTime)
        assertNull(schedule.disposalMethod)
        assertEquals("대형폐기물 지정 장소", schedule.disposalPlace)
    }

    @Test
    fun `관리 부서와 연락처는 UI 문의 정보로 합쳐 표시한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = emptyList(),
            departmentName = "청소행정과",
            departmentPhoneNumber = "02-1234-5678",
        )

        val uiModel = guide.toUiModel()

        assertEquals("청소행정과 02-1234-5678", uiModel.departmentInfo)
    }
}
