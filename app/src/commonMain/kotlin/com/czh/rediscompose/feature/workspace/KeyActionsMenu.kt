package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.czh.rediscompose.design.component.AppIconButton
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.component.AppTooltip
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.key.KeyType

/** Redis command that opens the selected key, used by "复制为命令". */
internal fun redisCommandFor(
    key: String,
    type: KeyType?,
): String {
    val argument = if (key.any { it.isWhitespace() }) "\"$key\"" else key
    return when (type) {
        KeyType.STRING -> "GET $argument"
        KeyType.HASH -> "HGETALL $argument"
        KeyType.LIST -> "LRANGE $argument 0 -1"
        KeyType.SET -> "SMEMBERS $argument"
        KeyType.ZSET -> "ZRANGE $argument 0 -1 WITHSCORES"
        KeyType.STREAM -> "XRANGE $argument - +"
        KeyType.NONE, null -> "TYPE $argument"
    }
}

/** Pin, copy, console and the more menu shown at the right of the value header. */
@Composable
internal fun ValueHeaderActions(
    state: WorkspaceState,
    type: KeyType?,
    onCopyKey: (String) -> Unit,
    onCopyCommand: (String) -> Unit,
) {
    val key = state.selectedKey ?: return
    val pinned = state.isPinned(key)
    var menuOpen by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.valueActionsGap),
    ) {
        AppTooltip(if (pinned) "取消置顶" else "置顶 Key") {
            AppIconButton(
                icon = if (pinned) AppIcons.StarFilled else AppIcons.Star,
                contentDescription = if (pinned) "取消置顶" else "置顶 Key",
                onClick = { state.togglePinned(key) },
                tint = if (pinned) colors.warning else null,
            )
        }
        AppTooltip("复制 Key") {
            AppIconButton(
                icon = AppIcons.Copy,
                contentDescription = "复制 Key",
                onClick = { onCopyKey(key) },
            )
        }
        AppTooltip("在控制台打开") {
            AppIconButton(
                icon = AppIcons.Console,
                contentDescription = "在控制台打开",
                onClick = state::openInConsole,
            )
        }
        Box {
            AppTooltip("更多操作") {
                AppIconButton(
                    icon = AppIcons.More,
                    contentDescription = "更多操作",
                    onClick = { menuOpen = !menuOpen },
                )
            }
            AppMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                items =
                    listOf(
                        AppMenuItem(
                            label = "复制为命令",
                            icon = AppIcons.Console,
                            shortcut = "Redis",
                            onClick = { onCopyCommand(redisCommandFor(key, type)) },
                        ),
                        AppMenuItem(
                            label = "修改 TTL",
                            icon = AppIcons.Clock,
                            enabled = state.canWrite,
                            onClick = state::openTtlDialog,
                        ),
                        AppMenuItem(
                            label = "重命名",
                            icon = AppIcons.Edit,
                            enabled = state.canWrite,
                            onClick = state::openRenameDialog,
                        ),
                        AppMenuItem(
                            label = "删除 Key",
                            icon = AppIcons.Delete,
                            danger = true,
                            enabled = state.canWrite,
                            separatorBefore = true,
                            onClick = state::openDeleteDialog,
                        ),
                    ),
            )
        }
    }
}

@Preview(widthDp = 300)
@Composable
private fun ValueHeaderActionsPreview() {
    AppTheme(dark = true) {
        val state = remember { previewWorkspaceState().also { it.selectKey("cache:user:1001:profile") } }
        ValueHeaderActions(
            state = state,
            type = KeyType.HASH,
            onCopyKey = {},
            onCopyCommand = {},
        )
    }
}
