package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.components.MapSpotFilterChipPolicy

internal fun MapRoute.toInitialCollectionSpotTypeOrNull(): CollectionSpotType? {
    val type = initialSpotType
        ?.toCollectionSpotType()
        ?: return null

    return type.takeIf { it in MapSpotFilterChipPolicy.visibleTypes }
}
