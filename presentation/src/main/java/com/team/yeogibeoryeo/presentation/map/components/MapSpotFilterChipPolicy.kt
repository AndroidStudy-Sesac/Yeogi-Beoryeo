package com.team.yeogibeoryeo.presentation.map.components

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

object MapSpotFilterChipPolicy {
    val visibleTypes: List<CollectionSpotType> = listOf(
        CollectionSpotType.SMALL_E_WASTE_BIN,
        CollectionSpotType.BATTERY_BIN,
        CollectionSpotType.PHONE_DROP_OFF,
        CollectionSpotType.RECYCLING_CENTER,
        CollectionSpotType.STANDARD_BAG_STORE,
        CollectionSpotType.OTHER,
    )
}
