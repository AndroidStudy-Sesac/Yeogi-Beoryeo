package com.team.yeogibeoryeo.data.favorite.mapper

import com.team.yeogibeoryeo.data.favorite.local.RegionalGuideFavoriteSnapshotEntity
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region

fun RegionalGuideFavoriteSnapshot.toEntity(): RegionalGuideFavoriteSnapshotEntity =
    RegionalGuideFavoriteSnapshotEntity(
        targetId = targetId,
        sido = region.sido,
        sigungu = region.sigungu,
        eupmyeondong = region.eupmyeondong,
        targetRegionName = targetRegionName,
        managementZoneName = managementZoneName,
    )

fun RegionalGuideFavoriteSnapshotEntity.toDomain(): RegionalGuideFavoriteSnapshot? {
    val key = RegionalGuideFavoriteKey.decodeOrNull(targetId) ?: return null

    return RegionalGuideFavoriteSnapshot(
        targetId = targetId,
        region =
            Region(
                sido = sido ?: key.sido,
                sigungu = sigungu ?: key.sigungu,
                eupmyeondong = eupmyeondong ?: key.eupmyeondong,
            ),
        targetRegionName = targetRegionName ?: key.targetRegionName,
        managementZoneName = managementZoneName,
    )
}
