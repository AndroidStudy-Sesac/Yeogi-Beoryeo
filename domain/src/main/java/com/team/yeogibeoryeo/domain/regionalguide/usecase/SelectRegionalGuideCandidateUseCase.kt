package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionSidoAliasPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideCandidateLookupReason
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideFavoriteCompatibilityPolicy
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer
import javax.inject.Inject

class SelectRegionalGuideCandidateUseCase @Inject constructor() {

    operator fun invoke(
        candidates: List<RegionalDisposalGuide>,
        query: RegionalGuideQuery,
        preferredTargetRegionName: String? = null,
        preferredManagementZoneName: String? = null,
        favoriteKey: RegionalGuideFavoriteKey? = null,
        mappedAdminDongCandidates: List<Region> = emptyList(),
    ): RegionalGuideLookupResult {
        if (candidates.isEmpty()) return RegionalGuideLookupResult.NotFound

        val filteredCandidates = candidates
            .filterBySido(query.displayRegion)
            .filterBySigungu(query)
            .mergeDuplicateCandidateRows()

        if (filteredCandidates.isEmpty()) {
            return RegionalGuideLookupResult.CandidateNotFound
        }

        if (favoriteKey != null) {
            return filteredCandidates
                .filterByFavoriteKey(favoriteKey)
                .toSingleSuccessOrCandidates(
                    displayRegion = query.displayRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES
                )
                ?: RegionalGuideLookupResult.CandidateNotFound
        }

        if (!preferredTargetRegionName.isNullOrBlank() || !preferredManagementZoneName.isNullOrBlank()) {
            return filteredCandidates
                .filterByPreferredCandidate(
                    preferredTargetRegionName = preferredTargetRegionName,
                    preferredManagementZoneName = preferredManagementZoneName
                )
                .toSingleSuccessOrCandidates(
                    displayRegion = query.displayRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES
                )
                ?: RegionalGuideLookupResult.CandidateNotFound
        }

        if (
            filteredCandidates.size == 1 &&
            query.displayRegion.eupmyeondong.isNullOrBlank()
        ) {
            return filteredCandidates.toSingleSuccessOrCandidates(query.displayRegion)
                ?: RegionalGuideLookupResult.CandidateNotFound
        }

        filteredCandidates.selectOverallCandidates(query.sigunguQuery)
            .toSingleSuccessOrCandidates(
                displayRegion = query.displayRegion,
                candidateReason = query.displayRegion.toOverallCandidateReason()
            )
            ?.let { result -> return result }

        selectByTargetRegion(
            candidates = filteredCandidates,
            requestedRegion = query.displayRegion,
            sigunguQuery = query.sigunguQuery,
            mappedAdminDongCandidates = mappedAdminDongCandidates
        )?.let { result -> return result }

        return filteredCandidates.toCandidateResultOrNotFound(query.displayRegion)
    }

    private fun List<RegionalDisposalGuide>.filterBySido(
        requestedRegion: Region
    ): List<RegionalDisposalGuide> {
        if (requestedRegion.sido.isNullOrBlank()) return this

        return filter { guide ->
            RegionSidoAliasPolicy.isSameSido(
                requestedSido = requestedRegion.sido,
                requestedSigungu = requestedRegion.sigungu,
                candidateSido = guide.region.sido,
                candidateSigungu = guide.region.sigungu,
            )
        }
    }

    private fun List<RegionalDisposalGuide>.filterBySigungu(
        query: RegionalGuideQuery
    ): List<RegionalDisposalGuide> {
        val sigunguQuery = query.sigunguQuery
        if (sigunguQuery == SEJONG_SIGUNGU_QUERY) return this

        return filter { guide ->
            guide.region.sigungu == sigunguQuery ||
                guide.matchesDisplayRegionSigungu(query)
        }
    }

    private fun RegionalDisposalGuide.matchesDisplayRegionSigungu(
        query: RegionalGuideQuery
    ): Boolean {
        if (query.displayRegion.eupmyeondong.isNullOrBlank()) return false

        return region.sigungu == query.displayRegion.sigungu &&
            region.sigungu?.let(RegionalGuideRegionKeyNormalizer::normalizeSigungu) == query.sigunguQuery
    }

