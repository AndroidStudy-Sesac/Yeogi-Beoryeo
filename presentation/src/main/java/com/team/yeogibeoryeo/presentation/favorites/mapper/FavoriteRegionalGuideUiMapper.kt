package com.team.yeogibeoryeo.presentation.favorites.mapper

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import javax.inject.Inject

class FavoriteRegionalGuideUiMapper
    @Inject
    constructor() {
    suspend fun map(favorite: Favorite): FavoriteUiModel? {
        // TODO: Resolve regional guide source data when REGIONAL_GUIDE favorites are connected.
        return null
    }
}
