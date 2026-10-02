package com.team.yeogibeoryeo.presentation.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.components.AppBackButton
import com.team.yeogibeoryeo.presentation.common.components.AppTopBar

@Composable
internal fun ItemSearchResultQuery(
    query: String,
    modifier: Modifier = Modifier,
    resultCount: Int? = null,
) {
    Text(
        text = if (resultCount == null) {
            stringResource(R.string.item_search_result_query, query)
        } else {
            stringResource(R.string.item_search_result_summary, query, resultCount)
        },
        modifier = modifier.semantics {
            heading()
            if (resultCount != null) {
                liveRegion = LiveRegionMode.Polite
            }
        },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun ItemSearchTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTopBar(
        modifier = modifier,
        navigationIcon = {
            AppBackButton(onClick = onBackClick)
        },
        title = {
            Text(
                text = stringResource(R.string.item_search_screen_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}
