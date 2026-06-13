package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteSnackbar
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusDescription
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusContent
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchLoadingContent
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusTitle

@Composable
fun ItemGuideDetailRoute(
    guideId: String,
    onBackClick: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ItemGuideDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentContext by rememberUpdatedState(LocalContext.current)

    LaunchedEffect(guideId) {
        viewModel.loadGuide(guideId)
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemGuideDetailEvent.ShowFavoriteMessage -> {
                    snackbarHostState.showSnackbar(currentContext.getString(event.messageResId))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                FavoriteSnackbar(message = snackbarData.visuals.message)
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        when (val state = uiState) {
            ItemGuideDetailUiState.Loading -> {
                ItemSearchLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            is ItemGuideDetailUiState.Success -> {
                ItemGuideDetailScreen(
                    guide = state.guide,
                    isFavorite = state.isFavorite,
                    onBackClick = onBackClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is ItemGuideDetailUiState.Error -> {
                ItemSearchStatusContent(
                    title = {
                        ItemSearchStatusTitle(
                            text = stringResource(R.string.item_guide_detail_not_found_title),
                        )
                    },
                    description = {
                        ItemSearchStatusDescription(text = stringResource(state.messageResId))
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    action = {
                        Button(onClick = onBackClick) {
                            Text(text = stringResource(R.string.back_action))
                        }
                    },
                )
            }
        }
    }
}
