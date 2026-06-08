package com.team.yeogibeoryeo.presentation.favorites.model

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType

enum class FavoriteTab(
    val targetType: FavoriteTargetType,
    val label: String,
    val emptyTitle: String,
    val emptyDescription: String,
) {
    ITEM_GUIDE(
        targetType = FavoriteTargetType.ITEM_GUIDE,
        label = "품목",
        emptyTitle = "아직 즐겨찾기한 품목이 없어요",
        emptyDescription = "품목 가이드 상세 화면에서 별을 누르면 여기에 모아볼 수 있어요.",
    ),
    COLLECTION_SPOT(
        targetType = FavoriteTargetType.COLLECTION_SPOT,
        label = "장소",
        emptyTitle = "아직 즐겨찾기한 장소가 없어요",
        emptyDescription = "수거 장소 즐겨찾기 연결 후 이곳에서 모아볼 수 있어요.",
    ),
    REGIONAL_GUIDE(
        targetType = FavoriteTargetType.REGIONAL_GUIDE,
        label = "지역",
        emptyTitle = "아직 즐겨찾기한 지역 가이드가 없어요",
        emptyDescription = "지역 가이드 즐겨찾기 연결 후 이곳에서 모아볼 수 있어요.",
    ),
}
