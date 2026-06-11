package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenLoadingPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState = FavoritesUiState(isLoading = true),
            onTabClick = {},
            onItemGuideClick = {},
            onCollectionSpotClick = {},
            onRegionalGuideClick = {},
            onCollectionSpotFavoriteRemoveClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenEmptyPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState = FavoritesUiState(),
            onTabClick = {},
            onItemGuideClick = {},
            onCollectionSpotClick = {},
            onRegionalGuideClick = {},
            onCollectionSpotFavoriteRemoveClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenSpotEmptyPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState = FavoritesUiState(selectedTab = FavoriteTab.COLLECTION_SPOT),
            onTabClick = {},
            onItemGuideClick = {},
            onCollectionSpotClick = {},
            onRegionalGuideClick = {},
            onCollectionSpotFavoriteRemoveClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenListPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState =
                FavoritesUiState(
                    itemGuideFavorites =
                        listOf(
                            FavoriteUiModel(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = "paper-pack",
                                title = "종이팩",
                                subtitle = "종이팩",
                            ),
                            FavoriteUiModel(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = "large-waste",
                                title = "대형폐기물",
                                subtitle = "대형폐기물",
                            ),
                            FavoriteUiModel(
                                type = FavoriteTargetType.ITEM_GUIDE,
                                targetId = "very-long-title",
                                title = "아주 긴 이름의 즐겨찾기 품목 가이드",
                                subtitle = "생활계 유해폐기물",
                            ),
                        ),
                ),
            onTabClick = {},
            onItemGuideClick = {},
            onCollectionSpotClick = {},
            onRegionalGuideClick = {},
            onCollectionSpotFavoriteRemoveClick = {},
        )
    }
}

@Composable
private fun FavoriteScreenPreviewContainer(content: @Composable () -> Unit) {
    YeogiBeoryeoTheme {
        Surface {
            content()
        }
    }
}
