package com.team.yeogibeoryeo.presentation.search.components

import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

internal val quickCategoryOrder =
    listOf(
        RepresentativeGuideCategory.PAPER,
        RepresentativeGuideCategory.PAPER_PACK,
        RepresentativeGuideCategory.COLORLESS_PET,
        RepresentativeGuideCategory.PLASTIC,
        RepresentativeGuideCategory.VINYL,
        RepresentativeGuideCategory.STYROFOAM,
        RepresentativeGuideCategory.GLASS,
        RepresentativeGuideCategory.METAL,
        RepresentativeGuideCategory.CLOTHING,
        RepresentativeGuideCategory.BATTERY,
        RepresentativeGuideCategory.LIGHTING,
        RepresentativeGuideCategory.ELECTRONICS,
        RepresentativeGuideCategory.FOOD_WASTE,
        RepresentativeGuideCategory.GENERAL,
        RepresentativeGuideCategory.NON_COMBUSTIBLE,
        RepresentativeGuideCategory.LARGE_WASTE,
        RepresentativeGuideCategory.CONSTRUCTION_WASTE,
        RepresentativeGuideCategory.HAZARDOUS,
        RepresentativeGuideCategory.OTHER,
    )

internal fun orderedQuickCategories(
    selectedCategories: List<RepresentativeGuideCategory>,
): List<RepresentativeGuideCategory> {
    val selected = selectedCategories.distinct().filter { it in quickCategoryOrder }

    return selected + quickCategoryOrder.filterNot { it in selected }
}
