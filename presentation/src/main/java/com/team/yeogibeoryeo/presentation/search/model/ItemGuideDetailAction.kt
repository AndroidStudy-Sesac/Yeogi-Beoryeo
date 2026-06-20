package com.team.yeogibeoryeo.presentation.search.model

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.R

sealed interface ItemGuideDetailAction {
    @get:StringRes
    val labelResId: Int

    @get:StringRes
    val descriptionResId: Int

    data class MapSpot(
        val type: CollectionSpotType,
        @param:StringRes override val labelResId: Int,
        @param:StringRes override val descriptionResId: Int,
    ) : ItemGuideDetailAction

    data class OfficialGuide(
        val url: String,
        @param:StringRes override val labelResId: Int,
        @param:StringRes override val descriptionResId: Int,
    ) : ItemGuideDetailAction
}

fun DisposalItemGuide.toDetailActions(): List<ItemGuideDetailAction> {
    val relatedTypes = relatedSpotTypes.orEmpty()
    val detailText = searchableText()
    val disposalText = disposalText()
    val actions = mutableListOf<ItemGuideDetailAction>()

    if (
        RelatedSpotType.SMALL_E_WASTE_BIN in relatedTypes ||
        RelatedSpotType.E_WASTE_BIN in relatedTypes ||
        detailText.contains("소형전기전자제품") ||
        detailText.contains("소형폐가전")
    ) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.SMALL_E_WASTE_BIN,
            labelResId = R.string.item_guide_action_find_small_e_waste_bin,
            descriptionResId = R.string.item_guide_action_find_small_e_waste_bin_description,
        )
    }

    if (RelatedSpotType.GENERAL_WASTE_BAG in relatedTypes || category == DisposalCategory.GENERAL) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.STANDARD_BAG_STORE,
            labelResId = R.string.item_guide_action_find_standard_bag_store,
            descriptionResId = R.string.item_guide_action_find_standard_bag_store_description,
        )
    }

    if (name == MOBILE_PHONE_ITEM_NAME || name == ELECTRONICS_REPRESENTATIVE_NAME) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.PHONE_DROP_OFF,
            labelResId = R.string.item_guide_action_find_phone_drop_off,
            descriptionResId = R.string.item_guide_action_find_phone_drop_off_description,
        )
    }

    if (category == DisposalCategory.BATTERY || detailText.contains("전지수거함")) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.BATTERY_BIN,
            labelResId = R.string.item_guide_action_find_battery_bin,
            descriptionResId = R.string.item_guide_action_find_battery_bin_description,
        )
    }

    if (category == DisposalCategory.LIGHTING || detailText.contains("형광등수거함")) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.FLUORESCENT_LAMP_BIN,
            labelResId = R.string.item_guide_action_find_fluorescent_lamp_bin,
            descriptionResId = R.string.item_guide_action_find_fluorescent_lamp_bin_description,
        )
    }

    if (category == DisposalCategory.CLOTHING || detailText.contains("의류수거함")) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.CLOTHING_BIN,
            labelResId = R.string.item_guide_action_find_clothing_bin,
            descriptionResId = R.string.item_guide_action_find_clothing_bin_description,
        )
    }

    if (
        disposalText.contains("폐의약품수거함") ||
        disposalText.contains("폐의약품전용수거함") ||
        (
            name == HAZARDOUS_WASTE_ITEM_NAME &&
                disposalText.contains("폐의약품") &&
                disposalText.contains("전용수거함")
            )
    ) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.MEDICINE_DROP_BOX,
            labelResId = R.string.item_guide_action_find_medicine_drop_box,
            descriptionResId = R.string.item_guide_action_find_medicine_drop_box_description,
        )
    }

    if (
        detailText.contains("생활계유해폐기물") &&
        detailText.contains("전용수거함")
    ) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.HAZARDOUS_WASTE_BIN,
            labelResId = R.string.item_guide_action_find_hazardous_waste_bin,
            descriptionResId = R.string.item_guide_action_find_hazardous_waste_bin_description,
        )
    }

    if (
        name == HAZARDOUS_WASTE_ITEM_NAME ||
        detailText.contains("아이스팩") && detailText.contains("전용수거함")
    ) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.ICE_PACK_BIN,
            labelResId = R.string.item_guide_action_find_ice_pack_bin,
            descriptionResId = R.string.item_guide_action_find_ice_pack_bin_description,
        )
    }

    if (name == HAZARDOUS_WASTE_ITEM_NAME || detailText.contains("폐식용유수거함")) {
        actions += ItemGuideDetailAction.MapSpot(
            type = CollectionSpotType.WASTE_COOKING_OIL_BIN,
            labelResId = R.string.item_guide_action_find_waste_cooking_oil_bin,
            descriptionResId = R.string.item_guide_action_find_waste_cooking_oil_bin_description,
        )
    }

    if (RelatedSpotType.FREE_PICKUP in relatedTypes || detailText.contains("무상방문수거서비스")) {
        actions += ItemGuideDetailAction.OfficialGuide(
            url = FREE_PICKUP_GUIDE_URL,
            labelResId = R.string.item_guide_action_free_pickup,
            descriptionResId = R.string.item_guide_action_free_pickup_description,
        )
    }

    return actions
}

private fun DisposalItemGuide.disposalText(): String =
    buildString {
        append(name)
        append(' ')
        append(instructions.joinToString(" ") { it.method })
        append(' ')
        append(detailSections.joinToString(" ") { section ->
            buildString {
                append(section.title)
                append(' ')
                append(section.lines.joinToString(" "))
                append(' ')
                append(section.rows.joinToString(" ") { "${it.label} ${it.value}" })
            }
        })
    }.compact()

private fun DisposalItemGuide.searchableText(): String =
    buildString {
        append(name)
        append(' ')
        append(instructions.joinToString(" ") { it.method })
        append(' ')
        append(steps.joinToString(" "))
        append(' ')
        append(features.joinToString(" "))
        append(' ')
        append(cautions.joinToString(" "))
        append(' ')
        append(subGuides.joinToString(" ") { "${it.name} ${it.summary}" })
        append(' ')
        append(detailSections.joinToString(" ") { section ->
            buildString {
                append(section.title)
                append(' ')
                append(section.lines.joinToString(" "))
                append(' ')
                append(section.rows.joinToString(" ") { "${it.label} ${it.value}" })
            }
        })
        append(' ')
        append(tip.orEmpty())
    }.compact()

private fun String.compact(): String = replace(" ", "")

private const val MOBILE_PHONE_ITEM_NAME = "핸드폰"
private const val ELECTRONICS_REPRESENTATIVE_NAME = "전기전자제품"
private const val HAZARDOUS_WASTE_ITEM_NAME = "생활계 유해폐기물"
private const val FREE_PICKUP_GUIDE_URL = "https://www.15990903.or.kr/portal/cnts/userGuide.do"