    private fun selectByTargetRegion(
        candidates: List<RegionalDisposalGuide>,
        requestedRegion: Region,
        sigunguQuery: String,
        mappedAdminDongCandidates: List<Region>
    ): RegionalGuideLookupResult? {
        val eupmyeondong = requestedRegion.eupmyeondong?.trim()

        if (!eupmyeondong.isNullOrBlank()) {
            candidates
                .filterByRequestedEupmyeondong(eupmyeondong)
                .toSingleSuccessOrCandidates(
                    displayRegion = requestedRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES
                )
                ?.let { result -> return result }

            candidates
                .filter { guide ->
                    guide.targetRegionName.isSejongDongArea() &&
                        eupmyeondong in SEJONG_DONG_AREA_NAMES
                }
                .toSingleSuccessOrCandidates(
                    displayRegion = requestedRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES
                )
                ?.let { result -> return result }

            candidates
                .filterByMappedAdminDongs(mappedAdminDongCandidates)
                .toSingleSuccessOrCandidates(
                    displayRegion = requestedRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.MULTIPLE_EXACT_MATCHES
                )
                ?.let { result -> return result }

            return candidates
                .selectOverallCandidates(sigunguQuery)
                .toSingleSuccessOrCandidates(
                    displayRegion = requestedRegion,
                    candidateReason = RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND
                )
                ?: candidates
                    .selectUnmatchedSelectorFallbackCandidates(eupmyeondong)
                    .toSingleSuccessOrCandidates(
                        displayRegion = requestedRegion,
                        candidateReason = RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND
                    )
        }

        return null
    }

    private fun List<RegionalDisposalGuide>.filterByRequestedEupmyeondong(
        eupmyeondong: String
    ): List<RegionalDisposalGuide> {
        val exactMatches = filter { guide ->
            guide.targetRegionName.isExactRegionName(eupmyeondong) ||
                guide.managementZoneName.isExactRegionName(eupmyeondong)
        }

        if (exactMatches.isNotEmpty()) {
            return exactMatches
                .mergeDuplicateCandidateRows()
                .mergeApplicableRowsForSelectedRegion()
        }

        val targetRegionMatches = filter { guide ->
            guide.targetRegionName.matchesEupmyeondong(eupmyeondong)
        }
        val managementZoneMatches = filter { guide ->
            guide.managementZoneName.matchesEupmyeondong(eupmyeondong)
        }

        return (targetRegionMatches + managementZoneMatches)
            .mergeDuplicateCandidateRows()
            .mergeApplicableRowsForSelectedRegion()
    }

    private fun List<RegionalDisposalGuide>.filterByMappedAdminDongs(
        mappedAdminDongCandidates: List<Region>
    ): List<RegionalDisposalGuide> {
        val adminDongNames = mappedAdminDongCandidates
            .flatMap { region -> region.eupmyeondong.toComparableAdminDongNames() }
            .toSet()

        if (adminDongNames.isEmpty()) return emptyList()

        return filter { guide ->
            guide.managementZoneName.matchesExactMappedAdminDong(adminDongNames) ||
                guide.targetRegionName.matchesExactMappedAdminDong(adminDongNames)
        }
            .mergeDuplicateCandidateRows()
    }

    private fun List<RegionalDisposalGuide>.selectOverallCandidates(
        sigunguQuery: String
    ): List<RegionalDisposalGuide> {
        val targetRegionNames = map { guide ->
            guide.targetRegionName
                ?.trim()
                .orEmpty()
        }.distinct()

        return if (
            targetRegionNames.size == 1 &&
            targetRegionNames.single().isOverallTarget(sigunguQuery)
        ) {
            this
        } else {
            emptyList()
        }
    }

    private fun List<RegionalDisposalGuide>.selectUnmatchedSelectorFallbackCandidates(
        eupmyeondong: String
    ): List<RegionalDisposalGuide> {
        val broadCandidates = filter { guide ->
            !guide.hasExplicitEupmyeondongTarget() &&
                guide.matchesRequestedBroadArea(eupmyeondong)
        }

        return broadCandidates
    }

    private fun List<RegionalDisposalGuide>.toCandidateResultOrNotFound(
        displayRegion: Region
    ): RegionalGuideLookupResult {
        if (!displayRegion.eupmyeondong.isNullOrBlank() || size <= 1) {
            return RegionalGuideLookupResult.CandidateNotFound
        }

        return RegionalGuideLookupResult.Candidates(
            guides = map { guide -> guide.withDisplayRegion(displayRegion) },
            reason = RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES
        )
    }

