package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase
    @Inject
    constructor(
        private val repository: FavoriteRepository,
    ) {
        suspend operator fun invoke(favorite: Favorite): Boolean = repository.toggleFavorite(favorite)
    }
