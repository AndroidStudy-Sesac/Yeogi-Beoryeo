package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?
) {
    val displayText: String =
        primaryDisplayParts()
            .ifEmpty { fallbackDisplayParts() }
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

    private fun primaryDisplayParts(): List<String> =
        listOfNotNull(
            guide.managementZoneName.takeIfRegionalGuideDisplayValue(),
            guide.targetRegionName.takeIfRegionalGuideDisplayValue()
        )

    private fun fallbackDisplayParts(): List<String> =
        listOfNotNull(
            regionalGuideRegionFallbackText(sido, sigungu, eupmyeondong),
            guide.disposalPlaceType.takeIfRegionalGuideDisplayValue()
                ?: guide.disposalPlaceDescription.takeIfRegionalGuideDisplayValue()
                ?: guide.schedules.firstNotNullOfOrNull { schedule ->
                    schedule.toCandidateSummary()
                },
        )

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

internal val regionalGuideCandidateDisplayComparator: Comparator<RegionalGuideCandidateUiModel> =
    Comparator { left, right ->
        compareNaturalText(left.sortText, right.sortText)
            .takeIf { comparison -> comparison != 0 }
            ?: compareNaturalText(left.displayText, right.displayText)
    }

private val naturalSortTokenRegex = Regex("\\d+|\\D+")

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
