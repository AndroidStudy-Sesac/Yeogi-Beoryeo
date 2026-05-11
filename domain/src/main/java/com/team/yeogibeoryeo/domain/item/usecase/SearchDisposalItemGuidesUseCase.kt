package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository

class SearchDisposalItemGuidesUseCase(
    private val repository: DisposalItemGuideRepository,
) {
    suspend operator fun invoke(query: String): List<DisposalItemGuide> = repository.searchItemGuides(query)
}
