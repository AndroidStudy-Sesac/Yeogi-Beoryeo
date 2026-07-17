package com.team.yeogibeoryeo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.common.navigation.BottomNavigationItem
import com.team.yeogibeoryeo.common.navigation.navigateBottomTab
import com.team.yeogibeoryeo.R as AppR
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
internal fun NavHostController.createBottomNavigationItems(
    currentBackStackEntry: NavBackStackEntry?,
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
            selected = currentBackStackEntry.isMapSelected(),
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
        isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES) ||
        isFavoriteRegionalGuideSelected()

private fun NavBackStackEntry?.isMapSelected(): Boolean =
    this?.destination?.hasRoute<MapRoute>() == true ||
        isMapRegionalGuideSelected()

private fun NavBackStackEntry?.isRegionalGuideSelected(): Boolean =
    this?.destination?.hasRoute<RegionalGuideRoute>() == true &&
        !isFavoriteRegionalGuideSelected() &&
        !isMapRegionalGuideSelected()

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

    if (currentRoute != null) {
        resetBottomTabToRoot(RegionalGuideRoute())
        return
    }

    popRegionalGuideReentryToSourceRoot(currentBackStackEntry)
    navigateBottomTab<RegionalGuideRoute, ItemSearchRoute>(RegionalGuideRoute())
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
        currentBackStackEntry?.destination?.hasRoute<FavoritesRoute>() == true -> {
            resetBottomTabToRoot(FavoritesRoute)
        }
        currentBackStackEntry.isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES) -> {
            popBackStack<FavoritesRoute>(inclusive = false)
        }
        else -> {
            navigateBottomTab<FavoritesRoute, ItemSearchRoute>(FavoritesRoute)
        }
    }
}

private fun NavHostController.navigateMapRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    if (currentBackStackEntry.isMapRegionalGuideSelected()) {
        resetBottomTabToRoot(MapRoute())
        return
    }

    if (currentBackStackEntry?.destination?.hasRoute<MapRoute>() == true) {
        resetBottomTabToRoot(MapRoute())
        return
    }

    if (currentBackStackEntry.isRegionalGuideSelected()) {
        navigateBottomTab<MapRoute, ItemSearchRoute>(MapRoute())
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
        resetBottomTabToRoot(FavoritesRoute)
        return
    }

    popRegionalGuideReentryToSourceRoot(currentBackStackEntry)
    navigateFavoritesRoot(
        currentBackStackEntry = currentBackStackEntry,
    )
}

private inline fun <reified T : Any> NavHostController.resetBottomTabToRoot(route: T) {
    popBackStack<T>(inclusive = true)
    clearBackStack<T>()
    navigateBottomTab<T, ItemSearchRoute>(route)
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
