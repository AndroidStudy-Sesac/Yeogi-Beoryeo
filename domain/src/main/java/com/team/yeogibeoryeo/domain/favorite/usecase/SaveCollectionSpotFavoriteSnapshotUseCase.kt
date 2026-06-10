package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import javax.inject.Inject

class SaveCollectionSpotFavoriteSnapshotUseCase
    @Inject
    constructor(
        private val repository: CollectionSpotFavoriteSnapshotRepository,
    ) {
        suspend operator fun invoke(snapshot: CollectionSpotFavoriteSnapshot) {
            repository.upsertSnapshot(snapshot)
        }
    }
