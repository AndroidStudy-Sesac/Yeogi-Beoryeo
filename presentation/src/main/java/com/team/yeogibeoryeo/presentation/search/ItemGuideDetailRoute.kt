package com.team.yeogibeoryeo.presentation.search

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.MessageSnackbar
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusDescription
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusContent
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchLoadingContent
import com.team.yeogibeoryeo.presentation.search.components.ItemSearchStatusTitle

@Composable
fun ItemGuideDetailRoute(
    guideId: String,
    onBackClick: () -> Unit,
    onCollectionSpotTypeClick: (CollectionSpotType) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: ItemGuideDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIcon by remember { mutableStateOf(ItemGuideDetailMessageIcon.Favorite) }
    val currentContext by rememberUpdatedState(LocalContext.current)

    LaunchedEffect(guideId) {
        viewModel.loadGuide(guideId)
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemGuideDetailEvent.ShowMessage -> {
                    snackbarIcon = event.icon
                    snackbarHostState.showSnackbar(currentContext.getString(event.messageResId))
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                MessageSnackbar(
                    message = snackbarData.visuals.message,
                    icon = { ItemGuideDetailSnackbarIcon(icon = snackbarIcon) },
                )
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
                    actions = state.actions,
                    isFavorite = state.isFavorite,
                    onBackClick = onBackClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onCollectionSpotTypeClick = onCollectionSpotTypeClick,
                    onOfficialGuideClick = onOpenExternalUrl,
                    onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            ItemGuideDetailUiState.NotFound -> {
                ItemGuideDetailStatusContent(
                    titleResId = R.string.item_guide_detail_not_found_title,
                    descriptionResId = R.string.item_guide_detail_select_again_message,
                    actionResId = R.string.back_action,
                    onActionClick = onBackClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            ItemGuideDetailUiState.LoadFailed -> {
                ItemGuideDetailStatusContent(
                    titleResId = R.string.item_guide_detail_load_failed_title,
                    descriptionResId = R.string.item_guide_detail_load_failed_message,
                    actionResId = R.string.retry_action,
                    onActionClick = viewModel::retryLoadGuide,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ItemGuideDetailStatusContent(
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int,
    @StringRes actionResId: Int,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ItemSearchStatusContent(
        title = {
            ItemSearchStatusTitle(text = stringResource(titleResId))
        },
        description = {
            ItemSearchStatusDescription(text = stringResource(descriptionResId))
        },
        modifier = modifier,
        action = {
            Button(onClick = onActionClick) {
                Text(text = stringResource(actionResId))
            }
        },
    )
}

@Composable
private fun ItemGuideDetailSnackbarIcon(
    icon: ItemGuideDetailMessageIcon,
    modifier: Modifier = Modifier,
) {
    when (icon) {
        ItemGuideDetailMessageIcon.Favorite -> {
            Icon(
                painter = painterResource(id = CommonR.drawable.ic_favorite_filled),
                contentDescription = null,
                modifier = modifier.size(SnackbarIconSize),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }

        ItemGuideDetailMessageIcon.Warning -> {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                modifier = modifier.size(SnackbarIconSize),
                tint = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

private val SnackbarIconSize = 20.dp
