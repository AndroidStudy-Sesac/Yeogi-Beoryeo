package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class GetSidoOptionsUseCase @Inject constructor(
    private val repository: RegionOptionsRepository
) {
    suspend operator fun invoke(): List<String> {
        return repository.getSidoOptions()
    }
}
