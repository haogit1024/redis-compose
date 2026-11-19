package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTypography
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.command.SlowLogEntry
import com.czh.rediscompose.util.formatDurationMicros
import com.czh.rediscompose.util.formatEpochSeconds

@Composable
fun SlowLogCard(
    entries: List<SlowLogEntry>,
    modifier: Modifier = Modifier,
    onCopyCommand: (String) -> Unit = {},
    onOpenConsole: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    InsightsCard(
        title = "慢查询",
        description = "SLOWLOG GET 10",
        modifier = modifier,
        actions = {
            if (onOpenConsole != null) {
                AppButton(
                    text = "打开控制台",
                    onClick = onOpenConsole,
                    size = AppButtonSize.Small,
                    variant = AppButtonVariant.Ghost,
                    icon = AppIcons.Console,
                )
            }
        },
    ) {
        if (entries.isEmpty()) {
            InsightsEmptyHint(
                text = "没有慢查询记录",
                description = "SLOWLOG GET 10 返回空",
                icon = AppIcons.History,
            )
        } else {
            Column {
                HeaderRow()
                entries.take(10).forEach { entry ->
                    SlowLogRow(entry = entry, onCopy = onCopyCommand)
                }
                Text(
                    text = "点击一行复制完整命令",
                    style = LocalAppTypography.current.monoMicro,
                    color = colors.textQuaternary,
                    modifier = Modifier.padding(start = dimens.chartCardPaddingH, top = dimens.spaceSm, bottom = dimens.spaceSm),
                )
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.collectionHeaderHeight)
                .background(colors.surface2)
                .padding(horizontal = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderCell("命令", 1.7f, TextAlign.Start, typography, colors.textQuaternary)
        HeaderCell("客户端", 0.8f, TextAlign.Start, typography, colors.textQuaternary)
        HeaderCell("耗时", 0.6f, TextAlign.End, typography, colors.textQuaternary)
        HeaderCell("时间", 0.6f, TextAlign.End, typography, colors.textQuaternary)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign,
    typography: AppTypography,
    color: Color,
) {
    Text(
        text = text,
        style = typography.tableHeader,
        color = color,
        textAlign = align,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun SlowLogRow(
    entry: SlowLogEntry,
    onCopy: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val command = entry.command.joinToString(" ")
    val slow = entry.durationMicros > 10_000L
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.miniTableRowHeight)
                .background(if (hovered) colors.surface2 else colors.surface)
                .hoverable(interaction)
                .clickable { onCopy(command) }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = command,
            style = typography.tableText,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.7f),
        )
        Text(
            text = entry.clientName?.takeIf { it.isNotBlank() } ?: entry.clientAddress ?: "—",
            style = typography.tableText,
            color = colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.8f).padding(start = dimens.spaceSm),
        )
        Text(
            text = formatDurationMicros(entry.durationMicros),
            style = typography.tableText,
            color = if (slow) colors.warning else colors.textSecondary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(0.6f),
        )
        Text(
            text = formatEpochSeconds(entry.timestampEpochSeconds),
            style = typography.tableText,
            color = colors.textTertiary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(0.6f),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spaceMd)
            .height(1.dp)
            .background(colors.border),
    )
}
