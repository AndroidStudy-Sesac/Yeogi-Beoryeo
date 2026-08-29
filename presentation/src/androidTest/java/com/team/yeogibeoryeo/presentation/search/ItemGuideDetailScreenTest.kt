package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItemGuideDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 상세_화면은_배출_정보_섹션을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide = sampleGuide(),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("유리컵").assertIsDisplayed()
        composeTestRule.onNodeWithText("유리컵").assert(hasHeading())
        composeTestRule.onNodeWithContentDescription("재활용 분리배출").assertIsDisplayed()
        composeTestRule.onNodeWithText("유리병").assertIsDisplayed()
        composeTestRule.onNodeWithText("유리컵·그릇").assertIsDisplayed()
        composeTestRule.onNodeWithText("주의사항").assertIsDisplayed()
        composeTestRule.onNodeWithText("주의사항").assert(hasHeading())
        composeTestRule.onNodeWithText("품목 특징").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("배출 절차").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("재활용·처리 정보").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 뒤로가기_아이콘을_누르면_콜백을_호출한다() {
        var backClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide = sampleGuide(),
                    isFavorite = false,
                    onBackClick = { backClickCount += 1 },
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, backClickCount)
    }

    @Test
    fun 상세_화면은_버리는_방법_카드를_보여주지_않는다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide = sampleGuide(),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("버리는 방법").assertDoesNotExist()
    }

    @Test
    fun 하위_분류가_있으면_세부_분류_섹션을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide =
                        sampleGuide().copy(
                            name = "종이",
                            subGuides =
                                listOf(
                                    DisposalSubGuide("골판지", "접어서 배출합니다."),
                                    DisposalSubGuide("기타 종이류", "이물질을 제거합니다."),
                                ),
                        ),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("세부 분류").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("골판지").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("기타 종이류").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun 상세_섹션이_있으면_문서_섹션_제목을_우선_표시한다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide =
                        sampleGuide().copy(
                            detailSections =
                                listOf(
                                    DisposalGuideSection("대상품목", listOf("유리병")),
                                    DisposalGuideSection("배출방법", listOf("내용물을 비웁니다.")),
                                ),
                        ),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("대상품목").assertIsDisplayed()
        composeTestRule.onNodeWithText("배출방법").assertIsDisplayed()
        composeTestRule.onNodeWithText("배출 절차").assertDoesNotExist()
        composeTestRule.onNodeWithText("재활용·처리 정보").assertDoesNotExist()
    }

    @Test
    fun 상세_섹션의_표_행을_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide =
                        sampleGuide().copy(
                            detailSections =
                                listOf(
                                    DisposalGuideSection(
                                        title = "품목별 배출 방법",
                                        lines = emptyList(),
                                        rows =
                                            listOf(
                                                DisposalGuideSectionRow(
                                                    label = "폐의약품",
                                                    value = "약국, 보건소, 주민센터 등 전용수거함에 배출하거나 우체통에 배출(물약 제외)",
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("품목별 배출 방법").assertIsDisplayed()
        composeTestRule.onNodeWithText("폐의약품").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("약국, 보건소, 주민센터", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun 상세_화면_하단에는_지역별_배출_기준_안내를_항상_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide = sampleGuide(),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("지역별 배출 기준 안내")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun 대표_카테고리_상세_화면에도_지역별_배출_기준_안내를_보여준다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide =
                        sampleGuide().copy(
                            id = RepresentativeGuideCategory.PAPER.representativeGuideId,
                            name = RepresentativeGuideCategory.PAPER.representativeGuideName,
                            category = DisposalCategory.PAPER,
                            subCategory = null,
                            detailSections =
                                listOf(
                                    DisposalGuideSection("배출방법", listOf("종이류로 분리배출합니다.")),
                                ),
                        ),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("지역별 배출 기준 안내")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun 비닐_상세는_하단_탐색바가_숨은_뒤에도_마지막_안내를_끝까지_보여준다() {
        var isBottomBarVisible by mutableStateOf(true)

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(752.dp)
                        .padding(bottom = if (isBottomBarVisible) 80.dp else 0.dp),
                ) {
                    ItemGuideDetailScreen(
                        guide = vinylGuide(),
                        isFavorite = false,
                        onBackClick = {},
                        onFavoriteClick = {},
                        onBottomBarVisibilityChanged = { isBottomBarVisible = it },
                    )
                }
            }
        }

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        composeTestRule.onNodeWithText(LOCAL_DISPOSAL_NOTICE)
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            assertEquals(false, isBottomBarVisible)
        }
        composeTestRule.onNode(hasScrollAction()).assert(hasPositiveVerticalScrollPosition())
    }

    @Test
    fun 짧은_품목_상세도_스크롤할_때_하단_탐색바를_다시_표시하지_않는다() {
        var isBottomBarVisible by mutableStateOf(true)

        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(540.dp)
                        .padding(bottom = if (isBottomBarVisible) 80.dp else 0.dp),
                ) {
                    ItemGuideDetailScreen(
                        guide = shortGuide(),
                        isFavorite = false,
                        onBackClick = {},
                        onFavoriteClick = {},
                        onBottomBarVisibilityChanged = { isBottomBarVisible = it },
                    )
                }
            }
        }

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { !isBottomBarVisible }

        composeTestRule.runOnIdle {
            assertEquals(false, isBottomBarVisible)
        }
        composeTestRule.onNode(hasScrollAction()).assert(hasPositiveVerticalScrollPosition())

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) { isBottomBarVisible }
        composeTestRule.runOnIdle {
            assertEquals(true, isBottomBarVisible)
        }
    }

    @Test
    fun 대표_카테고리_상세_화면은_제목과_같은_카테고리_칩을_숨긴다() {
        composeTestRule.setContent {
            MaterialTheme {
                ItemGuideDetailScreen(
                    guide =
                        sampleGuide().copy(
                            id = RepresentativeGuideCategory.PAPER.representativeGuideId,
                            name = RepresentativeGuideCategory.PAPER.representativeGuideName,
                            category = DisposalCategory.PAPER,
                            subCategory = null,
                        ),
                    isFavorite = false,
                    onBackClick = {},
                    onFavoriteClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("종이").assertCountEquals(1)
    }

    private fun sampleGuide() =
        DisposalItemGuide(
            id = "glass",
            name = "유리컵",
            category = DisposalCategory.GLASS,
            subCategory = DisposalSubCategory.GLASS_CONTAINER,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = listOf("내용물을 비웁니다."),
            features = listOf("재활용 가능한 유리 재질입니다."),
            cautions = listOf("깨진 유리는 별도 배출합니다."),
            tip = "라벨을 제거합니다.",
            isRecyclable = true,
            relatedSpotTypes = emptyList(),
        )

    private fun vinylGuide() =
        sampleGuide().copy(
            id = "vinyl",
            name = "비닐",
            category = DisposalCategory.VINYL,
            subCategory = null,
            detailSections =
                listOf(
                    DisposalGuideSection(
                        title = "배출방법",
                        lines =
                            listOf(
                                "비닐포장재, 비닐봉투, 필름류는 비닐류 수거함으로 배출합니다.",
                                "이물질과 내용물을 제거하고 흩날리지 않도록 투명비닐봉투에 모아서 배출합니다.",
                                "PP, PE, PS 등 재질 구분없이 배출합니다.",
                                "양파 등 농산물을 담는 그물망은 비닐로 함께 배출합니다.",
                            ),
                    ),
                    DisposalGuideSection(
                        title = "특징",
                        lines = listOf("비닐류에는 비닐포장재, 1회용 비닐봉투, 필름류가 있습니다."),
                    ),
                ),
        )

    private fun shortGuide() =
        sampleGuide().copy(
            id = "short-guide",
            name = "짧은 품목",
            subCategory = null,
            detailSections =
                listOf(
                    DisposalGuideSection(
                        title = "배출방법",
                        lines = listOf("내용물을 비우고 분리배출합니다."),
                    ),
                ),
        )

    private fun hasHeading() =
        SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)

    private fun hasPositiveVerticalScrollPosition() =
        SemanticsMatcher("has positive vertical scroll position") { node ->
            node.config[SemanticsProperties.VerticalScrollAxisRange].value() > 0f
        }

    private companion object {
        const val LOCAL_DISPOSAL_NOTICE =
            "배출방법은 지역별로 차이가 있을 수 있으니, 해당 지방자치단체에서 정하는 방법이 있는 경우에는 해당 방법에 따라 배출하시기 바랍니다."
    }
}
