package com.team.yeogibeoryeo.common.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

data class BottomNavigationItem(
    val label: String,
    @param:DrawableRes val iconResId: Int,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun AppBottomNavigationBar(
    items: List<BottomNavigationItem>,
    modifier: Modifier = Modifier,
    itemModifier: (Int) -> Modifier = { Modifier },
) {
    NavigationBar(modifier = modifier) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                modifier = itemModifier(index),
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = null,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}
