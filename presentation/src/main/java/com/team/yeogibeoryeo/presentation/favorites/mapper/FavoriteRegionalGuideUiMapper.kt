package com.team.yeogibeoryeo.presentation.favorites.mapper

import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import javax.inject.Inject

class FavoriteRegionalGuideUiMapper
    @Inject
    constructor() {
        fun map(snapshot: RegionalGuideFavoriteSnapshot): FavoriteUiModel =
            FavoriteUiModel(
                type = FavoriteTargetType.REGIONAL_GUIDE,
                targetId = snapshot.targetId,
                title = snapshot.regionDisplayName(),
                subtitle = snapshot.toSubtitle(),
            )

        private fun RegionalGuideFavoriteSnapshot.regionDisplayName(): String =
            listOfNotNull(
                region.sido,
                region.sigungu,
                region.eupmyeondong,
            )
                .filter { regionName -> regionName.isNotBlank() }
                .joinToString(" > ")
                .ifBlank {
                    targetRegionName
                        ?: managementZoneName
                        ?: "지역 가이드"
                }

        private fun RegionalGuideFavoriteSnapshot.toSubtitle(): String? =
            listOfNotNull(
                targetRegionName?.takeIf { it.isNotBlank() },
                managementZoneName?.takeIf { it.isNotBlank() },
            )
                .joinToString(" · ")
                .takeIf { it.isNotBlank() }
    }
