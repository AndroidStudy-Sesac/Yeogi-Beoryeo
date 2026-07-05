package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
internal fun SelectedCategorySummary(
    selectedCount: Int,
    maxSelectedCount: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(
                R.string.quick_category_selected_count,
                selectedCount,
                maxSelectedCount,
            ),
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
