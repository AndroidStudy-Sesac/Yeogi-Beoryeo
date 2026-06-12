package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.R

@Composable
fun ItemSearchStatusContent(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    description: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues =
        PaddingValues(
            horizontal = ItemSearchStatusHorizontalPadding,
            vertical = ItemSearchStatusVerticalPadding,
        ),
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = ItemSearchStatusContentSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        title()

        description?.invoke(this)

        action?.invoke(this)
    }
}

@Composable
fun ItemSearchStatusTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ItemSearchStatusDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun ItemSearchStatusContentEmptyPreview() {
    YeogiBeoryeoTheme {
        Surface {
            ItemSearchStatusContent(
                title = {
                    ItemSearchStatusTitle(text = stringResource(R.string.no_search_results_title))
                },
                description = {
                    ItemSearchStatusDescription(
                        text = stringResource(R.string.no_search_results_description),
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemSearchStatusContentActionPreview() {
    YeogiBeoryeoTheme {
        Surface {
            ItemSearchStatusContent(
                title = {
                    ItemSearchStatusTitle(
                        text = stringResource(R.string.item_guide_detail_not_found_title),
                    )
                },
                description = {
                    ItemSearchStatusDescription(
                        text = stringResource(R.string.item_guide_detail_select_again_message),
                    )
                },
                action = {
                    Button(onClick = {}) {
                        Text(text = stringResource(R.string.back_action))
                    }
                },
            )
        }
    }
}

private val ItemSearchStatusHorizontalPadding = 24.dp
private val ItemSearchStatusVerticalPadding = 28.dp
private val ItemSearchStatusContentSpacing = 8.dp
