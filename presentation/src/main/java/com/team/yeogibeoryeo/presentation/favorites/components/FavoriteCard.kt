package com.team.yeogibeoryeo.presentation.favorites.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.favorites.model.FavoriteUiModel
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun FavoriteCard(
    favorite: FavoriteUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRemoveClick: (() -> Unit)? = null,
    onHomePrimaryClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                favorite.subtitle?.let { subtitle ->
                    FavoriteMetadataChip(text = subtitle)
                }
            }

            if (onHomePrimaryClick != null) {
                HomeRegionalGuidePrimaryButton(
                    isHomePrimary = favorite.isHomeRegionalGuidePrimary,
                    onClick = onHomePrimaryClick,
                )
            }

            if (onRemoveClick != null) {
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        painter = painterResource(id = CommonR.drawable.ic_favorite_filled),
                        contentDescription = stringResource(R.string.favorite_remove_action),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeRegionalGuidePrimaryButton(
    isHomePrimary: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            painter =
                painterResource(
                    id = if (isHomePrimary) {
                        CommonR.drawable.ic_home_pin_filled
                    } else {
                        CommonR.drawable.ic_home_pin
                    },
                ),
            contentDescription =
                stringResource(
                    id = if (isHomePrimary) {
                        R.string.favorite_home_regional_guide_unpin_action
                    } else {
                        R.string.favorite_home_regional_guide_pin_action
                    },
                ),
            modifier = Modifier.size(20.dp),
            tint =
                if (isHomePrimary) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
        )
    }
}

@Composable
private fun FavoriteMetadataChip(text: String) {
    Text(
        text = text,
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
