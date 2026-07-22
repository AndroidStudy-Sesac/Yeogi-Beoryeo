package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R

@Composable
fun EmptySpotResult(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp,
) {
    val displayTitle = title ?: stringResource(R.string.map_empty_spot_result_title)
    val displayDescription = description ?: stringResource(R.string.map_empty_spot_result_description)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EmptySpotResultMinHeight + bottomContentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(
                top = EmptySpotResultVerticalPadding,
                bottom = EmptySpotResultVerticalPadding + bottomContentPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = displayDescription,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

private val EmptySpotResultMinHeight = 180.dp
private val EmptySpotResultVerticalPadding = 32.dp

@Preview(showBackground = true)
@Composable
private fun EmptySpotResultPreview() {
    MaterialTheme {
        Surface {
            EmptySpotResult()
        }
    }
}
