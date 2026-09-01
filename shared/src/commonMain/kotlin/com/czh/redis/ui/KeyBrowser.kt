package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.redis.KeyTreeNode
import com.czh.redis.redis.RedisType
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonVariant
import com.czh.redis.ui.components.TypeBadge
import com.czh.redis.ui.components.formatCount
import com.czh.redis.ui.components.rememberRightClickHandler

@Composable
fun KeyBrowserPage(
    vm: AppViewModel,
    browser: KeyBrowserState,
    session: Session,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxSize()) {
        KeyListPanel(
            vm = vm,
            browser = browser,
            modifier = Modifier.width(360.dp).fillMaxHeight().background(colors.panel),
        )
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.border),
        )
        ValuePanel(
            vm = vm,
            browser = browser,
            session = session,
            modifier = Modifier.weight(1f).fillMaxHeight().background(colors.background),
        )
    }
}

@Composable
private fun KeyListPanel(
    vm: AppViewModel,
    browser: KeyBrowserState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()

    LaunchedEffect(listState, browser.keys.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { lastIndex ->
            val total = listState.layoutInfo.totalItemsCount
            if (lastIndex >= total - 6) {
                browser.loadMore()
            }
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(
                value = browser.searchPattern,
                onValueChange = { browser.searchPattern = it },
                placeholder = "搜索 Key（支持 * 通配符）",
                mono = true,
                height = 30.dp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            AppButton(
                text = "⟳",
                variant = ButtonVariant.GHOST,
                size = com.czh.redis.ui.components.ButtonSize.SMALL,
                onClick = { browser.applySearch() },
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(AppDimens.cornerMedium))
                        .background(colors.panelAlt),
            ) {
                ViewToggleItem("树形", browser.viewMode == "tree") { browser.viewMode = "tree" }
                ViewToggleItem("平铺", browser.viewMode == "flat") { browser.viewMode = "flat" }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "已加载 ${formatCount(browser.keys.size.toLong())} / ${formatCount(browser.total)}",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val flattened =
            remember(browser.tree, browser.collapsedDirs, browser.viewMode) {
                if (browser.viewMode == "tree") flatten(browser.tree, browser.collapsedDirs) else emptyList()
            }

        if (browser.viewMode == "tree") {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(flattened.size) { index ->
                    val (node, depth) = flattened[index]
                    KeyTreeRow(
                        vm = vm,
                        browser = browser,
                        node = node,
                        depth = depth,
                    )
                }
                if (!browser.loadedAll) {
                    item { LoadingRow(browser) }
                }
                if (flattened.isEmpty() && !browser.loading) {
                    item { EmptyKeysRow(vm, browser) }
                }
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(browser.keys.size) { index ->
                    val key = browser.keys[index]
                    KeyFlatRow(vm = vm, browser = browser, key = key)
                }
                if (!browser.loadedAll) {
                    item { LoadingRow(browser) }
                }
                if (browser.keys.isEmpty() && !browser.loading) {
                    item { EmptyKeysRow(vm, browser) }
                }
            }
        }
    }
}

@Composable
private fun ViewToggleItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier =
            Modifier
                .background(if (selected) colors.panel else Color.Transparent, RoundedCornerShape(AppDimens.cornerMedium))
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colors.primary else colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun KeyTreeRow(
    vm: AppViewModel,
    browser: KeyBrowserState,
    node: KeyTreeNode,
    depth: Int,
) {
    val colors = LocalAppColors.current
    if (node.isDirectory) {
        val expanded = node.path !in browser.collapsedDirs
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clickable { browser.toggleDir(node.path) }
                    .padding(start = (8 + depth * 14).dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = if (expanded) "▾" else "▸", color = colors.textSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "📁", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = node.name,
                color = colors.text,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = "(${node.count})", color = colors.textSecondary, fontSize = 11.sp)
        }
    } else {
        KeyLeafRow(
            vm = vm,
            browser = browser,
            key = node.key ?: node.name,
            indent = (8 + depth * 14).dp,
        )
    }
}

@Composable
private fun KeyFlatRow(
    vm: AppViewModel,
    browser: KeyBrowserState,
    key: String,
) {
    KeyLeafRow(vm, browser, key, indent = 8.dp)
}

@Composable
private fun KeyLeafRow(
    vm: AppViewModel,
    browser: KeyBrowserState,
    key: String,
    indent: androidx.compose.ui.unit.Dp,
) {
    val colors = LocalAppColors.current
    val selected = browser.selectedKey == key
    val type = browser.keyTypes[key]
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    LaunchedEffect(key) {
        browser.ensureType(key)
    }

    val contextModifier =
        rememberRightClickHandler { position ->
            vm.contextMenu.show(
                position.x,
                position.y,
                listOf(
                    ContextMenuItem("复制 Key", onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(key))
                    }),
                    ContextMenuItem("重命名", onClick = { vm.renameRequest = key }),
                    ContextMenuItem("删除", onClick = {
                        vm.confirm("确定删除 Key「$key」？") { vm.deleteKey(key) }
                    }, danger = true),
                ),
            )
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(if (selected) colors.selectedBg else Color.Transparent)
                .clickable { browser.selectKey(key) }
                .then(contextModifier)
                .padding(start = indent, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(18.dp))
        TypeBadge(type ?: RedisType.NONE)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = key,
            color = colors.text,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LoadingRow(browser: KeyBrowserState) {
    val colors = LocalAppColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (browser.loading) "⏳ 加载中..." else "▾ 点击加载更多（SCAN cursor: ${browser.cursor}）",
            color = colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun EmptyKeysRow(
    vm: AppViewModel,
    browser: KeyBrowserState,
) {
    val colors = LocalAppColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "当前 DB 没有 Key", color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        AppButton(
            text = "＋ 新建 Key",
            size = com.czh.redis.ui.components.ButtonSize.SMALL,
            onClick = { vm.newKeyRequest = "new" },
        )
    }
}

fun flatten(
    nodes: List<KeyTreeNode>,
    collapsed: Set<String>,
): List<Pair<KeyTreeNode, Int>> {
    val result = mutableListOf<Pair<KeyTreeNode, Int>>()

    fun walk(
        list: List<KeyTreeNode>,
        depth: Int,
    ) {
        for (node in list) {
            result.add(node to depth)
            if (node.isDirectory && node.path !in collapsed) {
                walk(node.children, depth + 1)
            }
        }
    }
    walk(nodes, 0)
    return result
}
