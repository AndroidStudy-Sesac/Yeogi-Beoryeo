package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.KoreanLineBreakText
import com.team.yeogibeoryeo.presentation.search.components.DisposalGuideMetadataChips
import com.team.yeogibeoryeo.presentation.search.components.SectionCard
import com.team.yeogibeoryeo.presentation.search.components.SubGuideSection
import com.team.yeogibeoryeo.presentation.search.components.containerColor
import com.team.yeogibeoryeo.presentation.search.components.iconResId
import com.team.yeogibeoryeo.presentation.search.components.iconTint
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemGuideDetailScreen(
    guide: DisposalItemGuide,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val textStyles = itemGuideDetailTextStyles()
    val scrollState = rememberScrollState()
    val onBottomBarVisibilityChangedState by rememberUpdatedState(onBottomBarVisibilityChanged)

    LaunchedEffect(scrollState) {
        var previousOffset = 0
        onBottomBarVisibilityChangedState(true)

        snapshotFlow { scrollState.value }
            .collect { currentOffset ->
                if (scrollState.maxValue > 0 && currentOffset >= scrollState.maxValue) {
                    onBottomBarVisibilityChangedState(false)
                    previousOffset = currentOffset
                    return@collect
                }

                if (currentOffset == 0) {
                    onBottomBarVisibilityChangedState(true)
                } else if (currentOffset > previousOffset) {
                    onBottomBarVisibilityChangedState(false)
                } else if (currentOffset < previousOffset) {
                    onBottomBarVisibilityChangedState(true)
                }
                previousOffset = currentOffset
            }
    }

    val backActionDescription = stringResource(R.string.back_action)
    val favoriteActionDescription =
        stringResource(
            if (isFavorite) {
                R.string.favorite_remove_action
            } else {
                R.string.favorite_add_action
            },
        )
    val matchedRepresentativeCategory = RepresentativeGuideCategory.fromGuideName(guide.name)
    val isRepresentativeGuide = matchedRepresentativeCategory != null
    val representativeCategory =
        matchedRepresentativeCategory
            ?: RepresentativeGuideCategory.fromDisposalCategory(guide.category)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(
                start = spacing.xl,
                top = spacing.md,
                end = spacing.xl,
                bottom = spacing.xl,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = CommonR.drawable.ic_action_back),
                    contentDescription = backActionDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    painter = painterResource(
                        id = if (isFavorite) {
                            CommonR.drawable.ic_favorite_filled
                        } else {
                            CommonR.drawable.ic_favorite
                        },
                    ),
                    contentDescription = favoriteActionDescription,
                    tint = if (isFavorite) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(size.detailIconContainer)
                    .background(
                        color = representativeCategory.containerColor(),
                        shape = MaterialTheme.shapes.extraLarge
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = representativeCategory.iconResId),
                    // 옆의 품목명과 metadata chip이 의미를 제공하므로 아이콘은 중복 읽기를 피합니다.
                    contentDescription = null,
                    modifier = Modifier.size(size.iconProminent),
                    tint = representativeCategory.iconTint()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                KoreanLineBreakText(
                    text = guide.name,
                    style = textStyles.title.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                DisposalGuideMetadataChips(
                    guide = guide,
                    showCategoryChip = !isRepresentativeGuide,
                )
            }
        }

        if (guide.detailSections.isNotEmpty()) {
            guide.detailSections.forEach { section ->
                SectionCard(
                    title = section.title,
                    lines = section.lines,
                    rows = section.rows,
                )
            }
        } else {
            if (guide.steps.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.disposal_steps_title),
                    lines = guide.steps,
                    numbered = true,
                )
            }

            if (guide.cautions.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.cautions_title),
                    lines = guide.cautions,
                )
            }

            if (guide.subGuides.isNotEmpty()) {
                SubGuideSection(
                    title = stringResource(R.string.sub_guides_title),
                    subGuides = guide.subGuides,
                )
            }

            if (guide.features.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.features_title),
                    lines = guide.features,
                )
            }

            guide.tip?.let {
                SectionCard(
                    title = stringResource(R.string.tip_title),
                    lines = listOf(it),
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.local_disposal_notice_title),
            lines = listOf(stringResource(R.string.local_disposal_notice)),
        )
    }
}
