package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideViewModelSupportTest {

    @Test
    fun `가이드 즐겨찾기 스냅샷은 빈 관리구역과 대상지역명을 정규화한다`() {
        val guide = RegionalDisposalGuide(
            region = Region(sido = "경기도", sigungu = "수원시"),
            targetRegionName = "  ",
            managementZoneName = " 장안동 ",
            schedules = emptyList(),
        )

        val snapshot = RegionalGuideFavoriteSnapshotFactory.from(guide)

        assertNull(snapshot.targetRegionName)
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
