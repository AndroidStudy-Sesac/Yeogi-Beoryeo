package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import javax.inject.Inject

class RemoveRegionalGuideFavoriteUseCase
    @Inject
    constructor(
        private val repository: RegionalGuideFavoriteRepository,
    ) {
        suspend operator fun invoke(targetId: String) {
            repository.removeFavorite(targetId)
        }
    }
