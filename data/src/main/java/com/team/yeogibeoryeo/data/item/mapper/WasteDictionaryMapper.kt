package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType

fun WasteDictionaryItem.toDomain(): DisposalItemGuide {
    val category = categoryPaths.toPrimaryCategory()
    return DisposalItemGuide(
        id = name,
        name = name,
        category = category,
        subCategory = categoryPaths.toSubCategory(),
        instructions =
            dischargeMethods.map {
                DisposalInstruction(
                    method = it,
                )
            },
        steps = features,
        cautions = notes,
        tip = null,
        isRecyclable = DisposalRecyclability.fromCategory(category),
        relatedSpotTypes = dischargeMethods.flatMap { it.toRelatedSpotTypes() }.distinct(),
    )
}

private fun List<List<String>>.toPrimaryCategory(): DisposalCategory =
    when {
        hasLeaf("음식물류폐기물") -> DisposalCategory.FOOD_WASTE
        hasLeaf("불연성종량제폐기물") -> DisposalCategory.OTHER
        hasAnyLeaf("폐의약품", "폐농약", "생활계 유해폐기물", "수은함유 폐기물", "천연방사성제품") ->
            DisposalCategory.HAZARDOUS
        hasAnyLeaf("무색페트병", "합성수지 용기류", "합성수지 재질") -> DisposalCategory.PLASTIC
        hasLeaf("합성수지 비닐류") -> DisposalCategory.VINYL
        hasLeaf("발포합성수지(스티로폼 등)") -> DisposalCategory.STYROFOAM
        hasLeaf("유리병") -> DisposalCategory.GLASS
        any { it.last().startsWith("금속류") } -> DisposalCategory.METAL
        any { it.last() == "종이" || it.last().startsWith("종이팩") } -> DisposalCategory.PAPER
        hasLeaf("의류 및 원단") -> DisposalCategory.CLOTHING
        hasAnyLeaf("전기전자 제품류", "전지류", "조명제품") -> DisposalCategory.ELECTRONICS
        hasLeaf("대형폐기물") && !hasLeaf("일반종량제폐기물") -> DisposalCategory.LARGE_WASTE
        hasLeaf("일반종량제폐기물") -> DisposalCategory.GENERAL
        hasLeaf("대형폐기물") -> DisposalCategory.LARGE_WASTE
        else -> DisposalCategory.OTHER
    }

private fun List<List<String>>.toSubCategory(): DisposalSubCategory? =
    when {
        hasLeaf("무색페트병") -> DisposalSubCategory.TRANSPARENT_PET_BOTTLE
        hasLeaf("금속류 금속캔") -> DisposalSubCategory.ALUMINUM_CAN
        hasLeaf("금속류 고철") -> DisposalSubCategory.SCRAP_METAL
        any { it.last().startsWith("종이팩") } -> DisposalSubCategory.MILK_CARTON
        else -> null
    }

private fun List<List<String>>.hasLeaf(label: String): Boolean = any { it.last() == label }

private fun List<List<String>>.hasAnyLeaf(vararg labels: String): Boolean =
    any { path -> path.last() in labels }

private fun String.toRelatedSpotTypes(): List<RelatedSpotType> =
    buildList<RelatedSpotType> {
        val isSpecialBag =
            this@toRelatedSpotTypes.contains("불연성") ||
                this@toRelatedSpotTypes.contains("특수규격마대")
        if (isSpecialBag) {
            add(RelatedSpotType.SPECIAL_BAG)
        } else if (this@toRelatedSpotTypes.contains("종량제봉투")) {
            add(RelatedSpotType.GENERAL_WASTE_BAG)
        }
        if (this@toRelatedSpotTypes.contains("대형폐기물")) add(RelatedSpotType.LARGE_WASTE)
        if (
            this@toRelatedSpotTypes.contains("의류수거함") ||
            this@toRelatedSpotTypes.contains("폐의약품수거함") ||
            this@toRelatedSpotTypes.contains("전용수거함")
        ) {
            add(RelatedSpotType.DEDICATED_BIN)
        }
        if (this@toRelatedSpotTypes.contains("수거함") || this@toRelatedSpotTypes.contains("재활용")) {
            add(RelatedSpotType.RECYCLING_BIN)
        }
    }.distinct()
