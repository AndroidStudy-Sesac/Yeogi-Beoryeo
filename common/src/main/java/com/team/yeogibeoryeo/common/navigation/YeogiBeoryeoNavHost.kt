package com.team.yeogibeoryeo.common.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
                currentDestination = currentDestination,
                onHomeClick = { navController.navigateBottomTab(HomeRoute) },
                onMapClick = { navController.navigateBottomTab(MapRoute) },
                onRegionalGuideClick = {
                    navController.navigateBottomTab(RegionalGuideRoute())
                },
                onFavoritesClick = { navController.navigateBottomTab(FavoritesRoute) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onItemSearch = { query ->
                        navController.navigate(ItemSearchRoute(initialQuery = query))
                    },
                    onMapClick = { navController.navigateBottomTab(MapRoute) },
                    onRegionalGuideClick = {
                        navController.navigateBottomTab(RegionalGuideRoute())
                    },
                    onFavoritesClick = { navController.navigateBottomTab(FavoritesRoute) },
                )
            }

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

@Composable
private fun AppBottomNavigationBar(
    currentDestination: NavDestination?,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onRegionalGuideClick: () -> Unit,
    onFavoritesClick: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination.isHomeSelected(),
            onClick = onHomeClick,
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = currentDestination.hasRouteName<MapRoute>(),
            onClick = onMapClick,
            icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            label = { Text("Map") },
        )
        NavigationBarItem(
            selected = currentDestination.hasRouteName<RegionalGuideRoute>(),
            onClick = onRegionalGuideClick,
            icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
            label = { Text("Guide") },
        )
        NavigationBarItem(
            selected = currentDestination.hasRouteName<FavoritesRoute>(),
            onClick = onFavoritesClick,
            icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = null) },
            label = { Text("Favorites") },
        )
    }
}

private fun NavDestination?.isHomeSelected(): Boolean =
    hasRouteName<HomeRoute>() ||
        hasRouteName<ItemSearchRoute>() ||
        hasRouteName<ItemGuideDetailRoute>()

private inline fun <reified T : Any> NavDestination?.hasRouteName(): Boolean =
    this?.route?.substringBefore("?") == requireNotNull(T::class.qualifiedName)

private inline fun <reified T : Any> NavHostController.navigateBottomTab(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
