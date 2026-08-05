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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.czh.redis.data.RedisConnection
import com.czh.redis.redis.createRedisClient
import com.czh.redis.ui.components.AppButton
import com.czh.redis.ui.components.AppDialog
import com.czh.redis.ui.components.AppSelect
import com.czh.redis.ui.components.AppTextField
import com.czh.redis.ui.components.ButtonVariant
import kotlinx.coroutines.launch

class ConnectionFormState(initial: RedisConnection) {
    var name by mutableStateOf(initial.name)
    var host by mutableStateOf(initial.host)
    var port by mutableStateOf(initial.port)
    var username by mutableStateOf(initial.username)
    var password by mutableStateOf(initial.password)
    var defaultDb by mutableStateOf(initial.defaultDb)
    var separator by mutableStateOf(initial.separator)
    var color by mutableStateOf(initial.color)
    var group by mutableStateOf(initial.group)
    var ssh by mutableStateOf(initial.ssh)
    var sshHost by mutableStateOf(initial.sshHost)
    var sshPort by mutableStateOf(initial.sshPort)
    var sshUsername by mutableStateOf(initial.sshUsername)
    var sshPassword by mutableStateOf(initial.sshPassword)
    var sshPrivateKey by mutableStateOf(initial.sshPrivateKey)
    var ssl by mutableStateOf(initial.ssl)
    var sslCa by mutableStateOf(initial.sslCa)
    var sslCert by mutableStateOf(initial.sslCert)
    var sslKey by mutableStateOf(initial.sslKey)
    var connectTimeoutMs by mutableStateOf(initial.connectTimeoutMs)
    var readonly by mutableStateOf(initial.readonly)
    var sshExpanded by mutableStateOf(false)
    var sslExpanded by mutableStateOf(false)
    var advancedExpanded by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var testing by mutableStateOf(false)
    var testResult by mutableStateOf<String?>(null)

    fun build(): RedisConnection = RedisConnection(
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        defaultDb = defaultDb,
        separator = separator,
        color = color,
        group = group,
        ssh = ssh,
        sshHost = sshHost,
        sshPort = sshPort,
        sshUsername = sshUsername,
        sshPassword = sshPassword,
        sshPrivateKey = sshPrivateKey,
        ssl = ssl,
        sslCa = sslCa,
        sslCert = sslCert,
        sslKey = sslKey,
        connectTimeoutMs = connectTimeoutMs,
        readonly = readonly,
    )
}

val connectionColorOptions = listOf(
    "" to "默认",
    "#DC382C" to "红",
    "#F59E0B" to "橙",
    "#22A45D" to "绿",
    "#3B82F6" to "蓝",
    "#8B5CF6" to "紫",
    "#EC4899" to "粉",
)

