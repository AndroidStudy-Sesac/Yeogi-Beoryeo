package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel

internal object RegionalGuideFavoriteSnapshotFactory {

    fun from(candidate: RegionalGuideCandidateUiModel): RegionalGuideFavoriteSnapshot =
        RegionalGuideFavoriteSnapshot(
            targetId = RegionalGuideFavoriteKey(
                sido = candidate.sido,
                sigungu = candidate.sigungu,
                eupmyeondong = candidate.eupmyeondong,
                targetRegionName = candidate.guide.targetRegionName,
                managementZoneName = candidate.guide.managementZoneName,
            ).encode(),
            region = Region(
                sido = candidate.sido,
                sigungu = candidate.sigungu,
                eupmyeondong = candidate.eupmyeondong,
            ),
            targetRegionName = candidate.guide.targetRegionName,
            managementZoneName = candidate.guide.managementZoneName,
        )
}
