package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionCandidateComparator
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class RegionSearchIndex private constructor(
    private val administrativeRegions: List<AdministrativeRegionEntry>,
    private val legalAdminDongMappings: List<LegalAdminDongMappingEntry>,
    private val regionalGuideRegions: List<RegionalGuideRegionEntry>,
    private val administrativeRegionsByScope: Map<RegionScopeKey, List<AdministrativeRegionEntry>>,
    private val legalAdminDongNamesByScopeAndName: Map<LegalDongScopeKey, List<String>>,
) {

    suspend fun findSigunguRegions(keyword: String): List<Region> {
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

    suspend fun findEupmyeondongRegions(keyword: String): List<Region> {
        val targetKeyword = keyword.trim()
        val normalizedKeyword = RegionalGuideEupmyeondongNamePolicy.normalizeForSearch(targetKeyword)
            ?: return emptyList()
        val administrativeMatches = administrativeRegions
            .filterCancellable { region -> region.matchesEupmyeondongKeyword(normalizedKeyword) }
            .map { region -> region.toEupmyeondongRegion() }
        val legalMatches = legalAdminDongMappings
            .filterCancellable { mapping -> mapping.hasSameSigunguCode }
            .mapNotNullCancellable { mapping ->
                val legalDongName = mapping.legalDongName.matchedLegalDongNameForKeyword(
                    targetKeyword = targetKeyword,
                    allowSuffixlessDongMatch = administrativeMatches.isEmpty(),
                ) ?: return@mapNotNullCancellable null

                mapping.toLegalDongRegion(legalDongName)
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    suspend fun findRegionalGuideEupmyeondongRegions(keyword: String): List<Region> {
        val targetKeyword = keyword.trim()
        val normalizedKeyword = RegionalGuideEupmyeondongNamePolicy.normalizeForSearch(targetKeyword)
            ?: return emptyList()
        val administrativeMatches = findRegionalGuideAdministrativeMatches(
            targetKeyword = targetKeyword,
            normalizedKeyword = normalizedKeyword,
        )
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
            .filterCancellable { mapping -> mapping.hasSameSigunguCode }
            .mapNotNullCancellable { mapping ->
                val legalDongName = mapping.legalDongName.matchedLegalDongNameForKeyword(
                    targetKeyword = targetKeyword,
                    allowSuffixlessDongMatch = administrativeMatches.isEmpty(),
                ) ?: return@mapNotNullCancellable null

                if (
                    numberedDongAliasMatchedScopes.isNotEmpty() &&
                    mapping.legalDongName == targetKeyword &&
                    mapping.scopeKey in numberedDongAliasMatchedScopes
                ) {
                    return@mapNotNullCancellable null
                }

                mapping.toLegalDongRegion(legalDongName)
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGIONAL_GUIDE_REGION_NAME_COMPARATOR)
    }

    suspend fun findLegalDongKeywordsByRegion(
        region: Region,
        keyword: String,
    ): List<String> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val sido = region.sido.trimToNull()
        val sigungu = region.sigungu.trimToNull()

        return legalAdminDongMappings
            .filterCancellable { mapping ->
                (sido == null || mapping.sidoName == sido) &&
                    (sigungu == null || mapping.sigunguName == sigungu) &&
                    mapping.legalDongName.matchedLegalDongNameForKeyword(targetKeyword) != null
            }
            .map { mapping -> mapping.legalDongName }
            .filter { legalDongName -> legalDongName.isNotBlank() }
            .distinct()
            .sorted()
    }

    suspend fun findAdminDongCandidatesForLegalDong(region: Region): List<Region> {
        val sido = region.sido.trimToNull() ?: return emptyList()
        val sigungu = region.sigungu.trimToNull().orEmpty()
        val legalDongName = region.eupmyeondong.trimToNull() ?: return emptyList()

        return legalAdminDongMappings
            .filterCancellable { mapping ->
                mapping.sidoName == sido &&
                    mapping.sigunguName == sigungu &&
                    mapping.legalDongName == legalDongName
            }
            .mapNotNull { mapping ->
                mapping.adminDongName.trimToNull()?.let { adminDongName ->
                    mapping.toLegalDongRegion(adminDongName)
                }
            }
            .distinct()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    private suspend fun AdministrativeRegionEntry.matchesNumberedDongAliasInSameScope(
        targetKeyword: String,
        normalizedKeyword: String,
    ): Boolean {
        if (!RegionalGuideEupmyeondongNamePolicy.isNumberedDongAliasOf(eupmyeondongName, targetKeyword)) {
            return false
        }

        val hasAdministrativeKeywordMatch = administrativeRegionsByScope[scopeKey]
            .orEmpty()
            .anyCancellable { region -> region.matchesEupmyeondongKeyword(normalizedKeyword) }
        if (hasAdministrativeKeywordMatch) return false

        val adminDongNames = legalAdminDongNamesByScopeAndName[
            LegalDongScopeKey(scopeKey = scopeKey, legalDongName = targetKeyword)
        ].orEmpty()
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

    private suspend fun findRegionalGuideAdministrativeMatches(
        targetKeyword: String,
        normalizedKeyword: String,
    ): List<AdministrativeRegionEntry> {
        val results = mutableListOf<AdministrativeRegionEntry>()
        administrativeRegions.forEachIndexed { index, region ->
            index.ensureCoroutineActive()
            if (
                region.matchesEupmyeondongKeyword(normalizedKeyword) ||
                region.matchesNumberedDongAliasInSameScope(
                    targetKeyword = targetKeyword,
                    normalizedKeyword = normalizedKeyword,
                )
            ) {
                results += region
            }
        }
        return results
    }

    private suspend fun List<RegionalGuideRegionEntry>.findByGuideRegionName(
        targetKeyword: String,
    ): List<RegionalGuideRegionEntry> {
        val normalizedKeyword = targetKeyword.toGuideSigunguCompareKey()
        val exactMatches = filterCancellable { region ->
            region.displaySigunguName == targetKeyword ||
                region.guideSigunguCompareKey == normalizedKeyword
        }
        val prefixMatches = exactMatches.ifEmpty {
            filterCancellable { region -> region.guideSigunguCompareKey.startsWith(normalizedKeyword) }
        }

        return prefixMatches.ifEmpty {
            filterCancellable { region -> region.guideSigunguCompareKey.contains(normalizedKeyword) }
        }
    }

    private suspend fun <T> List<T>.findByRegionName(
        targetKeyword: String,
        regionNameSelector: (T) -> String,
    ): List<T> {
        val exactMatches = filterCancellable { region -> regionNameSelector(region) == targetKeyword }
        val prefixMatches = exactMatches.ifEmpty {
            filterCancellable { region -> regionNameSelector(region).startsWith(targetKeyword) }
        }

        return prefixMatches.ifEmpty {
            filterCancellable { region -> regionNameSelector(region).contains(targetKeyword) }
        }
    }

    private fun AdministrativeRegionEntry.matchesEupmyeondongKeyword(normalizedKeyword: String): Boolean =
        comparableEupmyeondongNames.any { candidateName ->
            candidateName == normalizedKeyword ||
                candidateName.startsWith(normalizedKeyword) &&
                candidateName.lastOrNull() in EUPMYEONDONG_SUFFIXES
        }

    private fun AdministrativeRegionEntry.toEupmyeondongRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
                eupmyeondong = eupmyeondongName,
            )
        )

    private fun LegalAdminDongMappingEntry.toLegalDongRegion(eupmyeondong: String): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
                eupmyeondong = eupmyeondong,
            )
        )

    private fun RegionalGuideRegionEntry.toRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
            )
        )

    private fun AdministrativeRegionEntry.toSigunguRegion(): Region =
        RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
            )
        )

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

    private fun List<Region>.distinctByRegion(): List<Region> =
        distinctBy { region -> listOf(region.sido.orEmpty(), region.sigungu.orEmpty()) }

    private fun List<Region>.distinctByRegionWithEupmyeondong(): List<Region> =
        distinctBy { region ->
            listOf(region.sido.orEmpty(), region.sigungu.orEmpty(), region.eupmyeondong.orEmpty())
        }

    private fun Region.toRegionScopeKey(): RegionScopeKey =
        RegionScopeKey(sido = sido.orEmpty(), sigungu = sigungu.orEmpty())

    private fun String.toGuideSigunguCompareKey(): String = trim().removeSuffix(CITY_SUFFIX)

    private fun String?.trimToNull(): String? = this?.trim()?.takeIf(String::isNotBlank)

    private suspend fun <T> Iterable<T>.filterCancellable(
        predicate: (T) -> Boolean,
    ): List<T> {
        val results = mutableListOf<T>()
        forEachIndexed { index, item ->
            index.ensureCoroutineActive()
            if (predicate(item)) results += item
        }
        return results
    }

    private suspend fun <T, R : Any> Iterable<T>.mapNotNullCancellable(
        transform: (T) -> R?,
    ): List<R> {
        val results = mutableListOf<R>()
        forEachIndexed { index, item ->
            index.ensureCoroutineActive()
            transform(item)?.let(results::add)
        }
        return results
    }

    private suspend fun <T> Iterable<T>.anyCancellable(predicate: (T) -> Boolean): Boolean {
        forEachIndexed { index, item ->
            index.ensureCoroutineActive()
            if (predicate(item)) return true
        }
        return false
    }

    private suspend fun Int.ensureCoroutineActive() {
        if (this % CANCELLATION_CHECK_INTERVAL == 0) {
            currentCoroutineContext().ensureActive()
        }
    }

    private data class AdministrativeRegionEntry(
        val sidoName: String,
        val sigunguName: String,
        val eupmyeondongName: String,
        val comparableEupmyeondongNames: Set<String>,
        val scopeKey: RegionScopeKey,
    )

    private data class LegalAdminDongMappingEntry(
        val legalDongName: String,
        val sidoName: String,
        val sigunguName: String,
        val adminDongName: String,
        val hasSameSigunguCode: Boolean,
        val scopeKey: RegionScopeKey,
    )

    private data class RegionalGuideRegionEntry(
        val sidoName: String,
        val sigunguName: String,
        val displaySigunguName: String,
        val guideSigunguCompareKey: String,
    )

    private data class RegionScopeKey(
        val sido: String,
        val sigungu: String,
    )

    private data class LegalDongScopeKey(
        val scopeKey: RegionScopeKey,
        val legalDongName: String,
    )

    companion object {
        val REGION_NAME_COMPARATOR = compareBy<Region>(
            { region -> region.sido.orEmpty() },
            { region -> region.sigungu.orEmpty() },
            { region -> region.eupmyeondong.orEmpty() },
        )
        val REGIONAL_GUIDE_REGION_NAME_COMPARATOR = RegionCandidateComparator
        val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()

        const val CANCELLATION_CHECK_INTERVAL = 64
        const val SEJONG_SIDO = "세종특별자치시"
        const val NO_SIGUNGU_NAME = "없음"
        const val CITY_SUFFIX = "시"
        const val SINGLE_NUMBERED_ALIAS_MATCH_SIZE = 1
        const val SIGUNGU_CODE_PREFIX_LENGTH = 5
        val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')

        suspend fun create(
            administrativeRegions: List<AdministrativeRegionDto>,
            legalAdminDongMappings: List<LegalAdminDongMappingDto>,
            regionalGuideRegions: List<RegionalGuideRegionDto>,
        ): RegionSearchIndex {
            val administrativeEntries = administrativeRegions.mapCancellable { region ->
                    AdministrativeRegionEntry(
                        sidoName = region.sidoName,
                        sigunguName = region.sigunguName,
                        eupmyeondongName = region.eupmyeondongName,
                        comparableEupmyeondongNames =
                            RegionalGuideEupmyeondongNamePolicy.comparableNames(
                                region.eupmyeondongName,
                            ),
                        scopeKey = RegionScopeKey(
                            sido = region.sidoName.trim(),
                            sigungu = region.sigunguName.trim(),
                        ),
                    )
                }
            val legalAdminDongMappingEntries = legalAdminDongMappings.mapCancellable { mapping ->
                    LegalAdminDongMappingEntry(
                        legalDongName = mapping.legalDongName.trim(),
                        sidoName = mapping.sidoName.trim(),
                        sigunguName = mapping.sigunguName.trim(),
                        adminDongName = mapping.adminDongName,
                        hasSameSigunguCode = mapping.hasSameSigunguCode(),
                        scopeKey = RegionScopeKey(
                            sido = mapping.sidoName.trim(),
                            sigungu = mapping.sigunguName.trim(),
                        ),
                    )
                }
            val regionalGuideRegionEntries = regionalGuideRegions.mapCancellable { region ->
                    val displaySigunguName =
                        if (region.sidoName == SEJONG_SIDO && region.sigunguName == NO_SIGUNGU_NAME) {
                            region.sidoName
                        } else {
                            region.sigunguName
                        }
                    RegionalGuideRegionEntry(
                        sidoName = region.sidoName,
                        sigunguName = region.sigunguName,
                        displaySigunguName = displaySigunguName,
                        guideSigunguCompareKey = displaySigunguName.trim().removeSuffix(CITY_SUFFIX),
                    )
                }

            return RegionSearchIndex(
                administrativeRegions = administrativeEntries,
                legalAdminDongMappings = legalAdminDongMappingEntries,
                regionalGuideRegions = regionalGuideRegionEntries,
                administrativeRegionsByScope = administrativeEntries.groupBy { entry -> entry.scopeKey },
                legalAdminDongNamesByScopeAndName = legalAdminDongMappingEntries
                    .asSequence()
                    .filter { entry -> entry.hasSameSigunguCode }
                    .groupBy { entry ->
                        LegalDongScopeKey(
                            scopeKey = entry.scopeKey,
                            legalDongName = entry.legalDongName,
                        )
                    }
                    .mapValues { (_, entries) ->
                        entries
                            .map { entry -> entry.adminDongName.trim() }
                            .filter(String::isNotBlank)
                            .distinct()
                    },
            )
        }

        private suspend fun <T, R> Iterable<T>.mapCancellable(
            transform: (T) -> R,
        ): List<R> {
            val results = mutableListOf<R>()
            forEachIndexed { index, item ->
                if (index % CANCELLATION_CHECK_INTERVAL == 0) {
                    currentCoroutineContext().ensureActive()
                }
                results += transform(item)
            }
            return results
        }

        private fun LegalAdminDongMappingDto.hasSameSigunguCode(): Boolean {
            val legalSigunguCode = legalCode.trim().sigunguCodePrefixOrNull() ?: return true
            val adminSigunguCode = adminCode.trim().sigunguCodePrefixOrNull() ?: return true

            return legalSigunguCode == adminSigunguCode
        }

        private fun String.sigunguCodePrefixOrNull(): String? =
            takeIf { code -> code.length >= SIGUNGU_CODE_PREFIX_LENGTH }
                ?.take(SIGUNGU_CODE_PREFIX_LENGTH)
    }
}
