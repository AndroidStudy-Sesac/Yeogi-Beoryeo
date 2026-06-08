package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.favorites.components.FavoriteSnackbar

@Composable
fun ItemGuideDetailRoute(
    guideId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemGuideDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(guideId) {
        viewModel.loadGuide(guideId)
    }

    val favoriteMessage = (uiState as? ItemGuideDetailUiState.Success)?.favoriteMessage
    LaunchedEffect(favoriteMessage) {
        val message = favoriteMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearFavoriteMessage()
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is ItemGuideDetailUiState.Success -> {
                ItemGuideDetailScreen(
                    guide = state.guide,
                    isFavorite = state.isFavorite,
                    onBackClick = onBackClick,
                    onFavoriteClick = viewModel::toggleFavorite,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is ItemGuideDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "품목 가이드를 찾을 수 없습니다",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onBackClick) {
                        Text("돌아가기")
                    }
                }
            }
        }
    }
}