@Composable
fun NewConnectionDialog(vm: AppViewModel) {
    val data = vm.newConnectionDialog ?: return
    val initial = data.editing ?: RedisConnection(host = "127.0.0.1", port = "6379")
    val form = remember(data) { ConnectionFormState(initial) }
    val scope = rememberCoroutineScope()
    val colors = LocalAppColors.current

    fun testConnection() {
        form.testing = true
        form.testResult = null
        val connection = form.build().copy(id = data.editing?.id ?: "")
        scope.launch {
            val start = System.nanoTime()
            val result = runCatching {
                val client = createRedisClient(connection)
                try {
                    client.ping()
                } finally {
                    runCatching { client.close() }
                }
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            form.testing = false
            result.fold(
                onSuccess = { form.testResult = "✓ 连接成功（${elapsed}ms）" },
                onFailure = { form.testResult = "✕ 连接失败：${it.message}" },
            )
        }
    }

    AppDialog(
        visible = true,
        onDismiss = { vm.newConnectionDialog = null },
        title = if (data.editing == null) "新建连接" else "编辑连接",
        width = 560.dp,
        footer = {
            if (form.testResult != null) {
                Text(
                    text = form.testResult!!,
                    color = if (form.testResult!!.startsWith("✓")) colors.success else colors.error,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            AppButton(
                text = "测试连接",
                variant = ButtonVariant.OUTLINE,
                enabled = !form.testing,
                onClick = { testConnection() },
            )
            Spacer(modifier = Modifier.width(10.dp))
            AppButton(
                text = "取消",
                variant = ButtonVariant.OUTLINE,
                onClick = { vm.newConnectionDialog = null },
            )
            Spacer(modifier = Modifier.width(10.dp))
            AppButton(
                text = "确定",
                onClick = {
                    val error = form.build().validate()
                    if (error != null) {
                        form.error = error
                        return@AppButton
                    }
                    if (data.editing != null) {
                        vm.updateConnection(form.build().copy(id = data.editing.id))
                    } else {
                        vm.addConnection(form.build())
                    }
                    vm.newConnectionDialog = null
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (form.error != null) {
                Text(text = form.error!!, color = colors.error, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            FormField(label = "连接名称 *") {
                AppTextField(value = form.name, onValueChange = { form.name = it })
            }

            SectionLabel("基础")
            Row {
                FormField(label = "Host *", modifier = Modifier.weight(2f)) {
                    AppTextField(value = form.host, onValueChange = { form.host = it }, mono = true)
                }
                Spacer(modifier = Modifier.width(10.dp))
                FormField(label = "Port", modifier = Modifier.weight(1f)) {
                    AppTextField(
                        value = form.port,
                        onValueChange = { form.port = it.filter { c -> c.isDigit() } },
                        mono = true,
                    )
                }
            }
            Row {
                FormField(label = "用户名（Redis 6+ ACL）", modifier = Modifier.weight(1f)) {
                    AppTextField(value = form.username, onValueChange = { form.username = it })
                }
                Spacer(modifier = Modifier.width(10.dp))
                FormField(label = "密码", modifier = Modifier.weight(1f)) {
                    AppTextField(value = form.password, onValueChange = { form.password = it }, password = true)
                }
            }
            Row {
                FormField(label = "默认 DB", modifier = Modifier.weight(1f)) {
                    AppSelect(
                        options = (0 until 16).map { it.toString() },
                        selected = form.defaultDb.toString(),
                        onSelect = { form.defaultDb = it.toInt() },
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                FormField(label = "Key 分隔符", modifier = Modifier.weight(1f)) {
                    AppTextField(value = form.separator, onValueChange = { form.separator = it }, mono = true)
                }
                Spacer(modifier = Modifier.width(10.dp))
                FormField(label = "颜色标签", modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        connectionColorOptions.forEach { (hex, label) ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 5.dp)
                                    .size(18.dp)
                                    .background(parseColor(hex, Color.Gray), RoundedCornerShape(50))
                                    .clickable { form.color = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (form.color == hex) {
                                    Text(text = "✓", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            FormField(label = "分组") {
                AppTextField(value = form.group, onValueChange = { form.group = it }, placeholder = "可选，用于连接分组")
            }

            SectionLabel("扩展")
            Accordion(title = "SSH 隧道", expanded = form.sshExpanded, onToggle = { form.sshExpanded = !form.sshExpanded }) {
                SwitchRow(label = "启用 SSH 隧道", checked = form.ssh) { form.ssh = it }
                if (form.ssh) {
                    Row {
                        FormField(label = "SSH Host", modifier = Modifier.weight(2f)) {
                            AppTextField(value = form.sshHost, onValueChange = { form.sshHost = it }, mono = true)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        FormField(label = "SSH Port", modifier = Modifier.weight(1f)) {
                            AppTextField(
                                value = form.sshPort,
                                onValueChange = { form.sshPort = it.filter { c -> c.isDigit() } },
                                mono = true,
                            )
                        }
                    }
                    FormField(label = "SSH 用户名") {
                        AppTextField(value = form.sshUsername, onValueChange = { form.sshUsername = it })
                    }
                    FormField(label = "SSH 密码 / 私钥路径") {
                        AppTextField(value = form.sshPassword, onValueChange = { form.sshPassword = it })
                    }
                }
            }
            Accordion(title = "SSL / TLS", expanded = form.sslExpanded, onToggle = { form.sslExpanded = !form.sslExpanded }) {
                SwitchRow(label = "启用 SSL / TLS", checked = form.ssl) { form.ssl = it }
                if (form.ssl) {
                    FormField(label = "CA 证书路径") {
                        AppTextField(value = form.sslCa, onValueChange = { form.sslCa = it }, mono = true)
                    }
                    FormField(label = "客户端证书路径") {
                        AppTextField(value = form.sslCert, onValueChange = { form.sslCert = it }, mono = true)
                    }
                    FormField(label = "客户端私钥路径") {
                        AppTextField(value = form.sslKey, onValueChange = { form.sslKey = it }, mono = true)
                    }
                }
            }
            Accordion(title = "高级（超时 / 只读模式）", expanded = form.advancedExpanded, onToggle = { form.advancedExpanded = !form.advancedExpanded }) {
                FormField(label = "连接超时（ms）") {
                    AppTextField(
                        value = form.connectTimeoutMs.toString(),
                        onValueChange = { form.connectTimeoutMs = it.toLongOrNull() ?: 0 },
                        mono = true,
                    )
                }
                SwitchRow(label = "只读模式（拦截写命令）", checked = form.readonly) { form.readonly = it }
            }
        }
    }
}

@Composable
private fun FormField(
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

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            color = colors.textSecondary,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.border),
        )
    }
}

@Composable
private fun Accordion(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.panelAlt, RoundedCornerShape(AppDimens.cornerMedium))
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) "▾" else "▸",
            color = colors.textSecondary,
            fontSize = 11.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = colors.text, fontSize = 13.sp)
        Spacer(modifier = Modifier.weight(1f))
    }
    if (expanded) {
        Column(modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) {
            content()
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = colors.text, fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 18.dp)
                .background(if (checked) colors.success else colors.border, RoundedCornerShape(50))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.White, RoundedCornerShape(50)),
            )
        }
    }
}
