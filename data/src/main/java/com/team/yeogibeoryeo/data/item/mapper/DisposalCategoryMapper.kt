package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory

internal fun String?.toSourceCategoryInfo(): Pair<DisposalCategory, DisposalSubCategory?>? =
    when (this) {
        "공사장 생활폐기물" -> DisposalCategory.CONSTRUCTION_WASTE to null
        "금속류 > 고철" -> DisposalCategory.METAL to DisposalSubCategory.SCRAP_METAL
        "대형폐기물" -> DisposalCategory.LARGE_WASTE to null
        "무색페트병" -> DisposalCategory.COLORLESS_PET to DisposalSubCategory.TRANSPARENT_PET_BOTTLE
        "발포합성수지(스티로폼 등)" -> DisposalCategory.STYROFOAM to null
        "불연성종량제폐기물" -> DisposalCategory.NON_COMBUSTIBLE to null
        "비닐류" -> DisposalCategory.VINYL to null
        "생활계 유해폐기물" -> DisposalCategory.HAZARDOUS to null
        "유리병" -> DisposalCategory.GLASS to DisposalSubCategory.GLASS_BOTTLE
        "음식물류폐기물" -> DisposalCategory.FOOD_WASTE to null
        "의류 및 원단" -> DisposalCategory.CLOTHING to DisposalSubCategory.CLOTHING
        "일반종량제폐기물" -> DisposalCategory.GENERAL to null
        "전기전자제품" -> DisposalCategory.ELECTRONICS to null
        "전지" -> DisposalCategory.BATTERY to DisposalSubCategory.BATTERY
        "조명제품 > 형광등" -> DisposalCategory.LIGHTING to DisposalSubCategory.FLUORESCENT_LAMP
        "종이류" -> DisposalCategory.PAPER to null
        "종이팩" -> DisposalCategory.PAPER_PACK to DisposalSubCategory.MILK_CARTON
        "플라스틱류" -> DisposalCategory.PLASTIC to DisposalSubCategory.PLASTIC_CONTAINER
        "기타" -> DisposalCategory.OTHER to null
        else -> null
    }
