package com.team.yeogibeoryeo.presentation.search.model

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory

enum class RepresentativeGuideCategory(
    val displayName: String,
    val representativeGuideName: String,
    val disposalCategory: DisposalCategory,
) {
    CONSTRUCTION_WASTE("공사장 생활폐기물", "공사장 생활폐기물", DisposalCategory.CONSTRUCTION_WASTE),
    METAL("금속", "고철", DisposalCategory.METAL),
    LARGE_WASTE("대형폐기물", "대형폐기물", DisposalCategory.LARGE_WASTE),
    COLORLESS_PET("무색페트병", "무색페트병", DisposalCategory.COLORLESS_PET),
    STYROFOAM("스티로폼", "스티로폼", DisposalCategory.STYROFOAM),
    NON_COMBUSTIBLE("불연성 폐기물", "불연성종량제폐기물", DisposalCategory.NON_COMBUSTIBLE),
    VINYL("비닐", "비닐", DisposalCategory.VINYL),
    HAZARDOUS("생활계 유해폐기물", "생활계 유해폐기물", DisposalCategory.HAZARDOUS),
    GLASS("유리병", "유리병", DisposalCategory.GLASS),
    FOOD_WASTE("음식물", "음식물쓰레기", DisposalCategory.FOOD_WASTE),
    CLOTHING("의류", "의류", DisposalCategory.CLOTHING),
    GENERAL("일반쓰레기", "일반종량제폐기물", DisposalCategory.GENERAL),
    ELECTRONICS("전자제품", "전기전자제품", DisposalCategory.ELECTRONICS),
    BATTERY("배터리", "건전지", DisposalCategory.BATTERY),
    LIGHTING("조명", "형광등", DisposalCategory.LIGHTING),
    PAPER("종이", "종이", DisposalCategory.PAPER),
    PAPER_PACK("종이팩", "종이팩", DisposalCategory.PAPER_PACK),
    PLASTIC("플라스틱", "플라스틱 용기", DisposalCategory.PLASTIC),
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
