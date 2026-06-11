package com.team.yeogibeoryeo.presentation.favorites.mapper

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.usecase.GetCollectionSpotFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import com.team.yeogibeoryeo.presentation.map.mapper.toDisplayName
import javax.inject.Inject

class FavoriteCollectionSpotUiMapper
    @Inject
    constructor(
        private val getCollectionSpotFavoriteSnapshotUseCase: GetCollectionSpotFavoriteSnapshotUseCase,
    ) {
        suspend fun map(favorite: Favorite): FavoriteUiModel? {
            if (favorite.type != FavoriteTargetType.COLLECTION_SPOT) return null

            val snapshot = getCollectionSpotFavoriteSnapshotUseCase(favorite.targetId) ?: return null

            return FavoriteUiModel(
                type = FavoriteTargetType.COLLECTION_SPOT,
                targetId = favorite.targetId,
                title = snapshot.name,
                subtitle = snapshot.toSubtitle(),
            )
        }

        fun map(snapshot: CollectionSpotFavoriteSnapshot): FavoriteUiModel =
            FavoriteUiModel(
                type = FavoriteTargetType.COLLECTION_SPOT,
                targetId = snapshot.targetId,
                title = snapshot.name,
                subtitle = snapshot.toSubtitle(),
            )

        private fun CollectionSpotFavoriteSnapshot.toSubtitle(): String =
            listOf(
                type.toDisplayName(),
                address,
                detailLocation,
            )
                .filterNot { it.isNullOrBlank() }
                .joinToString(" · ")
    }
