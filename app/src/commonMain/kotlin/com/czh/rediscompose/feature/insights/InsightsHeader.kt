package com.czh.rediscompose.feature.insights

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppButton
import com.czh.rediscompose.design.component.AppButtonSize
import com.czh.rediscompose.design.component.AppButtonVariant
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppSwitch
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography
import com.czh.rediscompose.util.currentEpochMillis

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsHeader(
    data: InsightsHeaderData,
    onRangeChange: (InsightsRange) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalAppDimens.current

    Column(modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 860.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
                ) {
                    HeaderTitle(data = data, modifier = Modifier.weight(1f))
                    HeaderControls(
                        data = data,
                        onRangeChange = onRangeChange,
                        onAutoRefreshChange = onAutoRefreshChange,
                        onRefresh = onRefresh,
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    HeaderTitle(data = data)
                    HeaderControls(
                        data = data,
                        onRangeChange = onRangeChange,
                        onAutoRefreshChange = onAutoRefreshChange,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSm + 2.dp),
                        alignEnd = true,
                    )
                }
            }
        }
        if (data.error != null) {
            ErrorBanner(message = data.error, onRetry = onRefresh)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderTitle(
    data: InsightsHeaderData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val lastUpdated = InsightsMetrics.relativeMillis(data.lastUpdatedEpochMillis, currentEpochMillis())

    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            Icon(
                imageVector = AppIcons.Insights,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimens.iconSm),
            )
            Text(
                text = "OBSERVABILITY",
                style = typography.microLabel,
                color = colors.accent,
            )
        }
        Row(
            modifier = Modifier.padding(top = dimens.spaceXs + 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
        ) {
            Text(
                text = "服务器洞察",
                style = typography.pageTitle,
                color = colors.text,
            )
            LiveBadge()
        }
        FlowRow(
            modifier = Modifier.padding(top = dimens.spaceXs + 2.dp),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 2.dp),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MetaText("${data.host}:${data.port}", mono = true, strong = true)
            MetaSeparator()
            MetaText("Redis ${data.version ?: "未知"}")
            MetaSeparator()
            MetaText("运行 ${InsightsMetrics.formatUptime(data.uptimeSeconds)}")
            MetaSeparator()
            MetaText("自本次连接")
            MetaSeparator()
            MetaText("最后更新 $lastUpdated")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderControls(
    data: InsightsHeaderData,
    onRangeChange: (InsightsRange) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement =
            if (alignEnd) {
                Arrangement.spacedBy(dimens.spaceXs + 2.dp, Alignment.End)
            } else {
                Arrangement.spacedBy(dimens.spaceXs + 2.dp)
            },
        verticalArrangement = Arrangement.spacedBy(dimens.spaceXs),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (data.autoRefresh) "AUTO · 5s" else "已暂停",
            style = typography.monoMicro,
            color = colors.textQuaternary,
        )
        AppSegmented(
            options = InsightsRange.entries,
            selected = data.range,
            onSelect = onRangeChange,
            size = AppSegmentedSize.Small,
            label = { it.label },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceXs + 1.dp),
        ) {
            AppSwitch(
                checked = data.autoRefresh,
                onCheckedChange = onAutoRefreshChange,
            )
            Text(
                text = "自动刷新",
                style = typography.tooltip,
                color = colors.textTertiary,
            )
        }
        AppButton(
            text = "刷新",
            onClick = onRefresh,
            size = AppButtonSize.Small,
            variant = AppButtonVariant.Ghost,
            icon = AppIcons.Refresh,
            loading = data.isLoading,
        )
    }
}

@Composable
private fun LiveBadge() {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val transition = rememberInfiniteTransition(label = "insightsLive")
    val alpha by
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_400), repeatMode = RepeatMode.Reverse),
            label = "insightsLiveAlpha",
        )
    val shape = RoundedCornerShape(dimens.radiusChip)
    Row(
        modifier =
            Modifier
                .height(dimens.liveBadgeHeight)
                .background(colors.success.copy(alpha = 0.12f), shape)
                .border(1.dp, colors.success.copy(alpha = 0.24f), shape)
                .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(dimens.liveDotSize)
                .graphicsLayer { this.alpha = alpha }
                .background(colors.success, CircleShape),
        )
        Text(
            text = "LIVE",
            style = typography.liveBadge,
            color = colors.success,
        )
    }
}

@Composable
private fun MetaText(
    text: String,
    mono: Boolean = false,
    strong: Boolean = false,
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    Text(
        text = text,
        style = if (mono) typography.monoMeta else typography.bodySmall,
        color = if (strong) colors.textSecondary else colors.textTertiary,
        maxLines = 1,
    )
}

@Composable
private fun MetaSeparator() {
    Text(
        text = "·",
        style = LocalAppTypography.current.bodySmall,
        color = LocalAppColors.current.textQuaternary,
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val shape = RoundedCornerShape(dimens.radiusSm)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = dimens.spaceMd)
                .background(colors.danger.copy(alpha = 0.10f), shape)
                .border(1.dp, colors.danger.copy(alpha = 0.22f), shape)
                .padding(horizontal = dimens.spaceMd, vertical = dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm),
    ) {
        Icon(
            imageVector = AppIcons.Warning,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(dimens.iconSm),
        )
        Text(
            text = message,
            style = typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = "重试",
            onClick = onRetry,
            size = AppButtonSize.Small,
            variant = AppButtonVariant.Ghost,
            icon = AppIcons.Refresh,
        )
    }
}
