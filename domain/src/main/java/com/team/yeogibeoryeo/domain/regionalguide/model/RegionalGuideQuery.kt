package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.Region

data class RegionalGuideQuery(
    val displayRegion: Region,
    val sigunguQuery: String
)
