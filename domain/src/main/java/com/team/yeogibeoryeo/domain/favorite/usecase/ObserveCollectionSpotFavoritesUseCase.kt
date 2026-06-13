package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

class ObserveCollectionSpotFavoritesUseCase
    @Inject
    constructor(
        private val favoriteRepository: FavoriteRepository,
        private val snapshotRepository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        operator fun invoke() =
            combine(
                favoriteRepository.observeFavorites(),
                snapshotRepository.observeSnapshots(),
            ) { favorites, snapshots ->
                val snapshotsByTargetId = snapshots.associateBy { snapshot -> snapshot.targetId }

                favorites
                    .filter { favorite -> favorite.type == FavoriteTargetType.COLLECTION_SPOT }
                    .sortedByDescending { favorite -> favorite.savedAtMillis }
                    .mapNotNull { favorite ->
                        snapshotsByTargetId[favorite.targetId]?.let { snapshot ->
                            CollectionSpotFavorite(
                                targetId = favorite.targetId,
                                savedAtMillis = favorite.savedAtMillis,
                                snapshot = snapshot,
                            )
                        }
                    }
            }
    }
