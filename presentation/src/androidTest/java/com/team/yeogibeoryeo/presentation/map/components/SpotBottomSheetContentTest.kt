package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
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
}