    private fun List<RegionalDisposalGuide>.toCandidateListOrNull(
        displayRegion: Region,
        candidateReason: RegionalGuideCandidateLookupReason
    ): RegionalGuideLookupResult.Candidates? {
        if (isEmpty()) return null

        return RegionalGuideLookupResult.Candidates(
            guides = map { guide -> guide.withDisplayRegion(displayRegion) },
            reason = candidateReason
        )
    }

    private fun List<RegionalDisposalGuide>.toSingleSuccessOrCandidates(
        displayRegion: Region,
        candidateReason: RegionalGuideCandidateLookupReason =
            RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES
    ): RegionalGuideLookupResult? {
        return when (size) {
            0 -> null
            1 -> RegionalGuideLookupResult.Success(
                guide = first().withDisplayRegion(displayRegion)
            )
            else -> RegionalGuideLookupResult.Candidates(
                guides = map { guide -> guide.withDisplayRegion(displayRegion) },
                reason = candidateReason
            )
        }
    }

    private fun Region.toOverallCandidateReason(): RegionalGuideCandidateLookupReason =
        if (eupmyeondong.isNullOrBlank()) {
            RegionalGuideCandidateLookupReason.MULTIPLE_CANDIDATES
        } else {
            RegionalGuideCandidateLookupReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND
        }

    private fun List<RegionalDisposalGuide>.filterByPreferredCandidate(
        preferredTargetRegionName: String?,
        preferredManagementZoneName: String?
    ): List<RegionalDisposalGuide> {
        val targetRegionName = preferredTargetRegionName.normalizeRegionName()
        val managementZoneName = preferredManagementZoneName.normalizeRegionName()

        return filter { guide ->
            val targetMatches = targetRegionName == null ||
                guide.targetRegionName.normalizeRegionName() == targetRegionName
            val managementMatches = managementZoneName == null ||
                guide.managementZoneName.normalizeRegionName() == managementZoneName

            targetMatches && managementMatches
        }
    }

    private fun List<RegionalDisposalGuide>.filterByFavoriteKey(
        favoriteKey: RegionalGuideFavoriteKey
    ): List<RegionalDisposalGuide> =
        filter { guide ->
            RegionalGuideFavoriteCompatibilityPolicy.isSameFavoriteTarget(
                favoriteKey = favoriteKey,
                candidate = guide,
            )
        }

    private fun List<RegionalDisposalGuide>.mergeDuplicateCandidateRows(): List<RegionalDisposalGuide> =
        groupBy { guide -> guide.toCandidateKey() }
            .values
            .map { guides ->
                val firstGuide = guides.first()
                firstGuide.copy(
                    schedules = guides
                        .flatMap { guide -> guide.schedules }
                        .distinct()
                )
            }

    private fun List<RegionalDisposalGuide>.mergeApplicableRowsForSelectedRegion(): List<RegionalDisposalGuide> {
        if (size <= 1) return this

        val schedules = flatMap { guide -> guide.schedules }
        if (schedules.isEmpty()) return this
        if (any { guide -> guide.schedules.isEmpty() }) return this

        val scheduleWasteTypes = schedules.map { schedule -> schedule.wasteType }
        if (scheduleWasteTypes.distinct().size != scheduleWasteTypes.size) return this

        val firstGuide = first()

        return listOf(
            firstGuide.copy(
                managementZoneName = commonValueOrNull { guide -> guide.managementZoneName },
                targetRegionName = commonValueOrNull { guide -> guide.targetRegionName },
                disposalPlaceType = commonValueOrNull { guide -> guide.disposalPlaceType },
                disposalPlaceDescription = commonValueOrNull { guide -> guide.disposalPlaceDescription },
                uncollectedDays = commonValueOrNull { guide -> guide.uncollectedDays },
                departmentName = commonValueOrNull { guide -> guide.departmentName },
                departmentPhoneNumber = commonValueOrNull { guide -> guide.departmentPhoneNumber },
                schedules = schedules.distinct()
            )
        )
    }

