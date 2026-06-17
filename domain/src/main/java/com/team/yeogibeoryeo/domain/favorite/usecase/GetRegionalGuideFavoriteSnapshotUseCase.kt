package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import javax.inject.Inject

class GetRegionalGuideFavoriteSnapshotUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteSnapshotRepository,
    ) {
        suspend operator fun invoke(targetId: String) = repository.getSnapshot(targetId)
    }
