package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@Composable
fun DisposalItemCard(
    guide: DisposalItemGuide,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    val stroke = ItemSearchLayoutDefaults.stroke
    val elevation = ItemSearchLayoutDefaults.elevation
    val favoriteStateDescription =
        stringResource(
            if (isFavorite) {
                R.string.item_card_favorite_saved_state
            } else {
                R.string.item_card_favorite_not_saved_state
            },
        )
    val openActionLabel = stringResource(R.string.item_card_open_action_label)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = favoriteStateDescription
            }
            .clickable(
                onClickLabel = openActionLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(stroke.outline, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.none)
    ) {
        DisposalItemCardLayout(
            favoriteIndicator = {
                if (isFavorite) {
                    FavoriteIndicator()
                }
            },
            content = {
                DisposalItemTitle(text = guide.name)
                DisposalGuideMetadataChips(guide = guide)
                guide.instructions.firstOrNull()?.let { instruction ->
                    DisposalItemDescription(text = instruction.method.toDisplayLabel())
                }
            },
            trailing = {
                ChevronIndicator()
            },
        )
    }
}

@Composable
private fun DisposalItemCardLayout(
    favoriteIndicator: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    Box(
        modifier = Modifier
            .padding(spacing.lg)
            .fillMaxWidth(),
    ) {
        favoriteIndicator()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(ItemCardContentWeight),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                content()
            }
            trailing()
        }
    }
}

@Composable
private fun BoxScope.FavoriteIndicator() {
    val size = ItemSearchLayoutDefaults.size

    Icon(
        painter = painterResource(id = CommonR.drawable.ic_favorite_filled),
        // 카드 stateDescription이 즐겨찾기 상태를 제공하므로 아이콘은 중복 읽기를 피합니다.
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(size.iconSmall),
        tint = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun ColumnScope.DisposalItemTitle(text: String) {
    Text(
        text = text.withKoreanLineBreakOpportunities(),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        maxLines = ItemCardTitleMaxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ColumnScope.DisposalItemDescription(text: String) {
    Text(
        text = text.withKoreanLineBreakOpportunities(),
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        maxLines = ItemCardDescriptionMaxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ChevronIndicator() {
    val size = ItemSearchLayoutDefaults.size

    Icon(
        painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
        // 카드의 click action label이 상세 이동을 제공하므로 아이콘은 중복 읽기를 피합니다.
        contentDescription = null,
        modifier = Modifier.size(size.iconSmall),
        tint = MaterialTheme.colorScheme.outlineVariant,
    )
}

private fun String.toDisplayLabel(): String =
    if (this == "일반종량제폐기물") "종량제봉투" else this

private const val ItemCardContentWeight = 1f
private const val ItemCardTitleMaxLines = 2
private const val ItemCardDescriptionMaxLines = 2
