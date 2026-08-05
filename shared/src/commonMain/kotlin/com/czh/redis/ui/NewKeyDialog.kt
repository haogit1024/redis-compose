package com.czh.redis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czh.redis.redis.RedisType
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppDialog
import com.czh.redis.ui.components.AppMultilineTextField
import com.czh.redis.ui.components.AppSelect
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonVariant
import com.czh.redis.ui.components.TypeBadge
import kotlinx.coroutines.launch

class NewKeyField(val original: String, var value: String)
class NewKeyHashRow(var field: String, var value: String)
class NewKeyZRow(val originalMember: String, var member: String, var score: String)

class NewKeyFormState {
    var type by mutableStateOf(RedisType.STRING)
    var keyName by mutableStateOf("")
    var ttl by mutableStateOf("-1")
    var stringValue by mutableStateOf("")
    var listHead by mutableStateOf(false)
    val hashRows = mutableStateListOf(NewKeyHashRow("", ""))
    val listRows = mutableStateListOf(NewKeyField("", ""))
    val setRows = mutableStateListOf(NewKeyField("", ""))
    val zsetRows = mutableStateListOf(NewKeyZRow("", "", ""))
    var error by mutableStateOf<String?>(null)
}

@Composable
fun NewKeyDialog(vm: AppViewModel, session: Session?) {
    if (vm.newKeyRequest == null) return
    if (session == null) {
        vm.newKeyRequest = null
        return
    }
    val colors = LocalAppColors.current
    val form = remember { NewKeyFormState() }
    val scope = rememberCoroutineScope()
    val client = session.client

    fun create() {
        if (form.keyName.isBlank()) {
            form.error = "Key 名称不能为空"
            return
        }
        if (vm.settings.readonly || session.isReadonly) {
            form.error = "当前处于只读模式"
            return
        }
        scope.launch {
            try {
                when (form.type) {
                    RedisType.STRING -> client.setString(form.keyName, form.stringValue.toByteArray())
                    RedisType.HASH -> form.hashRows.filter { it.value.isNotEmpty() || it.field.isNotEmpty() }
                        .forEach { client.hSet(form.keyName, it.field, it.value.toByteArray()) }
                    RedisType.LIST -> form.listRows.filter { it.value.isNotEmpty() }
                        .forEach { client.listPush(form.keyName, it.value.toByteArray(), form.listHead) }
                    RedisType.SET -> form.setRows.filter { it.value.isNotEmpty() }
                        .forEach { client.setAdd(form.keyName, it.value.toByteArray()) }
                    RedisType.ZSET -> form.zsetRows.filter { it.score.toDoubleOrNull() != null && it.member.isNotEmpty() }
                        .forEach { client.zAdd(form.keyName, it.member.toByteArray(), it.score.toDouble()) }
                    else -> Unit
                }
                val parsedTtl = form.ttl.toLongOrNull() ?: -1
                if (parsedTtl >= 0) client.expire(form.keyName, parsedTtl)
                vm.showToast("已创建 Key ${form.keyName}", ToastType.SUCCESS)
                vm.newKeyRequest = null
                vm.requestRefresh(session.connectionId)
                vm.activeBrowser()?.let { it.selectKey(form.keyName) }
            } catch (e: Exception) {
                vm.showToast("创建失败：${e.message}", ToastType.ERROR)
            }
        }
    }

    AppDialog(
        visible = true,
        onDismiss = { vm.newKeyRequest = null },
        title = "新建 Key",
        width = 560.dp,
        footer = {
            if (form.error != null) {
                Text(text = form.error!!, color = colors.error, fontSize = 12.sp, modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            AppButton(text = "取消", variant = ButtonVariant.OUTLINE, onClick = { vm.newKeyRequest = null })
            Spacer(modifier = Modifier.width(10.dp))
            AppButton(text = "创建", onClick = { create() })
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                TypePickerCard(RedisType.STRING, form.type == RedisType.STRING, { form.type = RedisType.STRING }, Modifier.weight(1f))
                TypePickerCard(RedisType.HASH, form.type == RedisType.HASH, { form.type = RedisType.HASH }, Modifier.weight(1f))
                TypePickerCard(RedisType.LIST, form.type == RedisType.LIST, { form.type = RedisType.LIST }, Modifier.weight(1f))
                TypePickerCard(RedisType.SET, form.type == RedisType.SET, { form.type = RedisType.SET }, Modifier.weight(1f))
                TypePickerCard(RedisType.ZSET, form.type == RedisType.ZSET, { form.type = RedisType.ZSET }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            FormField2("Key *") {
                AppTextField(value = form.keyName, onValueChange = { form.keyName = it }, mono = true, placeholder = "例如 user:1001:profile")
            }
            FormField2("TTL（秒，-1 永久）") {
                AppTextField(
                    value = form.ttl,
                    onValueChange = { form.ttl = it.filter { c -> c.isDigit() || c == '-' } },
                    mono = true,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            when (form.type) {
                RedisType.STRING -> {
                    Text(text = "值（自动识别 JSON）", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    AppMultilineTextField(
                        value = form.stringValue,
                        onValueChange = { form.stringValue = it },
                        mono = true,
                        minHeight = 100.dp,
                    )
                }
                RedisType.HASH -> {
                    KeyRowEditor(label = "初始字段（field / value）") {
                        form.hashRows.forEachIndexed { index, row ->
                            KeyFieldRow(
                                fields = listOf(row.field, row.value),
                                onFieldsChange = { newFields ->
                                    row.field = newFields[0]
                                    row.value = newFields[1]
                                },
                                onDelete = { form.hashRows.removeAt(index) },
                            )
                        }
                        AddRowButton("＋ 添加字段") { form.hashRows.add(NewKeyHashRow("", "")) }
                    }
                }
                RedisType.LIST -> {
                    FormField2("插入位置") {
                        AppSelect(
                            options = listOf("尾部插入（RPUSH）", "头部插入（LPUSH）"),
                            selected = if (form.listHead) "头部插入（LPUSH）" else "尾部插入（RPUSH）",
                            onSelect = { form.listHead = it.startsWith("头部") },
                        )
                    }
                    KeyRowEditor(label = "初始成员") {
                        form.listRows.forEachIndexed { index, row ->
                            KeyFieldRow(
                                fields = listOf(row.value),
                                onFieldsChange = { row.value = it[0] },
                                onDelete = { form.listRows.removeAt(index) },
                            )
                        }
                        AddRowButton("＋ 添加成员") { form.listRows.add(NewKeyField("", "")) }
                    }
                }
                RedisType.SET -> {
                    KeyRowEditor(label = "初始成员（自动去重）") {
                        form.setRows.forEachIndexed { index, row ->
                            KeyFieldRow(
                                fields = listOf(row.value),
                                onFieldsChange = { row.value = it[0] },
                                onDelete = { form.setRows.removeAt(index) },
                            )
                        }
                        AddRowButton("＋ 添加成员") { form.setRows.add(NewKeyField("", "")) }
                    }
                }
                RedisType.ZSET -> {
                    KeyRowEditor(label = "初始成员（member / score）") {
                        form.zsetRows.forEachIndexed { index, row ->
                            KeyFieldRow(
                                fields = listOf(row.member, row.score),
                                onFieldsChange = {
                                    row.member = it[0]
                                    row.score = it[1]
                                },
                                onDelete = { form.zsetRows.removeAt(index) },
                            )
                        }
                        AddRowButton("＋ 添加成员") { form.zsetRows.add(NewKeyZRow("", "", "")) }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun TypePickerCard(
    type: RedisType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .background(if (selected) colors.selectedBg else colors.panelAlt, RoundedCornerShape(AppDimens.cornerLarge))
            .border(1.dp, if (selected) colors.primary else colors.border, RoundedCornerShape(AppDimens.cornerLarge))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TypeBadge(type)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = type.label,
            color = if (selected) colors.primary else colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun KeyRowEditor(
    label: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(4.dp))
    content()
}

@Composable
private fun KeyFieldRow(
    fields: List<String>,
    onFieldsChange: (List<String>) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        fields.forEachIndexed { index, value ->
            AppTextField(
                value = value,
                onValueChange = { newValue ->
                    val newFields = fields.toMutableList()
                    newFields[index] = newValue
                    onFieldsChange(newFields)
                },
                mono = true,
                height = 30.dp,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🗑", fontSize = 12.sp)
        }
    }
}

@Composable
private fun AddRowButton(text: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Text(
        text = text,
        color = colors.primary,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 2.dp).clickable { onClick() },
    )
}

@Composable
private fun FormField2(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        Text(text = label, color = colors.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}
