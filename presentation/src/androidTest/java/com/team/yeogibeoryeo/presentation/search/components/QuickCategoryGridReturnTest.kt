package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickCategoryGridReturnTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 펼친_목록_끝에서_창_높이를_늘린_뒤_접으면_복원된_위치에서_개수를_늘린다() {
        assertCollapsedCategoriesAfterResize(328.dp, 400.dp, 328.dp, 600.dp, 7, 11)
    }

    @Test
    fun 고정_개수가_새_열_수로_나누어져도_창_폭_변경_후_접으면_다시_계산한다() {
        assertCollapsedCategoriesAfterResize(600.dp, 430.dp, 328.dp, 430.dp, 11, 7)
    }

    @Test
    fun 창_크기가_같으면_목록_끝에서_접어도_기존_개수와_스크롤을_유지한다() {
        assertCollapsedCategoriesAfterResize(328.dp, 400.dp, 328.dp, 400.dp, 7, 7)
    }

    private fun assertCollapsedCategoriesAfterResize(
        initialWidth: Dp,
        initialHeight: Dp,
        resizedWidth: Dp,
        resizedHeight: Dp,
        initialCategoryCount: Int,
        resizedCategoryCount: Int,
    ) {
        var width by mutableStateOf(initialWidth)
        var height by mutableStateOf(initialHeight)
        var isExpanded by mutableStateOf(false)
        var fixedCount by mutableIntStateOf(0)
        var viewportBottom by mutableIntStateOf(0)
        var viewportSize by mutableStateOf(IntSize.Zero)
        var restoreVersion by mutableIntStateOf(0)
        var handledRestoreVersion by mutableIntStateOf(0)
        lateinit var listState: LazyListState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme {
                    listState = rememberLazyListState()
                    LaunchedEffect(restoreVersion) {
                        if (restoreVersion > 0) {
                            listState.scrollToItem(0)
                            handledRestoreVersion = restoreVersion
                        }
                    }
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(width).height(height)) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().testTag("list")
                                    .onGloballyPositioned { coordinates ->
                                        if (viewportSize != coordinates.size) {
                                            viewportSize = coordinates.size
                                            fixedCount = 0
                                        }
                                        viewportBottom = coordinates.positionInRoot().y.toInt() +
                                            coordinates.size.height
                                    },
                            ) {
                                item { Spacer(Modifier.height(100.dp)) }
                                item {
                                    QuickCategoryGrid(
                                        onCategoryClick = {},
                                        isExpanded = isExpanded,
                                        fixedCollapsedItemCount = fixedCount,
                                        viewportBottomInRootPx = viewportBottom,
                                        collapsedMeasurementVersion = handledRestoreVersion,
                                        onMoreClick = {
                                            fixedCount = it
                                            isExpanded = true
                                        },
                                        onCollapseClick = {
                                            isExpanded = false
                                            restoreVersion += 1
                                        },
                                        itemContent = { _, _ ->
                                            Box(Modifier.height(140.dp).testTag("category"))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("category").assertCountEquals(initialCategoryCount)
        composeTestRule.onNodeWithContentDescription("더보기").assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("list").performScrollToNode(hasContentDescription("접기"))
        composeTestRule.onNodeWithContentDescription("접기").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            width = resizedWidth
            height = resizedHeight
        }
        composeTestRule.onNodeWithTag("list").performScrollToNode(hasContentDescription("접기"))
        composeTestRule.onNodeWithContentDescription("접기").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("접기").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("category").assertCountEquals(resizedCategoryCount)
        composeTestRule.onNodeWithContentDescription("더보기").assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertEquals(false, isExpanded)
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun 설정_복귀_후_접기를_누르면_목록이_접히고_더보기를_보여준다() {
        var requestVersion by mutableIntStateOf(0)
        var isExpanded by mutableStateOf(true)

        composeTestRule.setContent {
            MaterialTheme {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                ) {
                    item {
                        QuickCategoryGrid(
                            onCategoryClick = {},
                            isExpanded = isExpanded,
                            fixedCollapsedItemCount = 4,
                            viewportBottomInRootPx = 3000,
                            onCollapseClick = { isExpanded = false },
                            collapseBringIntoViewRequestVersion = requestVersion,
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("접기").assertIsNotDisplayed()

        composeTestRule.runOnIdle {
            requestVersion += 1
        }

        composeTestRule.onNodeWithContentDescription("접기").assertIsDisplayed()
            .performClick()

        composeTestRule.onNodeWithContentDescription("더보기").assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(RepresentativeGuideCategory.OTHER.displayName)
            .assertDoesNotExist()
    }
}
