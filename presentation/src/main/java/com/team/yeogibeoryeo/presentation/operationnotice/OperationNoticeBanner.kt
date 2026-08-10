package com.team.yeogibeoryeo.presentation.operationnotice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.operationnotice.model.OperationNoticeSeverity
import com.team.yeogibeoryeo.presentation.R

@Composable
fun OperationNoticeBanner(
    notice: OperationNoticeUiModel,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = notice.severity.bannerColors()
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.containerColor,
        contentColor = colors.contentColor,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(colors.iconColor),
            )

            Row(
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = notice.severity.icon(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp),
                    tint = colors.iconColor,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (notice.actionLabel != null && notice.actionUrl != null) {
                        TextButton(
                            onClick = { uriHandler.openUri(notice.actionUrl) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) {
                            Text(text = notice.actionLabel)
                        }
                    }
                }

                if (notice.isDismissible) {
                    IconButton(
                        onClick = { onDismiss(notice.id) },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.operation_notice_dismiss_action),
                            modifier = Modifier.size(20.dp),
                            tint = colors.contentColor,
                        )
                    }
                }
            }
        }
    }
}

private data class OperationNoticeBannerColors(
    val containerColor: Color,
    val contentColor: Color,
    val iconColor: Color,
)

@Composable
private fun OperationNoticeSeverity.bannerColors(): OperationNoticeBannerColors =
    when (this) {
        OperationNoticeSeverity.INFO ->
            OperationNoticeBannerColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.primary,
            )

        OperationNoticeSeverity.WARNING ->
            OperationNoticeBannerColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.tertiary,
            )

        OperationNoticeSeverity.CRITICAL ->
            OperationNoticeBannerColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                iconColor = MaterialTheme.colorScheme.error,
            )
    }

private fun OperationNoticeSeverity.icon(): ImageVector =
    when (this) {
        OperationNoticeSeverity.INFO -> Icons.Filled.Info
        OperationNoticeSeverity.WARNING -> Icons.Filled.Warning
        OperationNoticeSeverity.CRITICAL -> Icons.Filled.ErrorOutline
    }
