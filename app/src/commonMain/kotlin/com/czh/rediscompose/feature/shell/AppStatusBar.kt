package com.czh.rediscompose.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.app.AppPage
import com.czh.rediscompose.app.AppState
import com.czh.rediscompose.app.SessionStatus
import com.czh.rediscompose.app.SettingsSaveStatus
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.formatCount

@Composable
fun AppStatusBar(
    state: AppState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val connection = state.activeConnection
    val session = state.activeSession()
    val summary = session?.summary
    val status = session?.status ?: SessionStatus.DISCONNECTED
    val statusColor =
        when (status) {
            SessionStatus.CONNECTED -> colors.success
            SessionStatus.CONNECTING -> colors.warning
            SessionStatus.ERROR -> colors.danger
            SessionStatus.DISCONNECTED -> colors.textQuaternary
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(dimens.statusBarHeight)
                .background(colors.surface)
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
                .padding(horizontal = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.statusBarGap),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Box(
                Modifier
                    .size(dimens.tinyDotSize)
                    .background(statusColor, CircleShape),
            )
            Text(
                text = status.displayName(),
                style = typography.monoMeta,
                color = colors.textSecondary,
            )
        }
        if (connection != null) {
            StatusItem("${connection.host}:${connection.port}")
            if (session != null) StatusItem("db${session.currentDatabase}")
            if (summary?.totalKeys != null) {
                StatusItem(icon = AppIcons.Layers, text = "${formatCount(summary.totalKeys)} keys")
            }
            if (summary?.usedMemoryBytes != null) {
                val memoryText =
                    if (summary.maxMemoryBytes != null && summary.maxMemoryBytes > 0) {
                        "${formatBytes(summary.usedMemoryBytes)} / ${formatBytes(summary.maxMemoryBytes)}"
                    } else {
                        formatBytes(summary.usedMemoryBytes)
                    }
                StatusItem(icon = AppIcons.Memory, text = memoryText)
            }
            if (summary?.connectedClients != null) {
                StatusItem(icon = AppIcons.Clients, text = "${summary.connectedClients} clients")
            }
            session?.latencyMillis?.let { latency ->
                StatusItem(icon = AppIcons.Zap, text = "RTT $latency ms")
            }
        } else {
            Text(
                text = "选择一个连接后开始",
                style = typography.monoMeta,
                color = colors.textQuaternary,
            )
        }
        Spacer(Modifier.weight(1f))
        if (state.activePage == AppPage.SETTINGS) {
            SettingsStatus(state.settingsSaveStatus)
        }
        if (state.settings.globalReadOnly || connection?.readOnly == true) {
            StatusChip("只读")
        }
        StatusChip("UTF-8")
        summary?.version?.let { version -> StatusChip("Redis $version") }
        Text(
            text = "Redis Compose 0.1.0",
            style = typography.monoMeta,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun StatusItem(
    text: String,
    icon: ImageVector? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(dimens.iconXs),
            )
        }
        Text(
            text = text,
            style = typography.monoMeta,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsStatus(status: SettingsSaveStatus) {
    val colors = LocalAppColors.current
    val (icon, text, color) =
        when (status) {
            SettingsSaveStatus.SAVED -> Triple(AppIcons.Check, "设置已保存", colors.success)
            SettingsSaveStatus.SAVING -> Triple(AppIcons.Refresh, "设置保存中", colors.warning)
            SettingsSaveStatus.FAILED -> Triple(AppIcons.ErrorCircle, "设置未保存", colors.danger)
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spaceXs + 1.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(LocalAppDimens.current.iconXs),
        )
        Text(
            text = text,
            style = LocalAppTypography.current.monoMeta,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusChip(text: String) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusChip)
    Box(
        modifier =
            Modifier
                .height(dimens.statusChipHeight)
                .background(colors.surface2, shape)
                .border(1.dp, colors.border, shape)
                .padding(horizontal = dimens.iconTextGap),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = typography.monoMeta,
            color = colors.textTertiary,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 1440, heightDp = 30)
@Composable
private fun AppStatusBarPreview() {
    AppTheme(dark = true) {
        AppStatusBar(state = previewEmptyState())
    }
}
