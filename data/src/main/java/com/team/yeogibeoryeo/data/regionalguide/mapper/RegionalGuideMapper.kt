package com.team.yeogibeoryeo.data.regionalguide.mapper

import com.team.yeogibeoryeo.data.region.RegionNormalizer
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide

/**
 * Data 계층의 DTO를 Domain 계층의 최상위 모델인 RegionalDisposalGuide로 변환하는 매퍼.
 * API 응답에 포함된 상세 행정동 명칭을 기반으로 기존 Region 객체를 정교화합니다.
 */
object RegionalGuideMapper {
    fun mapToDomain(baseRegion: Region, dto: RegionalGuideItemDto): RegionalDisposalGuide {
        val targetRegionName = dto.dongName?.trim()
        val accurateRegion = RegionNormalizer.normalize(
            baseRegion.copy(
                sido = baseRegion.sido ?: dto.sidoName?.trim(),
                sigungu = baseRegion.sigungu ?: dto.sigunguName
                    ?.trim()
                    ?.takeIf { sigunguName -> sigunguName.isSpecificRegionName() },
                eupmyeondong = targetRegionName
                    ?.takeIf { regionName -> regionName.isSpecificRegionName() }
                    ?: baseRegion.eupmyeondong
            )
        )

        return RegionalDisposalGuide(
            region = accurateRegion,
            managementZoneName = dto.managementZoneName?.trim(),
            targetRegionName = targetRegionName,
            disposalPlaceType = dto.disposalPlaceType?.trim(),
            disposalPlaceDescription = dto.placeDescription?.trim(),
            schedules = RegionalWasteScheduleMapper.mapToSchedules(dto),
            uncollectedDays = RegionalWasteScheduleMapper.parseDays(dto.uncollectedDay),
            departmentName = dto.departmentName?.trim(),
            departmentPhoneNumber = dto.departmentPhoneNumber?.trim()
        )
    }

    private fun String.isSpecificRegionName(): Boolean {
        return isNotBlank() &&
            this !in NON_SPECIFIC_TARGET_REGION_NAMES &&
            !endsWith("전체") &&
            !endsWith("전역") &&
            containsEupmyeondongName()
    }

    private fun String.containsEupmyeondongName(): Boolean {
        return split("+", ",").any { regionName ->
            val trimmedRegionName = regionName.trim()
            trimmedRegionName.endsWith("읍") ||
                trimmedRegionName.endsWith("면") ||
                trimmedRegionName.endsWith("동")
        }
    }

    private val NON_SPECIFIC_TARGET_REGION_NAMES = setOf(
        "없음",
        "전체",
        "전역",
        "관내"
    )
}