    private fun List<RegionalDisposalGuide>.commonValueOrNull(
        selector: (RegionalDisposalGuide) -> String?
    ): String? {
        val values = mapNotNull { guide -> selector(guide).normalizeRegionName() }
            .distinct()

        return values.singleOrNull()
    }

    private fun RegionalDisposalGuide.toCandidateKey(): CandidateKey =
        CandidateKey(
            sido = region.sido.normalizeRegionName(),
            sigungu = region.sigungu.normalizeRegionName(),
            managementZoneName = managementZoneName.normalizeRegionName(),
            targetRegionName = targetRegionName.normalizeRegionName(),
            disposalPlaceType = disposalPlaceType.normalizeRegionName(),
            disposalPlaceDescription = disposalPlaceDescription.normalizeRegionName(),
            uncollectedDays = uncollectedDays.normalizeRegionName(),
            departmentName = departmentName.normalizeRegionName(),
            departmentPhoneNumber = departmentPhoneNumber.normalizeRegionName(),
        )

    private fun String?.matchesEupmyeondong(
        eupmyeondong: String
    ): Boolean {
        val targetRegionName = this?.trim().orEmpty()
        if (targetRegionName.isBlank()) return false

        if (targetRegionName == eupmyeondong || targetRegionName.contains(eupmyeondong)) {
            return true
        }

        val requestedDongNames = eupmyeondong.toComparableAdminDongNames()
        val targetDongNames = targetRegionName.toComparableAdminDongNames()
        if (
            requestedDongNames.isNotEmpty() &&
            targetDongNames.any { dongName -> dongName in requestedDongNames }
        ) {
            return true
        }

        if (targetRegionName.matchesBroadAdministrativeDong(eupmyeondong)) {
            return true
        }

        if (targetRegionName.matchesDongArea(eupmyeondong)) {
            return true
        }

        if (targetRegionName.matchesEupMyeonArea(eupmyeondong)) {
            return true
        }

        val normalizedEupmyeondong = eupmyeondong.removeAdministrativeSuffix()
        val allowSuffixlessTokenMatch = !targetRegionName.hasCondensedAdminDongExpression()

        return targetRegionName
            .split(TARGET_REGION_DELIMITER)
            .map { token -> token.trim() }
            .any { token ->
                token == eupmyeondong ||
                    token.hasAdministrativeSuffix() &&
                    token.removeAdministrativeSuffix() == normalizedEupmyeondong ||
                    allowSuffixlessTokenMatch &&
                    token == normalizedEupmyeondong
            }
    }

    private fun String.matchesBroadAdministrativeDong(
        eupmyeondong: String
    ): Boolean {
        val broadDongName = eupmyeondong.toBroadAdministrativeDongName() ?: return false

        return toManagementZoneTokens().any { token -> token == broadDongName }
    }

    private fun String.matchesDongArea(
        eupmyeondong: String
    ): Boolean {
        val requestedEupmyeondong = eupmyeondong.trim()

        return requestedEupmyeondong.endsWith(DONG) &&
            isDongAreaExpression()
    }

    private fun String.isDongAreaExpression(): Boolean {
        val tokens = splitAreaExpressionTokens()
        if (tokens.joinToString(separator = "") == DONG_AREA) return true
        if (tokens.lastOrNull() == DONG_AREA) return true

        return tokens.takeLast(2).joinToString(separator = "") == DONG_AREA
    }

    private fun String.matchesEupMyeonArea(
        eupmyeondong: String
    ): Boolean {
        val requestedEupmyeondong = eupmyeondong.trim()

        return (requestedEupmyeondong.endsWith(EUP) || requestedEupmyeondong.endsWith(MYEON)) &&
            isEupMyeonAreaExpression()
    }

    private fun String.isEupMyeonAreaExpression(): Boolean {
        val tokens = splitAreaExpressionTokens()
        if (tokens.joinToString(separator = "") == EUP_MYEON_AREA) return true
        if (tokens.lastOrNull() == EUP_MYEON_AREA) return true

        return tokens.takeLast(2).joinToString(separator = "") == EUP_MYEON_AREA
    }

    private fun String.splitAreaExpressionTokens(): List<String> {
        return trim()
            .split(WHITESPACE_REGEX)
            .filter { token -> token.isNotBlank() }
    }

    private fun String?.isExactRegionName(
        regionName: String
    ): Boolean =
        normalizeRegionName() == regionName.normalizeRegionName()

