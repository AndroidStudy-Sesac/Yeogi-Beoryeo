package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository

class GetDisposalCategoriesUseCase(
    private val repository: DisposalItemGuideRepository,
) {
    operator fun invoke(): List<DisposalCategory> = repository.getCategories()
}
