package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionCandidateComparator
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy

internal object RegionSearchCandidateMapper {

    fun findSigunguRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        keyword: String,
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val regionalGuideMatches = regionalGuideRegions.findByGuideRegionName(targetKeyword)
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
        keyword: String,
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val administrativeMatches = administrativeRegions
            .filter { region ->
                region.eupmyeondongName.matchesAdministrativeEupmyeondongKeyword(targetKeyword)
            }
            .map { region -> region.toEupmyeondongRegion() }
        val legalMatches = legalAdminDongMappings
            .filter { mapping -> mapping.hasSameSigunguCode() }
            .mapNotNull { mapping ->
                val legalDongName = mapping.legalDongName
                    .trim()
                    .matchedLegalDongNameForKeyword(
                        targetKeyword = targetKeyword,
                        allowSuffixlessDongMatch = administrativeMatches.isEmpty(),
                    )
                    ?: return@mapNotNull null

                mapping.toLegalDongRegion(legalDongName)
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    fun findRegionalGuideEupmyeondongRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>,
        keyword: String,
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val administrativeMatches = administrativeRegions
            .filter { region ->
                region.eupmyeondongName.matchesAdministrativeEupmyeondongKeyword(targetKeyword) ||
                    region.matchesNumberedDongAliasInSameScope(
                        targetKeyword = targetKeyword,
                        administrativeRegions = administrativeRegions,
                        legalAdminDongMappings = legalAdminDongMappings,
                    )
            }
            .map { region -> region.toEupmyeondongRegion() }
        val numberedDongAliasMatchedScopes = administrativeMatches
            .filter { region ->
                RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                    name = region.eupmyeondong,
                    keyword = targetKeyword,
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
                        allowSuffixlessDongMatch = administrativeMatches.isEmpty(),
                    )
                    ?: return@mapNotNull null

                if (
                    numberedDongAliasMatchedScopes.isNotEmpty() &&
                    mapping.legalDongName.trim() == targetKeyword &&
                    mapping.toRegionScopeKey() in numberedDongAliasMatchedScopes
                ) {
                    return@mapNotNull null
                }

                mapping.toLegalDongRegion(legalDongName)
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGIONAL_GUIDE_REGION_NAME_COMPARATOR)
    }

    fun findLegalDongKeywordsByRegion(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region,
        keyword: String,
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

    fun findAdminDongCandidatesForLegalDong(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region,
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
                mapping.toLegalDongRegion(adminDongName)
            }
            .distinct()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    private fun AdministrativeRegionDto.matchesNumberedDongAliasInSameScope(
        targetKeyword: String,
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>,
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
            scopeKey = scopeKey,
        )
        val numberedAliasAdminDongNames = adminDongNames.filter { adminDongName ->
            RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(
                name = adminDongName,
                keyword = targetKeyword,
            )
        }

        return adminDongNames.isEmpty() ||
            numberedAliasAdminDongNames.isNotEmpty() &&
            (
                numberedAliasAdminDongNames.size == adminDongNames.size ||
                    numberedAliasAdminDongNames.size == SINGLE_NUMBERED_ALIAS_MATCH_SIZE
                )
    }

    private fun List<LegalAdminDongMappingDto>.exactLegalAdminDongNames(
        targetKeyword: String,
        scopeKey: RegionScopeKey,
    ): List<String> =
        filter { mapping ->
            mapping.hasSameSigunguCode() &&
                mapping.toRegionScopeKey() == scopeKey &&
                mapping.legalDongName.trim() == targetKeyword
        }
            .mapNotNull { mapping -> mapping.adminDongName.trimToNull() }
            .distinct()

    private fun String.matchesAdministrativeEupmyeondongKeyword(keyword: String): Boolean =
        RegionalGuideEupmyeondongNamePolicy.matchesKeyword(
            eupmyeondongName = this,
            keyword = keyword,
        )

    private fun AdministrativeRegionDto.toEupmyeondongRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
                eupmyeondong = eupmyeondongName,
            )
        )

    private fun LegalAdminDongMappingDto.toLegalDongRegion(eupmyeondong: String): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName.trim(),
                sigungu = sigunguName.trimToNull(),
                eupmyeondong = eupmyeondong,
            )
        )

    private fun RegionalGuideRegionDto.toRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
            )
        )

    private fun AdministrativeRegionDto.toSigunguRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
            )
        )

    private fun <T> List<T>.findByRegionName(
        targetKeyword: String,
        regionNameSelector: (T) -> String,
    ): List<T> {
        val exactMatches = filter { region -> regionNameSelector(region) == targetKeyword }
        val prefixMatches = exactMatches.ifEmpty {
            filter { region -> regionNameSelector(region).startsWith(targetKeyword) }
        }

        return prefixMatches.ifEmpty {
            filter { region -> regionNameSelector(region).contains(targetKeyword) }
        }
    }

    private fun List<RegionalGuideRegionDto>.findByGuideRegionName(
        targetKeyword: String,
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

    private fun RegionalGuideRegionDto.toDisplaySigunguName(): String =
        if (sidoName == SEJONG_SIDO && sigunguName == NO_SIGUNGU_NAME) sidoName else sigunguName

    private fun String.isSameGuideSigunguName(other: String): Boolean =
        this == other || toGuideSigunguCompareKey() == other.toGuideSigunguCompareKey()

    private fun String.toGuideSigunguCompareKey(): String = trim().removeSuffix(CITY_SUFFIX)

    private fun List<Region>.distinctByRegion(): List<Region> =
        distinctBy { region -> listOf(region.sido.orEmpty(), region.sigungu.orEmpty()) }

    private fun List<Region>.distinctByRegionWithEupmyeondong(): List<Region> =
        distinctBy { region ->
            listOf(region.sido.orEmpty(), region.sigungu.orEmpty(), region.eupmyeondong.orEmpty())
        }

    private fun AdministrativeRegionDto.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(sido = sidoName.trim(), sigungu = sigunguName.trim())

    private fun LegalAdminDongMappingDto.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(sido = sidoName.trim(), sigungu = sigunguName.trim())

    private fun Region.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(sido = sido.orEmpty(), sigungu = sigungu.orEmpty())

    private fun String.matchedLegalDongNameForKeyword(
        targetKeyword: String,
        allowSuffixlessDongMatch: Boolean = true,
    ): String? =
        when {
            this == targetKeyword -> this
            allowSuffixlessDongMatch && matchesEupmyeondongKeyword(targetKeyword) -> this
            startsWith(targetKeyword) && LEGAL_DONG_GA_REGEX.matches(this) -> targetKeyword
            else -> null
        }

    private fun String.matchesEupmyeondongKeyword(targetKeyword: String): Boolean =
        this == targetKeyword ||
            startsWith(targetKeyword) &&
            length > targetKeyword.length &&
            last() in EUPMYEONDONG_SUFFIXES

    private fun LegalAdminDongMappingDto.hasSameSigunguCode(): Boolean {
        val legalSigunguCode = legalCode.trim().sigunguCodePrefixOrNull() ?: return true
        val adminSigunguCode = adminCode.trim().sigunguCodePrefixOrNull() ?: return true

        return legalSigunguCode == adminSigunguCode
    }

    private fun String.sigunguCodePrefixOrNull(): String? =
        takeIf { code -> code.length >= SIGUNGU_CODE_PREFIX_LENGTH }
            ?.take(SIGUNGU_CODE_PREFIX_LENGTH)

    private fun String?.trimToNull(): String? = this?.trim()?.takeIf { value -> value.isNotBlank() }

    private data class RegionScopeKey(
        val sido: String,
        val sigungu: String,
    )

    private val REGION_NAME_COMPARATOR = compareBy<Region>(
        { region -> region.sido.orEmpty() },
        { region -> region.sigungu.orEmpty() },
        { region -> region.eupmyeondong.orEmpty() },
    )
    private val REGIONAL_GUIDE_REGION_NAME_COMPARATOR = RegionCandidateComparator

    private const val SEJONG_SIDO = "세종특별자치시"
    private const val NO_SIGUNGU_NAME = "없음"
    private const val CITY_SUFFIX = "시"
    private const val SINGLE_NUMBERED_ALIAS_MATCH_SIZE = 1
    private const val SIGUNGU_CODE_PREFIX_LENGTH = 5
    private val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')
    private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()
}
