package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject

class ToggleRegionalGuideFavoriteUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteRepository,
        private val homeRegionalGuidePrimaryFavoriteRepository:
            HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        suspend operator fun invoke(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
            val isFavorite = repository.toggleFavorite(snapshot)
            if (!isFavorite) {
                snapshot.compatibleTargetIds.forEach { targetId ->
                    homeRegionalGuidePrimaryFavoriteRepository
                        .clearPrimaryFavoriteTargetIdIfMatches(targetId)
                    homeRegionalGuidePrimaryFavoriteRepository
                        .clearLastSelectedFavoriteTargetIdIfMatches(targetId)
                }
            }
            return isFavorite
        }
    }
