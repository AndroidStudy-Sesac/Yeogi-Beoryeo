package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionCandidateComparator
import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
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
            .sortedWith(REGION_OPTION_NAME_COMPARATOR)
            .toList()
    }

    fun getRegionalGuideEupmyeondongOptions(
        administrativeRegions: List<AdministrativeRegionDto>,
        sido: String,
        sigungu: String
    ): List<String> {
        return administrativeRegions
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
    }

    fun getRegionalGuideSigunguOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        sido: String
    ): List<String> {
        return regionalGuideRegions
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
    }

    fun filterRegionalGuideEupmyeondongOptions(
        options: List<String>,
        availability: List<RegionalGuideAvailabilityDto>,
        sido: String,
        sigungu: String,
    ): List<String> {
        val scopeRegion = Region(
            sido = sido,
            sigungu = sigungu,
        )
        val scopedAvailability = availability.filter { availableRegion ->
            availableRegion.matchesRegionalGuideScope(scopeRegion)
        }

        val availableOptions = options.filter { option ->
            scopedAvailability.any { availableRegion ->
                availableRegion.matchesRegionalGuideEupmyeondong(option)
            }
        }
        if (availableOptions.isNotEmpty()) {
            return availableOptions
                .distinct()
                .sortedWith(REGION_OPTION_NAME_COMPARATOR)
        }

        if (scopedAvailability.hasNoEupmyeondongCoverage()) {
            return options
                .distinct()
                .sortedWith(REGION_OPTION_NAME_COMPARATOR)
        }

        return emptyList()
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

    fun findEupmyeondongRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>,
        keyword: String
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val administrativeMatches = administrativeRegions
            .filter { region ->
                region.eupmyeondongName.matchesAdministrativeEupmyeondongKeyword(targetKeyword)
            }
            .map { region ->
                RegionNormalizer.normalize(
                    Region(
                        sido = region.sidoName,
                        sigungu = region.sigunguName.ifBlank { null },
                        eupmyeondong = region.eupmyeondongName
                    )
                )
            }
        val legalMatches = legalAdminDongMappings
            .filter { mapping -> mapping.hasSameSigunguCode() }
            .mapNotNull { mapping ->
                val legalDongName = mapping.legalDongName
                    .trim()
                    .matchedLegalDongNameForKeyword(
                        targetKeyword = targetKeyword,
                        allowSuffixlessDongMatch = administrativeMatches.isEmpty()
                    )
                    ?: return@mapNotNull null

                RegionNormalizer.normalize(
                    Region(
                        sido = mapping.sidoName.trim(),
                        sigungu = mapping.sigunguName.trimToNull(),
                        eupmyeondong = legalDongName
                    )
                )
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    fun findRegionalGuideEupmyeondongRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>,
        keyword: String
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val administrativeMatches = administrativeRegions
            .filter { region ->
                region.eupmyeondongName.matchesAdministrativeEupmyeondongKeyword(targetKeyword) ||
                    region.matchesNumberedDongAliasInSameScope(
                        targetKeyword = targetKeyword,
                        administrativeRegions = administrativeRegions,
                        legalAdminDongMappings = legalAdminDongMappings
                    )
            }
            .map { region ->
                RegionNormalizer.normalize(
                    Region(
                        sido = region.sidoName,
                        sigungu = region.sigunguName.ifBlank { null },
                        eupmyeondong = region.eupmyeondongName
                    )
                )
            }
        val hasNumberedDongAliasMatch = administrativeMatches.any { region ->
            RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                name = region.eupmyeondong,
                keyword = targetKeyword
            )
        }
        val numberedDongAliasMatchedScopes = administrativeMatches
            .filter { region ->
                RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                    name = region.eupmyeondong,
                    keyword = targetKeyword
                )
            }
            .map { region -> region.toRegionScopeKey() }
            .toSet()

        val legalMatches = legalAdminDongMappings
            .filter { mapping -> mapping.hasSameSigunguCode() }
            .mapNotNull { mapping ->
                val legalDongName = mapping.legalDongName
                    .trim()
                    .matchedLegalDongNameForKeyword(
                        targetKeyword = targetKeyword,
                        allowSuffixlessDongMatch = administrativeMatches.isEmpty()
                    )
                    ?: return@mapNotNull null

                if (
                    hasNumberedDongAliasMatch &&
                    mapping.legalDongName.trim() == targetKeyword &&
                    mapping.toRegionScopeKey() in numberedDongAliasMatchedScopes
                ) {
                    return@mapNotNull null
                }

                RegionNormalizer.normalize(
                    Region(
                        sido = mapping.sidoName.trim(),
                        sigungu = mapping.sigunguName.trimToNull(),
                        eupmyeondong = legalDongName
                    )
                )
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGIONAL_GUIDE_REGION_NAME_COMPARATOR)
    }

    fun filterAvailableRegionalGuideRegions(
        regions: List<Region>,
        availability: List<RegionalGuideAvailabilityDto>,
    ): List<Region> {
        val scopedAvailabilityByRegionScope = regions
            .map { region -> region.toRegionScopeKey() }
            .distinct()
            .associateWith { scope ->
                availability.filter { availableRegion ->
                    availableRegion.matchesRegionalGuideScope(
                        Region(
                            sido = scope.sido,
                            sigungu = scope.sigungu,
                        )
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

    private fun AdministrativeRegionDto.matchesNumberedDongAliasInSameScope(
        targetKeyword: String,
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>
    ): Boolean {
        if (!RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(eupmyeondongName, targetKeyword)) {
            return false
        }

        val scopeKey = toRegionScopeKey()
        val hasAdministrativeKeywordMatch = administrativeRegions.any { region ->
            region.toRegionScopeKey() == scopeKey &&
                region.eupmyeondongName.matchesAdministrativeEupmyeondongKeyword(targetKeyword)
        }
        if (hasAdministrativeKeywordMatch) return false

        val adminDongNames = legalAdminDongMappings.exactLegalAdminDongNames(
            targetKeyword = targetKeyword,
            scopeKey = scopeKey
        )
        val numberedAliasAdminDongNames = adminDongNames
            .filter { adminDongName ->
                RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                    name = adminDongName,
                    keyword = targetKeyword
                )
            }

        return adminDongNames.isEmpty() ||
            numberedAliasAdminDongNames.isNotEmpty() &&
            (
                numberedAliasAdminDongNames.size == adminDongNames.size ||
                    numberedAliasAdminDongNames.size == SINGLE_NUMBERED_ALIAS_MATCH_SIZE
                )
    }

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideRegion(region: Region): Boolean {
        val eupmyeondong = region.eupmyeondong ?: return false

        return matchesRegionalGuideScope(region) &&
            matchesRegionalGuideEupmyeondong(eupmyeondong)
    }

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideScope(region: Region): Boolean {
        return RegionSidoAliasPolicy.isSameSido(
            requestedSido = region.sido,
            requestedSigungu = region.sigungu,
            candidateSido = sidoName,
            candidateSigungu = sigunguName,
        ) &&
            sigunguName.isSameGuideSigunguName(region.sigungu.orEmpty())
    }

    private fun RegionalGuideAvailabilityDto.matchesRegionalGuideEupmyeondong(
        eupmyeondong: String
    ): Boolean {
        return RegionalGuideEupmyeondongNamePolicy.matchesManagementZoneOrTargetRegionName(
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
            eupmyeondong = eupmyeondong,
        ) ||
            listOf(managementZoneName, targetRegionName)
                .any { regionName -> regionName.matchesGuideAreaCoverage(eupmyeondong) }
    }

    private fun List<RegionalGuideAvailabilityDto>.hasNoEupmyeondongCoverage(): Boolean =
        isNotEmpty() && none { availableRegion ->
            RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                availableRegion.managementZoneName,
            ) ||
                RegionalGuideEupmyeondongNamePolicy.hasEupmyeondongCoverage(
                    availableRegion.targetRegionName,
                )
        }

    private fun String.matchesGuideAreaCoverage(eupmyeondong: String): Boolean {
        val normalizedName = replace(WHITESPACE_REGEX, "")
        val normalizedEupmyeondong = eupmyeondong.trim()

        return when (normalizedName) {
            DONG_AREA -> normalizedEupmyeondong.endsWith(DONG_SUFFIX)
            EUP_MYEON_AREA ->
                normalizedEupmyeondong.endsWith(EUP_SUFFIX) ||
                    normalizedEupmyeondong.endsWith(MYEON_SUFFIX)

            else -> false
        }
    }

    private fun List<LegalAdminDongMappingDto>.exactLegalAdminDongNames(
        targetKeyword: String,
        scopeKey: RegionScopeKey
    ): List<String> {
        return filter { mapping ->
            mapping.hasSameSigunguCode() &&
                mapping.toRegionScopeKey() == scopeKey &&
                mapping.legalDongName.trim() == targetKeyword
        }
            .mapNotNull { mapping -> mapping.adminDongName.trimToNull() }
            .distinct()
    }

    private fun String.matchesAdministrativeEupmyeondongKeyword(
        keyword: String
    ): Boolean {
        return RegionalGuideEupmyeondongNamePolicy.matchesKeyword(
            eupmyeondongName = this,
            keyword = keyword,
        )
    }

    fun findLegalDongKeywordsByRegion(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region,
        keyword: String
    ): List<String> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val sido = region.sido.trimToNull()
        val sigungu = region.sigungu.trimToNull()

        return mappings
            .asSequence()
            .filter { mapping ->
                (sido == null || mapping.sidoName.trim() == sido) &&
                    (sigungu == null || mapping.sigunguName.trim() == sigungu) &&
                    mapping.legalDongName.trim().matchedLegalDongNameForKeyword(targetKeyword) != null
            }
            .map { mapping -> mapping.legalDongName.trim() }
            .filter { legalDongName -> legalDongName.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
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

    private fun List<Region>.distinctByRegionWithEupmyeondong(): List<Region> {
        return distinctBy { region ->
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty(),
                region.eupmyeondong.orEmpty()
            )
        }
    }

    private fun AdministrativeRegionDto.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(
            sido = sidoName.trim(),
            sigungu = sigunguName.trim()
        )

    private fun LegalAdminDongMappingDto.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(
            sido = sidoName.trim(),
            sigungu = sigunguName.trim()
        )

    private fun Region.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(
            sido = sido.orEmpty(),
            sigungu = sigungu.orEmpty()
        )

    private fun String.matchedLegalDongNameForKeyword(
        targetKeyword: String,
        allowSuffixlessDongMatch: Boolean = true
    ): String? =
        when {
            this == targetKeyword -> this
            allowSuffixlessDongMatch && matchesEupmyeondongKeyword(targetKeyword) -> this
            startsWith(targetKeyword) && LEGAL_DONG_GA_REGEX.matches(this) -> targetKeyword
            else -> null
        }

    private fun String.matchesEupmyeondongKeyword(targetKeyword: String): Boolean {
        if (this == targetKeyword) return true

        return startsWith(targetKeyword) &&
            length > targetKeyword.length &&
            last() in EUPMYEONDONG_SUFFIXES
    }

    private fun LegalAdminDongMappingDto.hasSameSigunguCode(): Boolean {
        val legalSigunguCode = legalCode.trim().sigunguCodePrefixOrNull() ?: return true
        val adminSigunguCode = adminCode.trim().sigunguCodePrefixOrNull() ?: return true

        return legalSigunguCode == adminSigunguCode
    }

    private fun String.sigunguCodePrefixOrNull(): String? =
        takeIf { code -> code.length >= SIGUNGU_CODE_PREFIX_LENGTH }
            ?.take(SIGUNGU_CODE_PREFIX_LENGTH)

    private fun String?.trimToNull(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private val REGION_NAME_COMPARATOR = compareBy<Region>(
        { region -> region.sido.orEmpty() },
        { region -> region.sigungu.orEmpty() },
        { region -> region.eupmyeondong.orEmpty() },
    )

    private val REGIONAL_GUIDE_REGION_NAME_COMPARATOR = RegionCandidateComparator

    private val REGION_OPTION_NAME_COMPARATOR = RegionNameNaturalComparator

    private val WHITESPACE_REGEX = Regex("\\s+")

    private const val DONG_AREA = "동지역"
    private const val EUP_MYEON_AREA = "읍면지역"
    private const val EUP_SUFFIX = "읍"
    private const val MYEON_SUFFIX = "면"
    private const val DONG_SUFFIX = "동"

    private data class RegionScopeKey(
        val sido: String,
        val sigungu: String
    )

    private const val SEJONG_SIDO = "세종특별자치시"
    private const val NO_SIGUNGU_NAME = "없음"
    private const val CITY_SUFFIX = "시"
    private const val SINGLE_NUMBERED_ALIAS_MATCH_SIZE = 1
    private const val SIGUNGU_CODE_PREFIX_LENGTH = 5
    private val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')
    private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()
}
