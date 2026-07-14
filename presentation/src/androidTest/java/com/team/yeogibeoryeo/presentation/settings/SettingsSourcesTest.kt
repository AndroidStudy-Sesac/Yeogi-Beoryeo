package com.team.yeogibeoryeo.presentation.settings

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.ADMINISTRATIVE_CODE_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_API_URL
import com.team.yeogibeoryeo.presentation.common.DISPOSAL_PORTAL_URL
import com.team.yeogibeoryeo.presentation.common.REGIONAL_WASTE_API_URL
import com.team.yeogibeoryeo.presentation.settings.detail.SourcesDetail
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsSourcesTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 출처_화면에_정부기관_비제휴_안내가_표시된다() {
        val description = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.settings_sources_non_affiliation_description)

        setSourcesContent()

        composeTestRule
            .onNodeWithText(description)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun 공식_출처_버튼은_해당_URL을_전달한다() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var openedUrl: String? = null
        val sources = listOf(
            R.string.settings_source_disposal_portal_link to DISPOSAL_PORTAL_URL,
            R.string.settings_source_disposal_api_link to DISPOSAL_API_URL,
            R.string.settings_source_regional_waste_api_link to REGIONAL_WASTE_API_URL,
            R.string.settings_source_administrative_code_link to ADMINISTRATIVE_CODE_URL,
        )

        setSourcesContent(onOpenSourceClick = { openedUrl = it })

        sources.forEach { (labelResId, expectedUrl) ->
            composeTestRule
                .onNodeWithText(context.getString(labelResId))
                .performScrollTo()
                .performClick()

            assertEquals(expectedUrl, openedUrl)
        }
    }

    private fun setSourcesContent(
        onOpenSourceClick: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MaterialTheme {
                LazyColumn {
                    item {
                        SourcesDetail(onOpenSourceClick = onOpenSourceClick)
                    }
                }
            }
        }
    }
}
