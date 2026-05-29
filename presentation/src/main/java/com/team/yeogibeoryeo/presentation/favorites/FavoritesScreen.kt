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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.favorites.components.EmptyFavoritesCard
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteCard

@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onItemGuideClick: (String) -> Unit,
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

            uiState.favorites.isEmpty() -> {
                item {
                    EmptyFavoritesCard()
                }
            }

            else -> {
                items(
                    items = uiState.favorites,
                    key = { it.targetId },
                ) { favorite ->
                    FavoriteCard(
                        favorite = favorite,
                        onClick = { onItemGuideClick(favorite.targetId) },
                    )
                }
            }
        }
    }
}
