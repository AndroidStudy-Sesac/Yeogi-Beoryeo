package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Rule
import org.junit.Test

class KoreanLineBreakSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun section_text_semantics_uses_original_text_without_zero_width_space() {
        composeTestRule.setContent {
            MaterialTheme {
                SectionCard(
                    title = "배출 절차",
                    lines = listOf("비웁니다."),
                    numbered = true,
                )
            }
        }

        composeTestRule.onNodeWithText("1. 비웁니다.").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("1. 비\u200B웁\u200B니\u200B다\u200B.").assertCountEquals(0)
    }

    @Test
    fun quick_category_semantics_uses_original_label_without_zero_width_space() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickCategoryGrid(
                    categories = listOf(RepresentativeGuideCategory.NON_COMBUSTIBLE),
                    onCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("불연성종량제 폐기물").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("불\u200B연\u200B성\u200B종\u200B량\u200B제\u200B 폐\u200B기\u200B물\u200B")
            .assertCountEquals(0)
    }
}
