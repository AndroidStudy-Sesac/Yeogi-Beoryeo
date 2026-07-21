package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideViewModelSupportTest {

    @Test
    fun `후보 즐겨찾기 스냅샷은 후보 지역과 관리구역을 사용한다`() {
        val candidate = RegionalGuideCandidateUiModel(
            guide = RegionalGuideUiModel(
                regionName = "경기도 수원시 장안동",
                managementZoneName = "장안동",
                targetRegionName = "정자동",
                disposalPlaceType = null,
                disposalPlaceDescription = null,
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null,
            ),
            sido = "경기도",
            sigungu = "수원시",
            eupmyeondong = "장안동",
        )

        val snapshot = RegionalGuideFavoriteSnapshotFactory.from(candidate)

        assertEquals(Region(sido = "경기도", sigungu = "수원시", eupmyeondong = "장안동"), snapshot.region)
        assertEquals("정자동", snapshot.targetRegionName)
        assertEquals("장안동", snapshot.managementZoneName)
    }

    @Test
    fun `후보 복원 스택은 최신 화면 상태와 요청을 보존한다`() {
        val stack = RegionalGuideCandidateBackStack()
        val selectorState = RegionSelectorUiState(selectedSido = "경기도")
        val request = RegionalGuideRequest.Keyword("수원시")

        stack.push(
            uiState = RegionalGuideUiState.Idle,
            searchKeyword = "수원시",
            regionSelectorUiState = selectorState,
            lastRequest = request,
        )

        assertTrue(stack.canRestore)

        val entry = stack.pop()

        assertEquals(RegionalGuideUiState.Idle, entry?.uiState)
        assertEquals("수원시", entry?.searchKeyword)
        assertEquals(selectorState, entry?.regionSelectorUiState)
        assertEquals(request, entry?.lastRequest)
        assertFalse(stack.canRestore)
    }
}
