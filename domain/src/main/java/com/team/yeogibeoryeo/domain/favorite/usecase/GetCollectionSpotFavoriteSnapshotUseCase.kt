package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import javax.inject.Inject

class GetCollectionSpotFavoriteSnapshotUseCase
    @Inject
    constructor(
        private val repository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        suspend operator fun invoke(targetId: String) = repository.getSnapshot(targetId)
    }
