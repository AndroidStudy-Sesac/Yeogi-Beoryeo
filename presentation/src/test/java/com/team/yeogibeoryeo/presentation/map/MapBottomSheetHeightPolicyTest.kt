package com.team.yeogibeoryeo.presentation.map

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.presentation.map.components.MapSpotDetailBottomSheetPeekHeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapBottomSheetHeightPolicyTest {
    @Test
    fun `상태 메시지 높이는 글꼴 배율 1점0과 1점3을 반영한다`() {
        val defaultFontHeight = stateMessageExpandedHeight(fontScale = 1f)
        val largeFontHeight = stateMessageExpandedHeight(fontScale = 1.3f)

        assertDpEquals(384.dp, defaultFontHeight)
        assertDpEquals(432.dp, largeFontHeight)
    }

    @Test
    fun `상태 메시지 높이는 하단 inset을 더한다`() {
        val withoutInset = stateMessageExpandedHeight(bottomContentPadding = 0.dp)
        val withInset = stateMessageExpandedHeight(bottomContentPadding = 32.dp)

        assertDpEquals(32.dp, withInset - withoutInset)
    }

    @Test
    fun `상태 메시지 높이는 화면별 최대 비율을 넘지 않는다`() {
        val height = stateMessageExpandedHeight(
            maxHeight = 600.dp,
            bottomContentPadding = 100.dp,
            fontScale = 1.3f,
        )

        assertDpEquals(348.dp, height)
    }

    @Test
    fun `지역 후보가 늘어나면 행 높이만큼 바텀시트 높이가 증가한다`() {
        val oneCandidate = regionSelectionExpandedHeight(candidateCount = 1)
        val threeCandidates = regionSelectionExpandedHeight(candidateCount = 3)

        assertDpEquals(241.dp, oneCandidate)
        assertDpEquals(377.dp, threeCandidates)
        assertDpEquals(136.dp, threeCandidates - oneCandidate)
    }

    @Test
    fun `지역 후보 높이는 화면 높이의 최대 비율을 넘지 않는다`() {
        val height = regionSelectionExpandedHeight(
            maxHeight = 600.dp,
            candidateCount = 20,
        )

        assertDpEquals(528.dp, height)
    }

    @Test
    fun `세부 지역 높이는 중복을 뺀 키워드와 후보 목록 뒤로가기 영역을 반영한다`() {
        val candidate = MapRegionSearchCandidate(
            region = Region(
                sido = "서울특별시",
                sigungu = "중구",
                eupmyeondong = "명동",
            ),
            searchKeyword = "명동",
            searchKeywords = listOf("명동", "명동1가", "명동1가", "명동2가"),
        )

        val withBackButton = regionDetailExpandedHeight(
            candidate = candidate,
            canNavigateBack = true,
        )
        val withoutBackButton = regionDetailExpandedHeight(
            candidate = candidate,
            canNavigateBack = false,
        )

        assertDpEquals(519.dp, withBackButton)
        assertDpEquals(459.dp, withoutBackButton)
    }

    @Test
    fun `Medium 단계의 상태 메시지는 계산된 상태 메시지 높이를 그대로 사용한다`() {
        val expandedHeight = stateMessageExpandedHeight(
            maxHeight = 800.dp,
            bottomContentPadding = 24.dp,
            fontScale = 1.3f,
        )
        val mediumHeight = MapBottomSheetHeightPolicy.mediumVisibleHeight(
            hasStateMessageContent = true,
            maxHeight = 800.dp,
            bottomContentPadding = 24.dp,
            fontScale = 1.3f,
        )

        assertDpEquals(expandedHeight, mediumHeight)
    }

    @Test
    fun `상태 메시지가 없으면 Medium 단계는 장소 상세 기본 높이를 사용한다`() {
        val mediumHeight = MapBottomSheetHeightPolicy.mediumVisibleHeight(
            hasStateMessageContent = false,
            maxHeight = 800.dp,
            bottomContentPadding = 24.dp,
            fontScale = 1.3f,
        )

        assertDpEquals(MapSpotDetailBottomSheetPeekHeight, mediumHeight)
    }

    @Test
    fun `장소 상세 모드는 최대 확장 높이를 제한하지 않는다`() {
        val height = MapBottomSheetHeightPolicy.maxExpandedHeight(
            mapUiMode = MapUiMode.SpotDetail,
            hasRegionSelection = true,
            hasStateMessageContent = true,
            maxHeight = 800.dp,
            bottomContentPadding = 24.dp,
            regionCandidateCount = 3,
            regionDetailCandidate = null,
            canNavigateBackToRegionCandidates = false,
            fontScale = 1.3f,
        )

        assertNull(height)
    }

    private fun stateMessageExpandedHeight(
        maxHeight: Dp = 1_000.dp,
        bottomContentPadding: Dp = 24.dp,
        fontScale: Float = 1f,
    ): Dp = requireNotNull(
        MapBottomSheetHeightPolicy.maxExpandedHeight(
            mapUiMode = MapUiMode.ResultList,
            hasRegionSelection = false,
            hasStateMessageContent = true,
            maxHeight = maxHeight,
            bottomContentPadding = bottomContentPadding,
            regionCandidateCount = 0,
            regionDetailCandidate = null,
            canNavigateBackToRegionCandidates = false,
            fontScale = fontScale,
        ),
    )

    private fun regionSelectionExpandedHeight(
        maxHeight: Dp = 1_000.dp,
        candidateCount: Int,
    ): Dp = requireNotNull(
        MapBottomSheetHeightPolicy.maxExpandedHeight(
            mapUiMode = MapUiMode.ResultList,
            hasRegionSelection = true,
            hasStateMessageContent = false,
            maxHeight = maxHeight,
            bottomContentPadding = 0.dp,
            regionCandidateCount = candidateCount,
            regionDetailCandidate = null,
            canNavigateBackToRegionCandidates = false,
            fontScale = 1f,
        ),
    )

    private fun regionDetailExpandedHeight(
        candidate: MapRegionSearchCandidate,
        canNavigateBack: Boolean,
    ): Dp = requireNotNull(
        MapBottomSheetHeightPolicy.maxExpandedHeight(
            mapUiMode = MapUiMode.ResultList,
            hasRegionSelection = true,
            hasStateMessageContent = false,
            maxHeight = 1_000.dp,
            bottomContentPadding = 0.dp,
            regionCandidateCount = 0,
            regionDetailCandidate = candidate,
            canNavigateBackToRegionCandidates = canNavigateBack,
            fontScale = 1f,
        ),
    )

    private fun assertDpEquals(expected: Dp, actual: Dp) {
        assertEquals(expected.value.toDouble(), actual.value.toDouble(), 0.001)
    }
}
