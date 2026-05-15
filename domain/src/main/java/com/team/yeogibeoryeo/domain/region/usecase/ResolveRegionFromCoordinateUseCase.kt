package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

/**
 * 위도/경도 좌표를 기반으로 행정구역을 변환하는 UseCase (Reverse Geocoding)
 * [비동기 방식] 외부 Geocoder API와의 네트워크 통신을 거쳐 결과를 반환합니다.
 */
class ResolveRegionFromCoordinateUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Region? {
        return repository.resolveRegionFromCoordinate(latitude, longitude)
    }
}