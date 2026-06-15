package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?
) {
    val displayText: String =
        listOfNotNull(
            guide.managementZoneName.takeIfNotBlank(),
            guide.targetRegionName.takeIfNotBlank()
        )
            .distinct()
            .joinToString(CANDIDATE_LABEL_SEPARATOR)
            .ifBlank { guide.regionName }

    private fun String?.takeIfNotBlank(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private companion object {
        const val CANDIDATE_LABEL_SEPARATOR = " / "
    }
}
