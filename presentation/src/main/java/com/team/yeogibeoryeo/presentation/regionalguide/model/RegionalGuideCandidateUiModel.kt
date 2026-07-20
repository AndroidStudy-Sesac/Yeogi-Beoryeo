package com.team.yeogibeoryeo.presentation.regionalguide.model

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator
import com.team.yeogibeoryeo.presentation.R

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    internal val disambiguation: RegionalGuideCandidateDisambiguation? = null,
) {
    val stableKey: String =
        listOf(
            "sido=${sido.toStableKeyPart()}",
            "sigungu=${sigungu.toStableKeyPart()}",
            "eupmyeondong=${eupmyeondong.toStableKeyPart()}",
            "managementZone=${guide.managementZoneName.toStableKeyPart()}",
            "targetRegion=${guide.targetRegionName.toStableKeyPart()}",
            "placeType=${guide.disposalPlaceType.toStableKeyPart()}",
            "placeDescription=${guide.disposalPlaceDescription.toStableKeyPart()}",
            "uncollectedDays=${guide.uncollectedDays.toStableKeyPart()}",
            "department=${guide.departmentInfo.toStableKeyPart()}",
            "schedules=${guide.schedules.joinToString(SCHEDULE_KEY_SEPARATOR) { schedule -> schedule.stableKey }}",
        ).joinToString(STABLE_KEY_SEPARATOR)

    val displayText: String =
        baseDisplayParts()
            .appendDisambiguation()
            .distinct()
            .joinToString(CANDIDATE_LABEL_SEPARATOR)

    internal val displayTextForRow: RegionalGuideCandidateDisplayText =
        primaryDisplayParts()
            .distinct()
            .toCandidateDisplayTextForRow()
            ?.appendDisambiguation(disambiguation, primaryDisplayParts())
            ?: fallbackDisplayTextForRow()

    internal val sortText: String =
        primaryDisplayParts()
            .ifEmpty { listOf(displayText) }
            .joinToString(CANDIDATE_LABEL_SEPARATOR)

    internal val orderedManagementZoneSortText: String? =
        primaryDisplayParts()
            .takeIf { parts -> parts.size >= 2 }
            ?.let { parts ->
                parts[0].toOrderedManagementZoneSortText()
                    ?.let { managementZoneSortText ->
                        listOf(managementZoneSortText, parts[1]).joinToString(CANDIDATE_LABEL_SEPARATOR)
                    }
            }

    val collectionTypeHint: RegionalGuideCandidateCollectionTypeHint? =
        when (guide.disposalPlaceType?.trim()) {
            DOOR_TO_DOOR_COLLECTION_TYPE -> RegionalGuideCandidateCollectionTypeHint.DOOR_TO_DOOR
            BASE_POINT_COLLECTION_TYPE -> RegionalGuideCandidateCollectionTypeHint.BASE_POINT
            else -> null
        }

    internal val isOverallCollectionTypeCandidate: Boolean =
        guide.managementZoneName.takeIfRegionalGuideDisplayValue() == null &&
            guide.targetRegionName.takeIfRegionalGuideDisplayValue() == null &&
            guide.disposalPlaceType.takeIfRegionalGuideDisplayValue() != null

    internal val isCollectionTypeSelectionCandidate: Boolean =
        isOverallCollectionTypeCandidate || isNamedCollectionTypeCandidate()

    internal val collectionTypeOptionText: String =
        if (isOverallCollectionTypeCandidate) {
            guide.disposalPlaceType.takeIfRegionalGuideDisplayValue() ?: displayText
        } else {
            displayText
        }

    internal val collectionTypeDistinguishingText: RegionalGuideCandidateDistinguishingText? =
        listOfNotNull(
            guide.disposalPlaceDescription.toDistinguishingText(
                RegionalGuideCandidateDistinguishingLabel.DISPOSAL_PLACE
            ),
            guide.uncollectedDays.toDistinguishingText(
                RegionalGuideCandidateDistinguishingLabel.UNCOLLECTED_DAYS
            ),
            guide.schedules.firstNotNullOfOrNull { schedule ->
                schedule.toCandidateDistinguishingText()
            },
            guide.departmentInfo.toDistinguishingText(
                RegionalGuideCandidateDistinguishingLabel.DEPARTMENT
            ),
        ).firstOrNull()

    private fun primaryDisplayParts(): List<String> =
        listOfNotNull(
            guide.managementZoneName.takeIfRegionalGuideDisplayValue(),
            guide.targetRegionName.takeIfRegionalGuideDisplayValue()
        )

    private fun baseDisplayParts(): List<String> =
        primaryDisplayParts()
            .ifEmpty { fallbackDisplayParts() }

    private fun fallbackDisplayParts(): List<String> =
        fallbackRegionDisplayParts() + listOfNotNull(
            candidateFallbackDisambiguation()?.key,
        )

    private fun fallbackRegionDisplayParts(): List<String> =
        listOfNotNull(
            regionalGuideRegionFallbackText(sido, sigungu, eupmyeondong)
                .takeIf { text -> text.isNotBlank() },
        )

    private fun fallbackDisplayTextForRow(): RegionalGuideCandidateDisplayText {
        val regionText = listOfNotNull(
            sido.takeIfRegionalGuideDisplayValue(),
            sigungu.takeIfRegionalGuideDisplayValue(),
            eupmyeondong.takeIfRegionalGuideDisplayValue(),
        ).toRegionalGuideRegionDisplayText()
            ?: RegionalGuideCandidateDisplayText.Resource(
                resId = R.string.regional_guide_default_region_name,
                args = emptyList(),
            )

        return listOfNotNull(
            candidateFallbackDisambiguation(),
            disambiguation,
        )
            .distinctBy { disambiguation -> disambiguation.key }
            .fold(regionText) { displayText, disambiguation ->
                displayText.appendDisambiguation(disambiguation)
            }
    }

    private fun List<String>.toCandidateDisplayTextForRow(): RegionalGuideCandidateDisplayText? =
        when (size) {
            0 -> null
            1 -> RegionalGuideCandidateDisplayText.Plain(first())
            2 -> RegionalGuideCandidateDisplayText.Resource(
                resId = R.string.regional_guide_candidate_label_format,
                args = this,
            )

            else -> null
        }

    internal fun candidateDisambiguation(): RegionalGuideCandidateDisambiguation? =
        candidateFallbackDisambiguation()
            ?: guide.schedules.firstNotNullOfOrNull { schedule ->
                schedule.toCandidateDisambiguation()
            }

    private fun candidateFallbackDisambiguation(): RegionalGuideCandidateDisambiguation? =
        guide.disposalPlaceType.toCandidateDisambiguation()
            ?: guide.disposalPlaceDescription.toCandidateDisambiguation()
            ?: guide.schedules.firstNotNullOfOrNull { schedule ->
                schedule
                    .takeUnless { it.disposalTime.isFormattedTime() }
                    ?.toCandidateDisambiguation()
            }

    private fun List<String>.appendDisambiguation(): List<String> =
        this + listOfNotNull(disambiguation?.key)

    private fun isNamedCollectionTypeCandidate(): Boolean {
        val disposalPlaceType = guide.disposalPlaceType.takeIfRegionalGuideDisplayValue()
            ?: return false
        if (collectionTypeHint == null) return false

        val displayParts = primaryDisplayParts()
        if (displayParts.isEmpty()) return false

        return displayParts.all { displayPart ->
            displayPart.isCollectionTypeDisplayName(disposalPlaceType)
        }
    }

    private fun RegionalWasteScheduleUiModel.toCandidateDisambiguation():
        RegionalGuideCandidateDisambiguation? =
        toCandidateSummaryDisplayText()
            ?.let { displayText ->
                RegionalGuideCandidateDisambiguation(
                    key = candidateSummaryKey() ?: return null,
                    displayText = displayText,
                )
            }

    private fun RegionalWasteScheduleUiModel.candidateSummaryKey(): String? {
        val criterion = when {
            disposalDays.takeIfRegionalGuideDisplayValue() != null -> disposalDays
            disposalTime != null -> disposalTime.stableKey
            disposalMethod.takeIfRegionalGuideDisplayValue() != null -> disposalMethod
            disposalPlace.takeIfRegionalGuideDisplayValue() != null -> disposalPlace
            else -> null
        } ?: return null

        return listOfNotNull(
            wasteTypeName.takeIfRegionalGuideDisplayValue(),
            criterion,
        ).joinToString(SCHEDULE_SUMMARY_SEPARATOR)
    }

    private fun RegionalWasteScheduleUiModel.toCandidateSummaryDisplayText():
        RegionalGuideCandidateDisplayText? {
        val criterion = when {
            disposalDays.takeIfRegionalGuideDisplayValue() != null ->
                RegionalGuideCandidateDisplayText.Plain(disposalDays.orEmpty())

            disposalTime != null -> disposalTime.toCandidateDisplayText()

            disposalMethod.takeIfRegionalGuideDisplayValue() != null ->
                RegionalGuideCandidateDisplayText.Plain(disposalMethod.orEmpty())

            disposalPlace.takeIfRegionalGuideDisplayValue() != null ->
                RegionalGuideCandidateDisplayText.Plain(disposalPlace.orEmpty())

            else -> return null
        }

        return wasteTypeName.takeIfRegionalGuideDisplayValue()
            ?.let { wasteTypeName ->
                RegionalGuideCandidateDisplayText.Resource(
                    resId = R.string.regional_guide_candidate_schedule_summary_format,
                    args = listOf(wasteTypeName, criterion),
                )
            }
            ?: criterion
    }

    private fun RegionalWasteScheduleUiModel.toCandidateDistinguishingText():
        RegionalGuideCandidateDistinguishingText? {
        val value = toCandidateSummaryDisplayText() ?: return null

        return RegionalGuideCandidateDistinguishingText(
            label = RegionalGuideCandidateDistinguishingLabel.SCHEDULE,
            value = value,
        )
    }

    private companion object {
        const val CANDIDATE_LABEL_SEPARATOR = " / "
        const val SCHEDULE_SUMMARY_SEPARATOR = " "
        const val STABLE_KEY_SEPARATOR = "|"
        const val SCHEDULE_KEY_SEPARATOR = ";"
        const val DOOR_TO_DOOR_COLLECTION_TYPE = "문전수거"
        const val BASE_POINT_COLLECTION_TYPE = "거점수거"
    }
}

