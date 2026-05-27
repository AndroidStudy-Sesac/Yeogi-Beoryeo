package com.team.yeogibeoryeo.domain.region.model

data class RegionSearchContext(
    val originalKeyword: String,
    val detectedRegion: Region?,
    val source: RegionSource
)
