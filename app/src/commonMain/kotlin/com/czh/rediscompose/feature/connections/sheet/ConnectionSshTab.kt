package com.czh.rediscompose.feature.connections.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.design.theme.LocalAppDimens
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.SshAuthMethod
import com.czh.rediscompose.feature.connections.ConnectionDraft
import com.czh.rediscompose.feature.connections.FormField
import com.czh.rediscompose.feature.connections.InfoCallout
import com.czh.rediscompose.feature.connections.InfoTone
import com.czh.rediscompose.feature.connections.PasswordField
import com.czh.rediscompose.feature.connections.ToggleRow
import com.czh.rediscompose.feature.connections.error

@Composable
fun ConnectionSshTab(
    draft: ConnectionDraft,
    errors: Map<ConnectionField, String>,
    onDraftChange: ((ConnectionDraft) -> ConnectionDraft) -> Unit,
) {
    val dimens = LocalAppDimens.current
    Column(Modifier.fillMaxWidth()) {
        ToggleRow(
            title = "启用 SSH 隧道",
            hint = "通过跳板机安全访问内网 Redis",
            checked = draft.sshEnabled,
            onCheckedChange = { value -> onDraftChange { it.copy(sshEnabled = value) } },
        )
        if (draft.sshEnabled) {
            InfoCallout(
                text = "v1 使用 StrictHostKeyChecking=no，不校验 known_hosts；请只在可信网络中使用 SSH 隧道。",
                tone = InfoTone.Warning,
                modifier = Modifier.padding(bottom = dimens.spaceMd),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSm)) {
            Box(Modifier.weight(1f)) {
                FormField(label = "SSH Host", required = draft.sshEnabled, error = errors.error(ConnectionField.SSH_HOST)) {
                    AppTextField(
                        value = draft.sshHost,
                        onValueChange = { value -> onDraftChange { it.copy(sshHost = value) } },
                        placeholder = "bastion.example.com",
                        mono = true,
                        enabled = draft.sshEnabled,
                        isError = errors.error(ConnectionField.SSH_HOST) != null,
                    )
                }
            }
            Box(Modifier.width(100.dp)) {
                FormField(label = "Port", required = draft.sshEnabled, error = errors.error(ConnectionField.SSH_PORT)) {
                    AppTextField(
                        value = draft.sshPort,
                        onValueChange = { value -> onDraftChange { it.copy(sshPort = value) } },
                        placeholder = "22",
                        mono = true,
                        enabled = draft.sshEnabled,
                        isError = errors.error(ConnectionField.SSH_PORT) != null,
                    )
                }
            }
        }
        FormField(label = "用户名", required = draft.sshEnabled, error = errors.error(ConnectionField.SSH_USER)) {
            AppTextField(
                value = draft.sshUsername,
                onValueChange = { value -> onDraftChange { it.copy(sshUsername = value) } },
                placeholder = "deploy",
                enabled = draft.sshEnabled,
                isError = errors.error(ConnectionField.SSH_USER) != null,
            )
        }
        FormField(label = "认证方式") {
            AppSegmented(
                options = SshAuthMethod.entries,
                selected = draft.sshAuthMethod,
                onSelect = { value -> onDraftChange { it.copy(sshAuthMethod = value) } },
                size = AppSegmentedSize.Small,
                enabled = draft.sshEnabled,
                label = { if (it == SshAuthMethod.PRIVATE_KEY) "私钥" else "密码" },
            )
        }
        if (draft.sshAuthMethod == SshAuthMethod.PRIVATE_KEY) {
            FormField(
                label = "私钥路径",
                required = draft.sshEnabled,
                error = errors.error(ConnectionField.SSH_KEY),
                hint = "支持 ~ 开头路径；加密私钥可填写下方口令",
            ) {
                AppTextField(
                    value = draft.sshPrivateKeyPath,
                    onValueChange = { value -> onDraftChange { it.copy(sshPrivateKeyPath = value) } },
                    placeholder = "~/.ssh/id_ed25519",
                    mono = true,
                    enabled = draft.sshEnabled,
                    isError = errors.error(ConnectionField.SSH_KEY) != null,
                )
            }
            FormField(label = "私钥口令（可选）", hint = "未加密私钥可留空") {
                PasswordField(
                    value = draft.sshPassword,
                    onValueChange = { value -> onDraftChange { it.copy(sshPassword = value) } },
                    placeholder = "passphrase",
                    enabled = draft.sshEnabled,
                )
            }
        } else {
            FormField(label = "SSH 密码", required = draft.sshEnabled, error = errors.error(ConnectionField.SSH_PASSWORD)) {
                PasswordField(
                    value = draft.sshPassword,
                    onValueChange = { value -> onDraftChange { it.copy(sshPassword = value) } },
                    placeholder = "••••••••",
                    error = errors.error(ConnectionField.SSH_PASSWORD),
                    enabled = draft.sshEnabled,
                )
            }
        }
    }
}
