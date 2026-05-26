package com.team.yeogibeoryeo.common.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

inline fun <reified T : Any> NavDestination?.hasRouteName(): Boolean =
    this?.route?.substringBefore("?") == requireNotNull(T::class.qualifiedName)

inline fun <reified T : Any> NavHostController.navigateBottomTab(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
