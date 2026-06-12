package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide

data class RegionalGuideFavoriteSnapshot(
    val targetId: String,
    val region: Region,
    val targetRegionName: String?,
    val managementZoneName: String?,
) {
    val key: RegionalGuideFavoriteKey?
        get() = RegionalGuideFavoriteKey.decodeOrNull(targetId)
}

fun RegionalDisposalGuide.toFavoriteSnapshot(): RegionalGuideFavoriteSnapshot {
    val key =
        RegionalGuideFavoriteKey(
            sido = region.sido,
            sigungu = region.sigungu,
            eupmyeondong = region.eupmyeondong,
            targetRegionName = targetRegionName,
        )

    return RegionalGuideFavoriteSnapshot(
        targetId = key.encode(),
        region = region,
        targetRegionName = targetRegionName?.trim()?.takeIf { it.isNotBlank() },
        managementZoneName = managementZoneName?.trim()?.takeIf { it.isNotBlank() },
    )
}
