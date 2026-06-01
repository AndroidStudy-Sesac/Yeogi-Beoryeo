package com.team.yeogibeoryeo.presentation.search.model

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory

enum class RepresentativeGuideCategory(
    val displayName: String,
    val representativeGuideName: String,
    val disposalCategory: DisposalCategory,
) {
    PAPER("종이", "종이", DisposalCategory.PAPER),
    PAPER_PACK("종이팩", "종이팩", DisposalCategory.PAPER_PACK),
    COLORLESS_PET("무색페트병", "무색페트병", DisposalCategory.COLORLESS_PET),
    PLASTIC("플라스틱류", "플라스틱류", DisposalCategory.PLASTIC),
    VINYL("비닐류", "비닐류", DisposalCategory.VINYL),
    STYROFOAM("발포합성수지", "발포합성수지", DisposalCategory.STYROFOAM),
    GLASS("유리병", "유리병", DisposalCategory.GLASS),
    METAL("금속류", "금속류", DisposalCategory.METAL),
    CLOTHING("의류 및 원단", "의류 및 원단", DisposalCategory.CLOTHING),
    BATTERY("전지", "전지", DisposalCategory.BATTERY),
    LIGHTING("조명제품", "조명제품", DisposalCategory.LIGHTING),
    ELECTRONICS("전기전자제품", "전기전자제품", DisposalCategory.ELECTRONICS),
    FOOD_WASTE("음식물류폐기물", "음식물류폐기물", DisposalCategory.FOOD_WASTE),
    GENERAL("일반종량제폐기물", "일반종량제폐기물", DisposalCategory.GENERAL),
    NON_COMBUSTIBLE("불연성종량제폐기물", "불연성종량제폐기물", DisposalCategory.NON_COMBUSTIBLE),
    LARGE_WASTE("대형폐기물", "대형폐기물", DisposalCategory.LARGE_WASTE),
    CONSTRUCTION_WASTE("공사장 생활폐기물", "공사장 생활폐기물", DisposalCategory.CONSTRUCTION_WASTE),
    HAZARDOUS("생활계 유해폐기물", "생활계 유해폐기물", DisposalCategory.HAZARDOUS),
    OTHER("기타", "기타", DisposalCategory.OTHER),

    ;

    companion object {
        fun fromGuideName(name: String): RepresentativeGuideCategory? =
            entries.firstOrNull { it.representativeGuideName == name }

        fun fromDisposalCategory(category: DisposalCategory): RepresentativeGuideCategory =
            when (category) {
                DisposalCategory.PAPER -> PAPER
                DisposalCategory.PAPER_PACK -> PAPER_PACK
                DisposalCategory.COLORLESS_PET -> COLORLESS_PET
                DisposalCategory.GLASS -> GLASS
                DisposalCategory.METAL -> METAL
                DisposalCategory.PLASTIC -> PLASTIC
                DisposalCategory.STYROFOAM -> STYROFOAM
                DisposalCategory.VINYL -> VINYL
                DisposalCategory.FOOD_WASTE -> FOOD_WASTE
                DisposalCategory.LARGE_WASTE -> LARGE_WASTE
                DisposalCategory.ELECTRONICS -> ELECTRONICS
                DisposalCategory.BATTERY -> BATTERY
                DisposalCategory.LIGHTING -> LIGHTING
                DisposalCategory.CLOTHING -> CLOTHING
                DisposalCategory.HAZARDOUS -> HAZARDOUS
                DisposalCategory.NON_COMBUSTIBLE -> NON_COMBUSTIBLE
                DisposalCategory.CONSTRUCTION_WASTE -> CONSTRUCTION_WASTE
                DisposalCategory.GENERAL -> GENERAL
                DisposalCategory.OTHER -> OTHER
            }
    }
}
