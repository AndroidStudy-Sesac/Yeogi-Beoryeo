package com.team.yeogibeoryeo.presentation.search

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide

data class ItemSearchUiState(
    val query: String = "",
    val guides: List<DisposalItemGuide> = emptyList(),
    val favoriteGuideIds: Set<String> = emptySet(),
    val searchResultVersion: Int = 0,
    val isQuickCategoryExpanded: Boolean = false,
    val quickCategoryFixedCollapsedItemCount: Int = 0,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    @param:StringRes val errorMessageResId: Int? = null,
)
