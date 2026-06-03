package com.team.yeogibeoryeo.domain.regionalguide.model

sealed interface RegionalGuideLookupResult {
    data class Success(
        val guide: RegionalDisposalGuide
    ) : RegionalGuideLookupResult

    data object NotFound : RegionalGuideLookupResult

    data object CandidateNotFound : RegionalGuideLookupResult

    data class Failure(
        val throwable: Throwable? = null
    ) : RegionalGuideLookupResult
}
