package com.czh.rediscompose.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppVerticalScrollbar
import com.czh.rediscompose.design.icon.AppIcons
import com.czh.rediscompose.design.theme.LocalAppColors
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.design.theme.LocalAppTypography

/**
 * Server insights page.
 *
 * Polling is owned by [InsightsState] but tied to this composition: leaving the tab stops
 * the loop, returning starts it again with the samples that were already collected.
 */
@Composable
fun InsightsScreen(
    state: InsightsState,
    modifier: Modifier = Modifier,
    onOpenConsole: () -> Unit = {},
) {
    DisposableEffect(state) {
        state.start()
        onDispose { state.stop() }
    }
    InsightsDashboard(
        data = state.dashboardData(),
        onRangeChange = state::selectRange,
        onAutoRefreshChange = state::updateAutoRefresh,
        onRefresh = state::refreshNow,
        onCopyCommand = { command ->
            state.reportCopied()
        },
        onOpenConsole = onOpenConsole,
        modifier = modifier,
    )
}

@Composable
internal fun InsightsDashboard(
    data: InsightsDashboardData,
    onRangeChange: (InsightsRange) -> Unit,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onCopyCommand: (String) -> Unit,
    onOpenConsole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val dimens = LocalAppDimens.current
    val typography = LocalAppTypography.current
    val scrollState = rememberScrollState()
    val clipboard = LocalClipboardManager.current

    Box(modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = dimens.pageMargin,
                        end = dimens.pageMargin,
                        top = dimens.insightsPadding,
                        bottom = dimens.space3Xl,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = dimens.insightsMaxWidth)
                        .fillMaxWidth(),
            ) {
                InsightsHeader(
                    data = data.header,
                    onRangeChange = onRangeChange,
                    onAutoRefreshChange = onAutoRefreshChange,
                    onRefresh = onRefresh,
                )
                Spacer(Modifier.height(dimens.insightsGap + 4.dp))
                KpiGrid(kpis = data.kpis)
                Spacer(Modifier.height(dimens.insightsGap))
                MainCharts(data = data)
                Spacer(Modifier.height(dimens.insightsGap))
                BottomCharts(data = data)
                Spacer(Modifier.height(dimens.insightsGap))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if (maxWidth >= 820.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                            SlowLogCard(
                                entries = data.slowLog,
                                modifier = Modifier.weight(1f),
                                onCopyCommand = { command ->
                                    clipboard.setText(AnnotatedString(command))
                                    onCopyCommand(command)
                                },
                                onOpenConsole = onOpenConsole,
                            )
                            ClientsCard(
                                summary = data.clients,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                            SlowLogCard(
                                entries = data.slowLog,
                                modifier = Modifier.fillMaxWidth(),
                                onCopyCommand = { command ->
                                    clipboard.setText(AnnotatedString(command))
                                    onCopyCommand(command)
                                },
                                onOpenConsole = onOpenConsole,
                            )
                            ClientsCard(
                                summary = data.clients,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(dimens.spaceMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = colors.textQuaternary,
                        modifier = Modifier.size(dimens.iconXs),
                    )
                    Text(
                        text = "数据来自 INFO / COMMANDSTATS / SLOWLOG GET 10 / CLIENT LIST · 5 秒采样 · 最多保留 1 小时 · Redis 不提供历史数据",
                        style = typography.monoMicro,
                        color = colors.textQuaternary,
                        modifier = Modifier.padding(start = dimens.spaceXs),
                    )
                }
            }
        }
        AppVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun MainCharts(data: InsightsDashboardData) {
    val dimens = LocalAppDimens.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 720.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                ThroughputCard(data = data.throughput, modifier = Modifier.weight(1.65f))
                MemoryCard(stats = data.memory, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                ThroughputCard(data = data.throughput, modifier = Modifier.fillMaxWidth())
                MemoryCard(stats = data.memory, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BottomCharts(data: InsightsDashboardData) {
    val dimens = LocalAppDimens.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 820.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                CommandDistributionCard(
                    shares = data.commands,
                    sampling = data.commandsSampling,
                    modifier = Modifier.weight(1f),
                )
                KeyspaceCard(keyspace = data.keyspace, modifier = Modifier.weight(1f))
                HealthCard(items = data.health, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(dimens.insightsGap)) {
                CommandDistributionCard(
                    shares = data.commands,
                    sampling = data.commandsSampling,
                    modifier = Modifier.fillMaxWidth(),
                )
                KeyspaceCard(keyspace = data.keyspace, modifier = Modifier.fillMaxWidth())
                HealthCard(items = data.health, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
