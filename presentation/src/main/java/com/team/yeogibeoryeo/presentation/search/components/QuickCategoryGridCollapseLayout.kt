package com.team.yeogibeoryeo.presentation.search.components

private const val QuickCategoryCollapsedMinItemCount = 8

internal data class QuickCategoryGridCollapseLayout(
    val collapsedItemCount: Int,
    val visibleCategoryCount: Int,
    val showsMore: Boolean,
    val showsCollapse: Boolean,
)

internal fun quickCategoryGridCollapseLayout(
    categoryCount: Int,
    columnCount: Int,
    availableHeightPx: Int = 0,
    rowHeightPx: Int = 0,
    rowSpacingPx: Int = 0,
    isExpanded: Boolean,
): QuickCategoryGridCollapseLayout {
    val visibleRowCount = quickCategoryVisibleRowCount(
        columnCount = columnCount,
        availableHeightPx = availableHeightPx,
        rowHeightPx = rowHeightPx,
        rowSpacingPx = rowSpacingPx,
    )
    val collapsedItemCount = visibleRowCount * columnCount
    val needsToggle = categoryCount > collapsedItemCount
    val showsMore = !isExpanded && needsToggle

    return QuickCategoryGridCollapseLayout(
        collapsedItemCount = collapsedItemCount,
        visibleCategoryCount = if (showsMore) {
            (collapsedItemCount - 1).coerceAtLeast(0)
        } else {
            categoryCount
        },
        showsMore = showsMore,
        showsCollapse = isExpanded && needsToggle,
    )
}

internal fun quickCategoryVisibleRowCount(
    columnCount: Int,
    availableHeightPx: Int,
    rowHeightPx: Int,
    rowSpacingPx: Int,
): Int {
    if (availableHeightPx <= 0 || rowHeightPx <= 0) {
        return (QuickCategoryCollapsedMinItemCount + columnCount - 1) / columnCount
    }

    return ((availableHeightPx + rowSpacingPx) / (rowHeightPx + rowSpacingPx))
        .coerceAtLeast(1)
}
