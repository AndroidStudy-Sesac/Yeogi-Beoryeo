package com.team.yeogibeoryeo.domain.spot.model

sealed interface RecentCurrentLocationSpotCacheClearResult {
    data object Deleted : RecentCurrentLocationSpotCacheClearResult

    data object NoCache : RecentCurrentLocationSpotCacheClearResult

    data object Failed : RecentCurrentLocationSpotCacheClearResult
}
