package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeRegionalGuideSummaryBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `대표_지역_가이드_즐겨찾기가_없으면_카드가_검색_이동을_요청한다`() {
        var detailClickCount = 0
        var searchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                HomeRegionalGuideSummaryBanner(
                    state = HomeRegionalGuideSummaryUiState.NoFavorite,
                    onClick = { detailClickCount += 1 },
                    onSearchClick = { searchClickCount += 1 },
                    onRetryClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("지역 가이드를 즐겨찾기하면 배출 기준을 여기에서 확인할 수 있어요.")
            .assertIsDisplayed()
        composeTestRule.onNode(hasOnClickLabel("지역별 배출 가이드 찾아보기"))
            .assert(hasClickAction())
            .performClick()

        assertEquals(0, detailClickCount)
        assertEquals(1, searchClickCount)
    }

    @Test
    fun `대표_지역_가이드_즐겨찾기가_있으면_기존_대상_상세_이동을_요청한다`() {
        var clickedTargetId: String? = null
        var searchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                HomeRegionalGuideSummaryBanner(
                    state = HomeRegionalGuideSummaryUiState.Summary(
                        targetId = "regional-guide-v2|4:Sido",
                        regionName = "서울특별시 > 중구",
                        disposalDays = "월, 수, 금",
                        disposalTime = "18:00 이후",
                        hasDifferentDisposalDays = false,
                        hasDifferentDisposalTime = false,
                    ),
                    onClick = { targetId -> clickedTargetId = targetId },
                    onSearchClick = { searchClickCount += 1 },
                    onRetryClick = {},
                )
            }
        }

        composeTestRule.onNode(hasOnClickLabel("지역 가이드 상세 보기"))
            .assert(hasClickAction())
            .performClick()

        assertEquals("regional-guide-v2|4:Sido", clickedTargetId)
        assertEquals(0, searchClickCount)
    }

    private fun hasOnClickLabel(label: String) =
        SemanticsMatcher("onClick label is '$label'") { node ->
            SemanticsActions.OnClick in node.config &&
                node.config[SemanticsActions.OnClick].label == label
        }
}
