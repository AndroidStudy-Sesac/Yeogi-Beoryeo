package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer

internal object RegionOptionsMapper {

    fun getSidoOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>
    ): List<String> {
        return regionalGuideRegions
            .map { region -> region.sidoName }
            .filter { sido -> sido.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getSigunguOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        sido: String
    ): List<String> {
        return regionalGuideRegions
            .asSequence()
            .filter { region -> region.sidoName == sido }
            .map { region -> region.toDisplaySigunguName() }
            .filter { sigungu -> sigungu.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun getEupmyeondongOptions(
        administrativeRegions: List<AdministrativeRegionDto>,
        sido: String,
        sigungu: String
    ): List<String> {
        return administrativeRegions
            .asSequence()
            .filter { region ->
                region.sidoName == sido &&
                    region.toInfoSigunguOptionName().isSameGuideSigunguName(sigungu)
            }
            .map { region -> region.eupmyeondongName }
            .filter { eupmyeondong -> eupmyeondong.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun findSigunguRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        keyword: String
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val regionalGuideMatches = regionalGuideRegions
            .findByGuideRegionName(targetKeyword)

        if (regionalGuideMatches.isNotEmpty()) {
            return regionalGuideMatches
                .map { region -> region.toRegion() }
                .distinctByRegion()
                .sortedWith(REGION_NAME_COMPARATOR)
        }

        return administrativeRegions
            .findByRegionName(targetKeyword) { region -> region.sigunguName }
            .map { region -> region.toSigunguRegion() }
            .distinctByRegion()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    fun normalizeRegionForRegionalGuide(
        region: Region,
        regionalGuideRegions: List<RegionalGuideRegionDto>
    ): Region {
        val sido = region.sido?.trim()?.takeIf { sido -> sido.isNotBlank() }
            ?: return region

        if (sido == SEJONG_SIDO) {
            return region.copy(
                sido = sido,
                sigungu = SEJONG_SIDO
            )
        }

        val sigungu = region.sigungu
            ?.trim()
            ?.takeIf { sigungu -> sigungu.isNotBlank() }
            ?: return region.copy(sido = sido)

        val normalizedSigungu = RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
        val regionalGuideRegion = regionalGuideRegions.firstOrNull { regionalGuideRegion ->
            regionalGuideRegion.sidoName == sido &&
                regionalGuideRegion.sigunguName.isSameGuideSigunguName(normalizedSigungu)
        } ?: return region.copy(sido = sido)

        return region.copy(
            sido = sido,
            sigungu = regionalGuideRegion.toDisplaySigunguName()
        )
    }

    fun findAdminDongCandidatesForLegalDong(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region
    ): List<Region> {
        val sido = region.sido.trimToNull() ?: return emptyList()
        val sigungu = region.sigungu.trimToNull().orEmpty()
        val legalDongName = region.eupmyeondong.trimToNull() ?: return emptyList()

        return mappings
            .filter { mapping ->
                mapping.sidoName.trim() == sido &&
                    mapping.sigunguName.trim() == sigungu &&
                    mapping.legalDongName.trim() == legalDongName
            }
            .mapNotNull { mapping ->
                val adminDongName = mapping.adminDongName.trimToNull() ?: return@mapNotNull null

                RegionNormalizer.normalize(
                    Region(
                        sido = mapping.sidoName.trim(),
                        sigungu = mapping.sigunguName.trimToNull(),
                        eupmyeondong = adminDongName
                    )
                )
            }
            .distinct()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    private fun RegionalGuideRegionDto.toDisplaySigunguName(): String {
        return if (sidoName == SEJONG_SIDO && sigunguName == NO_SIGUNGU_NAME) {
            sidoName
        } else {
            sigunguName
        }
    }

    private fun AdministrativeRegionDto.toInfoSigunguOptionName(): String {
        val sigungu = sigunguName.ifBlank {
            return sidoName
        }

        return RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
    }

    private fun RegionalGuideRegionDto.toRegion(): Region {
        return RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null }
            )
        )
    }

    private fun AdministrativeRegionDto.toSigunguRegion(): Region {
        return RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
                eupmyeondong = null
            )
        )
    }

    private fun <T> List<T>.findByRegionName(
        targetKeyword: String,
        regionNameSelector: (T) -> String
    ): List<T> {
        val exactMatches = filter { region ->
            regionNameSelector(region) == targetKeyword
        }

        val prefixMatches = exactMatches.ifEmpty {
            filter { region ->
                regionNameSelector(region).startsWith(targetKeyword)
            }
        }

        return prefixMatches.ifEmpty {
            filter { region ->
                regionNameSelector(region).contains(targetKeyword)
            }
        }
    }

    private fun List<RegionalGuideRegionDto>.findByGuideRegionName(
        targetKeyword: String
    ): List<RegionalGuideRegionDto> {
        val exactMatches = filter { region ->
            region.toDisplaySigunguName().isSameGuideSigunguName(targetKeyword)
        }

        val normalizedKeyword = targetKeyword.toGuideSigunguCompareKey()
        val prefixMatches = exactMatches.ifEmpty {
            filter { region ->
                region.toDisplaySigunguName()
                    .toGuideSigunguCompareKey()
                    .startsWith(normalizedKeyword)
            }
        }

        return prefixMatches.ifEmpty {
            filter { region ->
                region.toDisplaySigunguName()
                    .toGuideSigunguCompareKey()
                    .contains(normalizedKeyword)
            }
        }
    }

    private fun String.isSameGuideSigunguName(
        other: String
    ): Boolean {
        return this == other ||
            toGuideSigunguCompareKey() == other.toGuideSigunguCompareKey()
    }

    private fun String.toGuideSigunguCompareKey(): String {
        return trim().removeSuffix(CITY_SUFFIX)
    }

    private fun List<Region>.distinctByRegion(): List<Region> {
        return distinctBy { region ->
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty()
            )
        }
    }

    private fun String?.trimToNull(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private val REGION_NAME_COMPARATOR = compareBy<Region>(
        { region -> region.sido.orEmpty() },
        { region -> region.sigungu.orEmpty() },
        { region -> region.eupmyeondong.orEmpty() }
    )

    private const val SEJONG_SIDO = "세종특별자치시"
    private const val NO_SIGUNGU_NAME = "없음"
    private const val CITY_SUFFIX = "시"
}
