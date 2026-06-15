package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.presentation.common.text.KoreanLineBreakText
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.itemGuideDetailTextStyles

@Composable
fun SubGuideSection(
    title: String,
    subGuides: List<DisposalSubGuide>,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    ItemGuideSectionCard(
        title = title,
        modifier = modifier,
        contentSpacing = spacing.md,
    ) {
        subGuides.forEach { subGuide ->
            SubGuideItem(
                name = subGuide.name,
                summary = subGuide.summary,
            )
        }
    }
}

@Composable
private fun SubGuideItem(
    name: String,
    summary: String,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val textStyles = itemGuideDetailTextStyles()

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        KoreanLineBreakText(
            text = name,
            style = textStyles.subGuideTitle.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        KoreanLineBreakText(
            text = summary,
            style = textStyles.body.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
