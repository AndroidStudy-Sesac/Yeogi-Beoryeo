package com.team.yeogibeoryeo.presentation.favorites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.MessageSnackbar
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest

@Composable
fun FavoritesRoute(
    onItemGuideClick: (String) -> Unit,
    onCollectionSpotClick: (FavoriteCollectionSpotMapMoveRequest) -> Unit,
    onRegionalGuideClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteUpdateFailedMessage = stringResource(R.string.favorite_update_failed_message)
    val currentFavoriteUpdateFailedMessage by rememberUpdatedState(favoriteUpdateFailedMessage)

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                FavoritesEvent.FavoriteUpdateFailed -> {
                    snackbarHostState.showSnackbar(currentFavoriteUpdateFailedMessage)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        FavoritesScreen(
            uiState = uiState,
            onTabClick = viewModel::selectTab,
            onItemGuideClick = onItemGuideClick,
            onCollectionSpotClick = onCollectionSpotClick,
            onRegionalGuideClick = onRegionalGuideClick,
            onItemGuideFavoriteRemoveClick = viewModel::removeItemGuideFavorite,
            onCollectionSpotFavoriteRemoveClick = viewModel::removeCollectionSpotFavorite,
            onRegionalGuideFavoriteRemoveClick = viewModel::removeRegionalGuideFavorite,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { snackbarData ->
            MessageSnackbar(
                message = snackbarData.visuals.message,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(FavoriteSnackbarIconSize),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
            )
        }
    }
}

private val FavoriteSnackbarIconSize = 20.dp
