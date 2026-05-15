package com.team.yeogibeoryeo.domain.regionalguide.model

/**
 * 지역별 배출 폐기물의 종류를 정의하는 Enum
 */
enum class RegionalWasteType(
    val description: String
) {
    GENERAL("일반쓰레기"),
    FOOD("음식물쓰레기"),
    RECYCLABLE("재활용품"),
    LARGE_ITEM("대형폐기물"),
    UNKNOWN("알 수 없음")
}