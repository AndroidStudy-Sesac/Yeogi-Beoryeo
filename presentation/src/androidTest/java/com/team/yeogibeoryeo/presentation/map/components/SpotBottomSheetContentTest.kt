package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.map.MapSearchFailures
import com.team.yeogibeoryeo.presentation.map.MapSearchMode
import com.team.yeogibeoryeo.presentation.operationnotice.OperationNoticeUiModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SpotBottomSheetContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 운영_공지와_검색_실패가_함께_있어도_재시도_버튼을_제공한다() {
        var retryClicked = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val retryLabel = context.getString(R.string.retry_action)

        composeTestRule.setContent {
            MaterialTheme {
                SpotBottomSheetContent(
                    spots = emptyList(),
                    selectedSpot = null,
                    isLoading = false,
                    hasSearched = true,
                    selectedTypes = emptySet(),
                    isFilterResultEmpty = false,
                    searchMode = MapSearchMode.KEYWORD,
                    regionSearchCandidates = emptyList(),
                    regionDetailSearchCandidate = null,
                    locationNotice = null,
                    operationNotice = OperationNoticeUiModel(
                        id = "critical-notice",
                        severity = OperationNoticeSeverity.CRITICAL,
                        title = "운영 공지",
                        message = "수거 장소 검색이 원활하지 않습니다.",
                        actionLabel = null,
                        actionUrl = null,
                    ),
                    searchFailure = MapSearchFailures.Network,
                    errorMessageResId = null,
                    partialWarningMessageResId = null,
                    onTypeClick = {},
                    onClearTypeFiltersClick = {},
                    onRegionCandidateClick = {},
                    onRegionDetailAllClick = {},
                    onRegionDetailKeywordClick = {},
                    onRegionDetailBackClick = {},
                    onLocationNoticeActionClick = {},
                    onSearchFailureRetryClick = { retryClicked = true },
                    onSpotClick = {},
                    onSpotFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(retryLabel)
            .assertIsDisplayed()
            .performClick()

        assertTrue(retryClicked)
    }

    @Test
    fun 수거_장소_즐겨찾기_버튼은_장소명과_현재_상태를_제공한다() {
        var spot by mutableStateOf(
            CollectionSpot(
                id = "battery-bin",
                name = "문래동 폐건전지 수거함",
                type = CollectionSpotType.BATTERY_BIN,
                address = "서울특별시 영등포구 문래동",
                detailLocation = null,
                coordinate = null,
                distanceMeter = 120,
                isBookmarked = false,
            ),
        )

        composeTestRule.setContent {
            MaterialTheme {
                SpotBottomCard(
                    spot = spot,
                    isSelected = false,
                    onClick = {},
                    onFavoriteClick = { selectedSpot ->
                        spot = selectedSpot.copy(isBookmarked = !selectedSpot.isBookmarked)
                    },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("문래동 폐건전지 수거함 즐겨찾기")
            .assert(hasStateDescription("즐겨찾기 안 됨"))
            .assert(hasToggleableState(ToggleableState.Off))
            .performClick()

        composeTestRule.onNodeWithContentDescription("문래동 폐건전지 수거함 즐겨찾기")
            .assert(hasStateDescription("즐겨찾기됨"))
            .assert(hasToggleableState(ToggleableState.On))
    }

    private fun hasStateDescription(description: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, description)

    private fun hasToggleableState(state: ToggleableState) =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)
}
