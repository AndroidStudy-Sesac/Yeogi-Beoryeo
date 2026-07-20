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
                navigateFavoritesRoot(
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
        isRegionalGuideBottomTabSelected(BottomTab.FAVORITES)

private fun NavBackStackEntry?.isMapSelected(): Boolean =
    this?.destination?.hasRoute<MapRoute>() == true ||
        isRegionalGuideBottomTabSelected(BottomTab.MAP)

private fun NavBackStackEntry?.isRegionalGuideSelected(): Boolean =
    isRegionalGuideBottomTabSelected(BottomTab.REGIONAL_GUIDE)

private fun NavBackStackEntry?.regionalGuideBottomTab(): BottomTab? =
    this
        ?.takeIf { entry -> entry.destination.hasRoute<RegionalGuideRoute>() }
        ?.toRoute<RegionalGuideRoute>()
        ?.bottomTab()

private fun NavBackStackEntry?.isRegionalGuideBottomTabSelected(tab: BottomTab): Boolean =
    regionalGuideBottomTab()?.navigationActionFor(tab) == BottomTabNavigationAction.RESET_TO_ROOT

internal enum class BottomTab {
    MAP,
    REGIONAL_GUIDE,
    FAVORITES,
}

internal enum class BottomTabNavigationAction {
    RESTORE_STATE,
    RESET_TO_ROOT,
}

internal fun BottomTab.navigationActionFor(targetTab: BottomTab): BottomTabNavigationAction =
    if (this == targetTab) {
        BottomTabNavigationAction.RESET_TO_ROOT
    } else {
        BottomTabNavigationAction.RESTORE_STATE
    }

internal fun RegionalGuideRoute.bottomTab(): BottomTab =
    when {
        isFavoriteReentryRoute() -> BottomTab.FAVORITES
        isMapReentryRoute() -> BottomTab.MAP
        else -> BottomTab.REGIONAL_GUIDE
    }

internal fun RegionalGuideRoute.isFavoriteReentryRoute(): Boolean =
    !initialFavoriteTargetId.isNullOrBlank() &&
        entrySource == RegionalGuideEntrySource.FAVORITES

internal fun RegionalGuideRoute.isMapReentryRoute(): Boolean =
    initialFavoriteTargetId.isNullOrBlank() &&
        !initialAddress.isNullOrBlank()

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
        else -> navigateItemSearchTab()
    }
}

private fun NavHostController.navigateRegionalGuideRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    if (currentBackStackEntry.isRegionalGuideSelected()) {
        resetBottomTabToRoot(RegionalGuideRoute())
        return
    }

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
        currentBackStackEntry.isItemGuideDetailSource(ItemGuideDetailSource.FAVORITES) ||
            currentBackStackEntry.isFavoritesSelected() -> {
            resetBottomTabToRoot(FavoritesRoute)
        }
        else -> {
            navigateBottomTab<FavoritesRoute, ItemSearchRoute>(FavoritesRoute)
        }
    }
}

private fun NavHostController.navigateMapRoot(
    currentBackStackEntry: NavBackStackEntry?,
) {
    if (currentBackStackEntry.isMapSelected()) {
        resetBottomTabToRoot(MapRoute())
        return
    }

    navigateBottomTab<MapRoute, ItemSearchRoute>(MapRoute())
}

private inline fun <reified T : Any> NavHostController.resetBottomTabToRoot(route: T) {
    popBackStack<T>(inclusive = true)
    clearBackStack<T>()
    navigateBottomTab<T, ItemSearchRoute>(route)
}
