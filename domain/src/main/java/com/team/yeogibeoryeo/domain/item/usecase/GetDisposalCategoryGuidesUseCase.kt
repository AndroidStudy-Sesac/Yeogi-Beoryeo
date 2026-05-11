package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository

class GetDisposalCategoryGuidesUseCase(
    private val repository: DisposalItemGuideRepository,
) {
    suspend operator fun invoke(category: DisposalCategory): List<DisposalItemGuide> = repository.getCategoryGuides(category)
}