data class RegionalGuideCandidateDisambiguation(
    val key: String,
    val displayText: RegionalGuideCandidateDisplayText,
)

internal data class RegionalGuideCandidateDistinguishingText(
    val label: RegionalGuideCandidateDistinguishingLabel,
    val value: RegionalGuideCandidateDisplayText,
)

sealed interface RegionalGuideCandidateDisplayText {
    data class Plain(
        val value: String,
    ) : RegionalGuideCandidateDisplayText

    data class Resource(
        @param:StringRes val resId: Int,
        val args: List<Any>,
    ) : RegionalGuideCandidateDisplayText
}

private fun String?.toCandidateDisambiguation(): RegionalGuideCandidateDisambiguation? =
    takeIfRegionalGuideDisplayValue()
        ?.let { value ->
            RegionalGuideCandidateDisambiguation(
                key = value,
                displayText = RegionalGuideCandidateDisplayText.Plain(value),
            )
        }

private fun RegionalWasteScheduleTime.toCandidateDisplayText(): RegionalGuideCandidateDisplayText =
    when (this) {
        is RegionalWasteScheduleTime.Value -> RegionalGuideCandidateDisplayText.Plain(value)
        is RegionalWasteScheduleTime.Range -> RegionalGuideCandidateDisplayText.Resource(
            resId = R.string.regional_waste_schedule_time_range_format,
            args = listOf(startTime, endTime),
        )

        is RegionalWasteScheduleTime.After -> RegionalGuideCandidateDisplayText.Resource(
            resId = R.string.regional_waste_schedule_time_after_format,
            args = listOf(startTime),
        )

        is RegionalWasteScheduleTime.Before -> RegionalGuideCandidateDisplayText.Resource(
            resId = R.string.regional_waste_schedule_time_before_format,
            args = listOf(endTime),
        )
    }