    private fun String?.matchesExactMappedAdminDong(
        adminDongNames: Set<String>
    ): Boolean {
        return toComparableAdminDongNames()
            .any { dongName -> dongName in adminDongNames }
    }

    private fun String?.normalizeRegionName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private fun String?.toComparableAdminDongNames(): Set<String> {
        val value = normalizeRegionName() ?: return emptySet()

        return value.expandAdminDongNames()
    }

    private fun String.expandAdminDongNames(): Set<String> {
        val value = replace(WHITESPACE_REGEX, "")
            .replace(ADMIN_DONG_NUMBER_MARKER_REGEX, "")
            .normalizeRegionName()
            ?: return emptySet()

        value.expandSingleCondensedAdminDongName()?.let { names ->
            return names
        }

        value.expandCompositeAdminDongName()?.let { names ->
            return names
        }

        val condensedMatches = ADMIN_DONG_CONDENSED_REGEXES
            .flatMap { regex -> regex.findAll(value).toList() }
            .sortedBy { match -> match.range.first }

        if (condensedMatches.isNotEmpty()) {
            val condensedNames = condensedMatches
                .flatMap { match -> match.value.expandAdminDongNames() }
            val remainingValue = value.removeCondensedAdminDongExpressions(condensedMatches)
            val remainingNames = remainingValue
                .split(TARGET_REGION_GROUP_DELIMITER)
                .filter { token -> token.isNotBlank() }
                .flatMap { token -> token.expandAdminDongNames() }

            return (condensedNames + remainingNames)
                .toSet()
        }

        val splitTokens = value
            .split(TARGET_REGION_GROUP_DELIMITER)
            .filter { token -> token.isNotBlank() }

        if (splitTokens.size > 1) {
            return splitTokens
                .flatMap { token -> token.expandAdminDongNames() }
                .toSet()
        }

        return setOf(value)
    }

    private fun String.toBroadAdministrativeDongName(): String? {
        val value = replace(WHITESPACE_REGEX, "")
            .replace(ADMIN_DONG_NUMBER_MARKER_REGEX, "")

        val match = ADMIN_DONG_SUBDIVISION_REGEX.matchEntire(value) ?: return null
        val baseName = match.groupValues[1].takeIf { name -> name.isNotBlank() } ?: return null

        return "$baseName$DONG"
    }

    private fun String.expandSingleCondensedAdminDongName(): Set<String>? {
        ADMIN_DONG_RANGE_REGEX.matchEntire(this)?.let { match ->
            val prefix = match.groupValues[1]
            val start = match.groupValues[2].toIntOrNull() ?: return setOf(this)
            val end = match.groupValues[3].toIntOrNull() ?: return setOf(this)
            val suffix = match.groupValues[4]

            if (start > end) return setOf(this)

            return (start..end)
                .map { number -> "$prefix$number$suffix" }
                .toSet()
        }

        ADMIN_DONG_GROUP_REGEX.matchEntire(this)?.let { match ->
            val prefix = match.groupValues[1]
            val numbers = match.groupValues[2]
                .split(GROUPED_NUMBER_DELIMITER)
                .mapNotNull { number -> number.toIntOrNull() }
            val suffix = match.groupValues[3]

            if (numbers.isEmpty()) return setOf(this)

            return numbers
                .map { number -> "$prefix$number$suffix" }
                .toSet()
        }

        return null
    }

    private fun String.expandCompositeAdminDongName(): Set<String>? {
        NUMERIC_DOT_COMPOSITE_DONG_REGEX.matchEntire(this)?.let { match ->
            val prefix = match.groupValues[1]
            val firstNumber = match.groupValues[2].toIntOrNull() ?: return setOf(this)
            val secondNumber = match.groupValues[3].toIntOrNull() ?: return setOf(this)
            val suffix = match.groupValues[4]

            return setOf(
                "$prefix$firstNumber$suffix",
                "$prefix$secondNumber$suffix"
            )
        }

        val joinedCompositeName = toJoinedCompositeDongNameOrNull() ?: return null

        return setOf(joinedCompositeName)
    }

