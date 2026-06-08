package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionalGuideCandidateUiModel(
    val guide: RegionalGuideUiModel,
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?
) {
    val displayText: String =
        guide.targetRegionName
            ?.takeIf { targetRegionName -> targetRegionName.isNotBlank() }
            ?: guide.managementZoneName
            ?: guide.regionName
}
