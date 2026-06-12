package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun MyLocationButton(
    isTracking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isTracking) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (isTracking) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
    ) {
        Icon(
            painter = painterResource(id = CommonR.drawable.ic_action_current_location),
            contentDescription = "현 위치 기준 검색",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyLocationButtonPreview() {
    MaterialTheme {
        MyLocationButton(
            isTracking = false,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
