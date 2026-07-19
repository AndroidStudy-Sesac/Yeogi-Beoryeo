package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleTimeFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideUiMapperTest {

    @Test
    fun `시작 시간과 종료 시간이 있으면 시간 범위 형식을 전달한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = listOf(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.GENERAL,
                    disposalStartTime = "18:00",
                    disposalEndTime = "23:00",
                )
            )
        )

        val timeFormat = guide.toUiModel().schedules.single().disposalTimeFormat

        assertEquals(
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_range_format,
                args = listOf("18:00", "23:00"),
            ),
            timeFormat,
        )
    }

    @Test
    fun `시작 시간만 있으면 이후 시간 형식을 전달한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = listOf(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.GENERAL,
                    disposalStartTime = "18:00",
                )
            )
        )

        val timeFormat = guide.toUiModel().schedules.single().disposalTimeFormat

        assertEquals(
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_after_format,
                args = listOf("18:00"),
            ),
            timeFormat,
        )
    }

    @Test
    fun `종료 시간만 있으면 이전 시간 형식을 전달한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = listOf(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.GENERAL,
                    disposalEndTime = "23:00",
                )
            )
        )

        val timeFormat = guide.toUiModel().schedules.single().disposalTimeFormat

        assertEquals(
            RegionalWasteScheduleTimeFormat(
                resId = R.string.regional_waste_schedule_time_before_format,
                args = listOf("23:00"),
            ),
            timeFormat,
        )
    }

    @Test
    fun `관리구역명과 대상지역명이 없음이어도 화면 모델에는 원본 값을 유지한다`() {
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
        assertEquals(listOf("경기도", "성남시"), uiModel.regionNameParts)
        assertEquals("없음", guide.managementZoneName)
        assertEquals("없음", guide.targetRegionName)
    }

    @Test
    fun `관리구역명과 대상지역명이 공백이어도 화면 모델에는 원본 값을 유지한다`() {
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
    fun `의미 있는 관리구역명과 대상지역명은 화면 모델에 유지한다`() {
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
    fun `대형폐기물 요일이 없으면 화면 일정 요일을 비워 행을 숨길 수 있게 한다`() {
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
    fun `관리 부서와 연락처는 화면 문의 정보로 합쳐 표시한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "서울특별시", sigungu = "중구"),
            schedules = emptyList(),
            departmentName = "청소행정과",
            departmentPhoneNumber = "02-1234-5678",
        )

        val uiModel = guide.toUiModel()

        assertEquals("청소행정과 02-1234-5678", uiModel.departmentInfo)
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
