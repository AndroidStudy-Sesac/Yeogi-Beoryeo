package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.detail.NoticeDetail
import com.team.yeogibeoryeo.presentation.settings.detail.formatNoticeDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsNoticeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 공지_조회_중에는_로딩을_표시한다() {
        setNoticeContent(SettingsNoticeUiState.Loading)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun 공지가_없으면_기존_빈_상태를_표시한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setNoticeContent(SettingsNoticeUiState.Content(emptyList()))

        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_notice_empty_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.settings_notice_empty_description))
            .assertIsDisplayed()
    }

    @Test
    fun 공지_조회에_실패하면_재시도_콜백을_호출한다() {
        val retryLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.retry_action)
        var retryCount = 0
        setNoticeContent(
            uiState = SettingsNoticeUiState.LoadFailed,
            onRetryClick = { retryCount += 1 },
        )

        composeTestRule.onNodeWithText(retryLabel).performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun 공지_목록에_제목과_게시일을_표시하고_선택_ID를_전달한다() {
        val notice = notice()
        var selectedNoticeId: String? = null
        setNoticeContent(
            uiState = SettingsNoticeUiState.Content(listOf(notice)),
            onNoticeClick = { selectedNoticeId = it },
        )

        composeTestRule.onNodeWithText(notice.title).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                R.string.settings_notice_published_date,
                formatNoticeDate(notice.publishedAtMillis),
            ),
        ).assertIsDisplayed()

        assertEquals(notice.id, selectedNoticeId)
    }

    @Test
    fun 선택한_공지의_제목과_긴_본문을_끝까지_표시한다() {
        val notice = notice().copy(
            body = (1..20).joinToString("\n") { index -> "공지 본문 $index" },
        )
        setNoticeContent(
            SettingsNoticeUiState.Content(
                notices = listOf(notice),
                selectedNoticeId = notice.id,
            ),
        )

        composeTestRule.onNodeWithText(notice.title).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("공지 본문 20", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setNoticeContent(
        uiState: SettingsNoticeUiState,
        onNoticeClick: (String) -> Unit = {},
        onRetryClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        NoticeDetail(
                            uiState = uiState,
                            onNoticeClick = onNoticeClick,
                            onRetryClick = onRetryClick,
                        )
                    }
                }
            }
        }
    }

    private fun notice(): Notice {
        return Notice(
            id = "notice-1",
            title = "서비스 업데이트 안내",
            body = "새 기능을 안내합니다.",
            publishedAtMillis = 1_754_000_000_000,
            updatedAtMillis = null,
        )
    }
}
