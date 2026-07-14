package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy

internal object RegionalGuideFavoriteCompatibilityPolicy {

    fun isSameFavoriteTarget(
        favoriteKey: RegionalGuideFavoriteKey,
        candidate: RegionalDisposalGuide,
    ): Boolean {
        val favoriteRegion = favoriteKey.toCompatibilityRegion()

        return favoriteKey.hasSameRegion(candidate) &&
            favoriteKey.targetRegionName.isSameNormalizedText(
                region = favoriteRegion,
                other = candidate.targetRegionName,
            ) &&
            favoriteKey.managementZoneName.isCompatibleManagementZone(
                region = favoriteRegion,
                candidateManagementZoneName = candidate.managementZoneName,
            )
    }

    private fun RegionalGuideFavoriteKey.hasSameRegion(
        candidate: RegionalDisposalGuide,
    ): Boolean {
        val favoriteRegion = Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
        )

        if (RegionalGuideLegacyRegionCompatibilityPolicy.replacementRegions(favoriteRegion).isNotEmpty()) {
            return RegionalGuideLegacyRegionCompatibilityPolicy.isSameRegion(
                requestedRegion = favoriteRegion,
                candidateRegion = candidate.region,
            )
        }

        return RegionSidoAliasPolicy.isSameSido(
            requestedSido = sido,
            requestedSigungu = sigungu,
            candidateSido = candidate.region.sido,
            candidateSigungu = candidate.region.sigungu,
        ) &&
            sigungu.normalizeRegionGuideName() == candidate.region.sigungu.normalizeRegionGuideName() &&
            eupmyeondong.normalizeRegionGuideName() == candidate.region.eupmyeondong.normalizeRegionGuideName()
    }

    private fun String?.isCompatibleManagementZone(
        region: Region,
        candidateManagementZoneName: String?,
    ): Boolean {
        val favoriteManagementZoneName = normalizeRegionGuideName(region)
            ?: return true

        return favoriteManagementZoneName == candidateManagementZoneName.normalizeRegionGuideName()
    }

    private fun String?.isSameNormalizedText(
        region: Region,
        other: String?,
    ): Boolean =
        normalizeRegionGuideName(region) == other.normalizeRegionGuideName()

    private fun String?.normalizeRegionGuideName(
        region: Region,
    ): String? =
        RegionalGuideLegacyRegionCompatibilityPolicy.normalizeRegionalGuideName(
            region = region,
            name = this,
        )

    private fun String?.normalizeRegionGuideName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private fun RegionalGuideFavoriteKey.toCompatibilityRegion(): Region =
        Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
        )
}
