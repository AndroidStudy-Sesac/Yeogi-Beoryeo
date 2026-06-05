package com.team.yeogibeoryeo.domain.regionalguide.repository

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery

interface RegionalDisposalGuideRepository {

    suspend fun getRegionalDisposalGuideCandidates(
        query: RegionalGuideQuery
    ): Result<List<RegionalDisposalGuide>>
}
