package com.team.yeogibeoryeo.domain.spot.model

import com.team.yeogibeoryeo.domain.region.model.Region

data class MapRegionSearchCandidate(
    val region: Region,
    val searchKeyword: String,
    val searchKeywords: List<String> = listOf(searchKeyword),
) {
    val displayName: String =
        listOfNotNull(
            region.sido,
            region.sigungu,
            region.eupmyeondong,
        ).joinToString(separator = " ")
}

sealed interface MapRegionSearchCandidateResult {
    data class ReadyToSearch(
        val searchKeyword: String,
        val selectedCandidate: MapRegionSearchCandidate? = null,
    ) : MapRegionSearchCandidateResult

    data class NeedSelection(
        val originalKeyword: String,
        val searchKeyword: String,
        val candidates: List<MapRegionSearchCandidate>,
    ) : MapRegionSearchCandidateResult
}
