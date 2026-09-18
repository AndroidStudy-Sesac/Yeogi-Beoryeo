package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemSearchHomeLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 넓은_창에서_홈_본문을_720dp로_제한하고_중앙에_정렬한다() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(1000.dp).height(900.dp).testTag("window")) {
                            ItemSearchScreen(
                                uiState = ItemSearchUiState(),
                                onQueryChange = {},
                                onSearchClick = {},
                                onGuideClick = {},
                                onQuickCategoryClick = {},
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val window = composeTestRule.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val body = homeList().fetchSemanticsNode().boundsInRoot
        val search = composeTestRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot

        assertEquals(1000f, window.width, 1f)
        assertEquals(720f, body.width, 1f)
        assertEquals(window.center.x, body.center.x, 1f)
        assertEquals(body.left + 32f, search.left, 1f)
        assertEquals(body.right - 32f, search.right, 1f)
    }

    @Test
    fun 낮은_가로_창의_전체_폭을_사용하고_높이_변경_후에도_입력을_유지한다() {
        var height by mutableStateOf(480.dp)
        var uiState by mutableStateOf(ItemSearchUiState())
        var viewportChanges = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .width(1000.dp)
                                .height(height)
                                .consumeWindowInsets(WindowInsets.statusBars)
                                .testTag("window"),
                        ) {
                            ItemSearchScreen(
                                uiState = uiState,
                                onQueryChange = { uiState = uiState.copy(query = it) },
                                onSearchClick = {},
                                onGuideClick = {},
                                onQuickCategoryClick = {},
                                onQuickCategoryViewportChanged = { viewportChanges += 1 },
                            )
                        }
                    }
                }
            }
        }

        homeList().performScrollToNode(hasSetTextAction())
        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("PET")
        composeTestRule.waitForIdle()
        val window = composeTestRule.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val landscapeBody = homeList().fetchSemanticsNode().boundsInRoot
        val landscapeSearch = composeTestRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(1000f, window.width, 1f)
        assertEquals(window.left, landscapeBody.left, 1f)
        assertEquals(window.right, landscapeBody.right, 1f)
        assertEquals(window.left + 24f, landscapeSearch.left, 1f)
        assertEquals(window.right - 24f, landscapeSearch.right, 1f)

        val changesBeforeResize = composeTestRule.runOnIdle {
            viewportChanges.also { height = 481.dp }
        }
        homeList().performScrollToNode(hasSetTextAction())
        composeTestRule.waitForIdle()
        val tallBody = homeList().fetchSemanticsNode().boundsInRoot
        val tallSearch = composeTestRule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        assertEquals(720f, tallBody.width, 1f)
        assertEquals(window.center.x, tallBody.center.x, 1f)
        assertEquals(tallBody.left + 32f, tallSearch.left, 1f)
        assertEquals(tallBody.right - 32f, tallSearch.right, 1f)
        composeTestRule.runOnIdle { assertTrue(viewportChanges > changesBeforeResize) }
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed().assertTextContains("PET")

        composeTestRule.runOnIdle { height = 360.dp }
        homeList().performScrollToNode(hasSetTextAction())
        composeTestRule.waitForIdle()
        assertEquals(window.width, homeList().fetchSemanticsNode().boundsInRoot.width, 1f)
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed().assertTextContains("PET")
    }

    @Test
    fun 높이가_같은_창_폭_변경에도_입력과_분류_펼침을_유지한다() {
        var width by mutableStateOf(1000.dp)
        var uiState by mutableStateOf(ItemSearchUiState())
        var viewportChanges = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        val listState = rememberLazyListState()
                        Box(Modifier.width(width).height(700.dp).testTag("window")) {
                            ItemSearchScreen(
                                uiState = uiState,
                                onQueryChange = { uiState = uiState.copy(query = it) },
                                onSearchClick = {},
                                onGuideClick = {},
                                onQuickCategoryClick = {},
                                onQuickCategoryMoreClick = { count, index, offset ->
                                    uiState = uiState.copy(
                                        isQuickCategoryExpanded = true,
                                        quickCategoryFixedCollapsedItemCount = count,
                                        quickCategoryScrollRestoreIndex = index,
                                        quickCategoryScrollRestoreOffset = offset,
                                    )
                                },
                                onQuickCategoryCollapseClick = {
                                    uiState = uiState.copy(
                                        isQuickCategoryExpanded = false,
                                        quickCategoryScrollRestoreVersion =
                                            uiState.quickCategoryScrollRestoreVersion + 1,
                                    )
                                },
                                onQuickCategoryViewportChanged = {
                                    viewportChanges += 1
                                    uiState = uiState.copy(quickCategoryFixedCollapsedItemCount = 0)
                                },
                                categoryListState = listState,
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performTextReplacement("유리병")
        homeList().performScrollToNode(hasContentDescription("더보기"))
        composeTestRule.onNodeWithContentDescription("더보기").performClick()
        composeTestRule.waitForIdle()
        val changesBeforeResize = composeTestRule.runOnIdle {
            assertTrue(uiState.isQuickCategoryExpanded)
            viewportChanges.also { width = 360.dp }
        }
        composeTestRule.waitForIdle()

        assertEquals(360f, homeList().fetchSemanticsNode().boundsInRoot.width, 1f)
        composeTestRule.runOnIdle {
            assertTrue(viewportChanges > changesBeforeResize)
            assertTrue(uiState.isQuickCategoryExpanded)
            assertEquals(0, uiState.quickCategoryFixedCollapsedItemCount)
            assertEquals("유리병", uiState.query)
        }
        homeList().performScrollToNode(hasContentDescription("접기"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("접기").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("접기").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("더보기").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertFalse(uiState.isQuickCategoryExpanded)
            width = 1000.dp
        }
        homeList().performScrollToNode(hasSetTextAction())
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed().assertTextContains("유리병")
        val window = composeTestRule.onNodeWithTag("window").fetchSemanticsNode().boundsInRoot
        val body = homeList().fetchSemanticsNode().boundsInRoot
        assertEquals(720f, body.width, 1f)
        assertEquals(window.center.x, body.center.x, 1f)
    }

    private fun homeList() = composeTestRule.onNode(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
    )
}
