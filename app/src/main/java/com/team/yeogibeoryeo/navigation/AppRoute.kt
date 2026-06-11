package com.team.yeogibeoryeo.navigation

import kotlinx.serialization.Serializable

@Serializable
data class MapRoute(
    val favoriteSpotTargetId: String? = null,
    val favoriteSpotName: String? = null,
    val favoriteSpotType: String? = null,
    val favoriteSpotAddress: String? = null,
    val favoriteSpotDetailLocation: String? = null,
    val favoriteSpotLatitude: Double? = null,
    val favoriteSpotLongitude: Double? = null,
)

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
    val source: ItemGuideDetailSource = ItemGuideDetailSource.SEARCH,
)

@Serializable
enum class ItemGuideDetailSource {
    SEARCH,
    FAVORITES,
}
