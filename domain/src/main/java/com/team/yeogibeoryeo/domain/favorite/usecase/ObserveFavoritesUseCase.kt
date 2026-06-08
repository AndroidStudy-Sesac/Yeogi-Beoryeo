package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class ObserveFavoritesUseCase
    @Inject
    constructor(
        private val repository: FavoriteRepository,
    ) {
        operator fun invoke(type: FavoriteTargetType? = null) =
            repository.observeFavorites()
                .map { favorites ->
                    if (type == null) {
                        favorites
                    } else {
                        favorites.filter { it.type == type }
                    }
                }
    }
