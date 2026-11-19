package com.czh.rediscompose.feature.connections.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.czh.rediscompose.design.component.AppSegmented
import com.czh.rediscompose.design.component.AppSegmentedSize
import com.czh.rediscompose.design.component.AppTextField
import com.czh.rediscompose.domain.connection.ConnectionField
import com.czh.rediscompose.domain.connection.TlsVerifyMode
import com.czh.rediscompose.feature.connections.ConnectionDraft
import com.czh.rediscompose.feature.connections.FormField
import com.czh.rediscompose.feature.connections.InfoCallout
import com.czh.rediscompose.feature.connections.InfoTone
import com.czh.rediscompose.feature.connections.ToggleRow
import com.czh.rediscompose.feature.connections.error

@Composable
fun ConnectionTlsTab(
    draft: ConnectionDraft,
    errors: Map<ConnectionField, String>,
    onDraftChange: ((ConnectionDraft) -> ConnectionDraft) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        ToggleRow(
            title = "启用 TLS",
            hint = "使用 TLS 1.2+ 加密客户端与 Redis 之间的流量",
            checked = draft.tlsEnabled,
            onCheckedChange = { value -> onDraftChange { it.copy(tlsEnabled = value) } },
        )
        FormField(
            label = "CA 证书路径",
            error = errors.error(ConnectionField.TLS_CA),
            hint = "支持 PEM，可留空使用系统信任链；支持 ~ 开头路径",
        ) {
            AppTextField(
                value = draft.tlsCaPath,
                onValueChange = { value -> onDraftChange { it.copy(tlsCaPath = value) } },
                placeholder = "/etc/ssl/certs/redis-ca.pem",
                mono = true,
                enabled = draft.tlsEnabled,
                isError = errors.error(ConnectionField.TLS_CA) != null,
            )
        }
        FormField(
            label = "客户端证书路径",
            error = errors.error(ConnectionField.TLS_CERT),
            hint = "客户端证书与私钥必须同时提供",
        ) {
            AppTextField(
                value = draft.tlsClientCertPath,
                onValueChange = { value -> onDraftChange { it.copy(tlsClientCertPath = value) } },
                placeholder = "~/.redis-compose/certs/client.pem",
                mono = true,
                enabled = draft.tlsEnabled,
                isError = errors.error(ConnectionField.TLS_CERT) != null,
            )
        }
        FormField(
            label = "客户端私钥路径",
            error = errors.error(ConnectionField.TLS_KEY),
            hint = "支持未加密的 PKCS#8 或 RSA PEM；加密私钥暂不支持",
        ) {
            AppTextField(
                value = draft.tlsClientKeyPath,
                onValueChange = { value -> onDraftChange { it.copy(tlsClientKeyPath = value) } },
                placeholder = "~/.redis-compose/certs/client-key.pem",
                mono = true,
                enabled = draft.tlsEnabled,
                isError = errors.error(ConnectionField.TLS_KEY) != null,
            )
        }
        FormField(label = "校验模式") {
            AppSegmented(
                options = TlsVerifyMode.entries,
                selected = draft.tlsVerifyMode,
                onSelect = { value -> onDraftChange { it.copy(tlsVerifyMode = value) } },
                size = AppSegmentedSize.Small,
                enabled = draft.tlsEnabled,
                label =
                    {
                        when (it) {
                            TlsVerifyMode.STRICT -> "严格校验"
                            TlsVerifyMode.ALLOW_SELF_SIGNED -> "允许自签名"
                            TlsVerifyMode.NONE -> "不校验"
                        }
                    },
            )
        }
        if (draft.tlsEnabled && draft.tlsVerifyMode != TlsVerifyMode.STRICT) {
            InfoCallout(
                text =
                    if (draft.tlsVerifyMode == TlsVerifyMode.NONE) {
                        "不校验证书仅用于本地开发；生产环境请使用严格校验。"
                    } else {
                        "允许自签名证书会跳过证书链校验，请确认目标服务器可信。"
                    },
                tone = InfoTone.Warning,
            )
        }
    }
}
