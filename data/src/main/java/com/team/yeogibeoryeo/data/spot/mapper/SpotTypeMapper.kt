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
            targetText.contains("건전지") -> {
                CollectionSpotType.BATTERY_BIN
            }

            targetText.contains("휴대폰") -> {
                CollectionSpotType.PHONE_DROP_OFF
            }

            targetText.contains("재활용센터") || targetText.contains("재활용 센터") -> {
                CollectionSpotType.RECYCLING_CENTER
            }

            targetText.contains("종량제") || targetText.contains("봉투") -> {
                CollectionSpotType.STANDARD_BAG_STORE
            }

            targetText.contains("중소형") || targetText.contains("수거함") -> {
                CollectionSpotType.SMALL_E_WASTE_BIN
            }

            else -> {
                CollectionSpotType.OTHER
            }
        }
    }
}