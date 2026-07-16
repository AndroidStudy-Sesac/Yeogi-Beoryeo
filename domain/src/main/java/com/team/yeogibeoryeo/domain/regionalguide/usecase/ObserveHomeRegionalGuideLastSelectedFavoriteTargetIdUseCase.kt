package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHomeRegionalGuideLastSelectedFavoriteTargetIdUseCase
    @Inject
    constructor(
        private val repository: HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        operator fun invoke(): Flow<String?> =
            repository.observeLastSelectedFavoriteTargetId()
    }
