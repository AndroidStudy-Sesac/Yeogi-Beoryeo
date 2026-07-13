package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy

object RegionalGuideFavoriteCompatibilityPolicy {

    fun isSameFavoriteTarget(
        favoriteKey: RegionalGuideFavoriteKey,
        candidate: RegionalDisposalGuide,
    ): Boolean {
        return favoriteKey.hasSameRegion(candidate) &&
            favoriteKey.targetRegionName.isSameNormalizedText(candidate.targetRegionName) &&
            favoriteKey.managementZoneName.isCompatibleManagementZone(candidate.managementZoneName)
    }

    private fun RegionalGuideFavoriteKey.hasSameRegion(
        candidate: RegionalDisposalGuide,
    ): Boolean {
        return RegionSidoAliasPolicy.isSameSido(
            requestedSido = sido,
            requestedSigungu = sigungu,
            candidateSido = candidate.region.sido,
            candidateSigungu = candidate.region.sigungu,
        ) &&
            sigungu.isSameNormalizedText(candidate.region.sigungu) &&
            eupmyeondong.isSameNormalizedText(candidate.region.eupmyeondong)
    }

    private fun String?.isCompatibleManagementZone(
        candidateManagementZoneName: String?,
    ): Boolean {
        val favoriteManagementZoneName = normalizeRegionGuideName()
            ?: return true

        return favoriteManagementZoneName == candidateManagementZoneName.normalizeRegionGuideName()
    }

    private fun String?.isSameNormalizedText(
        other: String?,
    ): Boolean =
        normalizeRegionGuideName() == other.normalizeRegionGuideName()

    private fun String?.normalizeRegionGuideName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }
}
