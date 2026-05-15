package com.team.yeogibeoryeo.domain.item.repository

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide

interface DisposalItemGuideRepository {
    suspend fun searchItemGuides(query: String): List<DisposalItemGuide>

    suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide>

    fun getCategories(): List<DisposalCategory>
}
