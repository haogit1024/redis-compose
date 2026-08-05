package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.ButtonSize
import com.czh.redis.ui.components.ButtonVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ServerInfoPage(state: ServerInfoState, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var autoRefresh by remember { mutableStateOf(true) }

    LaunchedEffect(autoRefresh) {
        while (autoRefresh && isActive) {
            state.refresh()
            delay(5000)
        }
    }

    Column(modifier = modifier.background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.panel)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "服务器信息 · ${state.session.connection.name}",
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            AppButton(
                text = if (autoRefresh) "暂停刷新" else "自动刷新",
                variant = ButtonVariant.GHOST,
                size = ButtonSize.SMALL,
                onClick = { autoRefresh = !autoRefresh },
            )
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(
                text = "刷新",
                variant = ButtonVariant.OUTLINE,
                size = ButtonSize.SMALL,
                onClick = { state.refresh() },
            )
        }

        if (state.loading && state.sections.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "加载中...", color = colors.textSecondary, fontSize = 12.sp)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OverviewCards(state)
            Spacer(modifier = Modifier.height(12.dp))
            state.sections.forEach { (section, entries) ->
                AccordionSection(section, entries)
            }
        }
    }
}

@Composable
private fun OverviewCards(state: ServerInfoState) {
    val colors = LocalAppColors.current
    val values = state.sections.associate { (name, entries) -> name to entries.associate { it.first to it.second } }
    val server = values["Server"].orEmpty()
    val memory = values["Memory"].orEmpty()
    val stats = values["Stats"].orEmpty()
    val clients = values["Clients"].orEmpty()
    val keyspace = values["Keyspace"].orEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewCard(
            "Redis 版本",
            server["redis_version"] ?: "-",
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            "运行时长",
            formatUptime(server["uptime_in_seconds"]?.toLongOrNull() ?: 0),
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            "内存使用",
            memory["used_memory_human"] ?: "-",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewCard(
            "连接数",
            clients["connected_clients"] ?: "-",
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            "命中率",
            computeHitRate(stats),
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            "Key 总数",
            computeTotalKeys(keyspace),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .background(colors.panel, RoundedCornerShape(AppDimens.cornerLarge))
            .padding(14.dp),
    ) {
        Text(text = label, color = colors.textSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun AccordionSection(
    section: String,
    entries: List<Pair<String, String>>,
) {
    val colors = LocalAppColors.current
    var expanded by remember { mutableStateOf(section in setOf("Server", "Clients", "Memory", "Stats", "Replication", "Keyspace")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(AppDimens.cornerLarge))
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "▾" else "▸",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = section,
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "${entries.size} 项", color = colors.textSecondary, fontSize = 11.sp)
        }
        if (expanded) {
            entries.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.panelAlt)
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = key,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(220.dp),
                    )
                    Text(
                        text = value,
                        color = colors.text,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    if (seconds <= 0) return "-"
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    return if (days > 0) "${days}d ${hours}h ${minutes}m" else "${hours}h ${minutes}m"
}

private fun computeHitRate(stats: Map<String, String>): String {
    val hits = stats["keyspace_hits"]?.toLongOrNull() ?: return "-"
    val misses = stats["keyspace_misses"]?.toLongOrNull() ?: return "-"
    val total = hits + misses
    if (total == 0L) return "-"
    return String.format("%.1f%%", hits * 100.0 / total)
}

private fun computeTotalKeys(keyspace: Map<String, String>): String {
    var total = 0L
    keyspace.forEach { (key, value) ->
        if (key.startsWith("db")) {
            Regex("keys=(\\d+)").find(value)?.let { total += it.groupValues[1].toLong() }
        }
    }
    return com.czh.redis.ui.components.formatCount(total)
}
