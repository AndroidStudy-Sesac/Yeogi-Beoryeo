package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

/**
 * 주소 문자열에서 행정구역을 추출하는 UseCase
 * [동기 방식] 로컬 파싱 로직을 사용하여 즉시 결과를 반환합니다.
 */
class ExtractRegionFromAddressUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    operator fun invoke(address: String): Region? {
        return repository.extractRegionFromAddress(address)
    }
}