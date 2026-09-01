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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.redis.RedisType
import com.czh.redis.redis.ValueCodec
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppMultilineTextField
import com.czh.redis.ui.components.AppSelect
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonSize
import com.czh.redis.ui.components.ButtonVariant
import com.czh.redis.ui.components.TypeBadge
import kotlinx.coroutines.launch

class HashRow(var originalField: String, field: String, value: String) {
    var field by mutableStateOf(field)
    var value by mutableStateOf(value)
}

class ListRow(value: String) {
    var value by mutableStateOf(value)
}

class SetRow(var originalMember: String, member: String) {
    var member by mutableStateOf(member)
}

class ZSetRow(var originalMember: String, member: String, score: String) {
    var member by mutableStateOf(member)
    var score by mutableStateOf(score)
}

class ValueState(
    private val session: Session,
    private val vm: AppViewModel,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    var key by mutableStateOf<String?>(null)
    var type by mutableStateOf<RedisType?>(null)
    var ttl by mutableStateOf(0L)
    var memory by mutableStateOf<Long?>(null)
    var loading by mutableStateOf(false)
    var formatMode by mutableStateOf("AUTO")
    var stringValue by mutableStateOf("")
    val hashRows = mutableStateListOf<HashRow>()
    val listRows = mutableStateListOf<ListRow>()
    val setRows = mutableStateListOf<SetRow>()
    val zsetRows = mutableStateListOf<ZSetRow>()

    val readonly: Boolean
        get() = session.isReadonly || vm.settings.readonly

    fun load(
        key: String,
        type: RedisType,
    ) {
        this.key = key
        this.type = type
        loading = true
        scope.launch {
            try {
                ttl = session.client.ttl(key)
                memory = runCatching { session.client.memoryUsage(key) }.getOrNull()
                when (type) {
                    RedisType.STRING -> {
                        stringValue = ValueCodec.decode(session.client.getString(key) ?: ByteArray(0)) ?: ""
                    }
                    RedisType.HASH -> {
                        hashRows.clear()
                        session.client.getHash(key).forEach { (field, value) ->
                            hashRows.add(HashRow(field, field, display(value)))
                        }
                    }
                    RedisType.LIST -> {
                        listRows.clear()
                        session.client.getList(key).forEach { listRows.add(ListRow(display(it))) }
                    }
                    RedisType.SET -> {
                        setRows.clear()
                        session.client.getSet(key).forEach { setRows.add(SetRow(display(it), display(it))) }
                    }
                    RedisType.ZSET -> {
                        zsetRows.clear()
                        session.client.getZSet(key).forEach {
                            zsetRows.add(ZSetRow(display(it.member), display(it.member), it.score.toString()))
                        }
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                vm.showToast("加载值失败：${e.message}", ToastType.ERROR)
            }
            loading = false
        }
    }

    fun refreshMeta() {
        val currentKey = key ?: return
        scope.launch {
            runCatching {
                ttl = session.client.ttl(currentKey)
                memory = runCatching { session.client.memoryUsage(currentKey) }.getOrNull()
            }
        }
    }

    fun saveString() {
        val currentKey = key ?: return
        scope.launch {
            try {
                session.client.setString(currentKey, stringValue.toByteArray())
                vm.showToast("已保存 $currentKey", ToastType.SUCCESS)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("保存失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun formatJson() {
        if (ValueCodec.isJson(stringValue)) {
            stringValue = ValueCodec.prettyJson(stringValue)
        }
    }

    fun hashSave(row: HashRow) {
        val currentKey = key ?: return
        scope.launch {
            try {
                if (row.field != row.originalField && row.originalField.isNotEmpty()) {
                    session.client.hDel(currentKey, row.originalField)
                }
                session.client.hSet(currentKey, row.field, row.value.toByteArray())
                row.originalField = row.field
                vm.showToast("已保存字段 ${row.field}", ToastType.SUCCESS)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("保存失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun hashAdd() {
        hashRows.add(HashRow("", "", ""))
    }

    fun hashDelete(row: HashRow) {
        val currentKey = key ?: return
        scope.launch {
            try {
                if (row.originalField.isNotEmpty()) session.client.hDel(currentKey, row.originalField)
                hashRows.remove(row)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("删除失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun listAdd(head: Boolean) {
        val currentKey = key ?: return
        val row = ListRow("")
        scope.launch {
            try {
                session.client.listPush(currentKey, ByteArray(0), head)
                listRows.add(0, row)
                vm.showToast(if (head) "已在头部插入空成员，请编辑后保存" else "已在尾部插入空成员，请编辑后保存", ToastType.INFO)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("操作失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun listSave(
        row: ListRow,
        index: Int,
    ) {
        val currentKey = key ?: return
        scope.launch {
            try {
                session.client.listSet(currentKey, index.toLong(), row.value.toByteArray())
                vm.showToast("已保存 index $index", ToastType.SUCCESS)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("保存失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun listDelete(row: ListRow) {
        val currentKey = key ?: return
        scope.launch {
            try {
                session.client.listRemove(currentKey, row.value.toByteArray())
                listRows.remove(row)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("删除失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun setAdd() {
        val currentKey = key ?: return
        scope.launch {
            try {
                setRows.add(SetRow("", ""))
                vm.showToast("请填写新成员并保存", ToastType.INFO)
            } catch (e: Exception) {
                vm.showToast("操作失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun setSave(row: SetRow) {
        val currentKey = key ?: return
        if (row.member.isBlank()) return
        scope.launch {
            try {
                if (row.originalMember.isNotEmpty() && row.member != row.originalMember) {
                    session.client.setRemove(currentKey, row.originalMember.toByteArray())
                }
                session.client.setAdd(currentKey, row.member.toByteArray())
                row.originalMember = row.member
                vm.showToast("已保存成员 ${row.member}", ToastType.SUCCESS)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("保存失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun setDelete(row: SetRow) {
        val currentKey = key ?: return
        scope.launch {
            try {
                session.client.setRemove(currentKey, row.member.toByteArray())
                setRows.remove(row)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("删除失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun zsetAdd() {
        zsetRows.add(ZSetRow("", "", ""))
    }

    fun zsetSave(row: ZSetRow) {
        val currentKey = key ?: return
        val score =
            row.score.toDoubleOrNull() ?: run {
                vm.showToast("score 必须是数字", ToastType.WARNING)
                return
            }
        scope.launch {
            try {
                if (row.originalMember.isNotEmpty() && row.member != row.originalMember) {
                    session.client.zRemove(currentKey, row.originalMember.toByteArray())
                }
                session.client.zAdd(currentKey, row.member.toByteArray(), score)
                row.originalMember = row.member
                vm.showToast("已保存成员 ${row.member}", ToastType.SUCCESS)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("保存失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    fun zsetDelete(row: ZSetRow) {
        val currentKey = key ?: return
        scope.launch {
            try {
                session.client.zRemove(currentKey, row.member.toByteArray())
                zsetRows.remove(row)
                refreshMeta()
            } catch (e: Exception) {
                vm.showToast("删除失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    private fun display(bytes: ByteArray): String = ValueCodec.decode(bytes) ?: ValueCodec.hex(bytes)
}

@Composable
fun ValuePanel(
    vm: AppViewModel,
    browser: KeyBrowserState,
    session: Session,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectedKey = browser.selectedKey
    val selectedType = browser.selectedType

    if (selectedKey == null || selectedType == null) {
        EmptyValueHint(modifier = modifier)
        return
    }

    val state = remember(selectedKey) { ValueState(session, vm, scope) }
    LaunchedEffect(selectedKey, selectedType) { state.load(selectedKey, selectedType) }

    Column(modifier = modifier) {
        ValueHeader(state = state, vm = vm, browser = browser)
        when (selectedType) {
            RedisType.STRING -> StringEditor(state)
            RedisType.HASH -> HashEditor(state)
            RedisType.LIST -> ListEditor(state)
            RedisType.SET -> SetEditor(state)
            RedisType.ZSET -> ZSetEditor(state)
            else ->
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "该类型暂不支持编辑", color = com.czh.redis.ui.LocalAppColors.current.textSecondary, fontSize = 12.sp)
                }
        }
    }
}

@Composable
private fun EmptyValueHint(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(colors.primary, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "R", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "从左侧选择一个 Key 查看/编辑值", color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ValueHeader(
    state: ValueState,
    vm: AppViewModel,
    browser: KeyBrowserState,
) {
    val colors = LocalAppColors.current
    val key = state.key ?: return
    val type = state.type ?: return

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.panel)
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TypeBadge(type)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = key,
                color = colors.text,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetaChip("TTL：${state.ttl}s")
            state.memory?.let { MetaChip("内存：${formatBytes(it)}") }
            AppButton(
                text = "修改 TTL",
                variant = ButtonVariant.OUTLINE,
                size = ButtonSize.SMALL,
                onClick = {
                    vm.ttlRequestInitial = state.ttl
                    vm.ttlRequest = key
                },
            )
            AppButton(
                text = "重命名",
                variant = ButtonVariant.OUTLINE,
                size = ButtonSize.SMALL,
                onClick = { vm.renameRequest = key },
            )
            AppButton(
                text = "刷新",
                variant = ButtonVariant.OUTLINE,
                size = ButtonSize.SMALL,
                onClick = { state.load(key, type) },
            )
            AppButton(
                text = "删除",
                variant = ButtonVariant.DANGER,
                size = ButtonSize.SMALL,
                onClick = {
                    vm.confirm("确定删除 Key「$key」？") { vm.deleteKey(key) }
                },
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    val colors = LocalAppColors.current
    Box(
        modifier =
            Modifier
                .background(colors.panelAlt, RoundedCornerShape(AppDimens.cornerMedium))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, color = colors.textSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ToolbarRow(
    title: String,
    actionText: String,
    onAction: () -> Unit,
    state: ValueState,
    extra: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        extra?.invoke()
        if (!state.readonly && actionText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            AppButton(
                text = actionText,
                variant = ButtonVariant.SOLID,
                size = ButtonSize.SMALL,
                onClick = onAction,
            )
        }
    }
}

@Composable
private fun StringEditor(state: ValueState) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppSelect(
                options = listOf("AUTO", "TEXT", "JSON", "HEX"),
                selected = state.formatMode,
                onSelect = { state.formatMode = it },
                height = 30.dp,
                modifier = Modifier.width(140.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            if (!state.readonly) {
                AppButton(
                    text = "JSON 格式化",
                    variant = ButtonVariant.OUTLINE,
                    size = ButtonSize.SMALL,
                    onClick = { state.formatJson() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(
                    text = "保存",
                    variant = ButtonVariant.SOLID,
                    size = ButtonSize.SMALL,
                    onClick = { state.saveString() },
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val displayed = ValueCodec.format(state.stringValue.toByteArray(), state.formatMode)
        AppMultilineTextField(
            value = displayed,
            onValueChange = {
                state.stringValue =
                    if (state.formatMode == "HEX") {
                        it.replace(" ", "").trim()
                    } else {
                        it
                    }
            },
            mono = true,
            minHeight = 220.dp,
        )
    }
}

@Composable
private fun HashEditor(state: ValueState) {
    val colors = LocalAppColors.current
    var search by remember { mutableStateOf("") }
    val rows = state.hashRows.filter { search.isBlank() || it.field.contains(search) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ToolbarRow(
            title = "Hash 字段",
            actionText = "＋ 新增字段",
            onAction = { state.hashAdd() },
            state = state,
        ) {
            AppTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "搜索字段...",
                height = 28.dp,
                modifier = Modifier.width(180.dp),
            )
        }
        TableHeader(listOf("Field", "Value", ""))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.panel),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextField(
                        value = row.field,
                        onValueChange = { row.field = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AppTextField(
                        value = row.value,
                        onValueChange = { row.value = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(2f),
                    )
                    RowActionButtons(
                        onSave = { state.hashSave(row) },
                        onDelete = { state.hashDelete(row) },
                        enabled = !state.readonly,
                    )
                }
            }
            if (rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = "暂无字段", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ListEditor(state: ValueState) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        ToolbarRow(
            title = "List 成员",
            actionText = "",
            onAction = {},
            state = state,
        ) {
            if (!state.readonly) {
                AppButton(text = "＋ LPUSH", variant = ButtonVariant.OUTLINE, size = ButtonSize.SMALL, onClick = { state.listAdd(true) })
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(text = "＋ RPUSH", variant = ButtonVariant.OUTLINE, size = ButtonSize.SMALL, onClick = { state.listAdd(false) })
            }
        }
        TableHeader(listOf("Index", "Value", ""))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.listRows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.panel),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = index.toString(),
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(50.dp),
                    )
                    AppTextField(
                        value = row.value,
                        onValueChange = { row.value = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(1f),
                    )
                    RowActionButtons(
                        onSave = { state.listSave(row, index) },
                        onDelete = { state.listDelete(row) },
                        enabled = !state.readonly,
                    )
                }
            }
            if (state.listRows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = "暂无成员", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SetEditor(state: ValueState) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        ToolbarRow(
            title = "Set 成员",
            actionText = "＋ 添加成员",
            onAction = { state.setAdd() },
            state = state,
        )
        TableHeader(listOf("Member", ""))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.setRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.panel),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextField(
                        value = row.member,
                        onValueChange = { row.member = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(1f),
                    )
                    RowActionButtons(
                        onSave = { state.setSave(row) },
                        onDelete = { state.setDelete(row) },
                        enabled = !state.readonly,
                    )
                }
            }
            if (state.setRows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = "暂无成员", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ZSetEditor(state: ValueState) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        ToolbarRow(
            title = "ZSet 成员（按 score 排序）",
            actionText = "＋ 添加成员",
            onAction = { state.zsetAdd() },
            state = state,
        )
        TableHeader(listOf("Member", "Score", ""))
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.zsetRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.panel),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextField(
                        value = row.member,
                        onValueChange = { row.member = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(2f),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AppTextField(
                        value = row.score,
                        onValueChange = { row.score = it },
                        mono = true,
                        height = 28.dp,
                        modifier = Modifier.weight(1f),
                    )
                    RowActionButtons(
                        onSave = { state.zsetSave(row) },
                        onDelete = { state.zsetDelete(row) },
                        enabled = !state.readonly,
                    )
                }
            }
            if (state.zsetRows.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(text = "暂无成员", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TableHeader(columns: List<String>) {
    val colors = LocalAppColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.panelAlt)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        columns.forEachIndexed { index, column ->
            Text(
                text = column,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(if (columns.size - index <= 1) 0.3f else 1f),
            )
        }
    }
}

@Composable
private fun RowActionButtons(
    onSave: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    if (enabled) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clickable { onSave() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "💾", fontSize = 12.sp)
        }
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🗑", fontSize = 12.sp)
        }
    } else {
        Spacer(modifier = Modifier.width(52.dp))
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.1f GB", mb / 1024.0)
}
