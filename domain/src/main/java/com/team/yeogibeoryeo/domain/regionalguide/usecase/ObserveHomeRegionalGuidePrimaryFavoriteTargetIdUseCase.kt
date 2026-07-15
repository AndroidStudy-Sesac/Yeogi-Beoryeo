package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject

class ObserveHomeRegionalGuidePrimaryFavoriteTargetIdUseCase
    @Inject
    constructor(
        private val repository: HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        operator fun invoke() = repository.observePrimaryFavoriteTargetId()
    }
