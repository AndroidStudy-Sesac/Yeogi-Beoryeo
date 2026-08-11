package com.team.yeogibeoryeo.presentation.operationnotice

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
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
}
