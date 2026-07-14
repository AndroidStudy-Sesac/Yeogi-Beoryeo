package com.team.yeogibeoryeo.presentation.regionalguide

data class RegionSelectorUiState(
    val sidoOptions: List<String> = emptyList(),
    val sigunguOptions: List<String> = emptyList(),
    val eupmyeondongOptions: List<String> = emptyList(),
    val isEupmyeondongOptionsLoading: Boolean = false,
    val selectedSido: String? = null,
    val selectedSigungu: String? = null,
    val selectedEupmyeondong: String? = null,
    val expandedDropdown: RegionSelectorDropdown? = null,
) {
    val selectedRegionText: String?
        get() = selectedRegionParts
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" > ")

    val isSigunguSelectionEnabled: Boolean
        get() = !selectedSido.isNullOrBlank()

    val isEupmyeondongSelectionEnabled: Boolean
        get() = !selectedSigungu.isNullOrBlank() &&
                !isEupmyeondongOptionsLoading &&
                eupmyeondongOptions.isNotEmpty()

    val eupmyeondongSelectionLabel: String
        get() = selectedEupmyeondong
            ?: when {
                isEupmyeondongOptionsLoading -> "읍면동 불러오는 중"
                !selectedSigungu.isNullOrBlank() && eupmyeondongOptions.isEmpty() -> "제공되는 읍면동 없음"
                else -> "읍면동 선택"
            }

    val canSearchSelectedRegion: Boolean
        get() = !selectedSido.isNullOrBlank() &&
                !selectedSigungu.isNullOrBlank()

    private val selectedRegionParts: List<String>
        get() = listOfNotNull(
            selectedSido,
            selectedSigungu,
            selectedEupmyeondong,
        )
}

enum class RegionSelectorDropdown {
    SIDO,
    SIGUNGU,
    EUPMYEONDONG,
}
