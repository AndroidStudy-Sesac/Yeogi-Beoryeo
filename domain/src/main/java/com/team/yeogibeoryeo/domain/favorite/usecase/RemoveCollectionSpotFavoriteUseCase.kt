package com.team.yeogibeoryeo.domain.favorite.usecase

import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import javax.inject.Inject

class RemoveCollectionSpotFavoriteUseCase
    @Inject
    constructor(
        private val collectionSpotFavoriteRepository: CollectionSpotFavoriteRepository,
    ) {
        suspend operator fun invoke(targetId: String) =
            collectionSpotFavoriteRepository.removeFavorite(targetId)
    }
