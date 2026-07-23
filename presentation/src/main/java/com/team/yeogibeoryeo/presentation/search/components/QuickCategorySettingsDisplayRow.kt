package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.QuickCategorySettingsDefaults
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
internal fun QuickCategorySettingsDisplayRow(
    category: RepresentativeGuideCategory,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                enabled = true,
                role = Role.Checkbox,
            ) {
                onClick()
            },
        shape = MaterialTheme.shapes.large,
        color =
            if (isSelected) {
                colorScheme.secondaryContainer
            } else {
                colorScheme.surface
            },
        border = BorderStroke(
            width = ItemSearchLayoutDefaults.stroke.outline,
            color =
                if (isSelected) {
                    colorScheme.outline
                } else {
                    colorScheme.outlineVariant
                },
        ),
    ) {
        Row(
            modifier = Modifier.padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(QuickCategorySettingsDefaults.iconContainerSize)
                    .background(
                        color = category.containerColor(),
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = category.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(size.iconStandard),
                    tint = category.iconTint(),
                )
            }
            Text(
                text = category.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color =
                    if (isSelected) {
                        colorScheme.onSecondaryContainer
                    } else {
                        colorScheme.onSurface
                    },
            )
            QuickCategorySelectionIndicator(isSelected = isSelected, enabled = enabled)
        }
    }
}
