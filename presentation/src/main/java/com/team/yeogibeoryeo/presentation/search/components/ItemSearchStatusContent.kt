package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
fun ItemSearchStatusContent(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    description: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues =
        PaddingValues(
            horizontal = ItemSearchLayoutDefaults.spacing.xl,
            vertical = ItemSearchLayoutDefaults.spacing.xxl,
        ),
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = ItemSearchLayoutDefaults.spacing.xs,
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

@Composable
fun ItemSearchLoadingContent(
    modifier: Modifier = Modifier,
) {
    val loadingContentDescription = stringResource(R.string.loading_action)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = loadingContentDescription
            },
            color = MaterialTheme.colorScheme.primary,
        )
    }
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
private fun ItemSearchLoadingContentPreview() {
    YeogiBeoryeoTheme {
        Surface {
            ItemSearchLoadingContent()
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
