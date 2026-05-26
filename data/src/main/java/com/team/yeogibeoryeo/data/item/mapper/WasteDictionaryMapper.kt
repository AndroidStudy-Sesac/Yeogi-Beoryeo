package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
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
        detailSections =
            buildList {
                if (dischargeMethods.isNotEmpty()) add(
                    DisposalGuideSection(
                        "배출방법",
                        dischargeMethods
                    )
                )
                if (features.isNotEmpty()) add(DisposalGuideSection("특징", features))
                if (notes.isNotEmpty()) add(DisposalGuideSection("유의사항", notes))
            },
        tip = null,
        isRecyclable = DisposalRecyclability.fromCategory(category),
        relatedSpotTypes = dischargeMethods.flatMap { it.toRelatedSpotTypes() }.distinct(),
    )
}

private fun List<List<String>>.toPrimaryCategory(): DisposalCategory =
    when {
        hasLeaf("음식물류폐기물") -> DisposalCategory.FOOD_WASTE
        hasLeaf("공사장 생활폐기물") -> DisposalCategory.CONSTRUCTION_WASTE
        hasLeaf("불연성종량제폐기물") -> DisposalCategory.NON_COMBUSTIBLE
        hasAnyLeaf("폐의약품", "폐농약", "생활계 유해폐기물", "수은함유 폐기물", "천연방사성제품") ->
            DisposalCategory.HAZARDOUS

        hasLeaf("무색페트병") -> DisposalCategory.COLORLESS_PET
        hasAnyLeaf("합성수지 용기류", "합성수지 재질") -> DisposalCategory.PLASTIC
        hasLeaf("합성수지 비닐류") -> DisposalCategory.VINYL
        hasLeaf("발포합성수지(스티로폼 등)") -> DisposalCategory.STYROFOAM
        hasLeaf("유리병") -> DisposalCategory.GLASS
        any { it.last().startsWith("금속류") } -> DisposalCategory.METAL
        any { it.last().startsWith("종이팩") } -> DisposalCategory.PAPER_PACK
        any { it.last() == "종이" } -> DisposalCategory.PAPER
        hasLeaf("의류 및 원단") -> DisposalCategory.CLOTHING
        hasLeaf("전지류") -> DisposalCategory.BATTERY
        hasLeaf("조명제품") -> DisposalCategory.LIGHTING
        hasLeaf("전기전자 제품류") -> DisposalCategory.ELECTRONICS
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
        val compactText = this@toRelatedSpotTypes.replace(" ", "")
        val isSpecialBag =
            this@toRelatedSpotTypes.contains("불연성") ||
                    compactText.contains("특수규격마대")
        val isDedicatedBin =
            compactText.contains("의류수거함") ||
                    compactText.contains("폐의약품수거함") ||
                    compactText.contains("형광등수거함") ||
                    compactText.contains("폐건전지수거함") ||
                    compactText.contains("전용수거함")
        val isGeneralRecyclingBin =
            this@toRelatedSpotTypes.contains("재활용폐기물") ||
                    compactText.contains("무색페트병수거함")
        if (isSpecialBag) {
            add(RelatedSpotType.SPECIAL_BAG)
        } else if (this@toRelatedSpotTypes.contains("종량제봉투")) {
            add(RelatedSpotType.GENERAL_WASTE_BAG)
        }
        if (this@toRelatedSpotTypes.contains("대형폐기물")) add(RelatedSpotType.LARGE_WASTE)
        if (isDedicatedBin) {
            add(RelatedSpotType.DEDICATED_BIN)
        }
        if (isGeneralRecyclingBin) {
            add(RelatedSpotType.RECYCLING_BIN)
        }
    }.distinct()
