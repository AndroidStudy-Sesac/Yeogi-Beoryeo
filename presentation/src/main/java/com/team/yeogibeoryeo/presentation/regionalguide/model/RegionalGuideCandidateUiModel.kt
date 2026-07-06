package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    internal val disambiguationText: String? = null
) {
    val displayText: String =
        baseDisplayParts()
            .appendDisambiguation()
            .distinct()
            .joinToString(CANDIDATE_LABEL_SEPARATOR)

    internal val sortText: String =
        primaryDisplayParts().let { parts ->
            when {
                parts.size >= 2 -> listOf(parts[1], parts[0])
                parts.isNotEmpty() -> parts
                else -> listOf(displayText)
            }
        }.joinToString(CANDIDATE_LABEL_SEPARATOR)

    internal val orderedManagementZoneSortText: String? =
        primaryDisplayParts()
            .takeIf { parts -> parts.size >= 2 }
            ?.let { parts ->
                parts[0].toOrderedManagementZoneSortText()
                    ?.let { managementZoneSortText ->
                        listOf(managementZoneSortText, parts[1]).joinToString(CANDIDATE_LABEL_SEPARATOR)
                    }
            }

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

    private fun RegionalWasteScheduleUiModel.toCandidateSummary(): String? {
        val criterion =
            disposalDays.takeIfRegionalGuideDisplayValue()
                ?: disposalTime.takeIfRegionalGuideDisplayValue()
                ?: disposalMethod.takeIfRegionalGuideDisplayValue()

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
    }
}

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

    if (disambiguationByDisplayText.isEmpty()) return this

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
            compareNaturalText(leftOrderedManagementZoneSortText, rightOrderedManagementZoneSortText)
        } else {
            compareNaturalText(left.sortText, right.sortText)
        }

        primaryComparison
            .takeIf { comparison -> comparison != 0 }
            ?: compareNaturalText(left.displayText, right.displayText)
    }

private val naturalSortTokenRegex = Regex("\\d+|\\D+")
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

private fun compareNaturalText(left: String, right: String): Int {
    val leftTokens = left.naturalSortTokens()
    val rightTokens = right.naturalSortTokens()
    val minSize = minOf(leftTokens.size, rightTokens.size)

    for (index in 0 until minSize) {
        val comparison = compareNaturalToken(leftTokens[index], rightTokens[index])
        if (comparison != 0) return comparison
    }

    return leftTokens.size.compareTo(rightTokens.size)
}

private fun String.naturalSortTokens(): List<String> =
    naturalSortTokenRegex.findAll(this).map { match -> match.value }.toList()

private fun compareNaturalToken(left: String, right: String): Int {
    val leftNumber = left.toLongOrNull()
    val rightNumber = right.toLongOrNull()

    return if (leftNumber != null && rightNumber != null) {
        leftNumber.compareTo(rightNumber)
            .takeIf { comparison -> comparison != 0 }
            ?: left.length.compareTo(right.length)
    } else {
        left.compareTo(right)
    }
}
