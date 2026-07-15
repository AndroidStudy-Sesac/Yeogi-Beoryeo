package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import javax.inject.Inject

class SelectHomeRegionalGuidePrimaryFavoriteUseCase
    @Inject
    constructor() {
        operator fun invoke(
            favorites: List<Favorite>,
            snapshots: List<RegionalGuideFavoriteSnapshot>,
            pinnedTargetId: String?,
            previousTargetId: String?,
        ): Favorite? {
            val regionalGuideFavorites =
                favorites.filter { favorite -> favorite.type == FavoriteTargetType.REGIONAL_GUIDE }
            val snapshotTargetIds = snapshots.map { snapshot -> snapshot.targetId }.toSet()
            val restorableFavorites =
                regionalGuideFavorites.filter { favorite -> favorite.targetId in snapshotTargetIds }

            return restorableFavorites.find { favorite -> favorite.targetId == pinnedTargetId }
                ?: restorableFavorites.find { favorite -> favorite.targetId == previousTargetId }
                ?: restorableFavorites.selectDeterministicFallback()
                ?: regionalGuideFavorites.selectDeterministicFallback()
        }

        private fun List<Favorite>.selectDeterministicFallback(): Favorite? =
            sortedWith(
                compareByDescending<Favorite> { favorite -> favorite.savedAtMillis }
                    .thenBy { favorite -> favorite.targetId },
            ).firstOrNull()
    }
