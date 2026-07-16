package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import javax.inject.Inject

class SetHomeRegionalGuidePrimaryFavoriteUseCase
    @Inject
    constructor(
        private val repository: HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        suspend operator fun invoke(targetId: String) {
            repository.setPrimaryFavoriteTargetId(targetId)
        }
    }
