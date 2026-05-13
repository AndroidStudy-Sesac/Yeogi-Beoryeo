package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import javax.inject.Inject

class SearchCollectionSpotsByKeywordUseCase @Inject constructor(
    private val repository: CollectionSpotRepository
) {
    suspend operator fun invoke(
        keyword: String,
        types: Set<CollectionSpotType> = emptySet()
    ): List<CollectionSpot> {
        return repository.searchByKeyword(
            keyword = keyword,
            types = types
        )
    }
}