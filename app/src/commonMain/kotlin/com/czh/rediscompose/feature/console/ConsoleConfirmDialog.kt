package com.czh.rediscompose.feature.console

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/** Confirmation sheet for FLUSHALL / CONFIG SET / EVAL and other high-impact commands. */
@Composable
internal fun ConsoleConfirmDialog(
    console: ConsoleState,
    modifier: Modifier = Modifier,
) {
    val pending = console.pendingDangerous ?: return
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusControl)

    AppDialog(
        visible = true,
        onDismissRequest = console::dismissDangerous,
        title = "确认执行危险命令",
        subtitle = "该命令可能清空数据或修改服务端配置，无法撤销。",
        modifier = modifier,
        footer = {
            AppButton(
                text = "取消",
                onClick = console::dismissDangerous,
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Small,
            )
            AppButton(
                text = "执行命令",
                onClick = console::confirmDangerous,
                variant = AppButtonVariant.Danger,
                size = AppButtonSize.Small,
                icon = AppIcons.Warning,
            )
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.iconTextGap),
        ) {
            Icon(
                imageVector = AppIcons.Warning,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(dimens.icon),
            )
            Text(
                text = "危险操作",
                style = typography.bodyStrong,
                color = colors.warning,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceMd)
                    .background(colors.codeBackground, shape)
                    .border(1.dp, colors.border, shape)
                    .padding(dimens.spaceMd),
        ) {
            Text(
                text = pending.command,
                style = typography.monoValue,
                color = colors.text,
            )
        }
        Column(
            modifier = Modifier.padding(top = dimens.spaceMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        ) {
            Text(
                text = "确认后将立即发送到 ${console.session.profile.host}。",
                style = typography.caption,
                color = colors.textTertiary,
            )
            Text(
                text = "可在设置 → 安全中关闭危险命令二次确认。",
                style = typography.caption,
                color = colors.textQuaternary,
            )
        }
    }
}

@Preview(widthDp = 900, heightDp = 620)
@Composable
private fun ConsoleConfirmDialogPreview() {
    AppTheme(dark = true) {
        val console =
            remember {
                previewConsoleState().also {
                    it.execute("FLUSHALL")
                }
            }
        ConsoleConfirmDialog(console = console)
    }
}
