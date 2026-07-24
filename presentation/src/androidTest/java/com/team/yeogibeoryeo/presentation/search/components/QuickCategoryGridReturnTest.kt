package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class QuickCategoryGridReturnTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 설정_복귀_요청을_받으면_펼친_목록의_접기를_화면에_보여준다() {
        var requestVersion by mutableIntStateOf(0)

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
                            isExpanded = true,
                            fixedCollapsedItemCount = 8,
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
    }
}
