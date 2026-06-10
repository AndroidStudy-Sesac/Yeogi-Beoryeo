package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.toFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import javax.inject.Inject

class ToggleCollectionSpotFavoriteUseCase
    @Inject
    constructor(
        private val favoriteRepository: FavoriteRepository,
        private val snapshotRepository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        suspend operator fun invoke(spot: CollectionSpot): Boolean {
            val isFavorite =
                favoriteRepository.toggleFavorite(
                    Favorite(
                        type = FavoriteTargetType.COLLECTION_SPOT,
                        targetId = spot.id,
                        savedAtMillis = System.currentTimeMillis(),
                    ),
                )

            if (isFavorite) {
                snapshotRepository.upsertSnapshot(spot.toFavoriteSnapshot())
            } else {
                snapshotRepository.deleteSnapshot(spot.id)
            }

            return isFavorite
        }
    }
