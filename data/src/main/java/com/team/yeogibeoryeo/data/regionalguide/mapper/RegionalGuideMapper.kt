package com.team.yeogibeoryeo.data.regionalguide.mapper

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide

/**
 * Data 계층의 DTO를 Domain 계층의 최상위 모델인 RegionalDisposalGuide로 변환하는 매퍼.
 * API 응답에 포함된 상세 행정동 명칭을 기반으로 기존 Region 객체를 정교화합니다.
 */
object RegionalGuideMapper {
    fun mapToDomain(baseRegion: Region, dto: RegionalGuideItemDto): RegionalDisposalGuide {
        val accurateRegion = baseRegion.copy(
            eupmyeondong = dto.dongName ?: baseRegion.eupmyeondong
        )

        return RegionalDisposalGuide(
            region = accurateRegion,
            schedules = RegionalWasteScheduleMapper.mapToSchedules(dto),
            uncollectedDays = dto.uncollectedDay,
            disposalPlaceType = dto.disposalPlaceType
        )
    }
}