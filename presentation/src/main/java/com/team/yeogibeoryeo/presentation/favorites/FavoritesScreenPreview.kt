package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteItemUiModel

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenLoadingPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState = FavoritesUiState(isLoading = true),
            onItemGuideClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoritesScreenEmptyPreview() {
    FavoriteScreenPreviewContainer {
        FavoritesScreen(
            uiState = FavoritesUiState(),
            onItemGuideClick = {},
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
                    favorites =
                        listOf(
                            FavoriteItemUiModel(
                                targetId = "paper-pack",
                                title = "종이팩",
                                subtitle = "종이팩",
                            ),
                            FavoriteItemUiModel(
                                targetId = "large-waste",
                                title = "대형폐기물",
                                subtitle = "대형폐기물",
                            ),
                            FavoriteItemUiModel(
                                targetId = "very-long-title",
                                title = "아주 긴 이름의 즐겨찾기 품목 가이드",
                                subtitle = "생활계 유해폐기물",
                            ),
                        ),
                ),
            onItemGuideClick = {},
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
