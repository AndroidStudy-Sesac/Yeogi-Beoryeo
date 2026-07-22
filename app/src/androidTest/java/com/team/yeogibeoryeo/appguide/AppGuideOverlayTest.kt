package com.team.yeogibeoryeo.appguide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppGuideOverlayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 앱_사용_가이드_카드_버튼은_단일_탭에_한_번씩_동작한다() {
        var previousClickCount = 0
        var nextClickCount = 0
        var skipClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                AppGuideOverlay(
                    step = AppGuideStep.QUICK_CATEGORY,
                    targetBounds = Rect(100f, 100f, 900f, 300f),
                    onPrevious = { previousClickCount += 1 },
                    onNext = { nextClickCount += 1 },
                    onSkip = { skipClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("다음")
            .assertIsDisplayed()
            .performTouchInput { click() }
        composeTestRule.onNodeWithText("이전")
            .assertIsDisplayed()
            .performTouchInput { click() }
        composeTestRule.onNodeWithText("건너뛰기")
            .assertIsDisplayed()
            .performTouchInput { click() }

        composeTestRule.runOnIdle {
            assertEquals(1, nextClickCount)
            assertEquals(1, previousClickCount)
            assertEquals(1, skipClickCount)
        }
    }

    @Test
    fun 앱_사용_가이드_배경은_기존_화면_터치를_차단한다() {
        var backgroundClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { backgroundClickCount += 1 },
                        modifier = Modifier.align(Alignment.TopStart),
                    ) {
                        Text("배경 버튼")
                    }
                    AppGuideOverlay(
                        step = AppGuideStep.QUICK_CATEGORY,
                        targetBounds = Rect(100f, 1_000f, 900f, 1_200f),
                        onPrevious = {},
                        onNext = {},
                        onSkip = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("배경 버튼")
            .assertIsDisplayed()
            .performTouchInput { click() }

        composeTestRule.runOnIdle {
            assertEquals(0, backgroundClickCount)
        }
    }
}
