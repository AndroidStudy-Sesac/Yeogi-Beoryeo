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
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.common.navigation.AppBottomNavigationBar
import com.team.yeogibeoryeo.common.navigation.BottomNavigationItem
import com.team.yeogibeoryeo.common.navigation.hasRouteName
import com.team.yeogibeoryeo.common.navigation.navigateBottomTab
import com.team.yeogibeoryeo.presentation.favorites.FavoritesScreen
import com.team.yeogibeoryeo.presentation.map.CollectionSpotMapScreen
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideRoute as RegionalGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemGuideDetailRoute as ItemGuideDetailScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemSearchRoute as ItemSearchScreenRoute

@Composable
fun YeogiBeoryeoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentDestination =
        navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomNavigationBar(
                items =
                    listOf(
                        BottomNavigationItem(
                            label = "Search",
                            icon = Icons.Outlined.Recycling,
                            selected = currentDestination.isItemSearchSelected(),
                            onClick = { navController.navigateBottomTab(ItemSearchRoute()) },
                        ),
                        BottomNavigationItem(
                            label = "Map",
                            icon = Icons.Outlined.LocationOn,
                            selected = currentDestination.hasRouteName<MapRoute>(),
                            onClick = { navController.navigateBottomTab(MapRoute) },
                        ),
                        BottomNavigationItem(
                            label = "Guide",
                            icon = Icons.Outlined.Today,
                            selected = currentDestination.hasRouteName<RegionalGuideRoute>(),
                            onClick = { navController.navigateBottomTab(RegionalGuideRoute()) },
                        ),
                        BottomNavigationItem(
                            label = "Favorites",
                            icon = Icons.Outlined.FavoriteBorder,
                            selected = currentDestination.hasRouteName<FavoritesRoute>(),
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
                FavoritesScreen()
            }

            composable<ItemSearchRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItemSearchRoute>()
                ItemSearchScreenRoute(
                    initialQuery = route.initialQuery,
                    onGuideSelected = { guide ->
                        navController.navigate(ItemGuideDetailRoute(guideId = guide.id))
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

private fun NavDestination?.isItemSearchSelected(): Boolean =
    hasRouteName<ItemSearchRoute>() ||
        hasRouteName<ItemGuideDetailRoute>()
