package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.detail.PrivacyPolicyDetail
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPrivacyPolicyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 설정_목록에서_개인정보처리방침을_선택할_수_있다() {
        var selectedType: SettingsDetailType? = null
        val title = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.settings_privacy_policy_title)

        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    onBackClick = {},
                    onDetailClick = { selectedType = it },
                )
            }
        }

        composeTestRule
            .onNodeWithText(title)
            .performScrollTo()
            .performClick()

        assertEquals(SettingsDetailType.PrivacyPolicy, selectedType)
    }

    @Test
    fun 개인정보처리방침_열기_버튼은_콜백을_호출한다() {
        var clickCount = 0
        val action = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.settings_open_privacy_policy)

        composeTestRule.setContent {
            MaterialTheme {
                PrivacyPolicyDetail(onOpenPrivacyPolicyClick = { clickCount += 1 })
            }
        }

        composeTestRule.onNodeWithText(action)
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clickCount)
    }
}
