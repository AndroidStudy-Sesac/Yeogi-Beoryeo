package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule

internal fun regionalGuideQuery(
    displayRegion: Region,
    sigunguQuery: String
): RegionalGuideQuery =
    RegionalGuideQuery(
        displayRegion = displayRegion,
        sigunguQuery = sigunguQuery
    )

internal fun regionalDisposalGuide(
    sido: String?,
    sigungu: String?,
    targetRegionName: String?,
    managementZoneName: String? = null,
    eupmyeondong: String? = null,
    schedules: List<RegionalWasteSchedule> = emptyList(),
    disposalPlaceType: String? = null,
    disposalPlaceDescription: String? = null,
    uncollectedDays: String? = null,
    departmentName: String? = null,
    departmentPhoneNumber: String? = null,
): RegionalDisposalGuide =
    RegionalDisposalGuide(
        region = Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong
        ),
        managementZoneName = managementZoneName,
        targetRegionName = targetRegionName,
        disposalPlaceType = disposalPlaceType,
        disposalPlaceDescription = disposalPlaceDescription,
        schedules = schedules,
        uncollectedDays = uncollectedDays,
        departmentName = departmentName,
        departmentPhoneNumber = departmentPhoneNumber,
    )
