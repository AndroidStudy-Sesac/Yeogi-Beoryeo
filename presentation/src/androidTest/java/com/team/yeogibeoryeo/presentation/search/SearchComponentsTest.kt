package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.presentation.search.components.DisposalCategoryChip
import com.team.yeogibeoryeo.presentation.search.components.DisposalGuideMetadataChips
import com.team.yeogibeoryeo.presentation.search.components.DisposalItemCard
import com.team.yeogibeoryeo.presentation.search.components.DisposalSubCategoryChip
import com.team.yeogibeoryeo.presentation.search.components.EmptySearchResult
import com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGrid
import com.team.yeogibeoryeo.presentation.search.components.SectionCard
import com.team.yeogibeoryeo.presentation.search.components.SubGuideSection
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 카테고리와_하위_카테고리_칩은_라벨을_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                DisposalCategoryChip(DisposalCategory.PAPER)
                DisposalSubCategoryChip(DisposalSubCategory.MILK_CARTON)
            }
        }

        composeTestRule.onNodeWithText("종이").assertIsDisplayed()
        composeTestRule.onNodeWithText("우유팩").assertIsDisplayed()
    }

    @Test
    fun 가이드_메타데이터_칩은_배출_성격을_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                DisposalGuideMetadataChips(sampleGuide())
            }
        }

        composeTestRule.onNodeWithContentDescription("재활용 분리배출").assertIsDisplayed()
        composeTestRule.onNodeWithText("유리병").assertIsDisplayed()
    }

    @Test
    fun 생활계_유해폐기물_가이드_메타데이터_칩은_전용_수거를_텍스트로_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                DisposalGuideMetadataChips(
                    sampleGuide(
                        name = "폐의약품",
                        category = DisposalCategory.HAZARDOUS,
                        instructions = listOf(DisposalInstruction(method = "전용수거함에 배출합니다.")),
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("전용 수거").assertIsDisplayed()
        composeTestRule.onNodeWithText("생활계 유해폐기물").assertIsDisplayed()
    }

    @Test
    fun 배출_성격이_없는_카테고리는_가이드_메타데이터_칩에서_성격_칩을_숨긴다() {
        val hiddenRouteCategories =
            listOf(
                DisposalCategory.FOOD_WASTE,
                DisposalCategory.NON_COMBUSTIBLE,
                DisposalCategory.CONSTRUCTION_WASTE,
                DisposalCategory.GENERAL,
                DisposalCategory.OTHER,
            )

        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    hiddenRouteCategories.forEach { category ->
                        DisposalGuideMetadataChips(
                            sampleGuide(
                                name = category.displayName,
                                category = category,
                                instructions = emptyList(),
                            ),
                        )
                    }
                }
            }
        }

        composeTestRule.onAllNodesWithContentDescription("재활용 분리배출").assertCountEquals(0)
        composeTestRule.onAllNodesWithContentDescription("신고 후 배출").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("전용 수거").assertCountEquals(0)
        hiddenRouteCategories.forEach { category ->
            composeTestRule.onNodeWithText(category.displayName).assertIsDisplayed()
        }
    }

    @Test
    fun 빈_상태_컴포넌트는_제목과_설명을_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                EmptySearchResult(title = "없어요", description = "다시 검색하세요")
            }
        }

        composeTestRule.onNodeWithText("없어요").assertIsDisplayed()
        composeTestRule.onNodeWithText("다시 검색하세요").assertIsDisplayed()
    }

    @Test
    fun 섹션_카드는_번호_목록을_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                SectionCard(title = "배출 절차", lines = listOf("비웁니다.", "헹굽니다."), numbered = true)
            }
        }

        composeTestRule.onNodeWithText("1. 비웁니다.").assertIsDisplayed()
        composeTestRule.onNodeWithText("2. 헹굽니다.").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("1. 비\u200B웁\u200B니\u200B다\u200B.").assertCountEquals(0)
    }

    @Test
    fun 세부_분류_섹션은_하위_가이드를_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                SubGuideSection(
                    title = "세부 분류",
                    subGuides = listOf(DisposalSubGuide("골판지", "접어서 배출합니다.")),
                )
            }
        }

        composeTestRule.onNodeWithText("세부 분류").assertIsDisplayed()
        composeTestRule.onNodeWithText("골판지").assertIsDisplayed()
        composeTestRule.onNodeWithText("접어서 배출합니다.").assertIsDisplayed()
    }

    @Test
    fun 결과_카드는_종량제_라벨을_사용자_표현으로_바꿔서_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                DisposalItemCard(
                    guide = sampleGuide(instructions = listOf(DisposalInstruction(method = "일반종량제폐기물"))),
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("종량제봉투").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("재활용 분리배출").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("일반종량제폐기물").assertCountEquals(0)
    }

    @Test
    fun 결과_카드를_누르면_클릭_콜백을_호출한다() {
        var clickCount = 0
        composeTestRule.setContent {
            MaterialTheme {
                DisposalItemCard(guide = sampleGuide(), onClick = { clickCount += 1 })
            }
        }

        composeTestRule.onNodeWithText("유리병").performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun 퀵_카테고리_그리드는_전달한_카테고리만_표시하고_클릭을_전달한다() {
        var clickedCategory: RepresentativeGuideCategory? = null
        composeTestRule.setContent {
            MaterialTheme {
                QuickCategoryGrid(
                    categories = listOf(
                        RepresentativeGuideCategory.PAPER,
                        RepresentativeGuideCategory.VINYL
                    ),
                    onCategoryClick = { clickedCategory = it },
                )
            }
        }

        composeTestRule.onNodeWithText("종이").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("비닐류").performClick()

        assertEquals(RepresentativeGuideCategory.VINYL, clickedCategory)
    }

    @Test
    fun 퀵_카테고리_라벨은_접근성_텍스트로_zero_width_space_없는_원문을_제공한다() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickCategoryGrid(
                    categories = listOf(RepresentativeGuideCategory.NON_COMBUSTIBLE),
                    onCategoryClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("불연성종량제 폐기물").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("불\u200B연\u200B성\u200B종\u200B량\u200B제\u200B 폐\u200B기\u200B물\u200B")
            .assertCountEquals(0)
    }

    private fun sampleGuide(
        name: String = "유리병",
        category: DisposalCategory = DisposalCategory.GLASS,
        subCategory: DisposalSubCategory? = null,
        instructions: List<DisposalInstruction> = listOf(DisposalInstruction(method = "재활용폐기물")),
    ) =
        DisposalItemGuide(
            id = "glass",
            name = name,
            category = category,
            subCategory = subCategory,
            instructions = instructions,
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )
}
