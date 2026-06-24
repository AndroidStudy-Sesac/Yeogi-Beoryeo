package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import javax.inject.Inject

class ToggleHomeQuickCategoryUseCase
    @Inject
    constructor(
        private val repository: HomeQuickCategoryRepository,
    ) {
    suspend operator fun invoke(
        category: DisposalCategory,
        maxSelectedCount: Int,
    ) =
        repository.toggleHomeQuickCategory(category, maxSelectedCount)
    }
