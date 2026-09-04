package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteCard
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 화면_제목과_빈_상태는_heading과_live_region을_제공한다() {
        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(selectedTab = FavoriteTab.ITEM_GUIDE),
                    onTabClick = {},
                    onItemSearchClick = {},
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("즐겨찾기").assert(hasHeading())
        composeTestRule.onNodeWithText("아직 즐겨찾기한 품목이 없어요").assert(hasHeading())
        composeTestRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)
    }

    @Test
    fun `지역_가이드_빈_상태는_검색_이동_버튼을_제공한다`() {
        var searchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(selectedTab = FavoriteTab.REGIONAL_GUIDE),
                    onTabClick = {},
                    onItemSearchClick = {},
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = { searchClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("아직 즐겨찾기한 지역 가이드가 없어요")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("지역별 배출 가이드 찾아보기")
            .assertIsDisplayed()
            .assert(hasClickAction())
            .performClick()

        assertEquals(1, searchClickCount)
    }

    @Test
    fun 로딩_상태는_한_개의_live_region을_제공한다() {
        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(isLoading = true),
                    onTabClick = {},
                    onItemSearchClick = {},
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("로딩 중")
            .assert(hasPoliteLiveRegion())
        composeTestRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)
        composeTestRule.onNodeWithText("품목 검색하기").assertDoesNotExist()
    }

    @Test
    fun 조회_실패_상태는_오류_안내와_재시도_동작을_제공한다() {
        var retryClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(hasLoadError = true),
                    onTabClick = {},
                    onItemSearchClick = {},
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = {},
                    onRetryClick = { retryClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("즐겨찾기를 불러오지 못했어요")
            .assert(hasHeading())
        composeTestRule.onNodeWithText("잠시 후 다시 시도해 주세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("다시 시도").performClick()
        composeTestRule.onAllNodes(hasPoliteLiveRegion()).assertCountEquals(1)

        assertEquals(1, retryClickCount)
        composeTestRule.onNodeWithText("품목 검색하기").assertDoesNotExist()
    }

    @Test
    fun 즐겨찾기_카드는_버튼_역할과_구체적인_동작명을_제공한다() {
        composeTestRule.setContent {
            MaterialTheme {
                FavoriteCard(
                    favorite = FavoriteUiModel(
                        type = FavoriteTargetType.ITEM_GUIDE,
                        targetId = "glass",
                        title = "유리병",
                        subtitle = null,
                    ),
                    onClick = {},
                    onRemoveClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("유리병")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasOnClickLabel("유리병 상세 보기"))
        composeTestRule.onNodeWithContentDescription("유리병 즐겨찾기")
            .assert(hasClickAction())
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
            .assertIsDisplayed()
    }

    @Test
    fun emptyItemFavoritesInvokeOnlyItemSearch() {
        var itemSearchClickCount = 0
        var regionalGuideSearchClickCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(selectedTab = FavoriteTab.ITEM_GUIDE),
                    onTabClick = {},
                    onItemSearchClick = { itemSearchClickCount += 1 },
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = { regionalGuideSearchClickCount += 1 },
                )
            }
        }

        composeTestRule.onNodeWithText("품목 검색하기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeTestRule.onNodeWithText("지역별 배출 가이드 찾아보기").assertDoesNotExist()

        assertEquals(1, itemSearchClickCount)
        assertEquals(0, regionalGuideSearchClickCount)
    }

    @Test
    fun emptyCollectionSpotFavoritesHaveNoSearchActions() {
        composeTestRule.setContent {
            MaterialTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(selectedTab = FavoriteTab.COLLECTION_SPOT),
                    onTabClick = {},
                    onItemSearchClick = {},
                    onItemGuideClick = {},
                    onCollectionSpotClick = {},
                    onRegionalGuideClick = {},
                    onItemGuideFavoriteRemoveClick = {},
                    onCollectionSpotFavoriteRemoveClick = {},
                    onRegionalGuideFavoriteRemoveClick = {},
                    onRegionalGuideHomePrimaryClick = {},
                    onRegionalGuideSearchClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("즐겨찾기한 수거 장소가 없어요").assertIsDisplayed()
        composeTestRule.onNodeWithText("품목 검색하기").assertDoesNotExist()
        composeTestRule.onNodeWithText("지역별 배출 가이드 찾아보기").assertDoesNotExist()
    }

    private fun hasHeading() =
        SemanticsMatcher.expectValue(SemanticsProperties.Heading, Unit)

    private fun hasPoliteLiveRegion() =
        SemanticsMatcher.expectValue(
            SemanticsProperties.LiveRegion,
            LiveRegionMode.Polite,
        )

    private fun hasOnClickLabel(label: String) =
        SemanticsMatcher("onClick label is '$label'") { node ->
            SemanticsActions.OnClick in node.config &&
                node.config[SemanticsActions.OnClick].label == label
        }
}
