package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.presentation.favorites.components.EmptyFavoritesCard
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteCard
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onTabClick: (FavoriteTab) -> Unit,
    onItemGuideClick: (String) -> Unit,
    onCollectionSpotClick: (String) -> Unit,
    onRegionalGuideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "즐겨찾기",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            FavoriteTabRow(
                selectedTab = uiState.selectedTab,
                onTabClick = onTabClick,
            )
        }

        when {
            uiState.isLoading -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(top = 96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            uiState.selectedFavorites.isEmpty() -> {
                item {
                    EmptyFavoritesCard(
                        title = uiState.selectedTab.emptyTitle,
                        description = uiState.selectedTab.emptyDescription,
                    )
                }
            }

            else -> {
                items(
                    items = uiState.selectedFavorites,
                    key = { "${it.type.name}:${it.targetId}" },
                ) { favorite ->
                    FavoriteCard(
                        favorite = favorite,
                        onClick = {
                            when (favorite.type) {
                                FavoriteTargetType.ITEM_GUIDE -> onItemGuideClick(favorite.targetId)
                                FavoriteTargetType.COLLECTION_SPOT -> onCollectionSpotClick(favorite.targetId)
                                FavoriteTargetType.REGIONAL_GUIDE -> onRegionalGuideClick(favorite.targetId)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTabRow(
    selectedTab: FavoriteTab,
    onTabClick: (FavoriteTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = FavoriteTab.entries.indexOf(selectedTab),
        modifier = modifier,
    ) {
        FavoriteTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabClick(tab) },
                text = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
    }
}
