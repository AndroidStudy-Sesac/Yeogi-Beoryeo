package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideUiModel(
    val regionName: String,
    val managementZoneName: String?,
    val targetRegionName: String?,
    val disposalPlaceType: String?,
    val disposalPlaceDescription: String?,
    val schedules: List<RegionalWasteScheduleUiModel>,
    val uncollectedDays: String?,
    val departmentInfo: String?
)

internal fun String?.takeIfRegionalGuideDisplayValue(): String? =
    this
        ?.trim()
        ?.takeIf { value ->
            value.isNotBlank() && value != NO_REGIONAL_GUIDE_VALUE
        }

internal fun regionalGuideRegionFallbackText(
    sido: String?,
    sigungu: String?,
    eupmyeondong: String? = null,
): String =
    listOfNotNull(
        sido.takeIfRegionalGuideDisplayValue(),
        sigungu.takeIfRegionalGuideDisplayValue(),
        eupmyeondong.takeIfRegionalGuideDisplayValue(),
    )
        .joinToString(REGION_FALLBACK_SEPARATOR)
        .ifBlank { DEFAULT_REGION_FALLBACK_TEXT }

private const val NO_REGIONAL_GUIDE_VALUE = "없음"
private const val REGION_FALLBACK_SEPARATOR = " > "
private const val DEFAULT_REGION_FALLBACK_TEXT = "지역 정보"
