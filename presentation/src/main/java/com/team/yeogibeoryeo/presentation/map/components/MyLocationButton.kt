package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
            imageVector = Icons.Default.MyLocation,
            contentDescription = "내 위치로 이동",
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
