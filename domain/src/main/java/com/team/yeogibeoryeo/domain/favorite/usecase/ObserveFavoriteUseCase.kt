package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject

class ObserveFavoriteUseCase
    @Inject
    constructor(
        private val repository: FavoriteRepository,
    ) {
        operator fun invoke(
            type: FavoriteTargetType,
            targetId: String,
        ) = repository.observeFavorite(type, targetId)
    }
