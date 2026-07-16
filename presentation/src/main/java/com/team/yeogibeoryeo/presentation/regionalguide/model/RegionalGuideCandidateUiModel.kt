package com.team.yeogibeoryeo.presentation.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    internal val disambiguationText: String? = null
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
                schedule.toCandidateSummary()
            }?.toDistinguishingText(
                RegionalGuideCandidateDistinguishingLabel.SCHEDULE
            ),
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
        listOfNotNull(
            regionalGuideRegionFallbackText(sido, sigungu, eupmyeondong),
            candidateDisambiguationText(),
        )

    internal fun candidateDisambiguationText(): String? =
        guide.disposalPlaceType.takeIfRegionalGuideDisplayValue()
            ?: guide.disposalPlaceDescription.takeIfRegionalGuideDisplayValue()
            ?: guide.schedules.firstNotNullOfOrNull { schedule ->
                schedule.toCandidateSummary()
            }

    private fun List<String>.appendDisambiguation(): List<String> =
        this + listOfNotNull(disambiguationText.takeIfRegionalGuideDisplayValue())

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

    private fun RegionalWasteScheduleUiModel.toCandidateSummary(): String? {
        val criterion =
            disposalDays.takeIfRegionalGuideDisplayValue()
                ?: disposalTime.takeIfRegionalGuideDisplayValue()
                ?: disposalMethod.takeIfRegionalGuideDisplayValue()
                ?: disposalPlace.takeIfRegionalGuideDisplayValue()

        return listOfNotNull(
            wasteTypeName.takeIfRegionalGuideDisplayValue(),
            criterion,
        )
            .joinToString(SCHEDULE_SUMMARY_SEPARATOR)
            .takeIf { summary -> summary.isNotBlank() }
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

internal data class RegionalGuideCandidateDistinguishingText(
    val label: RegionalGuideCandidateDistinguishingLabel,
    val value: String
)

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
        disposalTime.toStableKeyPart(),
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
                value = value
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
                .mapNotNull { candidate -> candidate.candidateDisambiguationText() }
                .distinct()
        }
        .filterValues { disambiguationTexts -> disambiguationTexts.size > 1 }

    return map { candidate ->
        if (candidate.displayText in disambiguationByDisplayText) {
            candidate.copy(disambiguationText = candidate.candidateDisambiguationText())
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
