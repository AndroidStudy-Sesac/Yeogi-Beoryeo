package com.team.yeogibeoryeo.presentation.favorites.model

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.presentation.R

enum class FavoriteTab(
    val targetType: FavoriteTargetType,
    @StringRes val labelResId: Int,
    @StringRes val emptyTitleResId: Int,
    @StringRes val emptyDescriptionResId: Int,
) {
    ITEM_GUIDE(
        targetType = FavoriteTargetType.ITEM_GUIDE,
        labelResId = R.string.favorites_tab_item,
        emptyTitleResId = R.string.favorites_empty_item_title,
        emptyDescriptionResId = R.string.favorites_empty_item_description,
    ),
    COLLECTION_SPOT(
        targetType = FavoriteTargetType.COLLECTION_SPOT,
        labelResId = R.string.favorites_tab_collection_spot,
        emptyTitleResId = R.string.favorites_empty_collection_spot_title,
        emptyDescriptionResId = R.string.favorites_empty_collection_spot_description,
    ),
    REGIONAL_GUIDE(
        targetType = FavoriteTargetType.REGIONAL_GUIDE,
        labelResId = R.string.favorites_tab_regional_guide,
        emptyTitleResId = R.string.favorites_empty_regional_guide_title,
        emptyDescriptionResId = R.string.favorites_empty_regional_guide_description,
    ),
}
