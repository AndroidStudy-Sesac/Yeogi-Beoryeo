package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import javax.inject.Inject

class ObserveRegionalGuideFavoriteSnapshotsUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteSnapshotRepository,
    ) {
        operator fun invoke() = repository.observeSnapshots()
    }
