package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.input.ImeAction
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import com.team.yeogibeoryeo.presentation.search.model.itemUsefulGuideContents
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemSearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 초기_상태에서는_분리배출_분류를_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("여기 버려").assertIsDisplayed()
        composeTestRule.onNodeWithText("품목 검색, 수거 장소, 지역별 배출 정보를 한 곳에서 확인하세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("안내 사항").assertIsDisplayed()
        composeTestRule.onNodeWithText("중소형 폐가전 수거함 안내").assertIsDisplayed()
        composeTestRule.onNodeWithText("분리배출 분류").assertIsDisplayed()
        composeTestRule.onNodeWithText("종이").assertIsDisplayed()
    }

    @Test
    fun useful_guide_목록에는_대표_분류_안내를_포함한다() {
        assertEquals(
            listOf(
                ItemUsefulGuideType.SMALL_E_WASTE,
                ItemUsefulGuideType.REGIONAL_GUIDE,
                ItemUsefulGuideType.REPRESENTATIVE_CATEGORY,
                ItemUsefulGuideType.ITEM_DICTIONARY,
            ),
            itemUsefulGuideContents.map { it.type },
        )
    }

    @Test
    fun 빈_결과_상태에서는_빈_상태_문구를_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(hasSearched = true),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("검색 결과가 없어요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("다른 이름으로 다시 검색해보세요.").assertIsDisplayed()
    }

    @Test
    fun 결과_상태에서는_결과_건수와_품목을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState =
                        ItemSearchUiState(
                            query = "유리",
                            hasSearched = true,
                            guides = listOf(sampleGuide("유리병")),
                        ),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("유리병").assertIsDisplayed()
    }

    @Test
    fun 검색_결과가_남아_있어도_앱_사용_가이드를_재실행하면_초기_홈을_보여준다() {
        var isAppGuideActive by mutableStateOf(false)
        var appGuideTarget by mutableStateOf<ItemSearchGuideTarget?>(null)

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState =
                        ItemSearchUiState(
                            query = "유리",
                            hasSearched = true,
                            guides = listOf(sampleGuide("유리병")),
                        ),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                    isAppGuideActive = isAppGuideActive,
                    appGuideTarget = appGuideTarget,
                )
            }
        }

        composeTestRule.onNodeWithText("유리병").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()

        composeTestRule.runOnIdle {
            isAppGuideActive = true
            appGuideTarget = ItemSearchGuideTarget.SEARCH
        }

        composeTestRule.onNodeWithText("분리배출 분류").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("뒤로가기").assertCountEquals(0)

        composeTestRule.runOnIdle {
            appGuideTarget = null
        }

        composeTestRule.onNodeWithText("분리배출 분류").assertIsDisplayed()
        composeTestRule.onAllNodesWithContentDescription("뒤로가기").assertCountEquals(0)
    }

    @Test
    fun 검색_결과_화면에서_뒤로가기_버튼을_누르면_검색을_초기화한다() {
        var backClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState =
                        ItemSearchUiState(
                            query = "유리",
                            hasSearched = true,
                            guides = listOf(sampleGuide("유리병")),
                        ),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                    onBackClick = { backClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("품목 검색").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, backClickCount)
    }

    @Test
    fun 검색_결과에서_초기_상태로_돌아오면_하단_네비게이션을_다시_보이게_요청한다() {
        var uiState by mutableStateOf(
            ItemSearchUiState(
                query = "유리",
                hasSearched = true,
                guides = listOf(sampleGuide("유리병")),
            ),
        )
        var isBottomBarVisible = false

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = uiState,
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                    onBottomBarVisibilityChanged = { isBottomBarVisible = it },
                )
            }
        }

        composeTestRule.runOnIdle {
            isBottomBarVisible = false
            uiState = ItemSearchUiState()
        }

        composeTestRule.runOnIdle {
            assertEquals(true, isBottomBarVisible)
        }
    }

    @Test
    fun 결과_카드는_대표_배출방법만_보여준다() {
        val primaryInstruction = DisposalInstruction(method = "재활용폐기물")
        val secondaryInstruction = DisposalInstruction(method = "대형폐기물")
        val guide =
            DisposalItemGuide(
                id = "cast-iron-pot",
                name = "무쇠 주물냄비",
                category = DisposalCategory.METAL,
                subCategory = null,
                instructions = listOf(primaryInstruction, secondaryInstruction),
                steps = emptyList(),
                cautions = emptyList(),
                tip = null,
                isRecyclable = true,
                relatedSpotTypes = emptyList(),
            )

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState =
                        ItemSearchUiState(
                            hasSearched = true,
                            guides = listOf(guide),
                        ),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText(primaryInstruction.method).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(secondaryInstruction.method).assertCountEquals(0)
    }

    @Test
    fun 에러_상태에서는_에러_리소스_문구를_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(errorMessageResId = R.string.search_load_failed_message),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("검색 결과를 불러오지 못했어요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("잠시 후 다시 시도해주세요.").assertIsDisplayed()
    }

    @Test
    fun 결과_카드를_누르면_선택한_가이드를_전달한다() {
        var clickedGuide: DisposalItemGuide? = null
        val guide = sampleGuide("플라스틱병")

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(hasSearched = true, guides = listOf(guide)),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = { clickedGuide = it },
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("플라스틱병").performClick()

        assertEquals(guide, clickedGuide)
    }

    @Test
    fun 검색_아이콘을_누르면_검색_콜백을_호출한다() {
        var searchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(query = "유리"),
                    onQueryChange = {},
                    onSearchClick = { searchClickCount += 1 },
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("검색").performClick()

        assertEquals(1, searchClickCount)
    }

    @Test
    fun 로딩_상태에서는_로딩_인디케이터를_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(isLoading = true, hasSearched = true),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("로딩 중").assertIsDisplayed()
    }

    @Test
    fun 키보드_검색_액션을_실행하면_검색_콜백을_호출한다() {
        var searchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(query = "유리"),
                    onQueryChange = {},
                    onSearchClick = { searchClickCount += 1 },
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNode(hasImeAction(ImeAction.Search)).performImeAction()

        assertEquals(1, searchClickCount)
    }

    @Test
    fun quick_category를_누르면_카테고리_클릭_콜백을_호출한다() {
        var clickedCategory: RepresentativeGuideCategory? = null

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(isQuickCategoryExpanded = true),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = { clickedCategory = it },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(RepresentativeGuideCategory.PLASTIC.displayName)
            .performScrollTo()
            .performClick()

        assertEquals(RepresentativeGuideCategory.PLASTIC, clickedCategory)
    }

    @Test
    fun quick_category는_처음에_adaptive_접힘_목록과_더보기를_보여주고_누르면_전체를_펼친다() {
        composeTestRule.setContent {
            var uiState by mutableStateOf(ItemSearchUiState())
            MaterialTheme {
                ItemSearchScreen(
                    uiState = uiState,
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                    onQuickCategoryMoreClick = { count, _, _ ->
                        uiState =
                            uiState.copy(
                                isQuickCategoryExpanded = true,
                                quickCategoryFixedCollapsedItemCount = count,
                            )
                    },
                    onQuickCategoryCollapseClick = {
                        uiState = uiState.copy(isQuickCategoryExpanded = false)
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("더보기")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(RepresentativeGuideCategory.METAL.displayName)
            .assertCountEquals(0)

        composeTestRule.onNodeWithContentDescription("더보기")
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(RepresentativeGuideCategory.METAL.displayName)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("접기")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("접기").performClick()

        composeTestRule.onAllNodesWithText(RepresentativeGuideCategory.METAL.displayName)
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("더보기").assertIsDisplayed()
    }

    @Test
    fun 펼친_quick_category를_눌러도_펼침_상태를_유지한다() {
        composeTestRule.setContent {
            var uiState by mutableStateOf(ItemSearchUiState())
            MaterialTheme {
                ItemSearchScreen(
                    uiState = uiState,
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                    onQuickCategoryMoreClick = { count, _, _ ->
                        uiState =
                            uiState.copy(
                                isQuickCategoryExpanded = true,
                                quickCategoryFixedCollapsedItemCount = count,
                            )
                    },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("더보기")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithContentDescription(RepresentativeGuideCategory.METAL.displayName)
            .performScrollTo()
            .performClick()

        composeTestRule.onNodeWithText(RepresentativeGuideCategory.METAL.displayName)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("접기").assertIsDisplayed()
    }

    @Test
    fun useful_guide_배너를_누르면_선택한_안내를_전달한다() {
        var clickedGuideType: ItemUsefulGuideType? = null

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onUsefulGuideClick = { clickedGuideType = it.type },
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("중소형 폐가전 수거함 안내").performClick()

        assertEquals(ItemUsefulGuideType.SMALL_E_WASTE, clickedGuideType)
    }

    @Test
    fun small_e_waste_안내_상세는_공식_안내_기준의_배출_조건을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemUsefulGuideRoute(
                    guideType = ItemUsefulGuideType.SMALL_E_WASTE,
                    onBackClick = {},
                    onSmallEWasteClick = {},
                    onFreePickupGuideClick = {},
                    onOfficialSiteClick = {},
                    onRegionalGuideClick = {},
                    onItemSearchClick = {},
                    onBottomBarVisibilityChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("어떻게 배출하나요?").assertIsDisplayed()
        composeTestRule.onNodeWithText("집 근처에 수거함이 있으면 가까운 장소에 가져가 배출할 수 있습니다.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("배터리 분리가 가능한 제품은 배터리를 분리한 뒤 배출하세요.")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("무상방문수거 안내 보기")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.swipeUpUntilTextExists("관련 사이트")
        composeTestRule.onNodeWithText("관련 사이트")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("생활폐기물 분리배출 누리집 보기")
            .assertIsDisplayed()
    }

    @Test
    fun small_e_waste_안내_상세에서_외부_링크_CTA를_전달한다() {
        var clickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                ItemUsefulGuideRoute(
                    guideType = ItemUsefulGuideType.SMALL_E_WASTE,
                    onBackClick = {},
                    onSmallEWasteClick = {},
                    onFreePickupGuideClick = { clickCount += 1 },
                    onOfficialSiteClick = {},
                    onRegionalGuideClick = {},
                    onItemSearchClick = {},
                    onBottomBarVisibilityChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithText("무상방문수거 안내 보기")
            .performScrollTo()
            .performClick()

        assertEquals(1, clickCount)
    }

    private fun sampleGuide(name: String): DisposalItemGuide =
        DisposalItemGuide(
            id = name,
            name = name,
            category = DisposalCategory.GLASS,
            subCategory = null,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.swipeUpUntilTextExists(
        text: String,
    ) {
        repeat(5) {
            if (onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            onRoot().performTouchInput { swipeUp() }
        }
    }
}