private val RegionalWasteScheduleTime.stableKey: String
    get() = when (this) {
        is RegionalWasteScheduleTime.Value -> "value:$value"
        is RegionalWasteScheduleTime.Range -> "range:$startTime:$endTime"
        is RegionalWasteScheduleTime.After -> "after:$startTime"
        is RegionalWasteScheduleTime.Before -> "before:$endTime"
    }

private fun RegionalWasteScheduleTime?.isFormattedTime(): Boolean =
    this is RegionalWasteScheduleTime.Range ||
        this is RegionalWasteScheduleTime.After ||
        this is RegionalWasteScheduleTime.Before

private fun RegionalGuideCandidateDisplayText.appendDisambiguation(
    disambiguation: RegionalGuideCandidateDisambiguation?,
    existingLabels: List<String> = emptyList(),
): RegionalGuideCandidateDisplayText {
    val label = disambiguation?.takeUnless { value -> value.key in existingLabels }
        ?: return this

    return RegionalGuideCandidateDisplayText.Resource(
        resId = R.string.regional_guide_candidate_label_format,
        args = listOf(this, label.displayText.asResourceArgument()),
    )
}

private fun RegionalGuideCandidateDisplayText.asResourceArgument(): Any =
    when (this) {
        is RegionalGuideCandidateDisplayText.Plain -> value
        is RegionalGuideCandidateDisplayText.Resource -> this
    }

