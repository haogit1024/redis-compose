package com.czh.rediscompose.feature.connections

import androidx.compose.runtime.Composable
import com.czh.rediscompose.design.component.AppMenu
import com.czh.rediscompose.design.component.AppMenuItem
import com.czh.rediscompose.design.icon.AppIcons

@Composable
fun ConnectionCardMenu(
    expanded: Boolean,
    testing: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
) {
    AppMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        items =
            listOf(
                AppMenuItem("打开工作区", AppIcons.Key, onClick = onOpen),
                AppMenuItem("编辑", AppIcons.Edit, onClick = onEdit),
                AppMenuItem("复制连接", AppIcons.Copy, onClick = onDuplicate),
                AppMenuItem(
                    label = if (testing) "测试中…" else "测试连接",
                    icon = AppIcons.Wifi,
                    enabled = !testing,
                    onClick = onTest,
                ),
                AppMenuItem(
                    label = "删除",
                    icon = AppIcons.Delete,
                    danger = true,
                    separatorBefore = true,
                    onClick = onDelete,
                ),
            ),
    )
}
