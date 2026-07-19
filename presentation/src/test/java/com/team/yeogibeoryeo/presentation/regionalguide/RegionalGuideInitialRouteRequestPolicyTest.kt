package com.team.yeogibeoryeo.presentation.regionalguide

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalGuideInitialRouteRequestPolicyTest {
    @Test
    fun `초기 상태에서는 최초 경로 요청을 처리한다`() {
        assertTrue(RegionalGuideUiState.Idle.shouldProcessInitialRouteRequest())
    }

    @Test
    fun `기존 조회 상태가 있으면 최초 경로 요청을 다시 처리하지 않는다`() {
        assertFalse(
            RegionalGuideUiState.Loading(query = "서울특별시 종로구").shouldProcessInitialRouteRequest(),
        )
    }
}
