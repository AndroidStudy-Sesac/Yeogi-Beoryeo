package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.QuickCategorySettingsDefaults

@Composable
internal fun QuickCategorySelectionIndicator(
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.size(QuickCategorySettingsDefaults.selectionControlSize),
        shape = MaterialTheme.shapes.extraLarge,
        color =
            if (isSelected) {
                colorScheme.primary
            } else {
                colorScheme.surface
            },
        border =
            if (isSelected) {
                null
            } else {
                BorderStroke(
                    width = ItemSearchLayoutDefaults.stroke.outline,
                    color =
                        if (enabled) {
                            colorScheme.outline
                        } else {
                            colorScheme.outlineVariant
                        },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(ItemSearchLayoutDefaults.size.iconSmall),
                    tint = colorScheme.onPrimary,
                )
            }
        }
    }
}
