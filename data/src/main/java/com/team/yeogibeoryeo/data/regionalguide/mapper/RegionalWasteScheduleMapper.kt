package com.team.yeogibeoryeo.data.regionalguide.mapper

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType

/**
 * 공공데이터 API의 폐기물 종류별 필드(Prefix)를 분석하여 도메인 모델 스케줄 리스트로 변환하는 매퍼.
 */
object RegionalWasteScheduleMapper {
    fun mapToSchedules(dto: RegionalGuideItemDto): List<RegionalWasteSchedule> {
        val schedules = mutableListOf<RegionalWasteSchedule>()

        if (!dto.generalDisposalDays.isNullOrBlank()) {
            schedules.add(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.GENERAL,
                    disposalDays = dto.generalDisposalDays,
                    disposalStartTime = dto.generalStartTime,
                    disposalEndTime = dto.generalEndTime,
                    disposalMethod = dto.generalMethod
                )
            )
        }

        if (!dto.foodDisposalDays.isNullOrBlank()) {
            schedules.add(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.FOOD,
                    disposalDays = dto.foodDisposalDays,
                    disposalStartTime = dto.foodStartTime,
                    disposalEndTime = dto.foodEndTime,
                    disposalMethod = dto.foodMethod
                )
            )
        }

        if (!dto.recycleDisposalDays.isNullOrBlank()) {
            schedules.add(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.RECYCLABLE,
                    disposalDays = dto.recycleDisposalDays,
                    disposalStartTime = dto.recycleStartTime,
                    disposalEndTime = dto.recycleEndTime,
                    disposalMethod = dto.recycleMethod
                )
            )
        }

        if (!dto.largeItemDisposalDays.isNullOrBlank()) {
            schedules.add(
                RegionalWasteSchedule(
                    wasteType = RegionalWasteType.LARGE_ITEM,
                    disposalDays = dto.largeItemDisposalDays,
                    disposalStartTime = dto.largeItemStartTime,
                    disposalEndTime = dto.largeItemEndTime,
                    disposalMethod = dto.largeItemMethod
                )
            )
        }

        return schedules
    }
}