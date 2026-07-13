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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.favorites.components.EmptyFavoritesCard
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteCard
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteTab

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onTabClick: (FavoriteTab) -> Unit,
    onItemGuideClick: (String) -> Unit,
    onCollectionSpotClick: (FavoriteCollectionSpotMapMoveRequest) -> Unit,
    onRegionalGuideClick: (String) -> Unit,
    onItemGuideFavoriteRemoveClick: (String) -> Unit,
    onCollectionSpotFavoriteRemoveClick: (String) -> Unit,
    onRegionalGuideFavoriteRemoveClick: (String) -> Unit,
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
                text = stringResource(R.string.favorites_title),
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
                        title = stringResource(uiState.selectedTab.emptyTitleResId),
                        description = stringResource(uiState.selectedTab.emptyDescriptionResId),
                    )
                }
            }

            else -> {
                items(
                    items = uiState.selectedFavorites,
                    key = { "${it.type.name}:${it.targetId}" },
                ) { favorite ->
                    val isCardClickEnabled =
                        favorite.type != FavoriteTargetType.COLLECTION_SPOT ||
                            favorite.collectionSpotMapMoveRequest != null

                    FavoriteCard(
                        favorite = favorite,
                        enabled = isCardClickEnabled,
                        onClick = {
                            when (favorite.type) {
                                FavoriteTargetType.ITEM_GUIDE -> onItemGuideClick(favorite.targetId)
                                FavoriteTargetType.COLLECTION_SPOT -> {
                                    favorite.collectionSpotMapMoveRequest?.let(onCollectionSpotClick)
                                }
                                FavoriteTargetType.REGIONAL_GUIDE -> onRegionalGuideClick(favorite.targetId)
                            }
                        },
                        onRemoveClick = when (favorite.type) {
                            FavoriteTargetType.ITEM_GUIDE -> {
                                { onItemGuideFavoriteRemoveClick(favorite.targetId) }
                            }

                            FavoriteTargetType.COLLECTION_SPOT -> {
                                { onCollectionSpotFavoriteRemoveClick(favorite.targetId) }
                            }

                            FavoriteTargetType.REGIONAL_GUIDE -> {
                                { onRegionalGuideFavoriteRemoveClick(favorite.targetId) }
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
                        text = stringResource(tab.labelResId),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
            )
        }
    }
}
