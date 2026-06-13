package com.team.yeogibeoryeo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.common.navigation.AppBottomNavigationBar
import com.team.yeogibeoryeo.presentation.favorites.FavoritesRoute as FavoritesScreenRoute
import com.team.yeogibeoryeo.presentation.map.CollectionSpotMapScreen
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideRoute as RegionalGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemGuideDetailRoute as ItemGuideDetailScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemSearchRoute as ItemSearchScreenRoute

@Composable
fun YeogiBeoryeoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val isItemDetailScreen = currentDestination?.hasRoute<ItemGuideDetailRoute>() == true
    var isBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentBackStackEntry) {
        isBottomBarVisible = true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val enterTransition =
                if (isItemDetailScreen) {
                    fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + slideInVertically { it }
                } else {
                    EnterTransition.None
                }
            val exitTransition =
                if (isItemDetailScreen) {
                    fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + slideOutVertically { it }
                } else {
                    ExitTransition.None
                }

            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                AppBottomNavigationBar(
                    items = navController.createBottomNavigationItems(
                        currentBackStackEntry = currentBackStackEntry,
                        currentDestination = currentDestination,
                        onMapTabSelected = { isBottomBarVisible = true },
                    ),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ItemSearchRoute(),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MapRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MapRoute>()
                CollectionSpotMapScreen(
                    favoriteSpotMoveRequest = route.toFavoriteSpotMapMoveRequest(),
                    onBottomBarVisibilityChanged = { isVisible ->
                        if (currentDestination?.hasRoute<MapRoute>() == true) {
                            isBottomBarVisible = isVisible
                        }
                    },
                )
            }

            composable<RegionalGuideRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<RegionalGuideRoute>()
                RegionalGuideScreenRoute(
                    initialKeyword = route.initialKeyword,
                    initialAddress = route.initialAddress,
                    initialFavoriteTargetId = route.initialFavoriteTargetId,
                )
            }

            composable<FavoritesRoute> {
                FavoritesScreenRoute(
                    onItemGuideClick = { guideId ->
                        navController.navigate(
                            ItemGuideDetailRoute(
                                guideId = guideId,
                                source = ItemGuideDetailSource.FAVORITES,
                            ),
                        )
                    },
                    onCollectionSpotClick = { request ->
                        navController.navigate(request.toMapRoute()) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onRegionalGuideClick = { targetId ->
                        navController.navigate(
                            RegionalGuideRoute(initialFavoriteTargetId = targetId),
                        )
                    },
                )
            }

            composable<ItemSearchRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItemSearchRoute>()
                ItemSearchScreenRoute(
                    initialQuery = route.initialQuery,
                    onGuideSelected = { guide ->
                        navController.navigate(
                            ItemGuideDetailRoute(
                                guideId = guide.id,
                                source = ItemGuideDetailSource.SEARCH,
                            ),
                        )
                    },
                )
            }

            composable<ItemGuideDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItemGuideDetailRoute>()
                ItemGuideDetailScreenRoute(
                    guideId = route.guideId,
                    onBackClick = navController::popBackStack,
                    onBottomBarVisibilityChanged = { isVisible ->
                        if (isItemDetailScreen) {
                            isBottomBarVisible = isVisible
                        }
                    },
                )
            }
        }
    }
}
