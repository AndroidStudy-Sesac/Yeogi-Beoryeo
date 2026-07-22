package com.team.yeogibeoryeo.navigation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naver.maps.map.app.LegalNoticeActivity
import com.naver.maps.map.app.OpenSourceLicenseActivity
import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.appguide.AppGuideOverlay
import com.team.yeogibeoryeo.appguide.AppGuideStep
import com.team.yeogibeoryeo.appguide.AppGuideViewModel
import com.team.yeogibeoryeo.common.navigation.AppBottomNavigationBar
import com.team.yeogibeoryeo.presentation.map.CollectionSpotMapScreen
import com.team.yeogibeoryeo.presentation.favorites.FavoritesRoute as FavoritesScreenRoute
import com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideRoute as RegionalGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemGuideDetailRoute as ItemGuideDetailScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemSearchRoute as ItemSearchScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemUsefulGuideRoute as ItemUsefulGuideScreenRoute
import com.team.yeogibeoryeo.presentation.search.ItemSearchGuideTarget
import com.team.yeogibeoryeo.presentation.search.QuickCategorySettingsRoute as QuickCategorySettingsScreenRoute
import com.team.yeogibeoryeo.presentation.settings.SettingsDetailRoute as SettingsDetailScreenRoute
import com.team.yeogibeoryeo.presentation.settings.SettingsRoute as SettingsScreenRoute

