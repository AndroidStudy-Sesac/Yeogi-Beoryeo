package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import org.junit.Rule
import org.junit.Test

class RegionalWasteScheduleCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `대형폐기물 장소만 있으면 빈 요일 시간 방법 행을 표시하지 않는다`() {
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
    fun `배출장소가 없으면 장소 행을 표시하지 않는다`() {
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
}
