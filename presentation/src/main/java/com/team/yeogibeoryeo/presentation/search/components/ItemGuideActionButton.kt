package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
fun ItemGuideActionButton(
    label: String,
    iconResId: Int,
    onClick: () -> Unit,
    prominent: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val colors = MaterialTheme.colorScheme
    val content: @Composable RowScope.() -> Unit = {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(size.iconStandard),
        )
        Spacer(modifier = Modifier.size(spacing.sm))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(size.iconStandard),
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = size.searchFieldHeight),
        shape = MaterialTheme.shapes.large,
        colors = if (prominent) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                containerColor = colors.tertiaryContainer,
                contentColor = colors.onTertiaryContainer,
            )
        },
        content = content,
    )
}
