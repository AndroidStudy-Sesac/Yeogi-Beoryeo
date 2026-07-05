package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotAddressSearchPolicy
import javax.inject.Inject

class NormalizeCollectionSpotSearchKeywordUseCase @Inject constructor() {

    operator fun invoke(keyword: String): String {
        return CollectionSpotAddressSearchPolicy.normalizeKeyword(keyword)
    }
}
