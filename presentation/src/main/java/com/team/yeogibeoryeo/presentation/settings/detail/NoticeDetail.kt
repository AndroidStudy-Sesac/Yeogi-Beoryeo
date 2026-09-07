package com.team.yeogibeoryeo.presentation.settings.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.notice.model.Notice
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.settings.SettingsLayoutDefaults
import com.team.yeogibeoryeo.presentation.settings.SettingsNoticeUiState
import com.team.yeogibeoryeo.presentation.settings.components.SettingsDetailContent
import com.team.yeogibeoryeo.presentation.settings.components.SettingsParagraph
import com.team.yeogibeoryeo.presentation.settings.components.SettingsSection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun NoticeDetail(
    uiState: SettingsNoticeUiState,
    onNoticeClick: (String) -> Unit,
    onRetryClick: () -> Unit,
) {
    when (uiState) {
        SettingsNoticeUiState.Loading -> NoticeLoading()
        SettingsNoticeUiState.LoadFailed -> NoticeLoadFailed(onRetryClick = onRetryClick)
        is SettingsNoticeUiState.Content -> NoticeContent(
            state = uiState,
            onNoticeClick = onNoticeClick,
        )
    }
}

@Composable
private fun NoticeLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NoticeLoadFailed(
    onRetryClick: () -> Unit,
) {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_notice_load_failed_title),
            description = stringResource(R.string.settings_notice_load_failed_description),
        )
        Button(onClick = onRetryClick) {
            Text(text = stringResource(R.string.retry_action))
        }
    }
}

@Composable
private fun NoticeContent(
    state: SettingsNoticeUiState.Content,
    onNoticeClick: (String) -> Unit,
) {
    val selectedNotice = state.selectedNotice
    when {
        selectedNotice != null -> NoticeBody(notice = selectedNotice)
        state.notices.isEmpty() -> NoticeEmpty()
        else -> NoticeList(
            state = state,
            onNoticeClick = onNoticeClick,
        )
    }
}

@Composable
private fun NoticeEmpty() {
    SettingsDetailContent {
        SettingsSection(
            title = stringResource(R.string.settings_notice_empty_title),
            description = stringResource(R.string.settings_notice_empty_description),
        )
    }
}

@Composable
private fun NoticeList(
    state: SettingsNoticeUiState.Content,
    onNoticeClick: (String) -> Unit,
) {
    val unreadStateDescription = stringResource(R.string.settings_notice_item_unread_state)

    Column(modifier = Modifier.fillMaxWidth()) {
        state.notices.forEach { notice ->
            val isUnread = state.isUnread(notice.id)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (isUnread) {
                            stateDescription = unreadStateDescription
                        }
                    }
                    .clickable(
                        role = Role.Button,
                        onClick = { onNoticeClick(notice.id) },
                    )
                    .padding(vertical = SettingsLayoutDefaults.listItemVerticalPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsLayoutDefaults.sectionSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        SettingsLayoutDefaults.sectionSpacing,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notice.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isUnread) {
                        Badge(modifier = Modifier.clearAndSetSemantics { })
                    }
                }
                Text(
                    text = stringResource(
                        R.string.settings_notice_published_date,
                        formatNoticeDate(notice.publishedAtMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun NoticeBody(
    notice: Notice,
) {
    SettingsDetailContent {
        Text(
            text = notice.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.settings_notice_published_date,
                formatNoticeDate(notice.publishedAtMillis),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsParagraph(text = notice.body)
    }
}

internal fun formatNoticeDate(
    publishedAtMillis: Long,
): String {
    return NOTICE_DATE_FORMATTER.format(
        Instant.ofEpochMilli(publishedAtMillis)
            .atZone(ZoneId.systemDefault()),
    )
}

private val NOTICE_DATE_FORMATTER = DateTimeFormatter.ofPattern(
    "yyyy.MM.dd",
    Locale.KOREA,
)
