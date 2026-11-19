package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.feature.insights.chart.DonutChart
import com.czh.rediscompose.util.formatBytes
import com.czh.rediscompose.util.formatCount

@Composable
fun MemoryCard(
    stats: MemoryStats,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    InsightsCard(
        title = "内存",
        description = if (stats.hasLimit) "INFO MEMORY · used vs max" else "INFO MEMORY · 未设置 maxmemory",
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier.padding(
                    start = dimens.chartCardPaddingH,
                    end = dimens.chartCardPaddingH,
                    bottom = dimens.spaceMd,
                ),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            if (stats.hasLimit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
                ) {
                    DonutChart(progress = stats.usageRatio ?: 0f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stats.usageRatio?.let { InsightsMetrics.formatPercent(it.toDouble()) } ?: "—",
                                style = typography.keyTitle,
                                color = colors.text,
                            )
                            Text(
                                text = "已使用",
                                style = typography.monoMicro,
                                color = colors.textQuaternary,
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceSm),
                    ) {
                        MetricLine(colors.accent, "已使用", formatBytes(stats.usedBytes))
                        MetricLine(colors.warning, "峰值", formatBytes(stats.peakBytes))
                        MetricLine(colors.success, "碎片率", InsightsMetrics.formatDecimal(stats.fragmentation))
                        MetricLine(colors.surfaceHover, "可用", freeMemory(stats))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceXs)) {
                    Text(
                        text = formatBytes(stats.usedBytes),
                        style = typography.kpiValue,
                        color = colors.text,
                    )
                    Text(
                        text = "未设置 maxmemory，只显示实际用量，不推算使用率。",
                        style = typography.tooltip,
                        color = colors.textTertiary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
                    MetricLine(colors.warning, "峰值", formatBytes(stats.peakBytes))
                    MetricLine(colors.success, "碎片率", InsightsMetrics.formatDecimal(stats.fragmentation))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceXs),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
            ) {
                MiniStat("峰值内存", formatBytes(stats.peakBytes), Modifier.weight(1f))
                MiniStat("淘汰键", stats.evictedKeys?.let { formatCount(it) } ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricLine(
    dotColor: Color,
    label: String,
    value: String,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(dotColor, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = typography.bodySmall,
            color = colors.textTertiary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = typography.monoMicro,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusControlSmall)
    Column(
        modifier =
            modifier
                .background(colors.surface2, shape)
                .padding(horizontal = dimens.spaceSm + 1.dp, vertical = dimens.spaceSm),
    ) {
        Text(
            text = label,
            style = typography.tooltip,
            color = colors.textQuaternary,
            maxLines = 1,
        )
        Text(
            text = value,
            style = typography.monoValue,
            color = colors.text,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun freeMemory(stats: MemoryStats): String {
    val max = stats.maxBytes ?: return "—"
    val used = stats.usedBytes ?: return "—"
    return formatBytes((max - used).coerceAtLeast(0L))
}
