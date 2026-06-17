package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.search.model.HomeRegionalGuideSummaryUiState

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
                        content.retryable -> Modifier.clickable { onRetryClick() }
                        content.targetId != null -> Modifier.clickable { onClick(content.targetId) }
                        else -> Modifier
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = content.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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
            )

        is HomeRegionalGuideSummaryUiState.NoTodaySchedule ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "오늘 배출 가능한 일정이 없어요.",
                detail = "상세 배출 기준을 확인하려면 눌러 보세요.",
                targetId = targetId,
            )

        is HomeRegionalGuideSummaryUiState.FavoriteRestoreFailed ->
            HomeRegionalGuideBannerContent(
                title = "지역별 배출 가이드",
                description = "저장된 지역 정보를 다시 확인해 주세요.",
                targetId = targetId,
            )

        is HomeRegionalGuideSummaryUiState.LoadFailed ->
            HomeRegionalGuideBannerContent(
                title = regionName,
                description = "최신 배출 정보를 불러오지 못했어요.",
                detail = "눌러서 다시 시도해 주세요.",
                targetId = targetId,
                retryable = true,
            )
    }
