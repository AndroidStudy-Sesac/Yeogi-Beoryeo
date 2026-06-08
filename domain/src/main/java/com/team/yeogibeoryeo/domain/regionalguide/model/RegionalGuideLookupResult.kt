package com.team.yeogibeoryeo.domain.regionalguide.model

sealed interface RegionalGuideLookupResult {
    data class Success(
        val guide: RegionalDisposalGuide
    ) : RegionalGuideLookupResult

    data class Candidates(
        val guides: List<RegionalDisposalGuide>
    ) : RegionalGuideLookupResult

    data object NotFound : RegionalGuideLookupResult

    data object CandidateNotFound : RegionalGuideLookupResult

    data class Failure(
        val reason: RegionalGuideFailureReason = RegionalGuideFailureReason.UNKNOWN,
        val throwable: Throwable? = null
    ) : RegionalGuideLookupResult
}

enum class RegionalGuideFailureReason {
    NETWORK,
    API,
    UNKNOWN
}

class RegionalGuideLookupException(
    val reason: RegionalGuideFailureReason,
    cause: Throwable? = null
) : RuntimeException(cause)
