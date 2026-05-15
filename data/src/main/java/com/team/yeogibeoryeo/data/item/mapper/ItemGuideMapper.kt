package com.team.yeogibeoryeo.data.item.mapper

import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideDto
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType

fun ItemGuideDto.toDomain(
    categoryMap: Map<String, Pair<DisposalCategory, DisposalSubCategory?>>,
    relatedSpotsMap: Map<String, List<RelatedSpotType>>,
    guideDetailsMap: Map<String, ItemGuideDetail>,
    guideDetailAliases: Map<String, String>,
): DisposalItemGuide {
    val methods = dschgMthd.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val guideDetailKey =
        when {
            itemNm in guideDetailsMap -> itemNm
            guideDetailAliases[itemNm] in guideDetailsMap -> guideDetailAliases[itemNm]
            else -> fallbackGuideDetailKeyByMethod(methods, guideDetailsMap)
        }
    val guideDetail =
        guideDetailsMap[itemNm]
            ?: guideDetailKey?.let { guideDetailsMap[it] }
    val (category, subCategory) =
        categoryMap[itemNm]
            ?: guideDetailKey?.let { categoryMap[it] }
            ?: (DisposalCategory.OTHER to null)
    val instructions = methods.map { DisposalInstruction(method = it) }
    val relatedSpotTypes = guideDetail?.relatedSpotTypes?.takeIf { it.isNotEmpty() } ?: relatedSpotsMap[itemNm]
    val isRecyclable = DisposalRecyclability.fromMethods(methods)

    return DisposalItemGuide(
        id = itemNm,
        name = itemNm,
        category = category,
        subCategory = subCategory,
        instructions = instructions,
        steps = guideDetail?.steps.orEmpty(),
        cautions = guideDetail?.cautions.orEmpty(),
        tip = guideDetail?.tip,
        isRecyclable = isRecyclable,
        relatedSpotTypes = relatedSpotTypes,
    )
}

private fun fallbackGuideDetailKeyByMethod(
    methods: List<String>,
    guideDetailsMap: Map<String, ItemGuideDetail>,
): String? =
    when {
        "음식물류폐기물" in methods -> "음식물쓰레기"
        "특수규격봉투" in methods -> "불연성종량제폐기물"
        methods == listOf("대형폐기물") -> "대형폐기물"
        methods == listOf("종량제봉투") -> "일반종량제폐기물"
        else -> null
    }?.takeIf { it in guideDetailsMap }
