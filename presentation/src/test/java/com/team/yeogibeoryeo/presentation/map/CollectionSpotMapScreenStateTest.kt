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
    fun `지역 후보 뒤로가기 후 현재 위치 결과 바텀시트 자동 노출을 막는다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.Browsing,
                searchMode = MapSearchMode.CURRENT_LOCATION,
                hasNoticeOrError = false,
                hasRegionSelection = false,
                hasEmptyResult = false,
            )

        assertTrue(shouldKeepHidden)
    }

    @Test
    fun `지역 후보 뒤로가기 후 위치 안내는 바텀시트로 노출한다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.Browsing,
                searchMode = MapSearchMode.KEYWORD,
                hasNoticeOrError = true,
                hasRegionSelection = false,
                hasEmptyResult = false,
            )

        assertFalse(shouldKeepHidden)
    }

    @Test
    fun `지역 후보 뒤로가기 숨김 상태는 현재 위치 결과에만 적용한다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.Browsing,
                searchMode = MapSearchMode.KEYWORD,
                hasNoticeOrError = false,
                hasRegionSelection = false,
                hasEmptyResult = false,
            )

        assertFalse(shouldKeepHidden)
    }

    @Test
    fun `지역 선택 상태가 남아 있으면 후보 바텀시트 노출을 허용한다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.Browsing,
                searchMode = MapSearchMode.CURRENT_LOCATION,
                hasNoticeOrError = false,
                hasRegionSelection = true,
                hasEmptyResult = false,
            )

        assertFalse(shouldKeepHidden)
    }

    @Test
    fun `지역 후보 뒤로가기 숨김 상태여도 빈 결과는 바텀시트로 노출한다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.Browsing,
                searchMode = MapSearchMode.CURRENT_LOCATION,
                hasNoticeOrError = false,
                hasRegionSelection = false,
                hasEmptyResult = true,
            )

        assertFalse(shouldKeepHidden)
    }

    @Test
    fun `장소 상세 화면에서는 지역 후보 뒤로가기 숨김 상태를 적용하지 않는다`() {
        val shouldKeepHidden =
            shouldKeepCurrentLocationSheetHiddenAfterRegionBack(
                shouldKeepCurrentLocationSheetHiddenAfterRegionBack = true,
                mapUiMode = MapUiMode.SpotDetail,
                searchMode = MapSearchMode.CURRENT_LOCATION,
                hasNoticeOrError = false,
                hasRegionSelection = false,
                hasEmptyResult = false,
            )

        assertFalse(shouldKeepHidden)
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
}
