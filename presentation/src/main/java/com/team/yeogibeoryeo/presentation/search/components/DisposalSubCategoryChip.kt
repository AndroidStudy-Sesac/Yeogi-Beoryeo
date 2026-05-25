package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory

@Composable
fun DisposalSubCategoryChip(
    subCategory: DisposalSubCategory,
    modifier: Modifier = Modifier,
) {
    MetadataChip(
        text = subCategory.displayName,
        modifier = modifier
    )
}

