package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.Region

data class RegionalDisposalGuide(
    val region: Region,
    val managementZoneName: String? = null,
    val targetRegionName: String? = null,
    val disposalPlaceType: String? = null,
    val disposalPlaceDescription: String? = null,
    val schedules: List<RegionalWasteSchedule>,
    val uncollectedDays: String? = null,
    val departmentName: String? = null,
    val departmentPhoneNumber: String? = null,
)
