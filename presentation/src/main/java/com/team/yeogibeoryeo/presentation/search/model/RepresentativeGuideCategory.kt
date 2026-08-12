package com.team.yeogibeoryeo.presentation.search.model

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory

enum class RepresentativeGuideCategory(
    val displayName: String,
    val representativeGuideName: String,
    val representativeGuideId: String,
    val disposalCategory: DisposalCategory,
) {
    PAPER("종이", "종이", "item-guide-0001", DisposalCategory.PAPER),
    PAPER_PACK("종이팩", "종이팩", "item-guide-0002", DisposalCategory.PAPER_PACK),
    COLORLESS_PET("무색페트병", "무색페트병", "item-guide-0003", DisposalCategory.COLORLESS_PET),
    PLASTIC("플라스틱류", "플라스틱류", "item-guide-0004", DisposalCategory.PLASTIC),
    VINYL("비닐류", "비닐류", "item-guide-0005", DisposalCategory.VINYL),
    STYROFOAM("발포합성수지", "발포합성수지", "item-guide-0006", DisposalCategory.STYROFOAM),
    GLASS("유리병", "유리병", "item-guide-0007", DisposalCategory.GLASS),
    METAL("금속류", "금속류", "item-guide-0008", DisposalCategory.METAL),
    CLOTHING("의류 및 원단", "의류 및 원단", "item-guide-0009", DisposalCategory.CLOTHING),
    BATTERY("전지", "전지", "item-guide-0010", DisposalCategory.BATTERY),
    LIGHTING("조명제품", "조명제품", "item-guide-0011", DisposalCategory.LIGHTING),
    ELECTRONICS("전기전자제품", "전기전자제품", "item-guide-0012", DisposalCategory.ELECTRONICS),
    FOOD_WASTE("음식물류폐기물", "음식물류폐기물", "item-guide-0013", DisposalCategory.FOOD_WASTE),
    GENERAL("일반종량제폐기물", "일반종량제폐기물", "item-guide-0014", DisposalCategory.GENERAL),
    NON_COMBUSTIBLE(
        "불연성종량제폐기물",
        "불연성종량제폐기물",
        "item-guide-0015",
        DisposalCategory.NON_COMBUSTIBLE,
    ),
    LARGE_WASTE("대형폐기물", "대형폐기물", "item-guide-0016", DisposalCategory.LARGE_WASTE),
    CONSTRUCTION_WASTE(
        "공사장 생활폐기물",
        "공사장 생활폐기물",
        "item-guide-0017",
        DisposalCategory.CONSTRUCTION_WASTE,
    ),
    HAZARDOUS(
        "생활계 유해폐기물",
        "생활계 유해폐기물",
        "item-guide-0018",
        DisposalCategory.HAZARDOUS,
    ),
    OTHER("기타", "기타", "item-guide-0019", DisposalCategory.OTHER),

    ;

    companion object {
        fun fromGuideName(name: String): RepresentativeGuideCategory? =
            entries.firstOrNull { it.representativeGuideName == name }

        fun fromGuideId(id: String): RepresentativeGuideCategory? =
            entries.firstOrNull { it.representativeGuideId == id }

        fun fromDisposalCategory(category: DisposalCategory): RepresentativeGuideCategory =
            entries.first { it.disposalCategory == category }
    }
}
