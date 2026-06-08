package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import javax.inject.Inject

class GetDisposalItemGuideUseCase
    @Inject
    constructor(
        private val repository: DisposalItemGuideRepository,
    ) {
        suspend operator fun invoke(guideId: String): DisposalItemGuide? =
            repository.getItemGuide(guideId)
    }
