package com.czh.rediscompose.feature.console

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppKbd
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

private data class SavedCommand(
    val command: String,
    val description: String,
)

private val SAVED_COMMANDS =
    listOf(
        SavedCommand("INFO memory", "检查内存使用"),
        SavedCommand("MEMORY DOCTOR", "内存诊断"),
        SavedCommand("SLOWLOG GET 10", "最近慢查询"),
        SavedCommand("INFO keyspace", "各 DB Key 数量"),
        SavedCommand("SCAN 0 COUNT 100", "第一批 Key"),
    )

/** Right rail: real session history, static saved commands and shortcut hints. */
@Composable
internal fun ConsoleHistoryPane(
    console: ConsoleState,
    onFillCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    LazyColumn(
        modifier =
            modifier
                .width(dimens.consoleSideWidth)
                .fillMaxHeight()
                .background(colors.surface.copy(alpha = 0.48f))
                .drawBehind {
                    drawRect(
                        color = colors.border,
                        topLeft = Offset.Zero,
                        size = Size(1.dp.toPx(), size.height),
                    )
                },
        contentPadding =
            PaddingValues(
                start = dimens.consoleSidePadding,
                end = dimens.consoleSidePadding,
                top = dimens.spaceMd,
                bottom = dimens.spaceXl,
            ),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item(key = "history-head") {
            SideHeader(title = "历史记录", count = console.historyItems.size)
        }
        if (console.historyItems.isEmpty()) {
            item(key = "history-empty") {
                Text(
                    text = "还没有历史记录。执行命令后会显示在这里。",
                    style = LocalAppTypography.current.caption,
                    color = colors.textQuaternary,
                    modifier = Modifier.padding(horizontal = dimens.spaceSm, vertical = dimens.spaceSm),
                )
            }
        } else {
            itemsIndexed(
                items = console.historyItems,
                key = { index, item -> "$index:${item.at}:${item.command}" },
            ) { _, item ->
                HistoryRow(item = item, onFill = { onFillCommand(item.command) })
            }
        }
        item(key = "saved-head") {
            SideHeader(title = "收藏的命令", modifier = Modifier.padding(top = dimens.spaceMd))
        }
        items(
            items = SAVED_COMMANDS,
            key = { command -> "saved:${command.command}" },
        ) { saved ->
            SavedCommandRow(saved = saved, onFill = { onFillCommand(saved.command) })
        }
        item(key = "tip") {
            TipCard()
        }
    }
}

@Composable
private fun SideHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.spaceXs, vertical = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
    ) {
        Text(
            text = title,
            style = LocalAppTypography.current.microLabel,
            color = colors.textQuaternary,
        )
        if (count != null) {
            AppBadge(text = count.toString())
        }
    }
}

@Composable
private fun HistoryRow(
    item: ConsoleHistoryItem,
    onFill: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dimens.radiusXs))
                .background(if (hovered) colors.surface2 else Color.Transparent)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onFill,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceXs + 1.dp, vertical = dimens.spaceXs + 2.dp),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.at.take(5),
            style = typography.monoMicro,
            color = colors.textQuaternary,
            modifier = Modifier.width(dimens.historyTimeWidth),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.command,
                style = typography.monoKeyRow,
                color = if (hovered) colors.text else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm - 1.dp),
            ) {
                if (item.durationMillis != null) {
                    Text(
                        text = if (item.durationMillis <= 0) "<1 ms" else "${item.durationMillis} ms",
                        style = typography.terminalSmall,
                        color = if (item.failed) colors.danger else colors.success,
                        maxLines = 1,
                    )
                }
                Text(
                    text = item.summary,
                    style = typography.terminalSmall,
                    color = if (item.failed) colors.danger else colors.textQuaternary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SavedCommandRow(
    saved: SavedCommand,
    onFill: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusSm)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = dimens.spaceXs + 1.dp)
                .clip(shape)
                .background(if (hovered) colors.surface2 else colors.surface)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onFill,
                )
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceSm + 1.dp, vertical = dimens.spaceSm),
    ) {
        Text(
            text = saved.command,
            style = typography.monoKeyRow,
            color = if (hovered) colors.text else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = saved.description,
            style = typography.terminalSmall,
            color = colors.textQuaternary,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

@Composable
private fun TipCard() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceMd)
                .background(colors.accentSofter, RoundedCornerShape(dimens.radiusSm))
                .padding(dimens.spaceSm + 1.dp),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Icon(
                imageVector = AppIcons.Sparkles,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimens.iconXs),
            )
            Text(
                text = "控制台提示",
                style = typography.caption,
                color = colors.text,
            )
        }
        Text(
            text = "使用 ↑ ↓ 浏览历史，Tab 接受补全。命令错误会作为终端结果展示，不会弹出 toast。",
            style = typography.terminalSmall,
            color = colors.textTertiary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
            AppKbd("⌘K")
            Text(
                text = "命令面板",
                style = typography.terminalSmall,
                color = colors.textTertiary,
            )
        }
    }
}

@Preview(widthDp = 272, heightDp = 720)
@Composable
private fun ConsoleHistoryPanePreview() {
    AppTheme(dark = true) {
        val console = remember { previewConsoleState() }
        ConsoleHistoryPane(
            console = console,
            onFillCommand = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
