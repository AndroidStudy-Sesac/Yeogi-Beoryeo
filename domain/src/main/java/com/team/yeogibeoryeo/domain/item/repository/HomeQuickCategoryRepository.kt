package com.team.yeogibeoryeo.domain.item.repository

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import kotlinx.coroutines.flow.Flow

interface HomeQuickCategoryRepository {
    fun observeHomeQuickCategories(): Flow<List<DisposalCategory>>

    suspend fun toggleHomeQuickCategory(
        category: DisposalCategory,
        maxSelectedCount: Int,
    )

    suspend fun limitHomeQuickCategories(maxSelectedCount: Int)
}
