package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionCandidateComparator
import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy

internal object RegionalGuideAvailabilityFilter {

    fun filterEupmyeondongOptions(
        options: List<String>,
        availability: List<RegionalGuideAvailabilityDto>,
        sido: String,
        sigungu: String,
    ): List<String> {
        val scopeRegion = Region(sido = sido, sigungu = sigungu)
        val scopedAvailability = availability.filter { availableRegion ->
            availableRegion.matchesRegionalGuideScope(scopeRegion)
        }
        val availableOptions = options.filter { option ->
            scopedAvailability.any { availableRegion ->
                availableRegion.matchesRegionalGuideEupmyeondong(option)
            }
        }
        if (availableOptions.isNotEmpty()) {
            return availableOptions.distinct().sortedWith(REGION_OPTION_NAME_COMPARATOR)
        }

        return if (scopedAvailability.hasNoEupmyeondongCoverage()) {
            options.distinct().sortedWith(REGION_OPTION_NAME_COMPARATOR)
        } else {
            emptyList()
        }
    }

    fun filterRegions(
        regions: List<Region>,
        availability: List<RegionalGuideAvailabilityDto>,
    ): List<Region> {
        val scopedAvailabilityByRegionScope = regions
            .map { region -> region.toRegionScopeKey() }
            .distinct()
            .associateWith { scope ->
                availability.filter { availableRegion ->
                    availableRegion.matchesRegionalGuideScope(
                        Region(sido = scope.sido, sigungu = scope.sigungu)
                    )
                }
            }

        return regions
            .groupBy { region -> region.toRegionScopeKey() }
            .flatMap { (scope, scopedRegions) ->
                val scopedAvailability = scopedAvailabilityByRegionScope.getValue(scope)
                val availableRegions = scopedRegions.filter { region ->
                    scopedAvailability.any { availableRegion ->
                        availableRegion.matchesRegionalGuideRegion(region)
                    }
                }

                when {
                    availableRegions.isNotEmpty() -> availableRegions
                    scopedAvailability.hasNoEupmyeondongCoverage() -> scopedRegions
                    else -> emptyList()
                }
            }
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGIONAL_GUIDE_REGION_NAME_COMPARATOR)
    }

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideRegion(region: Region): Boolean {
        val eupmyeondong = region.eupmyeondong ?: return false

        return matchesRegionalGuideScope(region) && matchesRegionalGuideEupmyeondong(eupmyeondong)
    }

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideScope(region: Region): Boolean =
        RegionSidoAliasPolicy.isSameSido(
            requestedSido = region.sido,
            requestedSigungu = region.sigungu,
            candidateSido = sidoName,
            candidateSigungu = sigunguName,
        ) && sigunguName.isSameGuideSigunguName(region.sigungu.orEmpty())

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideEupmyeondong(
        eupmyeondong: String,
    ): Boolean =
        RegionalGuideEupmyeondongNamePolicy.matchesManagementZoneOrTargetRegionName(
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
            eupmyeondong = eupmyeondong,
        ) || listOf(managementZoneName, targetRegionName)
            .any { regionName -> regionName.matchesGuideAreaCoverage(eupmyeondong) }

    private fun List<RegionalGuideAvailabilityDto>.hasNoEupmyeondongCoverage(): Boolean =
        isNotEmpty() && none { availableRegion ->
            RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                availableRegion.managementZoneName,
            ) || RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                availableRegion.targetRegionName,
            )
        }

    private fun String.matchesGuideAreaCoverage(eupmyeondong: String): Boolean {
        val normalizedName = replace(WHITESPACE_REGEX, "")
        val normalizedEupmyeondong = eupmyeondong.trim()

        return when (normalizedName) {
            DONG_AREA -> normalizedEupmyeondong.endsWith(DONG_SUFFIX)
            EUP_MYEON_AREA ->
                normalizedEupmyeondong.endsWith(EUP_SUFFIX) || normalizedEupmyeondong.endsWith(MYEON_SUFFIX)

            else -> false
        }
    }

    private fun String.isSameGuideSigunguName(other: String): Boolean =
        this == other || toGuideSigunguCompareKey() == other.toGuideSigunguCompareKey()

    private fun String.toGuideSigunguCompareKey(): String = trim().removeSuffix(CITY_SUFFIX)

    private fun Region.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(sido = sido.orEmpty(), sigungu = sigungu.orEmpty())

    private fun List<Region>.distinctByRegionWithEupmyeondong(): List<Region> =
        distinctBy { region ->
            listOf(region.sido.orEmpty(), region.sigungu.orEmpty(), region.eupmyeondong.orEmpty())
        }

    private data class RegionScopeKey(
        val sido: String,
        val sigungu: String,
    )

    private val REGION_OPTION_NAME_COMPARATOR = RegionNameNaturalComparator
    private val REGIONAL_GUIDE_REGION_NAME_COMPARATOR = RegionCandidateComparator
    private val WHITESPACE_REGEX = Regex("\\s+")

    private const val DONG_AREA = "동지역"
    private const val EUP_MYEON_AREA = "읍면지역"
    private const val EUP_SUFFIX = "읍"
    private const val MYEON_SUFFIX = "면"
    private const val DONG_SUFFIX = "동"
    private const val CITY_SUFFIX = "시"
}
