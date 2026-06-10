package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject

class RemoveCollectionSpotFavoriteUseCase
    @Inject
    constructor(
        private val favoriteRepository: FavoriteRepository,
        private val snapshotRepository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        suspend operator fun invoke(targetId: String) {
            favoriteRepository.removeFavorite(
                type = FavoriteTargetType.COLLECTION_SPOT,
                targetId = targetId,
            )
            snapshotRepository.deleteSnapshot(targetId)
        }
    }
