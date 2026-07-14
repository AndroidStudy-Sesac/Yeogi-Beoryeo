package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.takeIfRegionalGuideDisplayValue

fun RegionalDisposalGuide.toUiModel(): RegionalGuideUiModel {
    return RegionalGuideUiModel(
        regionName = displayRegionName(),
        managementZoneName = managementZoneName,
        targetRegionName = targetRegionName,
        disposalPlaceType = disposalPlaceType.takeIfNotBlank(),
        disposalPlaceDescription = disposalPlaceDescription.takeIfNotBlank(),
        schedules = schedules
            .sortedBy { schedule ->
                when (schedule.wasteType) {
                    RegionalWasteType.GENERAL -> 0
                    RegionalWasteType.FOOD -> 1
                    RegionalWasteType.RECYCLABLE -> 2
                    RegionalWasteType.LARGE_ITEM -> 3
                    RegionalWasteType.UNKNOWN -> 4
                }
            }
            .map { it.toUiModel() },
        uncollectedDays = uncollectedDays.takeIfNotBlank(),
        departmentInfo = listOfNotNull(
            departmentName.takeIfNotBlank(),
            departmentPhoneNumber.takeIfNotBlank()
        ).joinToString(" ").takeIf { it.isNotBlank() }
    )
}

private fun RegionalWasteSchedule.toUiModel(): RegionalWasteScheduleUiModel {
    return RegionalWasteScheduleUiModel(
        wasteTypeName = wasteType.description,
        disposalDays = disposalDays.takeIfNotBlank(),
        disposalTime = displayTime(),
        disposalMethod = disposalMethod.takeIfNotBlank(),
        disposalPlace = disposalPlace.takeIfNotBlank(),
    )
}

private fun RegionalDisposalGuide.displayRegionName(): String {
    val regionName = listOfNotNull(
        region.sido,
        region.sigungu,
        region.eupmyeondong
    ).filter { it.isNotBlank() }
        .joinToString(" ")

    return regionName.ifBlank {
        targetRegionName.takeIfRegionalGuideDisplayValue()
            ?: managementZoneName.takeIfRegionalGuideDisplayValue()
            ?: "지역 정보"
    }
}

private fun RegionalWasteSchedule.displayTime(): String? {
    val startTime = disposalStartTime
    val endTime = disposalEndTime

    return when {
        !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> "$startTime ~ $endTime"
        !startTime.isNullOrBlank() -> "$startTime 이후"
        !endTime.isNullOrBlank() -> "$endTime 이전"
        else -> null
    }
}

private fun String?.takeIfNotBlank(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }
