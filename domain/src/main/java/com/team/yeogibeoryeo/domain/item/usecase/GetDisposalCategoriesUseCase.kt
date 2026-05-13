package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import javax.inject.Inject

class GetDisposalCategoriesUseCase
    @Inject
    constructor(
    private val repository: DisposalItemGuideRepository,
    ) {
    operator fun invoke(): List<DisposalCategory> = repository.getCategories()
}
