package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickCategorySettingsScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 최대_개수에서_미선택_분류를_누르면_선택을_유지하고_스낵바를_보여준다() {
        var clickedCategory: RepresentativeGuideCategory? = null

        composeTestRule.setContent {
            MaterialTheme {
                QuickCategorySettingsScreen(
                    selectedCategories = setOf(RepresentativeGuideCategory.PAPER),
                    maxSelectedCount = 1,
                    onCategoryClick = { clickedCategory = it },
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(RepresentativeGuideCategory.PAPER_PACK.displayName)
            .performClick()

        composeTestRule.onNodeWithText("최대 1개까지만 선택할 수 있어요.")
            .assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(null, clickedCategory)
        }
    }
}
