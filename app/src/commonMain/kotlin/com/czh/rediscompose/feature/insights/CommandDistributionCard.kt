package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

@Composable
fun CommandDistributionCard(
    shares: List<CommandShare>,
    sampling: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    InsightsCard(
        title = "命令分布",
        description = "COMMANDSTATS · 会话内增量",
        modifier = modifier,
        actions = {
            Text(
                text = "TOP ${InsightsMetrics.COMMAND_LIMIT}",
                style = typography.monoMicro,
                color = colors.textQuaternary,
            )
        },
    ) {
        if (shares.isEmpty()) {
            InsightsEmptyHint(
                text = if (sampling) "正在采样…" else "暂无命令调用",
                description = if (sampling) "首次采样后才会出现命令增量" else "最近一次采样没有命令增量",
                icon = AppIcons.Command,
            )
        } else {
            Column(
                modifier =
                    Modifier.padding(
                        start = dimens.chartCardPaddingH,
                        end = dimens.chartCardPaddingH,
                        top = dimens.spaceXs,
                        bottom = dimens.chartCardPaddingH,
                    ),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSm + 1.dp),
            ) {
                shares.forEach { share ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm + 1.dp),
                    ) {
                        Text(
                            text = share.name,
                            style = typography.monoMeta,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(dimens.commandNameWidth),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(dimens.barTrackHeight)
                                    .background(colors.surface3, RoundedCornerShape(5.dp)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(share.share.coerceIn(0f, 1f))
                                    .background(
                                        if (share.name == "其他") colors.textTertiary else colors.accent,
                                        RoundedCornerShape(5.dp),
                                    ),
                            )
                        }
                        Text(
                            text = InsightsMetrics.formatPercent(share.share.toDouble()),
                            style = typography.monoMicro,
                            color = colors.textTertiary,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.width(dimens.commandValueWidth),
                        )
                    }
                }
            }
        }
    }
}
