package com.team.yeogibeoryeo.presentation.search

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickCategorySettingsScreenUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun 검색창과_빈_결과는_분류명_검색_범위를_안내한다() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickCategorySettingsScreen(
                    selectedCategories = emptySet(),
                    maxSelectedCount = 1,
                    onCategoryClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("분류 검색").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzznotfound")
        composeTestRule.onNodeWithText("다른 분류 이름으로 다시 검색해 주세요.")
            .assertIsDisplayed()
    }

    @Test
    fun 상단_뒤로가기는_검색어를_먼저_지우고_다음_클릭에_화면을_닫는다() {
        var backClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickCategorySettingsScreen(
                    selectedCategories = emptySet(),
                    maxSelectedCount = 1,
                    onCategoryClick = {},
                    onBackClick = { backClickCount += 1 },
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzznotfound")
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeTestRule.onNodeWithText("검색 결과가 없어요.").assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertEquals(0, backClickCount)
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeTestRule.runOnIdle {
            assertEquals(1, backClickCount)
        }
    }

    @Test
    fun 시스템_뒤로가기는_검색어를_먼저_지우고_다음_입력에_화면을_닫는다() {
        var backClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                QuickCategorySettingsScreen(
                    selectedCategories = emptySet(),
                    maxSelectedCount = 1,
                    onCategoryClick = {},
                    onBackClick = { backClickCount += 1 },
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzznotfound")
        pressSystemBack()

        composeTestRule.onNodeWithText("검색 결과가 없어요.").assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertEquals(0, backClickCount)
        }

        pressSystemBack()
        composeTestRule.runOnIdle {
            assertEquals(1, backClickCount)
        }
    }

    private fun pressSystemBack() {
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

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

    @Test
    fun 진입_순서는_선택과_해제_및_검색어를_지운_뒤에도_유지된다() {
        val battery = RepresentativeGuideCategory.BATTERY
        val paper = RepresentativeGuideCategory.PAPER
        val paperPack = RepresentativeGuideCategory.PAPER_PACK
        val entryOrder = listOf(battery) + quickCategoryOrder.filterNot { it == battery }

        composeTestRule.setContent {
            var selected by remember { mutableStateOf(setOf(battery)) }
            MaterialTheme {
                QuickCategorySettingsScreen(
                    selectedCategories = selected,
                    maxSelectedCount = 2,
                    onCategoryClick = { category ->
                        selected = if (category in selected) selected - category else selected + category
                    },
                    onBackClick = {},
                    categoryOrder = entryOrder,
                )
            }
        }

        fun assertEntryOrder() {
            composeTestRule.waitForIdle()
            val batteryTop = composeTestRule.onNodeWithText(battery.displayName).getUnclippedBoundsInRoot().top
            val paperTop = composeTestRule.onNodeWithText(paper.displayName).getUnclippedBoundsInRoot().top
            val paperPackTop = composeTestRule.onNodeWithText(paperPack.displayName).getUnclippedBoundsInRoot().top
            assertTrue(batteryTop < paperTop)
            assertTrue(paperTop < paperPackTop)
        }

        assertEntryOrder()
        composeTestRule.onNodeWithText(battery.displayName).performClick()
        composeTestRule.onNodeWithText(paperPack.displayName).performClick()
        assertEntryOrder()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("건전")
        composeTestRule.onNodeWithText(battery.displayName).assertIsDisplayed()
        composeTestRule.onNodeWithText(paper.displayName).assertDoesNotExist()
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        assertEntryOrder()
    }
}
