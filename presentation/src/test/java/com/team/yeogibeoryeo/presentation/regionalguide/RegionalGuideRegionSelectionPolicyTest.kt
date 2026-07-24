package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegionalGuideRegionSelectionPolicyTest {

    @Test
    fun `선택지에 없는 읍면동은 조회 지역을 유지하고 선택 UI에서만 제거한다`() {
        val lookupRegion = Region(
            sido = "경기도",
            sigungu = "수원시",
            eupmyeondong = "장안동",
        )

        val result = RegionalGuideRegionSelectionPolicy.prepare(
            lookupRegion = lookupRegion,
            eupmyeondongOptions = listOf("팔달동"),
        )

        assertEquals(lookupRegion, result.lookupRegion)
        assertEquals(lookupRegion.copy(eupmyeondong = null), result.selectorRegion)
        assertEquals("장안동", result.removedEupmyeondong)
    }

    @Test
    fun `관리구역 읍면동은 가이드 후보의 선택 가능한 표시 지역으로 사용한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "경기도", sigungu = "수원시"),
            managementZoneName = "장안동",
            schedules = emptyList(),
        )

        val result = RegionalGuideRegionSelectionPolicy.guideWithSelectableEupmyeondong(guide)

        assertEquals("장안동", result.region.eupmyeondong)
    }

    @Test
    fun `가이드 지역과 선택지가 일치할 때만 비동기 선택지를 선택 상태에 반영한다`() {
        val state = RegionSelectorUiState(
            selectedSido = "경기도",
            selectedSigungu = "수원시",
            eupmyeondongOptions = listOf("장안동"),
        )

        val result = RegionalGuideRegionSelectionPolicy.synchronizeSelectedEupmyeondong(
            state = state,
            guideRegion = Region(
                sido = "경기도",
                sigungu = "수원시",
                eupmyeondong = "장안동",
            ),
        )

        assertEquals("장안동", result.selectedEupmyeondong)
    }

    @Test
    fun `다른 시군구의 늦게 도착한 선택지는 현재 선택 상태를 바꾸지 않는다`() {
        val state = RegionSelectorUiState(
            selectedSido = "경기도",
            selectedSigungu = "수원시",
            eupmyeondongOptions = listOf("장안동"),
        )

        val result = RegionalGuideRegionSelectionPolicy.synchronizeSelectedEupmyeondong(
            state = state,
            guideRegion = Region(
                sido = "경기도",
                sigungu = "용인시",
                eupmyeondong = "장안동",
            ),
        )

        assertNull(result.selectedEupmyeondong)
    }
}
