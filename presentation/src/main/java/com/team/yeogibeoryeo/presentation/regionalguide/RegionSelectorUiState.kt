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
    val selectedRegionQuery: String?
        get() = selectedRegionParts
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")

    val isSigunguSelectionEnabled: Boolean
        get() = !selectedSido.isNullOrBlank()

    val isEupmyeondongSelectionEnabled: Boolean
        get() = !selectedSigungu.isNullOrBlank() &&
                !isEupmyeondongOptionsLoading &&
                eupmyeondongOptions.isNotEmpty()

    val eupmyeondongSelectionStatus: RegionSelectorEupmyeondongSelectionStatus
        get() = selectedEupmyeondong
            ?.let { RegionSelectorEupmyeondongSelectionStatus.Selected(it) }
            ?: when {
                isEupmyeondongOptionsLoading -> RegionSelectorEupmyeondongSelectionStatus.Loading
                !selectedSigungu.isNullOrBlank() && eupmyeondongOptions.isEmpty() ->
                    RegionSelectorEupmyeondongSelectionStatus.Unavailable

                else -> RegionSelectorEupmyeondongSelectionStatus.Default
            }

    val canSearchSelectedRegion: Boolean
        get() = !selectedSido.isNullOrBlank() &&
                !selectedSigungu.isNullOrBlank()

    val selectedRegionParts: List<String>
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

sealed interface RegionSelectorEupmyeondongSelectionStatus {
    data class Selected(
        val name: String,
    ) : RegionSelectorEupmyeondongSelectionStatus

    data object Loading : RegionSelectorEupmyeondongSelectionStatus

    data object Unavailable : RegionSelectorEupmyeondongSelectionStatus

    data object Default : RegionSelectorEupmyeondongSelectionStatus
}
