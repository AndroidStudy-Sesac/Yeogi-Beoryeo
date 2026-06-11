package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import javax.inject.Inject

class ToggleCollectionSpotFavoriteUseCase
    @Inject
    constructor(
        private val collectionSpotFavoriteRepository: CollectionSpotFavoriteRepository,
    ) {
        suspend operator fun invoke(spot: CollectionSpot): Boolean =
            collectionSpotFavoriteRepository.toggleFavorite(spot)
    }
