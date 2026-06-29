package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.repository.HomeQuickCategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHomeQuickCategoriesUseCase
    @Inject
    constructor(
        private val repository: HomeQuickCategoryRepository,
    ) {
        operator fun invoke(): Flow<List<DisposalCategory>> = repository.observeHomeQuickCategories()
    }
