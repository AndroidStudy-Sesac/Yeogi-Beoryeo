package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideSummaryCard
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@Suppress("TestFunctionName")
class RegionalGuideScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 지역_가이드_즐겨찾기_버튼은_지역명과_현재_상태를_제공한다() {
        var isFavorite by mutableStateOf(false)
        val guide =
            RegionalGuideUiModel(
                regionName = "충청남도 홍성군 은하면",
                managementZoneName = "홍성군",
                targetRegionName = "은하면",
                disposalPlaceType = "문전수거",
                disposalPlaceDescription = null,
                schedules = emptyList(),
                uncollectedDays = null,
                departmentInfo = null,
            )

        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideSummaryCard(
                    guide = guide,
                    isFavorite = isFavorite,
                    onFavoriteClick = { isFavorite = !isFavorite },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("충청남도 홍성군 은하면 즐겨찾기")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "즐겨찾기 안 됨",
                ),
            )
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.Off,
                ),
            )
            .performClick()

        composeTestRule.onNodeWithContentDescription("충청남도 홍성군 은하면 즐겨찾기")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "즐겨찾기됨",
                ),
            )
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ToggleableState,
                    ToggleableState.On,
                ),
            )
    }

    @Test
    fun 선택_지역_표시_상태에서_검색_실행은_원본_검색어를_전달한다() {
        var submittedKeyword: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.Idle,
                    searchKeyword = "대전광역시 서구",
                    searchKeywordRegionNameParts = listOf("대전광역시", "서구"),
                    regionSelectorUiState = RegionSelectorUiState(),
                    onSearchKeywordChange = {},
                    onSearchClick = { keyword -> submittedKeyword = keyword },
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

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.runOnIdle {
            assertEquals("대전광역시 서구", submittedKeyword)
        }
    }

    @Test
    fun 같은_폐기물_유형의_다른_장소_일정은_하나로_묶고_상세에서_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.Success(
                        query = "은하면",
                        guide = RegionalGuideUiModel(
                            regionName = "충청남도 홍성군 은하면",
                            managementZoneName = "홍성군",
                            targetRegionName = "은하면",
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
                    searchKeyword = "은하면",
                    regionSelectorUiState = RegionSelectorUiState(
                        sidoOptions = listOf("충청남도"),
                        sigunguOptions = listOf("홍성군"),
                        eupmyeondongOptions = listOf("은하면"),
                        selectedSido = "충청남도",
                        selectedSigungu = "홍성군",
                        selectedEupmyeondong = "은하면",
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

        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("대형폐기물"))
        composeTestRule.onNodeWithText("대형폐기물").performScrollTo()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("대형폐기물").assertCountEquals(1)
        composeTestRule.onNodeWithText("2곳").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 A").assertDoesNotExist()
        composeTestRule.onNodeWithText("- 장소 B").assertDoesNotExist()

        composeTestRule.onNodeWithText("자세히 보기").performScrollTo().performClick()

        composeTestRule.onNodeWithText("- 장소 A").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("- 장소 B").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 지역_선택_변경을_누르면_검색창_포커스를_해제한다() {
        var regionSelectionStarted = false

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
                            schedules = emptyList(),
                            uncollectedDays = null,
                            departmentInfo = null,
                        ),
                    ),
                    searchKeyword = "",
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
                    onRegionSelectionStarted = {
                        regionSelectionStarted = true
                    },
                    onRegionSelectionSearchClick = {},
                    onCandidateClick = {},
                    onGuideCandidateClick = {},
                )
            }
        }

        composeTestRule.onNode(hasSetTextAction()).performClick()
        composeTestRule.onNode(hasSetTextAction()).assertIsFocused()

        composeTestRule.onNodeWithText("변경").performClick()

        composeTestRule.onNode(hasSetTextAction()).assertIsNotFocused()
        assertTrue(regionSelectionStarted)
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

    @Test
    fun 후보_목록은_클릭_직전_스크롤_위치를_저장하고_복원한다() {
        val candidates = (0 until 20).map { index ->
            candidate(label = "zone-$index")
        }
        val candidateState = RegionalGuideUiState.GuideCandidates(
            query = "candidate-query",
            reason = RegionalGuideCandidateReason.MULTIPLE_CANDIDATES,
            candidates = candidates,
            candidateListScrollPosition = RegionalGuideCandidateListScrollPosition(
                firstVisibleItemIndex = 12,
                firstVisibleItemScrollOffset = 0,
            ),
        )
        var uiState by mutableStateOf<RegionalGuideUiState>(candidateState)
        var savedScrollPosition = RegionalGuideCandidateListScrollPosition.Initial

        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = uiState,
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
                    onGuideCandidateClick = { candidate ->
                        uiState = RegionalGuideUiState.Success(
                            query = "candidate-query",
                            guide = candidate.guide,
                            canRestoreCandidates = true,
                        )
                    },
                    onCandidateListScrollPositionChange = { candidateListScrollKey, position ->
                        val currentState = uiState

                        if (
                            currentState is RegionalGuideUiState.GuideCandidates &&
                            currentState.candidateListScrollKey() == candidateListScrollKey
                        ) {
                            savedScrollPosition = position
                            uiState = currentState.copy(candidateListScrollPosition = position)
                        }
                    },
                    onRestoreCandidates = {
                        uiState = candidateState.copy(
                            candidateListScrollPosition = savedScrollPosition,
                        )
                        true
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("zone-12")
            .performClick()

        composeTestRule.runOnIdle {
            assertTrue(savedScrollPosition.firstVisibleItemIndex > 0)
            uiState = candidateState.copy(
                candidateListScrollPosition = savedScrollPosition,
            )
        }

        composeTestRule.onNodeWithText("zone-12").assertIsDisplayed()
    }

    @Test
    fun 가로_오류_화면에서_다시_시도_버튼을_실행할_수_있다() {
        var retryCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(width = 800.dp, height = 360.dp)
                ) {
                    RegionalGuideScreen(
                        uiState = RegionalGuideUiState.Error(
                            query = "서구",
                            errorType = RegionalGuideErrorType.NETWORK,
                        ),
                        searchKeyword = "서구",
                        regionSelectorUiState = RegionSelectorUiState(),
                        onSearchKeywordChange = {},
                        onSearchClick = {},
                        onRetryClick = { retryCount += 1 },
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
        }

        composeTestRule.onNodeWithText("다시 시도")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, retryCount)
        }
    }
    @Test
    fun 가이드가_없을_때_공공_안내_CTA와_지역_재선택_동작을_제공한다() {
        var publicNoticeClickCount = 0
        var regionSelectionStarted = false

        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.Empty(
                        query = "서울특별시 종로구",
                        titleResId = R.string.regional_guide_empty_info_not_found_title,
                        messageResId = R.string.regional_guide_empty_info_not_found_message,
                        action = RegionalGuideEmptyActionUiModel(
                            type = RegionalGuideEmptyActionType.SELECT_REGION,
                            labelResId = R.string.regional_guide_empty_action_select_region,
                        ),
                        showsPublicNoticeCta = true,
                    ),
                    searchKeyword = "서울특별시 종로구",
                    regionSelectorUiState = RegionSelectorUiState(),
                    onSearchKeywordChange = {},
                    onSearchClick = {},
                    onRetryClick = {},
                    onEmptySearchActionClick = {},
                    onSidoSelected = {},
                    onSigunguSelected = {},
                    onEupmyeondongSelected = {},
                    onRegionSelectionStarted = { regionSelectionStarted = true },
                    onRegionSelectionSearchClick = {},
                    onCandidateClick = {},
                    onGuideCandidateClick = {},
                    onPublicNoticeClick = { publicNoticeClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("공공 안내에서 지자체 안내 링크 보기")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithContentDescription(
            "공공 안내에서 지자체 안내 링크 보기, 외부 공공 안내 페이지로 이동"
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("지역 다시 선택하기")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, publicNoticeClickCount)
            assertTrue(regionSelectionStarted)
        }
    }

    @Test
    fun 입력_오류_결과에는_공공_안내_CTA를_노출하지_않는다() {
        composeTestRule.setContent {
            MaterialTheme {
                RegionalGuideScreen(
                    uiState = RegionalGuideUiState.Empty(
                        query = "",
                        titleResId = R.string.regional_guide_empty_blank_keyword_title,
                        messageResId = R.string.regional_guide_empty_blank_keyword_message,
                        action = RegionalGuideEmptyActionUiModel(
                            type = RegionalGuideEmptyActionType.SEARCH_AGAIN,
                            labelResId = R.string.regional_guide_empty_action_search_again,
                        ),
                    ),
                    searchKeyword = "",
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

        composeTestRule.onNodeWithText("공공 안내에서 지자체 안내 링크 보기")
            .assertDoesNotExist()
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
