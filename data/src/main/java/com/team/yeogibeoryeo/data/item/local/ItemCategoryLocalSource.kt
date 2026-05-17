package com.team.yeogibeoryeo.data.item.local

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType

interface ItemCategoryLocalSource {
    fun getCategoryMap(): Map<String, Pair<DisposalCategory, DisposalSubCategory?>>

    fun getSynonyms(): Map<String, String>

    fun getRelatedSpots(): Map<String, List<RelatedSpotType>>

    fun getGuideDetails(): Map<String, ItemGuideDetail>

    fun getGuideDetailAliases(): Map<String, String>

    fun getLocalItems(): List<DisposalItemGuide>

    fun getWasteDictionaryItems(): List<WasteDictionaryItem>
}