    private fun String.toJoinedCompositeDongNameOrNull(): String? {
        if (none { character -> character in COMPOSITE_DONG_DELIMITERS }) return null
        if (any { character -> character.isDigit() }) return null
        if (!endsWith(DONG)) return null

        val parts = split(COMPOSITE_DONG_DELIMITER_REGEX)
            .filter { part -> part.isNotBlank() }
        if (parts.size <= 1) return null

        return parts.joinToString(separator = "")
    }

    private fun String.removeCondensedAdminDongExpressions(
        matches: List<MatchResult>
    ): String {
        val builder = StringBuilder()
        var cursor = 0

        matches.forEach { match ->
            if (match.range.first < cursor) return@forEach

            builder.append(substring(cursor, match.range.first))
            builder.append(GROUPED_NUMBER_DELIMITER)
            cursor = match.range.last + 1
        }

        builder.append(substring(cursor))

        return builder.toString()
    }

    private fun String.hasCondensedAdminDongExpression(): Boolean {
        val value = replace(WHITESPACE_REGEX, "")
            .replace(ADMIN_DONG_NUMBER_MARKER_REGEX, "")

        if (
            ADMIN_DONG_RANGE_REGEX.matches(value) ||
            ADMIN_DONG_GROUP_REGEX.matches(value)
        ) {
            return true
        }

        return value
            .split(TARGET_REGION_GROUP_DELIMITER)
            .any { token ->
                ADMIN_DONG_RANGE_REGEX.matches(token) ||
                    ADMIN_DONG_GROUP_REGEX.matches(token)
            }
    }

    private fun RegionalDisposalGuide.hasExplicitEupmyeondongTarget(): Boolean =
        managementZoneName.hasExplicitEupmyeondongExpression() ||
            targetRegionName.hasExplicitEupmyeondongExpression()

    private fun RegionalDisposalGuide.matchesRequestedBroadArea(
        eupmyeondong: String
    ): Boolean {
        val areaValues = listOf(managementZoneName, targetRegionName)
        val hasDongArea = areaValues.any { value -> value?.isDongAreaExpression() == true }
        val hasEupMyeonArea = areaValues.any { value -> value?.isEupMyeonAreaExpression() == true }
        val mentionsAreaExpression = areaValues.any { value -> value.mentionsAreaExpression() }

        return when {
            hasDongArea -> eupmyeondong.endsWith(DONG)
            hasEupMyeonArea -> eupmyeondong.endsWith(EUP) || eupmyeondong.endsWith(MYEON)
            mentionsAreaExpression -> false
            else -> true
        }
    }

    private fun String?.mentionsAreaExpression(): Boolean {
        val value = this
            ?.replace(WHITESPACE_REGEX, "")
            ?.takeIf { text -> text.isNotBlank() }
            ?: return false

        return value.contains(DONG_AREA) || value.contains(EUP_MYEON_AREA)
    }

    private fun String?.hasExplicitEupmyeondongExpression(): Boolean {
        val value = normalizeRegionName()
            ?.replace(WHITESPACE_REGEX, "")
            ?.replace(ADMIN_DONG_NUMBER_MARKER_REGEX, "")
            ?: return false

        return value
            .split(TARGET_REGION_GROUP_DELIMITER)
            .any { token ->
                token.hasAdministrativeSuffix() ||
                    ADMIN_DONG_RANGE_REGEX.matches(token) ||
                    ADMIN_DONG_GROUP_REGEX.matches(token)
            }
    }

    private fun String.removeAdministrativeSuffix(): String =
        removeSuffix(EUP)
            .removeSuffix(MYEON)
            .removeSuffix(DONG)

    private fun String.toManagementZoneTokens(): List<String> =
        replace(PARENTHESIZED_DESCRIPTION_REGEX, "")
            .split(TARGET_REGION_GROUP_DELIMITER)
            .mapNotNull { token -> token.toManagementZoneTokenOrNull() }

    private fun String.toManagementZoneTokenOrNull(): String? =
        replace(WHITESPACE_REGEX, "")
            .removeSuffix(OVERALL_WHOLE_AREA)
            .removeSuffix(PARTIAL_AREA)
            .removeSuffix(OVERALL_ALL)
            .takeIf { token -> token.isNotBlank() }

    private fun String.hasAdministrativeSuffix(): Boolean =
        endsWith(EUP) || endsWith(MYEON) || endsWith(DONG)

