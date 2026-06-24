package com.team.yeogibeoryeo.common.navigation

import androidx.navigation.NavHostController

inline fun <reified T : Any, reified StartDestination : Any> NavHostController.navigateBottomTab(route: T) {
    navigate(route) {
        popUpTo<StartDestination> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
