package com.team.yeogibeoryeo.presentation.favorites.mapper

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.item.usecase.GetDisposalItemGuideUseCase
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import javax.inject.Inject

class FavoriteItemGuideUiMapper
    @Inject
    constructor(
        private val getDisposalItemGuideUseCase: GetDisposalItemGuideUseCase,
    ) {
        suspend fun map(favorite: Favorite): FavoriteUiModel? {
            val guide = getDisposalItemGuideUseCase(favorite.targetId) ?: return null

            return FavoriteUiModel(
                type = FavoriteTargetType.ITEM_GUIDE,
                targetId = favorite.targetId,
                title = guide.name,
                subtitle = guide.subCategory?.displayName ?: guide.category.displayName,
            )
        }
    }
