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
    fixedCollapsedItemCount: Int = 0,
    isExpanded: Boolean,
): QuickCategoryGridCollapseLayout {
    val visibleRowCount = quickCategoryVisibleRowCount(
        columnCount = columnCount,
        availableHeightPx = availableHeightPx,
        rowHeightPx = rowHeightPx,
        rowSpacingPx = rowSpacingPx,
    )
    val measuredCollapsedItemCount = visibleRowCount * columnCount
    val minCollapsedItemCount = quickCategoryMinCollapsedItemCount(columnCount)
    val validFixedCollapsedItemCount = fixedCollapsedItemCount.takeIf {
        it > 0 &&
            it < categoryCount &&
            it % columnCount == 0
    }
    val collapsedItemCount =
        validFixedCollapsedItemCount
            ?: measuredCollapsedItemCount.takeIf { it < categoryCount }
            ?: minCollapsedItemCount
    val needsMoreToggle = categoryCount > collapsedItemCount
    val needsCollapseToggle = categoryCount > collapsedItemCount
    val showsMore = !isExpanded && needsMoreToggle

    return QuickCategoryGridCollapseLayout(
        collapsedItemCount = collapsedItemCount,
        visibleCategoryCount = if (showsMore) {
            (collapsedItemCount - 1).coerceAtLeast(0)
        } else {
            categoryCount
        },
        showsMore = showsMore,
        showsCollapse = isExpanded && needsCollapseToggle,
    )
}

internal fun quickCategoryVisibleRowCount(
    columnCount: Int,
    availableHeightPx: Int,
    rowHeightPx: Int,
    rowSpacingPx: Int,
): Int {
    if (availableHeightPx <= 0 || rowHeightPx <= 0) {
        return quickCategoryMinCollapsedRowCount(columnCount)
    }

    return ((availableHeightPx + rowSpacingPx) / (rowHeightPx + rowSpacingPx))
        .coerceAtLeast(1)
}

private fun quickCategoryMinCollapsedItemCount(columnCount: Int): Int =
    quickCategoryMinCollapsedRowCount(columnCount) * columnCount

private fun quickCategoryMinCollapsedRowCount(columnCount: Int): Int =
    (QuickCategoryCollapsedMinItemCount + columnCount - 1) / columnCount
