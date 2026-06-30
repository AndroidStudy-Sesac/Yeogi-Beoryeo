package com.team.yeogibeoryeo.navigation

import kotlinx.serialization.Serializable

@Serializable
data class MapRoute(
    val initialSpotType: CollectionSpotRouteType? = null,
    val favoriteSpotRequestId: String? = null,
    val favoriteSpotTargetId: String? = null,
    val favoriteSpotName: String? = null,
    val favoriteSpotType: CollectionSpotRouteType? = null,
    val favoriteSpotAddress: String? = null,
    val favoriteSpotDetailLocation: String? = null,
    val favoriteSpotLatitude: Double? = null,
    val favoriteSpotLongitude: Double? = null,
)

@Serializable
enum class CollectionSpotRouteType {
    SMALL_E_WASTE_BIN,
    BATTERY_BIN,
    PHONE_DROP_OFF,
    RECYCLING_CENTER,
    STANDARD_BAG_STORE,
    MEDICINE_DROP_BOX,
    FLUORESCENT_LAMP_BIN,
    CLOTHING_BIN,
    ICE_PACK_BIN,
    WASTE_COOKING_OIL_BIN,
    HAZARDOUS_WASTE_BIN,
    OTHER,
}

@Serializable
data class RegionalGuideRoute(
    val initialKeyword: String? = null,
    val initialAddress: String? = null,
    val initialFavoriteTargetId: String? = null,
    val entrySource: RegionalGuideEntrySource? = null,
)

@Serializable
enum class RegionalGuideEntrySource {
    FAVORITES,
}

internal fun String.toRegionalGuideAddressRouteOrNull(): RegionalGuideRoute? =
    trim()
        .takeIf { address -> address.isNotBlank() }
        ?.let { address -> RegionalGuideRoute(initialAddress = address) }

@Serializable
data object FavoritesRoute

@Serializable
data class ItemSearchRoute(
    val initialQuery: String? = null,
)

@Serializable
data class QuickCategorySettingsRoute(
    val maxSelectedCount: Int,
)

@Serializable
data object SettingsRoute

@Serializable
data class SettingsDetailRoute(
    val detailType: SettingsDetailRouteType,
)

@Serializable
enum class SettingsDetailRouteType {
    Notice,
    Contact,
    AppInfo,
    LocationPermission,
    Terms,
    Sources,
    Cache,
}

@Serializable
data class ItemUsefulGuideRoute(
    val guideType: ItemUsefulGuideRouteType,
)

@Serializable
enum class ItemUsefulGuideRouteType {
    SMALL_E_WASTE,
    REGIONAL_GUIDE,
    REPRESENTATIVE_CATEGORY,
    ITEM_DICTIONARY,
}

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
