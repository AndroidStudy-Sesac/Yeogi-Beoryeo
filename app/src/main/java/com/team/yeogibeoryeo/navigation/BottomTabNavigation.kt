package com.team.yeogibeoryeo.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.R as AppR
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.common.navigation.BottomNavigationItem
import com.team.yeogibeoryeo.common.navigation.navigateBottomTab

internal fun NavHostController.createBottomNavigationItems(
    currentBackStackEntry: NavBackStackEntry?,
    currentDestination: NavDestination?,
    onMapTabSelected: () -> Unit,
): List<BottomNavigationItem> =
    listOf(
        BottomNavigationItem(
            label = "품목",
            iconResId = CommonR.drawable.ic_symbol_recycle,
            selected = currentBackStackEntry.isItemSearchSelected(),
            onClick = {
                navigateBottomTabClearingFavoriteReentry(
                    currentBackStackEntry = currentBackStackEntry,
                    route = ItemSearchRoute(),
                )
            },
        ),
        BottomNavigationItem(
            label = "지도",
            iconResId = AppR.drawable.ic_navigation_map,
            selected = currentDestination?.hasRoute<MapRoute>() == true,
            onClick = {
                onMapTabSelected()
                navigateBottomTabClearingFavoriteReentry(
                    currentBackStackEntry = currentBackStackEntry,
                    route = MapRoute(),
                )
            },
        ),
        BottomNavigationItem(
            label = "안내",
            iconResId = AppR.drawable.ic_navigation_guide,
            selected = currentBackStackEntry.isRegionalGuideSelected(),
            onClick = {
                navigateBottomTabClearingFavoriteReentry(
                    currentBackStackEntry = currentBackStackEntry,
                    route = RegionalGuideRoute(),
                )
            },
        ),
        BottomNavigationItem(
            label = "저장",
            iconResId = CommonR.drawable.ic_favorite,
            selected = currentBackStackEntry.isFavoritesSelected(),
            onClick = {
                if (currentBackStackEntry.isFavoriteRegionalGuideSelected()) {
                    popBackStack<FavoritesRoute>(inclusive = false)
                } else {
                    navigateFavoritesRoot(
                        currentBackStackEntry = currentBackStackEntry,
                    )
                }
            },
        ),
    )

private fun NavBackStackEntry?.isItemSearchSelected(): Boolean =
    this?.destination?.hasRoute<ItemSearchRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.SEARCH)

private fun NavBackStackEntry?.isFavoritesSelected(): Boolean =
    this?.destination?.hasRoute<FavoritesRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES) ||
        isFavoriteRegionalGuideSelected()

private fun NavBackStackEntry?.isRegionalGuideSelected(): Boolean =
    this?.destination?.hasRoute<RegionalGuideRoute>() == true &&
        !isFavoriteRegionalGuideSelected()

private fun NavBackStackEntry?.isFavoriteRegionalGuideSelected(): Boolean =
    this != null &&
        destination.hasRoute<RegionalGuideRoute>() &&
        toRoute<RegionalGuideRoute>().isFavoriteReentryRoute()

internal fun RegionalGuideRoute.isFavoriteReentryRoute(): Boolean =
    !initialFavoriteTargetId.isNullOrBlank()

private inline fun <reified T : Any> NavHostController.navigateBottomTabClearingFavoriteReentry(
    currentBackStackEntry: NavBackStackEntry?,
    route: T,
) {
    if (currentBackStackEntry.isFavoriteRegionalGuideSelected()) {
        popBackStack<FavoritesRoute>(inclusive = false)
    }

    navigateBottomTab(route)
}

private fun NavBackStackEntry?.isItemGuideDetailSource(source: ItemGuideDetailSource): Boolean =
    this != null &&
        destination.hasRoute<ItemGuideDetailRoute>() &&
        toRoute<ItemGuideDetailRoute>().source == source

private fun NavHostController.navigateFavoritesRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    when {
        currentBackStackEntry?.destination?.hasRoute<FavoritesRoute>() == true -> return
        currentBackStackEntry.isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES) -> {
            popBackStack<FavoritesRoute>(inclusive = false)
        }
        else -> {
            navigate(FavoritesRoute) {
                popUpTo(graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
}
