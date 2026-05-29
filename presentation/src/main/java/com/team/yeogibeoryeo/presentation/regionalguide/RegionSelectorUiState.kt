package com.team.yeogibeoryeo.presentation.regionalguide

data class RegionSelectorUiState(
    val sidoOptions: List<String> = emptyList(),
    val sigunguOptions: List<String> = emptyList(),
    val eupmyeondongOptions: List<String> = emptyList(),
    val selectedSido: String? = null,
    val selectedSigungu: String? = null,
    val selectedEupmyeondong: String? = null,
) {
    val selectedRegionText: String?
        get() = selectedRegionParts
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" > ")

    val isSigunguSelectionEnabled: Boolean
        get() = !selectedSido.isNullOrBlank()

    val isEupmyeondongSelectionEnabled: Boolean
        get() = !selectedSigungu.isNullOrBlank()

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