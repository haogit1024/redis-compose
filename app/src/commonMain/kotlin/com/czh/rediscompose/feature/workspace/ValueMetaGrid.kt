package com.czh.rediscompose.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.AppTheme
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.domain.key.KeyDetail
import com.czh.rediscompose.domain.key.KeyMetadata
import com.czh.rediscompose.domain.key.KeySummary
import com.czh.rediscompose.domain.key.KeyType
import com.czh.rediscompose.domain.key.KeyValue
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.formatCount
import com.czh.rediscompose.util.formatTtl

private data class MetaCell(
    val label: String,
    val icon: ImageVector,
    val value: String,
    val sub: String,
    val ttlFraction: Float? = null,
)

/** Four metadata boxes; four columns on wide panes, two on narrow ones. */
@Composable
internal fun ValueMetaGrid(
    detail: KeyDetail,
    modifier: Modifier = Modifier,
) {
    val cells = metaCells(detail.metadata, detail.summary.type)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth >= LocalAppDimens.current.metaGridFourColumnWidth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.metaGridGap),
            ) {
                cells.forEach { cell -> MetaBox(cell, Modifier.weight(1f)) }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LocalAppDimens.current.metaGridGap),
            ) {
                cells.chunked(2).forEach { rowCells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LocalAppDimens.current.metaGridGap),
                    ) {
                        rowCells.forEach { cell -> MetaBox(cell, Modifier.weight(1f)) }
                        if (rowCells.size == 1) {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaBox(
    cell: MetaCell,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.metaBoxRadius)
    Box(
        modifier =
            modifier
                .background(colors.surface, shape)
                .border(1.dp, colors.border, shape),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.metaBoxPaddingH, vertical = dimens.metaBoxPaddingV),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            ) {
                Icon(
                    imageVector = cell.icon,
                    contentDescription = null,
                    tint = colors.textQuaternary,
                    modifier = Modifier.size(dimens.treeSectionIconSize),
                )
                Text(
                    text = cell.label.uppercase(),
                    style = typography.metaLabel,
                    color = colors.textQuaternary,
                    maxLines = 1,
                )
            }
            Text(
                text = cell.value,
                style = typography.metaValue,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = dimens.spaceXs + 1.dp),
            )
            Text(
                text = cell.sub,
                style = typography.monoMicro,
                color = colors.textQuaternary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (cell.ttlFraction != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(dimens.metaTrackHeight)
                        .background(colors.surface3),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(cell.ttlFraction)
                            .height(dimens.metaTrackHeight)
                            .background(
                                Brush.horizontalGradient(listOf(colors.warning, colors.typeZset)),
                            ),
                )
            }
        }
    }
}

private fun metaCells(
    metadata: KeyMetadata,
    type: KeyType,
): List<MetaCell> {
    val ttl = metadata.ttlSeconds
    val ttlCell =
        MetaCell(
            label = "TTL",
            icon = AppIcons.Clock,
            value = formatTtl(ttl),
            sub =
                when {
                    ttl == -1L -> "无过期时间"
                    ttl >= 0L -> "剩余生存时间"
                    else -> "Key 已过期"
                },
            ttlFraction = if (ttl > 0) (ttl / 3_600.0).coerceIn(0.06, 1.0).toFloat() else null,
        )
    val memoryCell =
        MetaCell(
            label = "MEMORY",
            icon = AppIcons.Memory,
            value = formatBytes(metadata.memoryBytes),
            sub = "MEMORY USAGE",
        )
    val encodingCell =
        MetaCell(
            label = "ENCODING",
            icon = AppIcons.Code,
            value = metadata.encoding ?: "—",
            sub = "OBJECT ENCODING",
        )
    val countCell =
        when (type) {
            KeyType.STRING ->
                MetaCell(
                    label = "VALUE SIZE",
                    icon = AppIcons.Code,
                    value = formatBytes(metadata.elementCount),
                    sub = "STRLEN",
                )
            KeyType.HASH ->
                MetaCell(
                    label = "FIELDS",
                    icon = AppIcons.Hash,
                    value = formatCount(metadata.elementCount ?: 0),
                    sub = "HLEN",
                )
            KeyType.LIST ->
                MetaCell(
                    label = "LENGTH",
                    icon = AppIcons.List,
                    value = formatCount(metadata.elementCount ?: 0),
                    sub = "LLEN",
                )
            KeyType.SET ->
                MetaCell(
                    label = "MEMBERS",
                    icon = AppIcons.Layers,
                    value = formatCount(metadata.elementCount ?: 0),
                    sub = "SCARD",
                )
            KeyType.ZSET ->
                MetaCell(
                    label = "MEMBERS",
                    icon = AppIcons.Sort,
                    value = formatCount(metadata.elementCount ?: 0),
                    sub = "ZCARD",
                )
            KeyType.STREAM ->
                MetaCell(
                    label = "ENTRIES",
                    icon = AppIcons.Timeline,
                    value = formatCount(metadata.elementCount ?: 0),
                    sub = "XLEN",
                )
            KeyType.NONE ->
                MetaCell(
                    label = "VALUE",
                    icon = AppIcons.Info,
                    value = "—",
                    sub = "—",
                )
        }
    return listOf(ttlCell, memoryCell, encodingCell, countCell)
}

@Preview(widthDp = 760)
@Composable
private fun ValueMetaGridPreview() {
    AppTheme(dark = true) {
        ValueMetaGrid(
            detail =
                KeyDetail(
                    summary = KeySummary("cache:user:1001:profile", KeyType.HASH, 1_694),
                    metadata = KeyMetadata(ttlSeconds = 1_694, memoryBytes = 1_433, encoding = "listpack", elementCount = 8),
                    value = KeyValue.HashValue(emptyList()),
                ),
            modifier = Modifier.padding(LocalAppDimens.current.spaceLg),
        )
    }
}
