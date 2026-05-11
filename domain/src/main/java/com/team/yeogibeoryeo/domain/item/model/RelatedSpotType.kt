package com.team.yeogibeoryeo.domain.item.model

enum class RelatedSpotType(
    val displayName: String,
) {
    RECYCLING_BIN("재활용폐기물 수거함"),
    GENERAL_WASTE_BAG("종량제봉투"),
    DEDICATED_BIN("전용수거함"),
    LARGE_WASTE("대형폐기물 신고"),
    FOOD_WASTE_BIN("음식물류폐기물 수거함"),
    SPECIAL_BAG("특수규격봉투"),
    DEPOSIT_REFUND("보증금 환급처"),
    FREE_PICKUP("무상방문수거"),
    E_WASTE_BIN("폐가전수거함"),
    SMALL_E_WASTE_BIN("중소형 가전 수거함"),
    COLLECTION_CENTER("공동집하장"),
    CONTACT_REQUIRED("담당기관 문의"),
    OTHER("기타"),
}
