package com.team.yeogibeoryeo.data.item.local

interface ItemCategoryLocalSource {
    fun getSynonyms(): Map<String, String>

    fun getGuideDetails(): Map<String, ItemGuideDetail>

    fun getWasteDictionaryItems(): List<WasteDictionaryItem>
}
