package com.team.yeogibeoryeo.presentation

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.common.components.AppBackButton
import com.team.yeogibeoryeo.presentation.common.components.AppTopBar
import com.team.yeogibeoryeo.presentation.search.components.ItemUsefulGuideBannerRow
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import com.team.yeogibeoryeo.presentation.search.model.itemUsefulGuideContents
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LargeTextClippingTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 작은_화면의_200퍼센트_글자에서_유용한_가이드_설명과_탭을_유지한다() {
        var clickedGuideType: ItemUsefulGuideType? = null

        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    ItemUsefulGuideBannerRow(
                        guides = itemUsefulGuideContents.take(1),
                        onGuideClick = { clickedGuideType = it.type },
                        modifier = Modifier
                            .width(320.dp)
                            .testTag(USEFUL_GUIDE_BANNER_TAG),
                        itemWidthFraction = 1f,
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(USEFUL_GUIDE_BANNER_TAG)
            .assertHeightIsAtLeast(250.dp)
        composeTestRule.onNodeWithText(
            text = "집 근처 수거함에 작은 가전을 쉽게 배출할 수 있어요.",
            useUnmergedTree = true,
        )
            .assertHeightIsAtLeast(100.dp)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("중소형 폐가전 수거함 안내")
            .performClick()

        assertEquals(ItemUsefulGuideType.SMALL_E_WASTE, clickedGuideType)
    }

    @Test
    fun 작은_화면의_200퍼센트_글자에서_개인정보처리방침_제목과_뒤로가기를_유지한다() {
        var backClickCount = 0

        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 2f),
            ) {
                MaterialTheme {
                    AppTopBar(
                        modifier = Modifier
                            .width(320.dp)
                            .testTag(APP_TOP_BAR_TAG),
                        navigationIcon = {
                            AppBackButton(onClick = { backClickCount += 1 })
                        },
                        title = {
                            Text(
                                text = "개인정보처리방침",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(APP_TOP_BAR_TAG)
            .assertHeightIsAtLeast(120.dp)
        composeTestRule.onNodeWithText("개인정보처리방침")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기")
            .performClick()

        assertEquals(1, backClickCount)
    }

    private companion object {
        const val USEFUL_GUIDE_BANNER_TAG = "useful_guide_banner"
        const val APP_TOP_BAR_TAG = "app_top_bar"
    }
}