    private fun RegionalDisposalGuide.withDisplayRegion(
        displayRegion: Region
    ): RegionalDisposalGuide {
        return copy(
            region = Region(
                sido = displayRegion.sido ?: region.sido,
                sigungu = displayRegion.sigungu ?: region.sigungu,
                eupmyeondong = displayRegion.eupmyeondong
            )
        )
    }

    private fun String?.isOverallTarget(
        sigunguQuery: String
    ): Boolean {
        val targetRegionName = this?.trim().orEmpty()

        if (targetRegionName.isBlank()) return true

        return targetRegionName == OVERALL_NONE ||
            targetRegionName == sigunguQuery ||
            targetRegionName.contains(OVERALL_ALL) ||
            targetRegionName.contains(OVERALL_WHOLE_AREA) ||
            targetRegionName.contains(OVERALL_WITHIN_REGION)
    }

    private fun String?.isSejongDongArea(): Boolean =
        this?.trim() == DONG_AREA

    private data class CandidateKey(
        val sido: String?,
        val sigungu: String?,
        val managementZoneName: String?,
        val targetRegionName: String?,
        val disposalPlaceType: String?,
        val disposalPlaceDescription: String?,
        val uncollectedDays: String?,
        val departmentName: String?,
        val departmentPhoneNumber: String?,
    )

    private companion object {
        const val SEJONG_SIGUNGU_QUERY = "없음"
        const val DONG_AREA = "동지역"
        const val EUP_MYEON_AREA = "읍면지역"

        const val OVERALL_NONE = "없음"
        const val OVERALL_ALL = "전체"
        const val OVERALL_WHOLE_AREA = "전역"
        const val PARTIAL_AREA = "일부"
        const val OVERALL_WITHIN_REGION = "관내"
        const val EUP = "읍"
        const val MYEON = "면"
        const val DONG = "동"

        val TARGET_REGION_DELIMITER = Regex("[,+/\\s]+")
        val WHITESPACE_REGEX = Regex("\\s+")
        val ADMIN_DONG_NUMBER_MARKER_REGEX = Regex("제(?=\\d)")
        val ADMIN_DONG_RANGE_REGEX = Regex("^([^\\d,+/~～-]+?)(\\d+)\\s*[~～-]\\s*(\\d+)([읍면동])$")
        val ADMIN_DONG_GROUP_REGEX = Regex("^([^\\d,+/~～-]+?)(\\d+(?:,\\d+)+)([읍면동])$")
        val ADMIN_DONG_SUBDIVISION_REGEX = Regex("^([^\\d,+/~～.·ㆍ-]+?)\\d+(?:[.·ㆍ]\\d+)?동$")
        val NUMERIC_DOT_COMPOSITE_DONG_REGEX = Regex("^([^\\d,+/~～.·ㆍ-]+?)(\\d+)[.·ㆍ](\\d+)([^\\d,+/~～.·ㆍ-]*동)$")
        val ADMIN_DONG_RANGE_EXPRESSION_REGEX = Regex("([^\\d,+/~～-]+?)(\\d+)\\s*[~～-]\\s*(\\d+)([읍면동])")
        val ADMIN_DONG_GROUP_EXPRESSION_REGEX = Regex("([^\\d,+/~～-]+?)(\\d+(?:,\\d+)+)([읍면동])")
        val PARENTHESIZED_DESCRIPTION_REGEX = Regex("\\([^)]*\\)|（[^）]*）")
        val ADMIN_DONG_CONDENSED_REGEXES = listOf(
            ADMIN_DONG_RANGE_EXPRESSION_REGEX,
            ADMIN_DONG_GROUP_EXPRESSION_REGEX,
        )
        const val GROUPED_NUMBER_DELIMITER = ","
        val TARGET_REGION_GROUP_DELIMITER = Regex("[,+/]+")
        val COMPOSITE_DONG_DELIMITERS = setOf('.', '·', 'ㆍ')
        val COMPOSITE_DONG_DELIMITER_REGEX = Regex("[.·ㆍ]+")

        val SEJONG_DONG_AREA_NAMES = setOf(
            "한솔동",
            "새롬동",
            "나성동",
            "도담동",
            "어진동",
            "해밀동",
            "아름동",
            "종촌동",
            "고운동",
            "소담동",
            "반곡동",
            "보람동",
            "대평동",
            "다정동"
        )
    }
}
