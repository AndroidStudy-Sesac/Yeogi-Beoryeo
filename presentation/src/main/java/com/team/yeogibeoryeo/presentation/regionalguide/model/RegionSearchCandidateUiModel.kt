package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionSearchCandidateUiModel(
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?
) {
    val displayText: String =
        listOfNotNull(
            sido,
            sigungu,
            eupmyeondong
        )
            .filter { regionName -> regionName.isNotBlank() }
            .joinToString(" > ")

    val isValid: Boolean
        get() = displayText.isNotBlank()
}
