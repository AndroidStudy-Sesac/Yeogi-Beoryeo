package com.team.yeogibeoryeo.presentation.regionalguide.model

data class RegionSearchCandidateUiModel(
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?
) {
    val stableKey: String =
        listOf(
            "sido=${sido.toStableKeyPart()}",
            "sigungu=${sigungu.toStableKeyPart()}",
            "eupmyeondong=${eupmyeondong.toStableKeyPart()}"
        ).joinToString(STABLE_KEY_SEPARATOR)

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

private fun String?.toStableKeyPart(): String =
    this
        ?.trim()
        ?.takeIf { value -> value.isNotEmpty() }
        ?: BLANK_KEY_PART

private const val STABLE_KEY_SEPARATOR = "|"
private const val BLANK_KEY_PART = "<blank>"
