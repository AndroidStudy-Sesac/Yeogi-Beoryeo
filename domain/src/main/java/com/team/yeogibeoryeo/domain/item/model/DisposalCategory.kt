package com.team.yeogibeoryeo.domain.item.model

enum class DisposalCategory(
    val displayName: String,
) {
    PAPER("종이"),
    PAPER_PACK("종이팩"),
    COLORLESS_PET("무색페트병"),
    GLASS("유리병"),
    METAL("금속류"),
    PLASTIC("플라스틱류"),
    STYROFOAM("발포합성수지"),
    VINYL("비닐류"),
    FOOD_WASTE("음식물류폐기물"),
    LARGE_WASTE("대형폐기물"),
    ELECTRONICS("전기전자제품"),
    BATTERY("전지"),
    LIGHTING("조명제품"),
    CLOTHING("의류 및 원단"),
    HAZARDOUS("생활계 유해폐기물"),
    NON_COMBUSTIBLE("불연성종량제폐기물"),
    CONSTRUCTION_WASTE("공사장 생활폐기물"),
    GENERAL("일반종량제폐기물"),
    OTHER("기타"),
    ;

    companion object {
        fun fromDisplayName(displayName: String?): DisposalCategory? =
            entries.firstOrNull { it.displayName == displayName }
    }
}
