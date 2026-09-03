package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.presentation.R
import org.junit.Rule
import org.junit.Test

class ItemSearchHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 읽지_않은_공지가_있으면_설정_버튼에_미확인_상태를_제공한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchHeader(
                    onSettingsClick = {},
                    hasUnreadNotices = true,
                )
            }
        }

        composeTestRule
            .onNode(
                hasContentDescription(context.getString(R.string.settings_action)) and
                    hasStateDescription(
                        context.getString(R.string.settings_notice_unread_state),
                    ),
            )
            .assertIsDisplayed()
    }
}
