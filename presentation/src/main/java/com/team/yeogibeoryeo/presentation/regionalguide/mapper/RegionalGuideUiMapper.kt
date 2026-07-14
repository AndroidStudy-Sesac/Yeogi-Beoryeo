package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
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
        disposalDays = disposalDays.orInfoEmpty(),
        disposalTime = displayTime(),
        disposalMethod = disposalMethod.orInfoEmpty()
    )
}

private fun RegionalDisposalGuide.displayRegionName(): String {
    val regionName = listOfNotNull(
        region.sido,
        region.sigungu,
        displayEupmyeondongName()
    ).filter { it.isNotBlank() }
        .joinToString(" ")

    return regionName.ifBlank {
        targetRegionName.takeIfRegionalGuideDisplayValue()
            ?: managementZoneName.takeIfRegionalGuideDisplayValue()
            ?: "지역 정보"
    }
}

private fun RegionalDisposalGuide.displayEupmyeondongName(): String? {
    val eupmyeondong = region.eupmyeondong?.trim()?.takeIf { it.isNotBlank() }
        ?: return null
    val apiCompatibleName = RegionalGuideEupmyeondongNamePolicy
        .toApiCompatibleDisplayName(eupmyeondong)
        ?: return eupmyeondong
    if (apiCompatibleName == eupmyeondong) return eupmyeondong

    return listOf(targetRegionName, managementZoneName)
        .firstNotNullOfOrNull { value ->
            value
                ?.split(REGION_NAME_DELIMITER)
                ?.map { token -> token.trim() }
                ?.firstOrNull { token ->
                    RegionalGuideEupmyeondongNamePolicy.isSameName(
                        first = token,
                        second = apiCompatibleName,
                    )
                }
        }
        ?: eupmyeondong
}

private fun RegionalWasteSchedule.displayTime(): String {
    val startTime = disposalStartTime
    val endTime = disposalEndTime

    return when {
        !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> "$startTime ~ $endTime"
        !startTime.isNullOrBlank() -> "$startTime 이후"
        !endTime.isNullOrBlank() -> "$endTime 이전"
        else -> "시간 정보 없음"
    }
}

private fun String?.orInfoEmpty(): String =
    if (isNullOrBlank()) "정보 없음" else this

private fun String?.takeIfNotBlank(): String? =
    this?.takeIf { it.isNotBlank() }

private val REGION_NAME_DELIMITER = Regex("[,+/]+")
