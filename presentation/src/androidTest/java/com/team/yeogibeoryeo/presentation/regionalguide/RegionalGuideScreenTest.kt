package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import org.junit.Rule
import org.junit.Test

class RegionalGuideScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 같은_폐기물_유형의_다른_장소_일정을_하나로_묶고_상세에서_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.Success(
                        query = "장암면",
                        guide = RegionalGuideUiModel(
                            regionName = "충청남도 부여군 장암면",
                            managementZoneName = "부여군",
                            targetRegionName = "장암면",
                            disposalPlaceType = "문전수거",
                            disposalPlaceDescription = null,
                            schedules = listOf(
                                RegionalWasteScheduleUiModel(
                                    wasteTypeName = "대형폐기물",
                                    disposalPlace = "장소 A",
                                ),
                                RegionalWasteScheduleUiModel(
                                    wasteTypeName = "대형폐기물",
                                    disposalPlace = "장소 B",
                                ),
                            ),
                            uncollectedDays = null,
                            departmentInfo = null,
                        ),
                    ),
                    searchKeyword = "장암면",
                    regionSelectorUiState = RegionSelectorUiState(
                        sidoOptions = listOf("충청남도"),
                        sigunguOptions = listOf("부여군"),
                        eupmyeondongOptions = listOf("장암면"),
                        selectedSido = "충청남도",
                        selectedSigungu = "부여군",
                        selectedEupmyeondong = "장암면",
                    ),
                    onSearchKeywordChange = {},
                    onSearchClick = {},
                    onRetryClick = {},
                    onEmptySearchActionClick = {},
                    onSidoSelected = {},
                    onSigunguSelected = {},
                    onEupmyeondongSelected = {},
                    onRegionSelectionSearchClick = {},
                    onCandidateClick = {},
                    onGuideCandidateClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("대형폐기물").assertCountEquals(1)
        composeTestRule.onNodeWithText("2곳").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 A").assertDoesNotExist()
        composeTestRule.onNodeWithText("- 장소 B").assertDoesNotExist()

        composeTestRule.onNodeWithText("자세히 보기").performClick()

        composeTestRule.onNodeWithText("- 장소 A").assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 B").assertIsDisplayed()
    }
    @Test
    fun 후보_목록은_저장된_스크롤_위치에서_시작한다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.GuideCandidates(
                        query = "candidate-query",
                        reason = RegionalGuideCandidateReason.MULTIPLE_CANDIDATES,
                        candidates = (0 until 20).map { index ->
                            candidate(label = "zone-$index")
                        },
                        candidateListScrollPosition = RegionalGuideCandidateListScrollPosition(
                            firstVisibleItemIndex = 8,
                            firstVisibleItemScrollOffset = 0,
                        ),
                    ),
                    searchKeyword = "candidate-query",
                    regionSelectorUiState = RegionSelectorUiState(),
                    onSearchKeywordChange = {},
                    onSearchClick = {},
                    onRetryClick = {},
                    onEmptySearchActionClick = {},
                    onSidoSelected = {},
                    onSigunguSelected = {},
                    onEupmyeondongSelected = {},
                    onRegionSelectionSearchClick = {},
                    onCandidateClick = {},
                    onGuideCandidateClick = {},
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("zone-0").assertDoesNotExist()
        composeTestRule.onNodeWithText("zone-8").assertIsDisplayed()
    }
}

private fun candidate(label: String): RegionalGuideCandidateUiModel =
    RegionalGuideCandidateUiModel(
        guide = RegionalGuideUiModel(
            regionName = "test-region",
            managementZoneName = null,
            targetRegionName = label,
            disposalPlaceType = null,
            disposalPlaceDescription = null,
            schedules = emptyList(),
            uncollectedDays = null,
            departmentInfo = null,
        ),
        sido = "test-sido",
        sigungu = "test-sigungu",
        eupmyeondong = null,
    )
