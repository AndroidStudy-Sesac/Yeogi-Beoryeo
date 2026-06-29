package com.team.yeogibeoryeo.presentation.map.components

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

object MapSpotFilterChipPolicy {
    val visibleTypes: List<CollectionSpotType> = listOf(
        CollectionSpotType.SMALL_E_WASTE_BIN,
        CollectionSpotType.BATTERY_BIN,
        CollectionSpotType.PHONE_DROP_OFF,
        CollectionSpotType.FLUORESCENT_LAMP_BIN,
        CollectionSpotType.MEDICINE_DROP_BOX,
        CollectionSpotType.CLOTHING_BIN,
        CollectionSpotType.ICE_PACK_BIN,
        CollectionSpotType.WASTE_COOKING_OIL_BIN,
        CollectionSpotType.HAZARDOUS_WASTE_BIN,
        CollectionSpotType.RECYCLING_CENTER,
        CollectionSpotType.STANDARD_BAG_STORE,
        CollectionSpotType.OTHER,
    )
}
