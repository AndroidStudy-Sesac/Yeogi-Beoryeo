package com.team.yeogibeoryeo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavigationBar(
                items =
                    listOf(
                        BottomNavigationItem(
                            label = "Search",
                            icon = Icons.Outlined.Recycling,
                            selected = currentBackStackEntry.isItemSearchSelected(),
                            onClick = { navController.navigateBottomTab(ItemSearchRoute()) },
                        ),
                        BottomNavigationItem(
                            label = "Map",
                            icon = Icons.Outlined.LocationOn,
                            selected = currentDestination?.hasRoute<MapRoute>() == true,
                            onClick = { navController.navigateBottomTab(MapRoute) },
                        ),
                        BottomNavigationItem(
                            label = "Guide",
                            icon = Icons.Outlined.Today,
                            selected = currentDestination?.hasRoute<RegionalGuideRoute>() == true,
                            onClick = { navController.navigateBottomTab(RegionalGuideRoute()) },
                        ),
                        BottomNavigationItem(
                            label = "Favorites",
                            icon = Icons.Outlined.FavoriteBorder,
                            selected = currentBackStackEntry.isFavoritesSelected(),
                            onClick = { navController.navigateBottomTab(FavoritesRoute) },
                        ),
                    ),
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ItemSearchRoute(),
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MapRoute> {
                CollectionSpotMapScreen()
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
