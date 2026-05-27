package com.team.yeogibeoryeo.domain.regionalguide.repository

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide

interface RegionalDisposalGuideRepository {

    suspend fun getRegionalDisposalGuide(region: Region): RegionalDisposalGuide?
}
