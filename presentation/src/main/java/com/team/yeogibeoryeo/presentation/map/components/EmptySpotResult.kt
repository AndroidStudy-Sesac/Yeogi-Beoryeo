package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R

@Composable
fun EmptySpotResult(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val displayTitle = title ?: stringResource(R.string.map_empty_spot_result_title)
    val displayDescription = description ?: stringResource(R.string.map_empty_spot_result_description)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
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

@Preview(showBackground = true)
@Composable
private fun EmptySpotResultPreview() {
    MaterialTheme {
        Surface {
            EmptySpotResult()
        }
    }
}
