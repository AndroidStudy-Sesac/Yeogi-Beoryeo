package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.map.components.MapSpotFilterChipPolicy

internal fun MapRoute.toInitialCollectionSpotTypeOrNull(): CollectionSpotType? {
    val type = initialSpotType
        ?.let { typeName ->
            runCatching { CollectionSpotType.valueOf(typeName) }.getOrNull()
        }
        ?: return null

    return type.takeIf { it in MapSpotFilterChipPolicy.visibleTypes }
}
