package com.team.yeogibeoryeo.presentation.map.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CurrentLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = null,
        )

        Text(
            text = "현재 위치",
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentLocationButtonPreview() {
    MaterialTheme {
        CurrentLocationButton(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}