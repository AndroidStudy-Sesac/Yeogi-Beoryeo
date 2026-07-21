package com.team.yeogibeoryeo.presentation.map.mapper

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

fun CollectionSpotType.toDisplayName(): String =
    when (this) {
        CollectionSpotType.SMALL_E_WASTE_BIN -> "중소형 수거함"
        CollectionSpotType.BATTERY_BIN -> "폐건전지"
        CollectionSpotType.PHONE_DROP_OFF -> "폐휴대폰"
        CollectionSpotType.RECYCLING_CENTER -> "재활용센터"
        CollectionSpotType.STANDARD_BAG_STORE -> "종량제봉투"
        CollectionSpotType.MEDICINE_DROP_BOX -> "폐의약품"
        CollectionSpotType.FLUORESCENT_LAMP_BIN -> "폐형광등"
        CollectionSpotType.CLOTHING_BIN -> "의류수거함"
        CollectionSpotType.ICE_PACK_BIN -> "아이스팩"
        CollectionSpotType.WASTE_COOKING_OIL_BIN -> "폐식용유"
        CollectionSpotType.HAZARDOUS_WASTE_BIN -> "생활계 유해폐기물"
        CollectionSpotType.OTHER -> "기타"
    }

fun CollectionSpotType.toFilterEmptyResultDisplayName(): String =
    when (this) {
        CollectionSpotType.SMALL_E_WASTE_BIN -> "중소형 수거함"
        CollectionSpotType.BATTERY_BIN -> "폐건전지 수거함"
        CollectionSpotType.PHONE_DROP_OFF -> "폐휴대폰 수거함"
        CollectionSpotType.RECYCLING_CENTER -> "재활용센터"
        CollectionSpotType.STANDARD_BAG_STORE -> "종량제봉투 판매처"
        CollectionSpotType.MEDICINE_DROP_BOX -> "폐의약품 수거함"
        CollectionSpotType.FLUORESCENT_LAMP_BIN -> "폐형광등 수거함"
        CollectionSpotType.CLOTHING_BIN -> "의류수거함"
        CollectionSpotType.ICE_PACK_BIN -> "아이스팩 수거함"
        CollectionSpotType.WASTE_COOKING_OIL_BIN -> "폐식용유 수거함"
        CollectionSpotType.HAZARDOUS_WASTE_BIN -> "생활계 유해폐기물 수거함"
        CollectionSpotType.OTHER -> "기타 수거 장소"
    }
