package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject

class RemoveRegionalGuideFavoriteUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteRepository,
        private val homeRegionalGuidePrimaryFavoriteRepository:
            HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        suspend operator fun invoke(targetId: String) {
            repository.removeFavorite(targetId)
            homeRegionalGuidePrimaryFavoriteRepository.clearPrimaryFavoriteTargetIdIfMatches(targetId)
        }
    }
