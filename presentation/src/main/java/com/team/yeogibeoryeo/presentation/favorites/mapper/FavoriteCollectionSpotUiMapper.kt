package com.team.yeogibeoryeo.presentation.favorites.mapper

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import javax.inject.Inject

class FavoriteCollectionSpotUiMapper
    @Inject
    constructor() {
    suspend fun map(favorite: Favorite): FavoriteUiModel? {
        // TODO: Resolve collection spot source data when COLLECTION_SPOT favorites are connected.
        return null
    }
}
