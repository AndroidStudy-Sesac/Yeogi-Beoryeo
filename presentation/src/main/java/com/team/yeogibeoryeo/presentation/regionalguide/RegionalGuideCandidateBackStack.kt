package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region

internal class RegionalGuideCandidateBackStack {

    private val entries = mutableListOf<RegionalGuideCandidateBackStackEntry>()

    val canRestore: Boolean
        get() = entries.isNotEmpty()

    fun push(
        uiState: RegionalGuideUiState,
        searchKeyword: String,
        regionSelectorUiState: RegionSelectorUiState,
        lastRequest: RegionalGuideRequest?,
    ) {
        entries += RegionalGuideCandidateBackStackEntry(
            uiState = uiState,
            searchKeyword = searchKeyword,
            regionSelectorUiState = regionSelectorUiState,
            lastRequest = lastRequest,
        )
    }

    fun pop(): RegionalGuideCandidateBackStackEntry? = entries.removeLastOrNull()

    fun clear() {
        entries.clear()
    }
}

internal sealed interface RegionalGuideRequest {
    data class Keyword(
        val keyword: String,
    ) : RegionalGuideRequest

    data class Address(
        val address: String,
    ) : RegionalGuideRequest

    data class Favorite(
        val targetId: String,
    ) : RegionalGuideRequest

    data class SelectedRegion(
        val query: String,
        val region: Region,
    ) : RegionalGuideRequest
}

internal data class RegionalGuideCandidateBackStackEntry(
    val uiState: RegionalGuideUiState,
    val searchKeyword: String,
    val regionSelectorUiState: RegionSelectorUiState,
    val lastRequest: RegionalGuideRequest?,
)