@Composable
fun YeogiBeoryeoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    appGuideViewModel: AppGuideViewModel = hiltViewModel(),
) {
    val currentContext by rememberUpdatedState(LocalContext.current)
    val layoutDirection = LocalLayoutDirection.current
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentDestination = currentBackStackEntry?.destination
    val appGuideUiState by appGuideViewModel.uiState.collectAsStateWithLifecycle()
    val isMapScreen = currentDestination?.hasRoute<MapRoute>() == true
    val isItemSearchScreen = currentDestination?.hasRoute<ItemSearchRoute>() == true
    val isItemDetailScreen = currentDestination?.hasRoute<ItemGuideDetailRoute>() == true
    val isUsefulGuideScreen = currentDestination?.hasRoute<ItemUsefulGuideRoute>() == true
    val isSettingsDetailScreen = currentDestination?.hasRoute<SettingsDetailRoute>() == true
    var isItemSearchBottomBarScrollEnabled by remember { mutableStateOf(false) }
    val hidesBottomBarOnScroll =
        (isItemSearchScreen && isItemSearchBottomBarScrollEnabled) ||
            isItemDetailScreen ||
            isUsefulGuideScreen ||
            isSettingsDetailScreen
    var isBottomBarVisible by remember { mutableStateOf(true) }
    var appGuideTargetBounds by remember {
        mutableStateOf<Map<AppGuideStep, Rect>>(emptyMap())
    }
    var bottomNavigationItemBounds by remember {
        mutableStateOf<Map<Int, Rect>>(emptyMap())
    }
    val isAppGuideRunningOnHome = appGuideUiState.isVisible && isItemSearchScreen
    val itemSearchGuideTarget =
        if (isAppGuideRunningOnHome) {
            when (appGuideUiState.currentStep) {
                AppGuideStep.ITEM_SEARCH -> ItemSearchGuideTarget.SEARCH
                AppGuideStep.QUICK_CATEGORY -> ItemSearchGuideTarget.QUICK_CATEGORY
                else -> null
            }
        } else {
            null
        }
    val currentAppGuideTargetBounds =
        when (appGuideUiState.currentStep) {
            AppGuideStep.ITEM_SEARCH,
            AppGuideStep.QUICK_CATEGORY,
            -> appGuideTargetBounds[appGuideUiState.currentStep]

            AppGuideStep.MAP,
            AppGuideStep.REGIONAL_GUIDE,
            AppGuideStep.FAVORITES,
            -> bottomNavigationItemBounds[appGuideUiState.currentStep.bottomTabIndex()]
        }
    val showAppGuideOverlay =
        isAppGuideRunningOnHome && currentAppGuideTargetBounds != null

    LaunchedEffect(currentBackStackEntry) {
        isBottomBarVisible = true
        if (!isItemSearchScreen && isItemSearchBottomBarScrollEnabled) {
            isItemSearchBottomBarScrollEnabled = false
        }
    }

    LaunchedEffect(isAppGuideRunningOnHome, appGuideUiState.currentStep) {
        if (isAppGuideRunningOnHome) {
            isBottomBarVisible = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showAppGuideOverlay) {
                        Modifier.clearAndSetSemantics { }
                    } else {
                        Modifier
                    },
                ),
            bottomBar = {
                val enterTransition =
                    if (hidesBottomBarOnScroll) {
                        fadeIn() +
                            expandVertically(expandFrom = Alignment.Bottom) +
                            slideInVertically { it }
                    } else {
                        EnterTransition.None
                    }
                val exitTransition =
                    if (hidesBottomBarOnScroll) {
                        fadeOut() +
                            shrinkVertically(shrinkTowards = Alignment.Bottom) +
                            slideOutVertically { it }
                    } else {
                        ExitTransition.None
                    }

                AnimatedVisibility(
                    visible = isAppGuideRunningOnHome || isBottomBarVisible,
                    enter = enterTransition,
                    exit = exitTransition,
                ) {
                    AppBottomNavigationBar(
                        items = navController.createBottomNavigationItems(
                            currentBackStackEntry = currentBackStackEntry,
                            onMapTabSelected = { isBottomBarVisible = true },
                        ),
                        itemModifier = { index ->
                            Modifier.onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInRoot()
                                if (bottomNavigationItemBounds[index] != bounds) {
                                    bottomNavigationItemBounds =
                                        bottomNavigationItemBounds + (index to bounds)
                                }
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            val navHostPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = 0.dp,
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = when {
                    isMapScreen && !isBottomBarVisible -> 0.dp
                    hidesBottomBarOnScroll && !isBottomBarVisible ->
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    else -> innerPadding.calculateBottomPadding()
                },
            )

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
                        onBottomBarVisibilityChanged = { isVisible ->
                            if (isItemSearchScreen) {
                                isBottomBarVisible = isVisible
                            }
                        },
                        onItemSearchBottomBarScrollEnabledChanged = { isEnabled ->
                            if (
                                isItemSearchScreen &&
                                isItemSearchBottomBarScrollEnabled != isEnabled
                            ) {
                                isItemSearchBottomBarScrollEnabled = isEnabled
                            }
                        },
                        appGuideTarget = itemSearchGuideTarget,
                        searchGuideModifier = Modifier.appGuideTarget(
                            step = AppGuideStep.ITEM_SEARCH,
                            onBoundsChanged = { step, bounds ->
                                if (appGuideTargetBounds[step] != bounds) {
                                    appGuideTargetBounds = appGuideTargetBounds + (step to bounds)
                                }
                            },
                        ),
                        quickCategoryGuideModifier = Modifier.appGuideTarget(
                            step = AppGuideStep.QUICK_CATEGORY,
                            onBoundsChanged = { step, bounds ->
                                if (appGuideTargetBounds[step] != bounds) {
                                    appGuideTargetBounds = appGuideTargetBounds + (step to bounds)
                                }
                            },
                        ),
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
                        onAppGuideClick = {
                            appGuideTargetBounds = emptyMap()
                            navController.navigate(ItemSearchRoute()) {
                                popUpTo<ItemSearchRoute> {
                                    inclusive = false
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            appGuideViewModel.startGuide()
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
                        onOpenPrivacyPolicyClick = {
                            currentContext.openUrl(PRIVACY_POLICY_URL)
                        },
                        onOpenNaverMapLegalNoticeClick = {
                            currentContext.openNaverMapLegalNotice()
                        },
                        onOpenNaverMapOpenSourceLicenseClick = {
                            currentContext.openNaverMapOpenSourceLicense()
                        },
                        onOpenSourceClick = currentContext::openUrl,
                        onBottomBarVisibilityChanged = { isVisible ->
                            if (isSettingsDetailScreen) {
                                isBottomBarVisible = isVisible
                            }
                        },
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

        if (showAppGuideOverlay) {
            AppGuideOverlay(
                step = appGuideUiState.currentStep,
                targetBounds = requireNotNull(currentAppGuideTargetBounds),
                onPrevious = appGuideViewModel::showPreviousStep,
                onNext = appGuideViewModel::showNextStep,
                onSkip = appGuideViewModel::skipGuide,
            )
        }
    }
}

private fun Modifier.appGuideTarget(
    step: AppGuideStep,
    onBoundsChanged: (AppGuideStep, Rect) -> Unit,
): Modifier = onGloballyPositioned { coordinates ->
    onBoundsChanged(step, coordinates.boundsInRoot())
}

private fun AppGuideStep.bottomTabIndex(): Int =
    when (this) {
        AppGuideStep.MAP -> MAP_BOTTOM_TAB_INDEX
        AppGuideStep.REGIONAL_GUIDE -> REGIONAL_GUIDE_BOTTOM_TAB_INDEX
        AppGuideStep.FAVORITES -> FAVORITES_BOTTOM_TAB_INDEX
        AppGuideStep.ITEM_SEARCH,
        AppGuideStep.QUICK_CATEGORY,
        -> error("홈 가이드 단계에는 하단 탭 인덱스가 없습니다.")
    }

private const val MAP_BOTTOM_TAB_INDEX = 1
private const val REGIONAL_GUIDE_BOTTOM_TAB_INDEX = 2
private const val FAVORITES_BOTTOM_TAB_INDEX = 3

private const val FreePickupGuideUrl = "https://www.15990903.or.kr/portal/cnts/userGuide.do"
private const val PRIVACY_POLICY_URL =
    "https://androidstudy-sesac.github.io/Yeogi-Beoryeo/privacy-policy/"

private fun android.content.Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private fun android.content.Context.openAppSettings() {
    val uri = Uri.fromParts("package", packageName, null)
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    runCatching {
        startActivity(intent)
    }
}

private fun android.content.Context.openNaverMapLegalNotice() {
    runCatching {
        startActivity(Intent(this, LegalNoticeActivity::class.java))
    }
}

private fun android.content.Context.openNaverMapOpenSourceLicense() {
    runCatching {
        startActivity(Intent(this, OpenSourceLicenseActivity::class.java))
    }
}
