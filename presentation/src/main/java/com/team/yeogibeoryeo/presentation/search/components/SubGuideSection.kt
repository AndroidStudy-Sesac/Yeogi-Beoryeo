package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

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

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
