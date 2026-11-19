package com.czh.rediscompose.feature.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.component.AppDialog
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.ShortcutLabels

/** Page header above the terminal. */
@Composable
internal fun ConsoleToolbar(
    console: ConsoleState,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Console,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(dimens.iconSm + 1.dp),
        )
        Text(
            text = "Command console",
            style = typography.bodyStrong,
            color = colors.text,
        )
        Text(
            text = "${console.session.profile.host}:${console.session.profile.port} / db${console.session.currentDatabase}",
            style = typography.monoMicro,
            color = colors.textQuaternary,
            modifier = Modifier.padding(start = dimens.spaceSm - 1.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (console.isReadOnly) {
                AppBadge(text = "只读", tone = AppBadgeTone.Warning, icon = AppIcons.Shield)
            }
            val latency = console.session.latencyMillis
            if (latency != null) {
                AppBadge(text = "RTT $latency ms", tone = AppBadgeTone.Success, icon = AppIcons.Zap)
            } else {
                AppBadge(text = "RTT —", tone = AppBadgeTone.Plain)
            }
            AppTooltip("命令帮助") {
                AppIconButton(
                    icon = AppIcons.Book,
                    contentDescription = "命令帮助",
                    onClick = onHelp,
                )
            }
        }
    }
}

/** Shortcut and safety reference; intentionally not a full Redis manual. */
@Composable
internal fun ConsoleHelpDialog(
    visible: Boolean,
    readOnly: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    AppDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "命令帮助",
        subtitle = "常用快捷键与只读规则",
        width = dimens.dialogWideWidth,
    ) {
        HelpRow(keys = "Enter", text = "执行当前输入")
        HelpRow(keys = ShortcutLabels.commandEnter(), text = "执行当前输入")
        HelpRow(keys = "↑ / ↓", text = "在历史命令中移动")
        HelpRow(keys = "Tab", text = "接受第一条命令补全")
        HelpRow(keys = "Esc", text = "关闭补全弹层")
        HelpRow(keys = ShortcutLabels.commandKey("K"), text = "打开全局命令面板")
        Text(
            text =
                if (readOnly) {
                    "当前为只读模式：SET / DEL / FLUSHDB 等写命令和未知命令会被拦截，GET / INFO / SCAN 等读命令正常执行。PUBLISH 会被允许，但仍会向频道发送消息。"
                } else {
                    "命令会直接发送到当前 Redis 连接。只读模式只拦截写命令；命令错误会以 (error) 显示在终端中，不会弹出 toast。"
                },
            style = typography.caption,
            color = if (readOnly) colors.warning else colors.textTertiary,
            modifier = Modifier.padding(top = dimens.spaceMd),
        )
    }
}

@Composable
private fun HelpRow(
    keys: String,
    text: String,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.spaceSm),
    ) {
        AppKbd(keys)
        Text(text = text, style = typography.caption, color = colors.textSecondary)
    }
}

@Preview(widthDp = 760)
@Composable
private fun ConsoleToolbarPreview() {
    AppTheme(dark = true) {
        ConsoleToolbar(
            console = remember { previewConsoleState() },
            onHelp = {},
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
