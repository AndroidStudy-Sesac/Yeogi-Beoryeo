package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import javax.inject.Inject

class ObserveCollectionSpotFavoriteSnapshotsUseCase
    @Inject
    constructor(
        private val repository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        operator fun invoke() = repository.observeSnapshots()
    }
