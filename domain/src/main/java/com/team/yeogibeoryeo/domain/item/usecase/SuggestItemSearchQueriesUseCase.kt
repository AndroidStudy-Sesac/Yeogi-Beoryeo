package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import javax.inject.Inject

class SuggestItemSearchQueriesUseCase @Inject constructor(
    private val repository: DisposalItemGuideRepository,
) {
    suspend operator fun invoke(query: String): List<String> = repository.suggestSearchQueries(query)
}
