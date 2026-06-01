package com.team.yeogibeoryeo.presentation.favorites.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun FavoriteSnackbar(
    message: String,
    modifier: Modifier = Modifier,
) {
    Snackbar(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = CommonR.drawable.ic_action_favorite_filled),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FavoriteSnackbarPreview() {
    YeogiBeoryeoTheme {
        Box(
            modifier = Modifier.size(width = 360.dp, height = 160.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FavoriteSnackbar(message = "즐겨찾기에 추가되었습니다")
        }
    }
}
