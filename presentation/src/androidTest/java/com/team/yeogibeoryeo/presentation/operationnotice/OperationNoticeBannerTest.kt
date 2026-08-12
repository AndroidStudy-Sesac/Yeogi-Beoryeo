package com.team.yeogibeoryeo.presentation.operationnotice

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OperationNoticeBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun actionUrl_열기에_실패해도_공지_화면을_유지한다() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUriHandler provides
                    object : UriHandler {
                        override fun openUri(uri: String) {
                            throw IllegalArgumentException("unsupported uri")
                        }
                    },
            ) {
                MaterialTheme {
                    OperationNoticeBanner(
                        notice =
                            OperationNoticeUiModel(
                                id = "notice",
                                severity = OperationNoticeSeverity.INFO,
                                title = "운영 공지",
                                message = "공지 내용",
                                actionLabel = "자세히 보기",
                                actionUrl = "https://www.data.go.kr",
                            ),
                        onDismiss = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("자세히 보기").performClick()

        composeTestRule.onNodeWithText("운영 공지").assertIsDisplayed()
        composeTestRule.onNodeWithText("공지 내용").assertIsDisplayed()
    }

    @Test
    fun dismiss_버튼은_최소_터치_영역을_유지한다() {
        var dismissCount = 0
        val dismissLabel = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getString(R.string.operation_notice_dismiss_action)

        composeTestRule.setContent {
            MaterialTheme {
                OperationNoticeBanner(
                    notice =
                        OperationNoticeUiModel(
                            id = "notice",
                            severity = OperationNoticeSeverity.WARNING,
                            title = "운영 공지",
                            message = "공지 내용",
                            actionLabel = null,
                            actionUrl = null,
                        ),
                    onDismiss = { dismissCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(dismissLabel)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, dismissCount)
    }
}
