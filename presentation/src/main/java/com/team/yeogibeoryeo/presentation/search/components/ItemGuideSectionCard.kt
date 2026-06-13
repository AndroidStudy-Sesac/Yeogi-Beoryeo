package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.team.yeogibeoryeo.presentation.common.text.withKoreanSyllableBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.itemGuideDetailTextStyles

@Composable
internal fun ItemGuideSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    contentSpacing: Dp = ItemSearchLayoutDefaults.spacing.sm,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val stroke = ItemSearchLayoutDefaults.stroke
    val elevation = ItemSearchLayoutDefaults.elevation

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(stroke.outline, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.none),
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
        ) {
            ItemGuideSectionTitle(text = title)
            content()
        }
    }
}

@Composable
internal fun ItemGuideSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val textStyles = itemGuideDetailTextStyles()

    Text(
        text = text.withKoreanSyllableBreakOpportunities(),
        modifier = modifier,
        style = textStyles.sectionTitle.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
