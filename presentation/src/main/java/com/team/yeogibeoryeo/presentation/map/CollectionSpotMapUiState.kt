package com.team.yeogibeoryeo.presentation.map

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

data class CollectionSpotMapUiState(
    val searchKeyword: String = "",
    val searchMode: MapSearchMode = MapSearchMode.KEYWORD,
    val spots: List<CollectionSpot> = emptyList(),
    val selectedSpot: CollectionSpot? = null,
    val selectedTypes: Set<CollectionSpotType> = emptySet(),
    val isLoading: Boolean = false,
    @param:StringRes val errorMessageResId: Int? = null,
    val hasSearched: Boolean = false,
    val locationNotice: MapLocationNotice? = null,
    val favoriteSpotMoveRequestId: String? = null,
    val favoriteSpotMoveRequestSequence: Int = 0,
    val isFavoriteSpotNearbyLoading: Boolean = false,
)
