package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    when {
                        content.retryable ->
                            Modifier.clickable(
                                onClickLabel = content.actionLabel,
                                role = Role.Button,
                                onClick = onRetryClick,
                            )
                        content.targetId != null ->
                            Modifier.clickable(
                                onClickLabel = content.actionLabel,
                                role = Role.Button,
                                onClick = { onClick(content.targetId) },
                            )
                        else -> Modifier
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = CommonR.drawable.ic_symbol_info),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (content.detail != null) {
                    Text(
                        text = content.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (content.showChevron) {
                Row(
                    modifier = Modifier.size(32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = CommonR.drawable.ic_action_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

private data class HomeRegionalGuideBannerContent(
    val title: String,
    val description: String,
    val detail: String? = null,
    val targetId: String? = null,
    val retryable: Boolean = false,
    val actionLabel: String? = null,
    val showChevron: Boolean = false,
)

private fun HomeRegionalGuideSummaryUiState.toBannerContent(): HomeRegionalGuideBannerContent =
    when (this) {
        HomeRegionalGuideSummaryUiState.Loading ->
            HomeRegionalGuideBannerContent(
                title = "지역별 배출 가이드",
                description = "즐겨찾기한 지역의 오늘 배출 정보를 불러오는 중입니다.",
            )

        HomeRegionalGuideSummaryUiState.NoFavorite ->
            HomeRegionalGuideBannerContent(
                title = "지역별 배출 가이드",
                description = "지역 가이드를 즐겨찾기하면 오늘 배출 정보를 여기에서 확인할 수 있어요.",
            )

        is HomeRegionalGuideSummaryUiState.Summary ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "오늘 배출: $wasteTypesText",
                detail =
                    buildList {
                        add("요일 $disposalDays")
                        disposalTime?.let { add("시간 $it") }
                    }.joinToString(" · "),
                targetId = targetId,
                actionLabel = "지역 가이드 상세 보기",
                showChevron = true,
            )

        is HomeRegionalGuideSummaryUiState.NoTodaySchedule ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "오늘 배출 가능한 일정이 없어요.",
                detail = "상세 배출 기준을 확인하려면 눌러 보세요.",
                targetId = targetId,
                actionLabel = "지역 가이드 상세 보기",
                showChevron = true,
            )

        is HomeRegionalGuideSummaryUiState.ScheduleNeedsConfirmation ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "배출 요일 정보 확인이 필요해요.",
                detail = "상세 안내에서 배출 기준을 확인해 주세요.",
                targetId = targetId,
                actionLabel = "지역 가이드 상세 보기",
                showChevron = true,
            )

        is HomeRegionalGuideSummaryUiState.FavoriteRestoreFailed ->
            HomeRegionalGuideBannerContent(
                title = "지역별 배출 가이드",
                description = "저장된 지역 정보를 다시 확인해 주세요.",
            )

        is HomeRegionalGuideSummaryUiState.LoadFailed ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "최신 배출 정보를 불러오지 못했어요.",
                detail = "눌러서 다시 시도해 주세요.",
                targetId = targetId,
                retryable = true,
                actionLabel = "배출 정보 다시 불러오기",
            )
    }
