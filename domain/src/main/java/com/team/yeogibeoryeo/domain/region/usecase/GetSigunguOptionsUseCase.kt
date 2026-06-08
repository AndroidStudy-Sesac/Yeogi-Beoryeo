package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class GetSigunguOptionsUseCase @Inject constructor(
    private val repository: RegionOptionsRepository
) {
    suspend operator fun invoke(sido: String): List<String> {
        return repository.getSigunguOptions(sido)
    }
}
