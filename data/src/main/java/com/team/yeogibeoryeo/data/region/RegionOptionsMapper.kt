package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer

internal object RegionOptionsMapper {

    fun getSidoOptions(regionalGuideRegions: List<RegionalGuideRegionDto>): List<String> =
        regionalGuideRegions
            .map { region -> region.sidoName }
            .filter { sido -> sido.isNotBlank() }
            .distinct()
            .sorted()

    fun getSigunguOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        sido: String,
    ): List<String> =
        regionalGuideRegions
            .asSequence()
            .filter { region -> region.sidoName == sido }
            .map { region -> region.toDisplaySigunguName() }
            .filter { sigungu -> sigungu.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

    fun getEupmyeondongOptions(
        administrativeRegions: List<AdministrativeRegionDto>,
        sido: String,
        sigungu: String,
    ): List<String> =
        administrativeRegions
            .asSequence()
            .filter { region ->
                region.sidoName == sido &&
                    region.toInfoSigunguOptionName().isSameGuideSigunguName(sigungu)
            }
            .map { region -> region.eupmyeondongName }
            .filter { eupmyeondong -> eupmyeondong.isNotBlank() }
            .distinct()
            .sortedWith(REGION_OPTION_NAME_COMPARATOR)
            .toList()

    fun getRegionalGuideEupmyeondongOptions(
        administrativeRegions: List<AdministrativeRegionDto>,
        sido: String,
        sigungu: String,
    ): List<String> =
        administrativeRegions
            .asSequence()
            .filter { region ->
                RegionSidoAliasPolicy.isSameSido(
                    requestedSido = sido,
                    requestedSigungu = sigungu,
                    candidateSido = region.sidoName,
                    candidateSigungu = region.sigunguName,
                ) && region.toInfoSigunguOptionName().isSameGuideSigunguName(sigungu)
            }
            .map { region -> region.eupmyeondongName }
            .filter { eupmyeondong -> eupmyeondong.isNotBlank() }
            .distinct()
            .sortedWith(REGION_OPTION_NAME_COMPARATOR)
            .toList()

    fun getRegionalGuideSigunguOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        sido: String,
    ): List<String> =
        regionalGuideRegions
            .asSequence()
            .filter { region ->
                region.sidoName == sido ||
                    RegionSidoAliasPolicy.isSameSido(
                        requestedSido = sido,
                        requestedSigungu = null,
                        candidateSido = region.sidoName,
                        candidateSigungu = region.sigunguName,
                    )
            }
            .map { region -> region.toDisplaySigunguName() }
            .filter { sigungu -> sigungu.isNotBlank() }
            .distinct()
            .sorted()
            .toList()

    fun normalizeRegionForRegionalGuide(
        region: Region,
        regionalGuideRegions: List<RegionalGuideRegionDto>,
    ): Region {
        val sido = region.sido?.trim()?.takeIf { value -> value.isNotBlank() } ?: return region
        if (sido == SEJONG_SIDO) {
            return region.copy(sido = sido, sigungu = SEJONG_SIDO)
        }

        val sigungu = region.sigungu?.trim()?.takeIf { value -> value.isNotBlank() }
            ?: return region.copy(sido = sido)
        val normalizedSigungu = RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
        val regionalGuideRegion = regionalGuideRegions.firstOrNull { candidate ->
            candidate.sidoName == sido &&
                candidate.sigunguName.isSameGuideSigunguName(normalizedSigungu)
        } ?: return region.copy(sido = sido)

        return region.copy(sido = sido, sigungu = regionalGuideRegion.toDisplaySigunguName())
    }

    private fun RegionalGuideRegionDto.toDisplaySigunguName(): String =
        if (sidoName == SEJONG_SIDO && sigunguName == NO_SIGUNGU_NAME) sidoName else sigunguName

    private fun AdministrativeRegionDto.toInfoSigunguOptionName(): String {
        val sigungu = sigunguName.ifBlank { return sidoName }
        return RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
    }

    private fun String.isSameGuideSigunguName(other: String): Boolean =
        this == other || toGuideSigunguCompareKey() == other.toGuideSigunguCompareKey()

    private fun String.toGuideSigunguCompareKey(): String = trim().removeSuffix(CITY_SUFFIX)

    private val REGION_OPTION_NAME_COMPARATOR = RegionNameNaturalComparator

    private const val SEJONG_SIDO = "세종특별자치시"
    private const val NO_SIGUNGU_NAME = "없음"
    private const val CITY_SUFFIX = "시"
}
