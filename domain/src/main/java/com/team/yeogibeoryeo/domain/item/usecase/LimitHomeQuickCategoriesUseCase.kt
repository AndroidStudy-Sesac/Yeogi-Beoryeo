package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import javax.inject.Inject

class LimitHomeQuickCategoriesUseCase
    @Inject
    constructor(
        private val repository: HomeQuickCategoryRepository,
    ) {
        suspend operator fun invoke(maxSelectedCount: Int) {
            repository.limitHomeQuickCategories(maxSelectedCount)
        }
    }
