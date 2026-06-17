package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.presentation.common.text.KoreanLineBreakText
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
fun DisposalCategoryChip(
    category: DisposalCategory,
    modifier: Modifier = Modifier,
) {
    MetadataChip(
        text = category.displayName,
        modifier = modifier,
    )
}


@Composable
internal fun MetadataChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Box(
        modifier =
            modifier
                .background(
                    color = containerColor,
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = spacing.sm, vertical = spacing.xs),
    ) {
        KoreanLineBreakText(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
