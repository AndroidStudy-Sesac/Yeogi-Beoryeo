package com.team.yeogibeoryeo.presentation.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionSpotMapScreenStateTest {

    @Test
    fun `장소 상세 표시 중 운영 공지만 수신하면 상세 화면을 유지한다`() {
        val shouldKeepDetail =
            shouldKeepSpotDetailOnOperationNotice(
                mapUiMode = MapUiMode.SpotDetail,
                hasOperationNotice = true,
                hasLocationNotice = false,
                hasError = false,
                isLoading = false,
            )

        assertTrue(shouldKeepDetail)
    }

    @Test
    fun `장소 상세이 아니면 운영 공지 바텀시트 전환을 허용한다`() {
        val shouldKeepDetail =
            shouldKeepSpotDetailOnOperationNotice(
                mapUiMode = MapUiMode.ResultList,
                hasOperationNotice = true,
                hasLocationNotice = false,
                hasError = false,
                isLoading = false,
            )

        assertFalse(shouldKeepDetail)
    }

    @Test
    fun `운영 공지 외 상태 변화가 있으면 기존 상태 전환을 허용한다`() {
        val shouldKeepDetail =
            shouldKeepSpotDetailOnOperationNotice(
                mapUiMode = MapUiMode.SpotDetail,
                hasOperationNotice = true,
                hasLocationNotice = false,
                hasError = true,
                isLoading = false,
            )

        assertFalse(shouldKeepDetail)
    }
}
