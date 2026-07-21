package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel

internal object RegionalGuideFavoriteSnapshotFactory {

    fun from(guide: RegionalDisposalGuide): RegionalGuideFavoriteSnapshot =
        create(
            region = guide.region,
            targetRegionName = guide.targetRegionName?.trim()?.takeIf(String::isNotBlank),
            managementZoneName = guide.managementZoneName?.trim()?.takeIf(String::isNotBlank),
        )

    fun from(candidate: RegionalGuideCandidateUiModel): RegionalGuideFavoriteSnapshot =
        create(
            region = Region(
                sido = candidate.sido,
                sigungu = candidate.sigungu,
                eupmyeondong = candidate.eupmyeondong,
            ),
            targetRegionName = candidate.guide.targetRegionName,
            managementZoneName = candidate.guide.managementZoneName,
        )

    private fun create(
        region: Region,
        targetRegionName: String?,
        managementZoneName: String?,
    ): RegionalGuideFavoriteSnapshot =
        RegionalGuideFavoriteSnapshot(
            targetId = RegionalGuideFavoriteKey(
                sido = region.sido,
                sigungu = region.sigungu,
                eupmyeondong = region.eupmyeondong,
                targetRegionName = targetRegionName,
                managementZoneName = managementZoneName,
            ).encode(),
            region = region,
            targetRegionName = targetRegionName,
            managementZoneName = managementZoneName,
        )
}
