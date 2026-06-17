package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import javax.inject.Inject

class ToggleRegionalGuideFavoriteUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteRepository,
    ) {
        suspend operator fun invoke(snapshot: RegionalGuideFavoriteSnapshot): Boolean =
            repository.toggleFavorite(snapshot)
    }
