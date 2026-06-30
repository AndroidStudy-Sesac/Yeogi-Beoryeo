package com.team.yeogibeoryeo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.R as AppR
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.common.navigation.BottomNavigationItem
import com.team.yeogibeoryeo.common.navigation.navigateBottomTab

@Composable
internal fun NavHostController.createBottomNavigationItems(
    currentBackStackEntry: NavBackStackEntry?,
    currentDestination: NavDestination?,
    onMapTabSelected: () -> Unit,
): List<BottomNavigationItem> =
    listOf(
        BottomNavigationItem(
            label = stringResource(AppR.string.bottom_tab_item_search),
            iconResId = CommonR.drawable.ic_symbol_recycle,
            selected = currentBackStackEntry.isItemSearchSelected(),
            onClick = {
                navigateItemSearchRoot(currentBackStackEntry)
            },
        ),
        BottomNavigationItem(
            label = stringResource(AppR.string.bottom_tab_map),
            iconResId = AppR.drawable.ic_navigation_map,
            selected = currentDestination?.hasRoute<MapRoute>() == true,
            onClick = {
                onMapTabSelected()
                navigateMapRoot(
                    currentBackStackEntry = currentBackStackEntry,
                )
            },
        ),
        BottomNavigationItem(
            label = stringResource(AppR.string.bottom_tab_regional_guide),
            iconResId = AppR.drawable.ic_navigation_guide,
            selected = currentBackStackEntry.isRegionalGuideSelected(),
            onClick = {
                navigateRegionalGuideRoot(currentBackStackEntry)
            },
        ),
        BottomNavigationItem(
            label = stringResource(AppR.string.bottom_tab_favorites),
            iconResId = CommonR.drawable.ic_favorite,
            selected = currentBackStackEntry.isFavoritesSelected(),
            onClick = {
                navigateFavoritesRootClearingRegionalGuideReentry(
                    currentBackStackEntry = currentBackStackEntry,
                )
            },
        ),
    )

private fun NavBackStackEntry?.isItemSearchSelected(): Boolean =
    this?.destination?.hasRoute<ItemSearchRoute>() == true ||
        this?.destination?.hasRoute<QuickCategorySettingsRoute>() == true ||
        this?.destination?.hasRoute<SettingsRoute>() == true ||
        this?.destination?.hasRoute<SettingsDetailRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.SEARCH)

private fun NavBackStackEntry?.isFavoritesSelected(): Boolean =
    this?.destination?.hasRoute<FavoritesRoute>() == true ||
        isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES)

private fun NavBackStackEntry?.isRegionalGuideSelected(): Boolean =
    this?.destination?.hasRoute<RegionalGuideRoute>() == true

private fun NavBackStackEntry?.isFavoriteRegionalGuideSelected(): Boolean =
    this != null &&
        destination.hasRoute<RegionalGuideRoute>() &&
        toRoute<RegionalGuideRoute>().isFavoriteReentryRoute()

private fun NavBackStackEntry?.isMapRegionalGuideSelected(): Boolean =
    this != null &&
        destination.hasRoute<RegionalGuideRoute>() &&
        toRoute<RegionalGuideRoute>().isMapReentryRoute()

internal fun RegionalGuideRoute.isFavoriteReentryRoute(): Boolean =
    !initialFavoriteTargetId.isNullOrBlank() &&
        entrySource == RegionalGuideEntrySource.FAVORITES

internal fun RegionalGuideRoute.isMapReentryRoute(): Boolean =
    initialFavoriteTargetId.isNullOrBlank() &&
        !initialAddress.isNullOrBlank()

private inline fun <reified T : Any> NavHostController.navigateBottomTabClearingRegionalGuideReentry(
    currentBackStackEntry: NavBackStackEntry?,
    route: T,
) {
    popRegionalGuideReentryToSourceRoot(currentBackStackEntry)

    navigateBottomTab<T, ItemSearchRoute>(route)
}

private fun NavBackStackEntry?.isItemGuideDetailSource(source: ItemGuideDetailSource): Boolean =
    this != null &&
        destination.hasRoute<ItemGuideDetailRoute>() &&
        toRoute<ItemGuideDetailRoute>().source == source

private fun NavHostController.navigateItemSearchRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    when {
        currentBackStackEntry?.destination?.hasRoute<ItemSearchRoute>() == true -> return
        currentBackStackEntry.isItemGuideDetailSource(ItemGuideDetailSource.SEARCH) -> {
            if (!popBackStack<ItemSearchRoute>(inclusive = false)) {
                navigateItemSearchTab()
            }
        }
        else -> {
            popRegionalGuideReentryToSourceRoot(currentBackStackEntry)
            navigateItemSearchTab()
        }
    }
}

private fun NavHostController.navigateRegionalGuideRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    val currentRoute = currentBackStackEntry
        ?.takeIf { entry -> entry.destination.hasRoute<RegionalGuideRoute>() }
        ?.toRoute<RegionalGuideRoute>()

    if (currentRoute == RegionalGuideRoute()) return

    popRegionalGuideReentryToSourceRoot(currentBackStackEntry)
    navigate(RegionalGuideRoute()) {
        popUpTo<ItemSearchRoute> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = false
    }
}

private fun NavHostController.navigateItemSearchTab() {
    navigate(ItemSearchRoute()) {
        popUpTo<ItemSearchRoute> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = false
    }
}

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
                popUpTo<ItemSearchRoute> {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = false
            }
        }
    }
}

private fun NavHostController.navigateMapRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    if (currentBackStackEntry.isMapRegionalGuideSelected()) {
        popBackStack<MapRoute>(inclusive = false)
        return
    }

    if (currentBackStackEntry.isRegionalGuideSelected()) {
        navigate(MapRoute()) {
            popUpTo<ItemSearchRoute> {
                saveState = true
            }
            launchSingleTop = true
            restoreState = false
        }
        return
    }

    navigateBottomTabClearingRegionalGuideReentry(
        currentBackStackEntry = currentBackStackEntry,
        route = MapRoute(),
    )
}

private fun NavHostController.navigateFavoritesRootClearingRegionalGuideReentry(
    currentBackStackEntry: NavBackStackEntry?,
) {
    if (currentBackStackEntry.isFavoriteRegionalGuideSelected()) {
        popBackStack<FavoritesRoute>(inclusive = false)
        return
    }

    popRegionalGuideReentryToSourceRoot(currentBackStackEntry)
    navigateFavoritesRoot(
        currentBackStackEntry = currentBackStackEntry,
    )
}

private fun NavHostController.popRegionalGuideReentryToSourceRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    when {
        currentBackStackEntry.isFavoriteRegionalGuideSelected() -> {
            popBackStack<FavoritesRoute>(inclusive = false)
        }
        currentBackStackEntry.isMapRegionalGuideSelected() -> {
            popBackStack<MapRoute>(inclusive = false)
        }
    }
}
