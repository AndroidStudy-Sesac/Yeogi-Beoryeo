package com.team.yeogibeoryeo.presentation.search.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState
import com.team.yeogibeoryeo.common.R as CommonR

@Composable
fun HomeRegionalGuideSummaryBanner(
    state: HomeRegionalGuideSummaryUiState,
    onClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = state.toBannerContent()
    val disposalDaysLabel =
        stringResource(id = R.string.home_regional_guide_summary_disposal_days_label)
    val disposalTimeLabel =
        stringResource(id = R.string.home_regional_guide_summary_disposal_time_label)
    val shape = RoundedCornerShape(8.dp)
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    when {
                        content.retryable ->
                            Modifier.clip(shape).clickable(
                                onClickLabel = content.actionLabel,
                                role = Role.Button,
                                onClick = onRetryClick,
                            )
                        content.targetId != null ->
                            Modifier.clip(shape).clickable(
                                onClickLabel = content.actionLabel,
                                role = Role.Button,
                                onClick = { onClick(content.targetId) },
                            )
                        else -> Modifier
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        shape = shape,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = content.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (content.showChevron) {
                    Icon(
                        painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Text(
                text = content.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (content.description != null) {
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (content.dayText != null || content.timeText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    content.dayText?.let { dayText ->
                        HomeRegionalGuideSummaryInfoBlock(
                            label = disposalDaysLabel,
                            value = dayText,
                            notice = content.dayNotice,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    content.timeText?.let { timeText ->
                        HomeRegionalGuideSummaryInfoBlock(
                            label = disposalTimeLabel,
                            value = timeText,
                            notice = content.timeNotice,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (content.detail != null) {
                Text(
                    text = content.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeRegionalGuideSummaryInfoBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (notice != null) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class HomeRegionalGuideBannerContent(
    val label: String,
    val title: String,
    val description: String? = null,
    val detail: String? = null,
    val dayText: String? = null,
    val dayNotice: String? = null,
    val timeText: String? = null,
    val timeNotice: String? = null,
    val targetId: String? = null,
    val retryable: Boolean = false,
    val actionLabel: String? = null,
    val showChevron: Boolean = false,
)

@Composable
private fun HomeRegionalGuideSummaryUiState.toBannerContent(): HomeRegionalGuideBannerContent {
    val label = stringResource(id = R.string.home_regional_guide_summary_label)
    val openDetailAction =
        stringResource(id = R.string.home_regional_guide_summary_open_detail_action)

    return when (this) {
        is HomeRegionalGuideSummaryUiState.Loading ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = regionName ?: label,
                description = stringResource(
                    id = R.string.home_regional_guide_summary_loading_description,
                ),
                targetId = targetId,
                actionLabel = openDetailAction.takeIf { targetId != null },
                showChevron = targetId != null,
            )

        HomeRegionalGuideSummaryUiState.NoFavorite ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = label,
                description = stringResource(
                    id = R.string.home_regional_guide_summary_no_favorite_description,
                ),
            )

        is HomeRegionalGuideSummaryUiState.Summary ->
            summaryBannerContent(
                label = label,
                openDetailAction = openDetailAction,
                detailRequiredText = stringResource(
                    id = R.string.home_regional_guide_summary_detail_required,
                ),
                differentByWasteTypeNotice = stringResource(
                    id = R.string.home_regional_guide_summary_different_by_waste_type_notice,
                ),
                generalWasteBasisDetail = stringResource(
                    id = R.string.home_regional_guide_summary_general_waste_basis_detail,
                ),
            )

        is HomeRegionalGuideSummaryUiState.NoRepresentativeSchedule ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = regionName,
                detail = stringResource(
                    id = R.string.home_regional_guide_summary_no_representative_schedule_detail,
                ),
                targetId = targetId,
                actionLabel = openDetailAction,
                showChevron = true,
            )

        is HomeRegionalGuideSummaryUiState.RepresentativeScheduleNeedsConfirmation ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = regionName,
                detail = stringResource(
                    id = R.string.home_regional_guide_summary_representative_schedule_needs_confirmation_detail,
                ),
                targetId = targetId,
                actionLabel = openDetailAction,
                showChevron = true,
            )

        is HomeRegionalGuideSummaryUiState.FavoriteRestoreFailed ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = label,
                description = stringResource(
                    id = R.string.home_regional_guide_summary_restore_failed_description,
                ),
            )

        is HomeRegionalGuideSummaryUiState.LoadFailed ->
            HomeRegionalGuideBannerContent(
                label = label,
                title = regionName,
                description = stringResource(
                    id = R.string.home_regional_guide_summary_load_failed_description,
                ),
                detail = stringResource(id = R.string.home_regional_guide_summary_retry_detail),
                targetId = targetId,
                retryable = true,
                actionLabel = stringResource(id = R.string.home_regional_guide_summary_retry_action),
            )
    }
}

private fun HomeRegionalGuideSummaryUiState.Summary.summaryBannerContent(
    label: String,
    openDetailAction: String,
    detailRequiredText: String,
    differentByWasteTypeNotice: String,
    generalWasteBasisDetail: String,
): HomeRegionalGuideBannerContent {
    return HomeRegionalGuideBannerContent(
        label = label,
        title = regionName,
        dayText = disposalDays ?: detailRequiredText,
        dayNotice = differentByWasteTypeNotice.takeIf { hasDifferentDisposalDays },
        timeText = disposalTime ?: detailRequiredText,
        timeNotice = differentByWasteTypeNotice.takeIf { hasDifferentDisposalTime },
        detail = generalWasteBasisDetail.takeIf {
            hasDifferentDisposalDays || hasDifferentDisposalTime
        },
        targetId = targetId,
        actionLabel = openDetailAction,
        showChevron = true,
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeRegionalGuideSummaryBannerSummaryPreview() {
    MaterialTheme {
        HomeRegionalGuideSummaryBanner(
            state = HomeRegionalGuideSummaryUiState.Summary(
                targetId = "preview",
                regionName = "서울특별시 > 노원구 > 하계동",
                disposalDays = "월, 수, 금",
                disposalTime = "18:00 이후",
                hasDifferentDisposalDays = false,
                hasDifferentDisposalTime = false,
            ),
            onClick = {},
            onRetryClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeRegionalGuideSummaryBannerMixedSchedulePreview() {
    MaterialTheme {
        HomeRegionalGuideSummaryBanner(
            state = HomeRegionalGuideSummaryUiState.Summary(
                targetId = "preview",
                regionName = "서울특별시 > 노원구 > 하계동",
                disposalDays = "월, 수, 금",
                disposalTime = "18:00 ~ 23:59",
                hasDifferentDisposalDays = true,
                hasDifferentDisposalTime = true,
            ),
            onClick = {},
            onRetryClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeRegionalGuideSummaryBannerRepresentativeScheduleNeedsConfirmationPreview() {
    MaterialTheme {
        HomeRegionalGuideSummaryBanner(
            state = HomeRegionalGuideSummaryUiState.RepresentativeScheduleNeedsConfirmation(
                targetId = "preview",
                regionName = "경기도 > 고양시",
            ),
            onClick = {},
            onRetryClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeRegionalGuideSummaryBannerNoFavoritePreview() {
    MaterialTheme {
        HomeRegionalGuideSummaryBanner(
            state = HomeRegionalGuideSummaryUiState.NoFavorite,
            onClick = {},
            onRetryClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
