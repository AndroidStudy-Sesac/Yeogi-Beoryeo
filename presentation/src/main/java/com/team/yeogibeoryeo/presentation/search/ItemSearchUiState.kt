package com.team.yeogibeoryeo.presentation.search

import androidx.annotation.StringRes
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.search.components.orderedQuickCategories
import com.team.yeogibeoryeo.presentation.search.components.quickCategoryOrder
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

data class ItemSearchUiState(
    val query: String = "",
    val submittedQuery: String? = null,
    val guides: List<DisposalItemGuide> = emptyList(),
    val favoriteGuideIds: Set<String> = emptySet(),
    val searchResultVersion: Int = 0,
    val isQuickCategoryExpanded: Boolean = false,
    val quickCategoryFixedCollapsedItemCount: Int = 0,
    val quickCategoryScrollRestoreIndex: Int = 0,
    val quickCategoryScrollRestoreOffset: Int = 0,
    val quickCategoryScrollRestoreVersion: Int = 0,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val homeQuickCategories: List<RepresentativeGuideCategory> = emptyList(),
    val homeQuickCategoriesAtEntry: List<RepresentativeGuideCategory>? = null,
    @param:StringRes val errorMessageResId: Int? = null,
) {
    val quickCategories: List<RepresentativeGuideCategory>
        get() =
            orderedQuickCategories(homeQuickCategories)

    internal fun quickCategorySettingsOrder(maxSelectedCount: Int): List<RepresentativeGuideCategory> {
        val selectedAtEntry = homeQuickCategoriesAtEntry
            ?.take(maxSelectedCount.coerceAtLeast(0))
            ?.toSet()
            ?: return emptyList()
        return quickCategoryOrder.sortedBy { it !in selectedAtEntry }
    }
}
