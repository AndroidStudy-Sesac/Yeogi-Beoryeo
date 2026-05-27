package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class GetEupmyeondongOptionsUseCase @Inject constructor(
    private val repository: RegionOptionsRepository
) {
    suspend operator fun invoke(
        sido: String,
        sigungu: String
    ): List<String> {
        return repository.getEupmyeondongOptions(
            sido = sido,
            sigungu = sigungu
        )
    }
}
