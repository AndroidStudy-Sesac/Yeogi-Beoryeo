package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import org.junit.Rule
import org.junit.Test

class RegionalWasteScheduleCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 대형폐기물_장소만_있으면_빈_요일_시간_방법_행을_표시하지_않는다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalWasteScheduleCard(
                    schedule = RegionalWasteScheduleUiModel(
                        wasteTypeName = "대형폐기물",
                        disposalPlace = "대형폐기물 지정 장소",
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("대형폐기물").assertIsDisplayed()
        composeTestRule.onNodeWithText("장소").assertIsDisplayed()
        composeTestRule.onNodeWithText("대형폐기물 지정 장소").assertIsDisplayed()
        composeTestRule.onNodeWithText("요일").assertDoesNotExist()
        composeTestRule.onNodeWithText("시간").assertDoesNotExist()
        composeTestRule.onNodeWithText("방법").assertDoesNotExist()
    }

    @Test
    fun 배출장소가_없으면_장소_행을_표시하지_않는다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalWasteScheduleCard(
                    schedule = RegionalWasteScheduleUiModel(
                        wasteTypeName = "일반쓰레기",
                        disposalDays = "월, 수, 금",
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("일반쓰레기").assertIsDisplayed()
        composeTestRule.onNodeWithText("요일").assertIsDisplayed()
        composeTestRule.onNodeWithText("월, 수, 금").assertIsDisplayed()
        composeTestRule.onNodeWithText("장소").assertDoesNotExist()
    }

    @Test
    fun 배출장소가_여러_개이면_개수를_보여주고_펼쳐서_전체_장소를_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalWasteScheduleCard(
                    schedule = RegionalWasteScheduleUiModel(
                        wasteTypeName = "대형폐기물",
                    ),
                    disposalPlaces = listOf("장소 A", "장소 B"),
                )
            }
        }

        composeTestRule.onNodeWithText("대형폐기물").assertIsDisplayed()
        composeTestRule.onNodeWithText("장소").assertIsDisplayed()
        composeTestRule.onNodeWithText("2곳").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 A").assertDoesNotExist()
        composeTestRule.onNodeWithText("- 장소 B").assertDoesNotExist()

        composeTestRule.onNodeWithText("자세히 보기").performClick()

        composeTestRule.onNodeWithText("- 장소 A").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 B").assertIsDisplayed()
        composeTestRule.onNodeWithText("접기").assertIsDisplayed()
    }

    @Test
    fun 배출장소_그룹이_바뀌면_펼침_상태를_초기화한다() {
        var disposalPlaces by mutableStateOf(listOf("장소 A", "장소 B"))

        composeTestRule.setContent {
            MaterialTheme {
                RegionalWasteScheduleCard(
                    schedule = RegionalWasteScheduleUiModel(
                        wasteTypeName = "대형폐기물",
                    ),
                    disposalPlaces = disposalPlaces,
                )
            }
        }

        composeTestRule.onNodeWithText("자세히 보기").performClick()
        composeTestRule.onNodeWithText("- 장소 A").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 B").assertIsDisplayed()

        composeTestRule.runOnIdle {
            disposalPlaces = listOf("장소 C", "장소 D")
        }

        composeTestRule.onNodeWithText("자세히 보기").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 C").assertDoesNotExist()
        composeTestRule.onNodeWithText("- 장소 D").assertDoesNotExist()
    }
}
