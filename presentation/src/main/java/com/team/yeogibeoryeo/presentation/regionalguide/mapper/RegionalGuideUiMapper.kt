package com.team.yeogibeoryeo.presentation.regionalguide.mapper

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleTime
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.takeIfRegionalGuideDisplayValue

fun RegionalDisposalGuide.toUiModel(): RegionalGuideUiModel {
    return RegionalGuideUiModel(
        regionName = displayRegionName(),
        regionNameParts = displayRegionNameParts(),
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
        disposalTime = time(),
        disposalMethod = disposalMethod.takeIfNotBlank(),
        disposalPlace = disposalPlace.takeIfNotBlank(),
    )
}

private fun RegionalDisposalGuide.displayRegionName(): String {
    val regionName = displayRegionNameParts()
        .joinToString(" ")

    return regionName.ifBlank {
        targetRegionName.takeIfRegionalGuideDisplayValue()
            ?: managementZoneName.takeIfRegionalGuideDisplayValue()
            .orEmpty()
    }
}

private fun RegionalDisposalGuide.displayRegionNameParts(): List<String> =
    listOfNotNull(
        region.sido,
        region.sigungu,
        displayEupmyeondongName()
    ).filter { it.isNotBlank() }

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

private fun RegionalWasteSchedule.time(): RegionalWasteScheduleTime? {
    val startTime = disposalStartTime
    val endTime = disposalEndTime

    return when {
        !startTime.isNullOrBlank() && !endTime.isNullOrBlank() ->
            RegionalWasteScheduleTime.Range(
                startTime = startTime,
                endTime = endTime,
            )

        !startTime.isNullOrBlank() ->
            RegionalWasteScheduleTime.After(
                startTime = startTime,
            )

        !endTime.isNullOrBlank() ->
            RegionalWasteScheduleTime.Before(
                endTime = endTime,
            )

        else -> null
    }
}

private fun String?.takeIfNotBlank(): String? =
    this?.trim()?.takeIf { it.isNotBlank() }

private val REGION_NAME_DELIMITER = Regex("[,+/]+")
