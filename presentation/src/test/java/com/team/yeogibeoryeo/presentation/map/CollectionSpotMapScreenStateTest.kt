package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.presentation.map.components.MapSheetLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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

    @Test
    fun `장소 상세를 닫을 때 수신한 운영 공지가 있으면 결과 목록을 Medium으로 보여준다`() {
        val returnState =
            mapDetailCloseReturnState(
                hasResultListToReturn = true,
                hasOperationNotice = true,
            )

        assertEquals(MapUiMode.ResultList, returnState.mapUiMode)
        assertEquals(MapSheetLevel.Medium, returnState.sheetLevel)
    }

    @Test
    fun `장소 상세를 닫을 때 운영 공지만 있어도 공지 결과 목록을 Medium으로 보여준다`() {
        val returnState =
            mapDetailCloseReturnState(
                hasResultListToReturn = false,
                hasOperationNotice = true,
            )

        assertEquals(MapUiMode.ResultList, returnState.mapUiMode)
        assertEquals(MapSheetLevel.Medium, returnState.sheetLevel)
    }

    @Test
    fun `장소 상세를 닫을 때 운영 공지가 없으면 기존 결과 목록 Peek로 돌아간다`() {
        val returnState =
            mapDetailCloseReturnState(
                hasResultListToReturn = true,
                hasOperationNotice = false,
            )

        assertEquals(MapUiMode.ResultList, returnState.mapUiMode)
        assertEquals(MapSheetLevel.Peek, returnState.sheetLevel)
    }

    @Test
    fun `검색창이 포커스되면 결과 바텀시트를 숨기고 탐색 상태로 전환한다`() {
        val returnState =
            mapSearchFocusReturnState(
                mapUiMode = MapUiMode.ResultList,
                sheetLevel = MapSheetLevel.Expanded,
            )

        assertEquals(MapUiMode.Browsing, returnState.mapUiMode)
        assertEquals(MapSheetLevel.Hidden, returnState.sheetLevel)
    }

    @Test
    fun `검색창 포커스는 장소 상세 상태를 변경하지 않는다`() {
        val returnState =
            mapSearchFocusReturnState(
                mapUiMode = MapUiMode.SpotDetail,
                sheetLevel = MapSheetLevel.Medium,
            )

        assertEquals(MapUiMode.SpotDetail, returnState.mapUiMode)
        assertEquals(MapSheetLevel.Medium, returnState.sheetLevel)
    }
}
