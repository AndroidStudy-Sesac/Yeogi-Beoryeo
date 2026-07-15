package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
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
            targetId.compatibleRegionalGuideTargetIds().forEach { compatibleTargetId ->
                homeRegionalGuidePrimaryFavoriteRepository
                    .clearPrimaryFavoriteTargetIdIfMatches(compatibleTargetId)
                homeRegionalGuidePrimaryFavoriteRepository
                    .clearLastSelectedFavoriteTargetIdIfMatches(compatibleTargetId)
            }
        }

        private fun String.compatibleRegionalGuideTargetIds(): List<String> {
            val key = RegionalGuideFavoriteKey.decodeOrNull(this)
            return listOfNotNull(
                this,
                key?.copy(managementZoneName = null)?.encodeLegacy(),
            ).distinct()
        }
    }
