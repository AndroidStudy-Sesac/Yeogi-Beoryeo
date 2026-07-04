package com.team.yeogibeoryeo.navigation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.core.net.toUri
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.common.navigation.AppBottomNavigationBar
import com.team.yeogibeoryeo.presentation.favorites.FavoritesRoute as FavoritesScreenRoute
import com.team.yeogibeoryeo.presentation.map.CollectionSpotMapScreen
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideRoute as RegionalGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemGuideDetailRoute as ItemGuideDetailScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemSearchRoute as ItemSearchScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemUsefulGuideRoute as ItemUsefulGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.QuickCategorySettingsRoute as QuickCategorySettingsScreenRoute
import com.team.yeogibeoryeo.presentation.settings.SettingsDetailRoute as SettingsDetailScreenRoute
import com.team.yeogibeoryeo.presentation.settings.SettingsRoute as SettingsScreenRoute

@Composable
fun YeogiBeoryeoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onClearLocationCacheClick: () -> Unit = {},
) {
    val currentContext by rememberUpdatedState(LocalContext.current)
    val layoutDirection = LocalLayoutDirection.current
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val isMapScreen = currentDestination?.hasRoute<MapRoute>() == true
    val isItemDetailScreen = currentDestination?.hasRoute<ItemGuideDetailRoute>() == true
    val isUsefulGuideScreen = currentDestination?.hasRoute<ItemUsefulGuideRoute>() == true
    val hidesBottomBarOnScroll = isItemDetailScreen || isUsefulGuideScreen
    var isBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentBackStackEntry) {
        isBottomBarVisible = true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val enterTransition =
                if (hidesBottomBarOnScroll) {
                    fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + slideInVertically { it }
                } else {
                    EnterTransition.None
                }
            val exitTransition =
                if (hidesBottomBarOnScroll) {
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
        val navHostPadding = if (isMapScreen) {
            PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = 0.dp,
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = if (isBottomBarVisible) {
                    innerPadding.calculateBottomPadding()
                } else {
                    0.dp
                },
            )
        } else {
            innerPadding
        }

        NavHost(
            navController = navController,
            startDestination = ItemSearchRoute(),
            modifier = Modifier.padding(navHostPadding),
        ) {
            composable<MapRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<MapRoute>()
                CollectionSpotMapScreen(
                    initialSpotType = route.toInitialCollectionSpotTypeOrNull(),
                    favoriteSpotMoveRequest = route.toFavoriteSpotMapMoveRequest(),
                    onBottomBarVisibilityChanged = { isVisible ->
                        if (isMapScreen) {
                            isBottomBarVisible = isVisible
                        }
                    },
                    onRegionalGuideClick = { address ->
                        address.toRegionalGuideAddressRouteOrNull()
                            ?.let { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
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
                            popUpTo<ItemSearchRoute> {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onRegionalGuideClick = { targetId ->
                        navController.navigate(
                            RegionalGuideRoute(
                                initialFavoriteTargetId = targetId,
                                entrySource = RegionalGuideEntrySource.FAVORITES,
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
                    onUsefulGuideClick = { guide ->
                        navController.navigate(ItemUsefulGuideRoute(guide.type.toRouteType()))
                    },
                    onRegionalGuideSummaryClick = { targetId ->
                        navController.navigate(
                            RegionalGuideRoute(
                                initialFavoriteTargetId = targetId,
                            ),
                        )
                    },
                    onQuickCategorySettingsClick = { maxSelectedCount ->
                        navController.navigate(
                            QuickCategorySettingsRoute(maxSelectedCount = maxSelectedCount),
                        )
                    },
                    onSettingsClick = {
                        navController.navigate(SettingsRoute)
                    },
                )
            }

            composable<QuickCategorySettingsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<QuickCategorySettingsRoute>()
                QuickCategorySettingsScreenRoute(
                    maxSelectedCount = route.maxSelectedCount,
                    onBackClick = navController::popBackStack,
                )
            }

            composable<SettingsRoute> {
                SettingsScreenRoute(
                    onBackClick = navController::popBackStack,
                    onDetailClick = { detailType ->
                        navController.navigate(SettingsDetailRoute(detailType.toRouteType()))
                    },
                )
            }

            composable<SettingsDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<SettingsDetailRoute>()
                SettingsDetailScreenRoute(
                    detailType = route.detailType.toScreenType(),
                    appVersionName = BuildConfig.VERSION_NAME,
                    onBackClick = navController::popBackStack,
                    onOpenAppSettingsClick = {
                        currentContext.openAppSettings()
                    },
                    onClearLocationCacheClick = onClearLocationCacheClick,
                )
            }

            composable<ItemUsefulGuideRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItemUsefulGuideRoute>()
                ItemUsefulGuideScreenRoute(
                    guideType = route.guideType.toItemUsefulGuideType(),
                    onBackClick = navController::popBackStack,
                    onSmallEWasteClick = { type ->
                        navController.navigate(MapRoute(initialSpotType = type.toRouteType())) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onFreePickupGuideClick = {
                        runCatching {
                            currentContext.startActivity(
                                Intent(Intent.ACTION_VIEW, FreePickupGuideUrl.toUri()),
                            )
                        }.isSuccess
                    },
                    onOfficialSiteClick = { url ->
                        runCatching {
                            currentContext.startActivity(
                                Intent(Intent.ACTION_VIEW, url.toUri()),
                            )
                        }.isSuccess
                    },
                    onRegionalGuideClick = {
                        navController.navigate(RegionalGuideRoute()) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onItemSearchClick = navController::popBackStack,
                    onBottomBarVisibilityChanged = { isVisible ->
                        if (isUsefulGuideScreen) {
                            isBottomBarVisible = isVisible
                        }
                    },
                )
            }

            composable<ItemGuideDetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<ItemGuideDetailRoute>()
                ItemGuideDetailScreenRoute(
                    guideId = route.guideId,
                    onBackClick = navController::popBackStack,
                    onCollectionSpotTypeClick = { type ->
                        navController.navigate(MapRoute(initialSpotType = type.toRouteType())) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
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

private const val FreePickupGuideUrl = "https://www.15990903.or.kr/portal/cnts/userGuide.do"

private fun android.content.Context.openAppSettings() {
    val uri = Uri.fromParts("package", packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    runCatching {
        startActivity(intent)
    }
}
