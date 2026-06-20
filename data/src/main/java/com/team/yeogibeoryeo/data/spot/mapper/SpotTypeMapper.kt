package com.team.yeogibeoryeo.data.spot.mapper

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

object SpotTypeMapper {

    fun mapToType(
        spotName: String?,
        detailAddress: String?,
    ): CollectionSpotType {
        val targetText = listOfNotNull(
            spotName,
            detailAddress,
        ).joinToString(separator = " ").trim()

        return when {
            targetText.containsAny(medicineKeywords) -> {
                CollectionSpotType.MEDICINE_DROP_BOX
            }

            targetText.containsAny(batteryKeywords) -> {
                CollectionSpotType.BATTERY_BIN
            }

            targetText.containsAny(fluorescentLampKeywords) -> {
                CollectionSpotType.FLUORESCENT_LAMP_BIN
            }

            targetText.containsAny(clothingKeywords) -> {
                CollectionSpotType.CLOTHING_BIN
            }

            targetText.containsAny(icePackKeywords) -> {
                CollectionSpotType.ICE_PACK_BIN
            }

            targetText.containsAny(wasteCookingOilKeywords) -> {
                CollectionSpotType.WASTE_COOKING_OIL_BIN
            }

            targetText.containsAny(hazardousWasteKeywords) -> {
                CollectionSpotType.HAZARDOUS_WASTE_BIN
            }

            targetText.containsAny(phoneKeywords) -> {
                CollectionSpotType.PHONE_DROP_OFF
            }

            targetText.containsAny(recyclingCenterKeywords) -> {
                CollectionSpotType.RECYCLING_CENTER
            }

            targetText.containsAny(standardBagKeywords) -> {
                CollectionSpotType.STANDARD_BAG_STORE
            }

            targetText.containsAny(smallEWasteKeywords) -> {
                CollectionSpotType.SMALL_E_WASTE_BIN
            }

            else -> {
                CollectionSpotType.OTHER
            }
        }
    }

    private val medicineKeywords = listOf(
        "폐의약품 수거함",
        "폐의약품수거함",
        "의약품 수거함",
        "의약품수거함",
        "약국 폐의약품 수거함",
        "약국 폐의약품수거함",
    )

    private val fluorescentLampKeywords = listOf(
        "폐형광등 수거함",
        "폐형광등수거함",
        "형광등 수거함",
        "형광등수거함",
    )

    private val clothingKeywords = listOf(
        "의류 수거함",
        "의류수거함",
    )

    private val icePackKeywords = listOf(
        "아이스팩 수거함",
    )

    private val wasteCookingOilKeywords = listOf(
        "폐식용유 수거함",
        "폐식용유수거함",
        "폐식용유 배출함",
        "식물성 식용유 수거함",
    )

    private val hazardousWasteKeywords = listOf(
        "생활계 유해폐기물 전용수거함",
        "생활계유해폐기물 전용수거함",
        "생활계유해폐기물전용수거함",
        "유해폐기물 전용수거함",
        "유해폐기물수거함",
    )

    private val batteryKeywords = listOf(
        "폐건전지 수거함",
        "건전지 수거함",
        "전지 수거함",
        "폐전지 수거함",
        "전지수거함",
        "건전지",
        "폐전지",
    )

    private val phoneKeywords = listOf(
        "폐휴대폰 배출처",
        "폐휴대폰",
        "휴대폰 수거함",
        "휴대폰 배출함",
    )

    private val recyclingCenterKeywords = listOf(
        "재활용센터",
        "재활용 센터",
        "재활용정거장",
        "재활용동네마당",
        "재활용 동네마당",
        "재활용도움센터",
        "클린하우스",
        "클린 하우스",
        "재활용품 분리배출함",
        "재활용품 분리수거함",
        "재활용품 공동배출장소",
        "재활용품 분리배출장소",
    )

    private val standardBagKeywords = listOf(
        "종량제",
        "봉투",
    )

    private val smallEWasteKeywords = listOf(
        "중소형 수거함",
        "중소형수거함",
        "중소형 폐가전수거함",
        "중소형폐가전수거함",
        "중소형 폐가전 수거함",
        "소형가전 수거함",
        "소형가전수거함",
        "소형전기전자제품 수거함",
        "소형전기전자제품수거함",
        "폐가전 수거함",
        "폐가전수거함",
    )

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any(::contains)
    }
}
