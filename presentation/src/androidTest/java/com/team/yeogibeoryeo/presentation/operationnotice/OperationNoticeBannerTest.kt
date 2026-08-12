package com.team.yeogibeoryeo.presentation.operationnotice

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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

    @Test
    fun 글자_크기를_확대해도_공지_내용을_줄이지_않는다() {
        val title = "수거 장소 검색 서비스 일시 장애 안내"
        val message =
            "공공데이터포털 제공 API 연결 문제로 수거 장소 검색이 원활하지 않을 수 있습니다. " +
                "잠시 후 다시 시도해 주세요."

        composeTestRule.setContent {
            val density = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    OperationNoticeBanner(
                        notice =
                            OperationNoticeUiModel(
                                id = "notice",
                                severity = OperationNoticeSeverity.WARNING,
                                title = title,
                                message = message,
                                actionLabel = null,
                                actionUrl = null,
                            ),
                        onDismiss = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("$title, $message")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(200.dp)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun 공지_내용과_닫기_버튼을_각각_탐색할_수_있다() {
        val title = "운영 공지"
        val message = "공지 내용"
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
                            title = title,
                            message = message,
                            actionLabel = null,
                            actionUrl = null,
                        ),
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("$title, $message").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(dismissLabel).assertIsDisplayed()
    }
}
