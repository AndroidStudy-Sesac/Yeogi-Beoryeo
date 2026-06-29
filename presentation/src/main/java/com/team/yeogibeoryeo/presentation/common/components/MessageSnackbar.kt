package com.team.yeogibeoryeo.presentation.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme

@Composable
fun MessageSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    Snackbar(
        modifier = modifier.padding(SnackbarPadding),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(SnackbarIconTextSpacing))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val SnackbarPadding = 16.dp
private val SnackbarIconTextSpacing = 12.dp

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MessageSnackbarPreview() {
    YeogiBeoryeoTheme {
        Box(
            modifier = Modifier.size(width = 360.dp, height = 160.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            MessageSnackbar(message = "즐겨찾기에 추가되었습니다")
        }
    }
}
