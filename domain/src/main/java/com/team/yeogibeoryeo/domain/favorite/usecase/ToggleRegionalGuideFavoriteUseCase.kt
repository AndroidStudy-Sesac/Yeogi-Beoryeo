package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import kotlinx.coroutines.CancellationException
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
            clearRepresentativeTargetIds(snapshot)
            return isFavorite
        }

        private suspend fun clearRepresentativeTargetIds(snapshot: RegionalGuideFavoriteSnapshot) {
            try {
                homeRegionalGuidePrimaryFavoriteRepository
                    .clearPrimaryAndLastSelectedFavoriteTargetIdsIfMatches(snapshot.compatibleTargetIds)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                // Favorite toggle has already completed. Representative cache cleanup is best-effort.
            }
        }
    }
