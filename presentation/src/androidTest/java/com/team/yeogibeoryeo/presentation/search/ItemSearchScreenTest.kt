package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.input.ImeAction
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemSearchScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 초기_상태에서는_퀵_카테고리를_보여준다() {
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

        composeTestRule.onNodeWithText("분리배출 분류").assertIsDisplayed()
        composeTestRule.onNodeWithText("종이팩").assertIsDisplayed()
        composeTestRule.onNodeWithText("기타").performScrollTo().assertIsDisplayed()
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

        composeTestRule.onNodeWithText("검색 결과 1건").assertIsDisplayed()
        composeTestRule.onNodeWithText("유리병").assertIsDisplayed()
    }

    @Test
    fun 결과_카드는_모든_배출방법을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState =
                        ItemSearchUiState(
                            hasSearched = true,
                            guides =
                                listOf(
                                    DisposalItemGuide(
                                        id = "cast-iron-pot",
                                        name = "무쇠 주물냄비",
                                        category = DisposalCategory.METAL,
                                        subCategory = null,
                                        instructions =
                                            listOf(
                                                DisposalInstruction(method = "재활용폐기물"),
                                                DisposalInstruction(method = "대형폐기물"),
                                            ),
                                        steps = emptyList(),
                                        cautions = emptyList(),
                                        tip = null,
                                        isRecyclable = true,
                                        relatedSpotTypes = emptyList(),
                                    ),
                                ),
                        ),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("재활용폐기물").assertIsDisplayed()
        composeTestRule.onNodeWithText("대형폐기물").assertIsDisplayed()
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
        val guide = sampleGuide("유리병")

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

        composeTestRule.onNodeWithText("유리병").performClick()

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
    fun 퀵_카테고리를_누르면_카테고리_콜백을_호출한다() {
        var clickedCategory: RepresentativeGuideCategory? = null

        composeTestRule.setContent {
            MaterialTheme {
                ItemSearchScreen(
                    uiState = ItemSearchUiState(),
                    onQueryChange = {},
                    onSearchClick = {},
                    onGuideClick = {},
                    onQuickCategoryClick = { clickedCategory = it },
                )
            }
        }

        composeTestRule.onNodeWithText("비닐").performScrollTo().performClick()

        assertEquals(RepresentativeGuideCategory.VINYL, clickedCategory)
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
}