private fun List<String>.toRegionalGuideRegionDisplayText(): RegionalGuideCandidateDisplayText? =
    when (size) {
        0 -> null
        1 -> RegionalGuideCandidateDisplayText.Plain(first())
        2 -> RegionalGuideCandidateDisplayText.Resource(
            resId = R.string.regional_guide_region_two_name_format,
            args = this,
        )

        3 -> RegionalGuideCandidateDisplayText.Resource(
            resId = R.string.regional_guide_region_three_name_format,
            args = this,
        )

        else -> null
    }

internal enum class RegionalGuideCandidateDistinguishingLabel {
    DISPOSAL_PLACE,
    UNCOLLECTED_DAYS,
    SCHEDULE,
    DEPARTMENT,
}

enum class RegionalGuideCandidateCollectionTypeHint {
    DOOR_TO_DOOR,
    BASE_POINT,
}

private val RegionalWasteScheduleUiModel.stableKey: String
    get() = listOf(
        wasteTypeName.toStableKeyPart(),
        disposalDays.toStableKeyPart(),
        disposalTime?.stableKey.toStableKeyPart(),
        disposalMethod.toStableKeyPart(),
        disposalPlace.toStableKeyPart(),
    ).joinToString("/")

private fun String?.toStableKeyPart(): String =
    this
        ?.trim()
        ?.takeIf { value -> value.isNotBlank() }
        ?: "<blank>"

private fun String?.toDistinguishingText(
    label: RegionalGuideCandidateDistinguishingLabel
): RegionalGuideCandidateDistinguishingText? =
    takeIfRegionalGuideDisplayValue()
        ?.let { value ->
            RegionalGuideCandidateDistinguishingText(
                label = label,
                value = RegionalGuideCandidateDisplayText.Plain(value),
            )
        }

