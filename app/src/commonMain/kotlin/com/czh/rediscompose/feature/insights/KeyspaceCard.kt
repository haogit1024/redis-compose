package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppBadge
import com.czh.rediscompose.design.component.AppBadgeTone
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.formatCount
import com.czh.rediscompose.util.formatTtlCompact

@Composable
fun KeyspaceCard(
    keyspace: KeyspaceData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    InsightsCard(
        title = "键空间",
        description = "INFO KEYSPACE",
        modifier = modifier,
        actions = {
            AppBadge(
                text = "${keyspace.nonEmptyCount} DB",
                tone = AppBadgeTone.Plain,
                icon = AppIcons.Layers,
            )
        },
    ) {
        if (!keyspace.available) {
            InsightsEmptyHint(
                text = "INFO 未返回 keyspace",
                description = "服务端可能尚未创建任何数据库",
                icon = AppIcons.Layers,
            )
        } else {
            val visibleRows = InsightsMetrics.visibleKeyspaceRows(keyspace.rows)
            Column {
                TableHeader()
                visibleRows.forEach { row ->
                    KeyspaceRowView(row)
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
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
        HeaderCell("DB", 0.7f, TextAlign.Start, typography.tableHeader, colors.textQuaternary)
        HeaderCell("KEYS", 1f, TextAlign.End, typography.tableHeader, colors.textQuaternary)
        HeaderCell("EXPIRES", 1f, TextAlign.End, typography.tableHeader, colors.textQuaternary)
        HeaderCell("TTL 均值", 1f, TextAlign.End, typography.tableHeader, colors.textQuaternary)
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
    style: TextStyle,
    color: Color,
) {
    Text(
        text = text,
        style = style,
        color = color,
        textAlign = align,
        maxLines = 1,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun KeyspaceRowView(row: KeyspaceRow) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val active = row.keys > 0L
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(dimens.miniTableRowHeight)
                .padding(horizontal = dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "db${row.databaseIndex}",
            style = typography.tableText,
            color = if (active) colors.text else colors.textTertiary,
            modifier = Modifier.weight(0.7f),
        )
        Text(
            text = formatCount(row.keys),
            style = typography.tableText,
            color = if (active) colors.textSecondary else colors.textQuaternary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatCount(row.expires),
            style = typography.tableText,
            color = if (active) colors.textSecondary else colors.textQuaternary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (row.avgTtlMillis > 0L) formatTtlCompact(row.avgTtlMillis / 1_000L) else "—",
            style = typography.tableText,
            color = if (active) colors.textSecondary else colors.textQuaternary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
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
