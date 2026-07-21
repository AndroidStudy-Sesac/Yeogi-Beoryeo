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

    val regionNameParts: List<String> =
        listOfNotNull(
            sido,
            sigungu,
            eupmyeondong
        )
            .filter { regionName -> regionName.isNotBlank() }

    val query: String =
        regionNameParts
            .joinToString(" ")

    val isValid: Boolean
        get() = regionNameParts.isNotEmpty()
}

private fun String?.toStableKeyPart(): String =
    this
        ?.trim()
        ?.takeIf { value -> value.isNotEmpty() }
        ?: BLANK_KEY_PART

private const val STABLE_KEY_SEPARATOR = "|"
private const val BLANK_KEY_PART = "<blank>"