private fun String.isCollectionTypeDisplayName(disposalPlaceType: String): Boolean {
    val normalizedDisplayName = toCollectionTypeComparableText()
    val normalizedType = disposalPlaceType.toCollectionTypeComparableText()

    return normalizedDisplayName == normalizedType ||
        normalizedDisplayName == "${normalizedType}지역"
}

private fun String.toCollectionTypeComparableText(): String =
    trim().replace(" ", "")

internal fun List<RegionalGuideCandidateUiModel>.withDuplicateDisplayDisambiguation():
    List<RegionalGuideCandidateUiModel> {
    val disambiguationByDisplayText = groupBy { candidate -> candidate.displayText }
        .filterValues { candidates -> candidates.size > 1 }
        .mapValues { (_, candidates) ->
            candidates
                .mapNotNull { candidate -> candidate.candidateDisambiguation() }
                .distinctBy { disambiguation -> disambiguation.key }
        }
        .filterValues { disambiguations -> disambiguations.size > 1 }

    return map { candidate ->
        if (candidate.displayText in disambiguationByDisplayText) {
            candidate.copy(disambiguation = candidate.candidateDisambiguation())
        } else {
            candidate
        }
    }
}

internal val regionalGuideCandidateDisplayComparator: Comparator<RegionalGuideCandidateUiModel> =
    Comparator { left, right ->
        val leftOrderedManagementZoneSortText = left.orderedManagementZoneSortText
        val rightOrderedManagementZoneSortText = right.orderedManagementZoneSortText
        val primaryComparison = if (
            leftOrderedManagementZoneSortText != null &&
            rightOrderedManagementZoneSortText != null
        ) {
            RegionNameNaturalComparator.compare(
                leftOrderedManagementZoneSortText,
                rightOrderedManagementZoneSortText
            )
        } else {
            RegionNameNaturalComparator.compare(left.sortText, right.sortText)
        }

        primaryComparison
            .takeIf { comparison -> comparison != 0 }
            ?: RegionNameNaturalComparator.compare(left.displayText, right.displayText)
    }

private val leadingNumberRegex = Regex("^제?\\s*(\\d+)")
private val leadingRomanNumeralRegex = Regex(
    "^([IVXLCDM]+|[ⅠⅡⅢⅣⅤⅥⅦⅧⅨⅩⅪⅫ]+)(?=\\s*(구역|권역))",
    RegexOption.IGNORE_CASE
)
private val unicodeRomanNumeralValues = mapOf(
    "Ⅰ" to 1,
    "Ⅱ" to 2,
    "Ⅲ" to 3,
    "Ⅳ" to 4,
    "Ⅴ" to 5,
    "Ⅵ" to 6,
    "Ⅶ" to 7,
    "Ⅷ" to 8,
    "Ⅸ" to 9,
    "Ⅹ" to 10,
    "Ⅺ" to 11,
    "Ⅻ" to 12
)

private fun String.toOrderedManagementZoneSortText(): String? {
    val trimmed = trim()
    val leadingOrder =
        leadingNumberRegex.find(trimmed)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: leadingRomanNumeralRegex.find(trimmed)
                ?.groupValues
                ?.getOrNull(1)
                ?.toRomanNumeralOrNull()

    return leadingOrder?.let { order ->
        leadingNumberRegex.replaceFirst(trimmed, order.toString())
            .let { normalized ->
                leadingRomanNumeralRegex.replaceFirst(normalized, order.toString())
            }
    }
}

private fun String.toRomanNumeralOrNull(): Int? {
    unicodeRomanNumeralValues[this]?.let { value -> return value }

    val values = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000
    )
    var total = 0
    var previous = 0

    for (char in uppercase().reversed()) {
        val value = values[char] ?: return null
        if (value < previous) {
            total -= value
        } else {
            total += value
            previous = value
        }
    }

    return total.takeIf { value -> value > 0 }
}
