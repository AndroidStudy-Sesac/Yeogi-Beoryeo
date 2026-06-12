package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun MapCenterSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = CommonR.drawable.ic_action_search),
            contentDescription = null,
        )
        Text(
            text = "현 지도에서 검색",
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapCenterSearchButtonPreview() {
    MaterialTheme {
        MapCenterSearchButton(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
