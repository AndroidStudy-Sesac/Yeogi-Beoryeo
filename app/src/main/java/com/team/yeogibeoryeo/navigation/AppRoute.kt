package com.team.yeogibeoryeo.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object MapRoute

@Serializable
data class RegionalGuideRoute(
    val initialKeyword: String? = null,
    val initialAddress: String? = null,
)

@Serializable
data object FavoritesRoute

@Serializable
data class ItemSearchRoute(
    val initialQuery: String? = null,
)

@Serializable
data class ItemGuideDetailRoute(
    val guideId: String,
)
