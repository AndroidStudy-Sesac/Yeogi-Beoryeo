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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.common.navigation.AppBottomNavigationBar
import com.team.yeogibeoryeo.common.navigation.BottomNavigationItem
import com.team.yeogibeoryeo.common.navigation.navigateBottomTab
import com.team.yeogibeoryeo.presentation.favorites.FavoritesRoute as FavoritesScreenRoute
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteCollectionSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.map.CollectionSpotMapScreen
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideRoute as RegionalGuideScreenRoute
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.R as AppR
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
                visible = !isItemDetailScreen || isBottomBarVisible,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                AppBottomNavigationBar(
                    items =
                        listOf(
                            BottomNavigationItem(
                                label = "품목",
                                iconResId = CommonR.drawable.ic_symbol_recycle,
                                selected = currentBackStackEntry.isItemSearchSelected(),
                                onClick = { navController.navigateBottomTab(ItemSearchRoute()) },
                            ),
                            BottomNavigationItem(
                                label = "지도",
                                iconResId = AppR.drawable.ic_navigation_map,
                                selected = currentDestination?.hasRoute<MapRoute>() == true,
                                onClick = { navController.navigateBottomTab(MapRoute()) },
                            ),
                            BottomNavigationItem(
                                label = "안내",
                                iconResId = AppR.drawable.ic_navigation_guide,
                                selected = currentDestination?.hasRoute<RegionalGuideRoute>() == true,
                                onClick = { navController.navigateBottomTab(RegionalGuideRoute()) },
                            ),
                            BottomNavigationItem(
                                label = "저장",
                                iconResId = CommonR.drawable.ic_favorite,
                                selected = currentBackStackEntry.isFavoritesSelected(),
                                onClick = { navController.navigateBottomTab(FavoritesRoute) },
                            ),
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
                )
            }

            composable<RegionalGuideRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<RegionalGuideRoute>()
                RegionalGuideScreenRoute(
                    initialKeyword = route.initialKeyword,
                    initialAddress = route.initialAddress,
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

private fun NavBackStackEntry?.isItemSearchSelected(): Boolean =
    this?.destination?.hasRoute<ItemSearchRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.SEARCH)

private fun NavBackStackEntry?.isFavoritesSelected(): Boolean =
    this?.destination?.hasRoute<FavoritesRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES)

private fun NavBackStackEntry?.isItemGuideDetailSource(source: ItemGuideDetailSource): Boolean =
    this != null &&
        destination.hasRoute<ItemGuideDetailRoute>() &&
        toRoute<ItemGuideDetailRoute>().source == source

private fun FavoriteCollectionSpotMapMoveRequest.toMapRoute(): MapRoute =
    MapRoute(
        favoriteSpotTargetId = targetId,
        favoriteSpotName = name,
        favoriteSpotType = type.name,
        favoriteSpotAddress = address,
        favoriteSpotDetailLocation = detailLocation,
        favoriteSpotLatitude = latitude,
        favoriteSpotLongitude = longitude,
    )

private fun MapRoute.toFavoriteSpotMapMoveRequest(): FavoriteSpotMapMoveRequest? {
    val targetId = favoriteSpotTargetId ?: return null
    val name = favoriteSpotName ?: return null
    val typeName = favoriteSpotType ?: return null
    val address = favoriteSpotAddress ?: return null
    val latitude = favoriteSpotLatitude ?: return null
    val longitude = favoriteSpotLongitude ?: return null
    val type =
        runCatching {
            CollectionSpotType.valueOf(typeName)
        }.getOrNull() ?: return null

    return FavoriteSpotMapMoveRequest(
        targetId = targetId,
        name = name,
        type = type,
        address = address,
        detailLocation = favoriteSpotDetailLocation,
        coordinate = Coordinate(
            latitude = latitude,
            longitude = longitude,
        ),
    )
}
