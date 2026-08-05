package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.data.RedisConnection
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonVariant
import com.czh.redis.ui.components.StatusDot
import com.czh.redis.ui.components.formatCount
import com.czh.redis.ui.components.rememberRightClickHandler

@Composable
fun ConnectionPanel(vm: AppViewModel, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    var search by remember { mutableStateOf("") }

    Column(modifier = modifier.background(colors.panel)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "🔍", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            AppTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索连接...",
                height = 30.dp,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val filtered = vm.connections.filter {
                search.isBlank() || it.name.contains(search, ignoreCase = true)
            }
            val grouped = filtered.groupBy { it.group.ifBlank { null } }
            val ordered = grouped.entries.sortedBy { it.key ?: "\uFFFF" }

            for ((group, connections) in ordered) {
                if (group != null) {
                    item(key = "group-$group") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.panelAlt)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(text = "📁 $group", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                items(connections, key = { it.id }) { conn ->
                    ConnectionItem(vm, conn)
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "没有匹配的连接", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            AppButton(
                text = "＋ 新建连接",
                onClick = { vm.openNewConnection(null) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectionItem(vm: AppViewModel, conn: RedisConnection) {
    val colors = LocalAppColors.current
    val session = vm.sessions[conn.id]
    val status = session?.status ?: ConnectionStatus.DISCONNECTED
    val connected = status == ConnectionStatus.CONNECTED

    val contextModifier = rememberRightClickHandler { position ->
        vm.contextMenu.show(
            position.x,
            position.y,
            listOf(
                ContextMenuItem(
                    label = if (connected) "断开连接" else "连接",
                    onClick = {
                        if (connected) vm.disconnectSession(conn.id) else vm.openTab(conn.id, TabType.KEYS)
                    },
                ),
                ContextMenuItem("编辑", onClick = { vm.openNewConnection(conn) }),
                ContextMenuItem("复制", onClick = { vm.duplicateConnection(conn.id) }),
                ContextMenuItem("打开控制台", onClick = { vm.openTab(conn.id, TabType.CONSOLE) }),
                ContextMenuItem("服务器信息", onClick = { vm.openTab(conn.id, TabType.SERVER_INFO) }),
                ContextMenuItem(
                    "删除",
                    onClick = {
                        vm.confirm("确定删除连接「${conn.name}」？该操作不可恢复。") { vm.deleteConnection(conn.id) }
                    },
                    danger = true,
                ),
            ),
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.rowHeight)
                .background(if (connected) colors.selectedBg else Color.Transparent)
                .clickable { vm.openTab(conn.id, TabType.KEYS) }
                .then(contextModifier)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(parseColor(conn.color, colors.border)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusDot(status)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = conn.name,
                color = if (connected) colors.text else colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (connected) {
            val dbList = session?.dbs.orEmpty()
            dbList.forEach { db ->
                val activeDb = session?.currentDb == db.index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clickable { vm.switchDb(conn.id, db.index) }
                        .background(if (activeDb) colors.hover else Color.Transparent)
                        .padding(start = 38.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "db${db.index}",
                        color = if (db.keyCount > 0) colors.text else colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${formatCount(db.keyCount)} keys",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (status == ConnectionStatus.CONNECTING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 38.dp),
            ) {
                Text(text = "连接中...", color = colors.warn, fontSize = 11.sp)
            }
        }
        if (status == ConnectionStatus.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 38.dp),
            ) {
                Text(
                    text = session?.statusError?.lineSequence()?.firstOrNull() ?: "连接失败",
                    color = colors.error,
                    fontSize = 11.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

fun parseColor(hex: String, fallback: Color): Color {
    val value = hex.removePrefix("#")
    if (value.length != 6) return fallback
    val parsed = value.toLongOrNull(16) ?: return fallback
    return Color(0xFF000000L or parsed)
}
